import { jest } from '@jest/globals';
import { INestApplication, NotFoundException } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';
import { createValidationPipe } from './validation.pipe';
import { JsonBodyAdapter } from './json-body.adapter';
import { UsersController } from '../../modules/users/users.controller';
import { UsersService } from '../../modules/users/users.service';
import { AuthController } from '../../modules/auth/auth.controller';
import { AuthService } from '../../modules/auth/auth.service';

describe('Validation errors (HTTP)', () => {
  let app: INestApplication<App>;

  const storedUser = {
    id: 1,
    username: 'alice',
    email: 'alice@example.com',
    password_hash: '$argon2id$v=19$m=19456,p=1,t=2$c2FsdA$aGFzaA',
    full_name: null,
    roles: ['USER'],
    account_id: null,
    is_active: true,
    failed_attempts: 0,
    locked_until: null,
    version: 0,
    created_at: new Date(),
    updated_at: new Date(),
    updated_by: 'SYSTEM',
  };

  const usersService = {
    createUser: jest.fn().mockResolvedValue(storedUser),
    updateUser: jest.fn().mockResolvedValue(storedUser),
  };
  const authService = {
    register: jest.fn().mockResolvedValue({ access_token: 't', token_type: 'Bearer', expires_in: 3600 }),
    login: jest.fn().mockResolvedValue({ access_token: 't', token_type: 'Bearer', expires_in: 3600 }),
  };

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [UsersController, AuthController],
      providers: [
        { provide: UsersService, useValue: usersService },
        { provide: AuthService, useValue: authService },
      ],
    }).compile();

    app = moduleRef.createNestApplication(new JsonBodyAdapter());
    app.useGlobalPipes(createValidationPipe());
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach(() => jest.clearAllMocks());

  const valid = { username: 'alice', email: 'alice@example.com', password: 'correct-horse-battery' };

  it.each<[string, 'post' | 'put', string, object, RegExp]>([
    ['POST /users with a short password', 'post', '/users', { ...valid, password: 'short' }, /password/],
    ['POST /users with a blank username', 'post', '/users', { ...valid, username: '   ' }, /username/],
    ['POST /users with a bad email', 'post', '/users', { ...valid, email: 'nope' }, /email/],
    ['POST /users with an unknown field', 'post', '/users', { ...valid, password_hash: 'x' }, /password_hash should not exist/],
    ['POST /users with no body', 'post', '/users', {}, /.+/],
    ['PUT /users/1 with a whitespace password', 'put', '/users/1', { password: ' '.repeat(20) }, /password/],
    ['POST /auth/register with a short password', 'post', '/auth/register', { ...valid, password: 'short' }, /password/],
    ['POST /auth/register choosing a role', 'post', '/auth/register', { ...valid, role: 'ADMIN' }, /role should not exist/],
    ['POST /auth/login with a blank password', 'post', '/auth/login', { username: 'demo', password: '   ' }, /password/],
  ])('%s returns 422 VAL-422', async (_label, method, url, body, message) => {
    const response = await request(app.getHttpServer())[method](url).send(body);

    expect(response.status).toBe(422);
    expect(response.body).toEqual({ errorCode: 'VAL-422', message: expect.stringMatching(message) });
    expect(usersService.createUser).not.toHaveBeenCalled();
    expect(usersService.updateUser).not.toHaveBeenCalled();
    expect(authService.register).not.toHaveBeenCalled();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('returns only the first problem as a single message', async () => {
    const response = await request(app.getHttpServer()).post('/users').send({ username: 'a', email: 'x', password: 'y' });

    expect(response.status).toBe(422);
    expect(typeof response.body.message).toBe('string');
  });

  it('lets a valid request through', async () => {
    const response = await request(app.getHttpServer()).post('/users').send(valid);

    expect(response.status).toBe(201);
    expect(usersService.createUser).toHaveBeenCalledWith(expect.objectContaining(valid));
    expect(response.body).not.toHaveProperty('password_hash');
  });

  it('lets an existing seed user with a short password log in', async () => {
    const response = await request(app.getHttpServer()).post('/auth/login').send({ username: 'demo', password: 'Demo123!' });

    expect(response.status).not.toBe(422);
    expect(authService.login).toHaveBeenCalled();
  });

  it('returns 422 VAL-422 for malformed JSON', async () => {
    const response = await request(app.getHttpServer())
      .post('/users')
      .set('Content-Type', 'application/json')
      .send('{"username": "alice",');

    expect(response.status).toBe(422);
    expect(response.body).toEqual({ errorCode: 'VAL-422', message: 'Malformed request body' });
  });

  it('leaves other errors unchanged', async () => {
    usersService.updateUser.mockRejectedValueOnce(new NotFoundException('User with id 9 not found'));

    const response = await request(app.getHttpServer()).put('/users/9').send({ email: 'new@example.com' });

    expect(response.status).toBe(404);
  });
});
