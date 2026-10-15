import { jest } from '@jest/globals';
import {
  CanActivate,
  ExecutionContext,
  INestApplication,
  UnauthorizedException,
} from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';
import { UsersController } from './users.controller';
import { UsersService } from '../services/users.service';
import { RolesGuard } from '../../../common/guards';
import { createValidationPipe } from '../../../common/validation/validation.pipe';
import { JsonBodyAdapter } from '../../../common/validation/json-body.adapter';
import { HttpExceptionFilter } from '../../../common/filters/http-exception.filter';

const USERS: Record<string, string[]> = {
  'Bearer trader': ['TRADER'],
  'Bearer admin': ['ADMIN'],
};

class FakeSignInGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const req = context.switchToHttp().getRequest();
    const roles = USERS[req.headers.authorization ?? ''];
    if (!roles) {
      throw new UnauthorizedException('Missing or invalid token');
    }
    req.user = { sub: '11', username: 'someone', roles };
    return true;
  }
}

describe('/users is for admins only (HTTP)', () => {
  let app: INestApplication<App>;
  const user = {
    id: 12,
    username: 'kermit',
    email: 'kermit@pond.io',
    full_name: 'Kermit',
    roles: ['TRADER'],
    account_id: 'ACC0012',
    is_active: true,
    created_at: new Date(),
    updated_at: new Date(),
  };
  const usersService = {
    createUser: jest.fn<() => Promise<unknown>>(),
    getAllUsers: jest.fn<() => Promise<unknown>>(),
    getUserById: jest.fn<() => Promise<unknown>>(),
    updateUser: jest.fn<() => Promise<unknown>>(),
    deleteUser: jest.fn<() => Promise<unknown>>(),
  };

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [UsersController],
      providers: [
        { provide: UsersService, useValue: usersService },
        { provide: APP_GUARD, useClass: FakeSignInGuard },
        { provide: APP_GUARD, useClass: RolesGuard },
      ],
    }).compile();

    app = moduleRef.createNestApplication(new JsonBodyAdapter());
    app.useGlobalPipes(createValidationPipe());
    app.useGlobalFilters(new HttpExceptionFilter());
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    usersService.createUser.mockResolvedValue(user);
    usersService.getAllUsers.mockResolvedValue([user]);
    usersService.getUserById.mockResolvedValue(user);
    usersService.updateUser.mockResolvedValue({ ...user, roles: ['ADMIN'] });
    usersService.deleteUser.mockResolvedValue(undefined);
  });

  const calls: [string, (server: App) => request.Test, () => jest.Mock][] = [
    [
      'create a user',
      (s) =>
        request(s)
          .post('/users')
          .send({
            username: 'kermit',
            email: 'kermit@pond.io',
            password: 'correct-horse-battery',
          }),
      () => usersService.createUser,
    ],
    [
      'list every user',
      (s) => request(s).get('/users'),
      () => usersService.getAllUsers,
    ],
    [
      'read another user',
      (s) => request(s).get('/users/1'),
      () => usersService.getUserById,
    ],
    [
      'make themselves ADMIN',
      (s) => request(s).put('/users/11').send({ role: 'ADMIN' }),
      () => usersService.updateUser,
    ],
    [
      "change another user's password",
      (s) =>
        request(s).put('/users/1').send({ password: 'correct-horse-battery' }),
      () => usersService.updateUser,
    ],
    [
      'delete a user',
      (s) => request(s).delete('/users/1'),
      () => usersService.deleteUser,
    ],
  ];

  it.each(calls)(
    'a trader cannot %s: 403 AUTH-403 and nothing happens',
    async (_label, call, service) => {
      const res = await call(app.getHttpServer())
        .set('Authorization', 'Bearer trader')
        .expect(403);

      expect(res.body.errorCode).toBe('AUTH-403');
      expect(service()).not.toHaveBeenCalled();
    },
  );

  it.each(calls)(
    'without a token, nobody can %s: 401',
    async (_label, call, service) => {
      await call(app.getHttpServer()).expect(401);

      expect(service()).not.toHaveBeenCalled();
    },
  );

  it.each(calls)('an admin can still %s', async (_label, call, service) => {
    const res = await call(app.getHttpServer()).set(
      'Authorization',
      'Bearer admin',
    );

    expect([200, 201, 204]).toContain(res.status);
    expect(service()).toHaveBeenCalledTimes(1);
  });

  it('never returns password hashes, even to an admin', async () => {
    const res = await request(app.getHttpServer())
      .get('/users/12')
      .set('Authorization', 'Bearer admin')
      .expect(200);

    expect(JSON.stringify(res.body)).not.toContain('password');
  });
});
