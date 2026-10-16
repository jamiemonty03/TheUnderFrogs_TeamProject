import { jest } from '@jest/globals';
import {
  CanActivate,
  ConflictException,
  ExecutionContext,
  ForbiddenException,
  INestApplication,
  UnauthorizedException,
} from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';
import { ProfileController } from '../controllers/profile.controller';
import {
  EMAIL_TAKEN,
  ProfileService,
  WRONG_CURRENT_PASSWORD,
} from '../services/profile.service';
import { createValidationPipe } from '../../../common/validation/validation.pipe';
import { JsonBodyAdapter } from '../../../common/validation/json-body.adapter';
import { HttpExceptionFilter } from '../../../common/filters/http-exception.filter';

class TokenFromHeaderGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const req = context.switchToHttp().getRequest();
    if (req.headers.authorization !== 'Bearer demo') {
      throw new UnauthorizedException('Missing or invalid token');
    }
    req.user = {
      sub: '11',
      username: 'demo',
      roles: ['TRADER'],
      accountId: 'ACC0011',
    };
    return true;
  }
}

describe('Profile endpoints (HTTP)', () => {
  let app: INestApplication<App>;
  const profile = {
    username: 'demo',
    email: 'demo@toadstock.io',
    fullName: 'Demo Trader',
  };
  const profileService = {
    getProfile: jest.fn<(userId: number) => Promise<unknown>>(),
    updateProfile: jest.fn<(userId: number, dto: object) => Promise<unknown>>(),
    changePassword: jest.fn<(userId: number, dto: object) => Promise<void>>(),
  };

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [ProfileController],
      providers: [
        { provide: ProfileService, useValue: profileService },
        { provide: APP_GUARD, useClass: TokenFromHeaderGuard },
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
    profileService.getProfile.mockResolvedValue(profile);
    profileService.updateProfile.mockResolvedValue(profile);
    profileService.changePassword.mockResolvedValue(undefined);
  });

  const asDemo = (req: request.Test) => req.set('Authorization', 'Bearer demo');

  describe('GET /auth/profile', () => {
    it("returns the signed-in user's profile and tells clients not to cache it", async () => {
      const res = await asDemo(
        request(app.getHttpServer()).get('/auth/profile'),
      ).expect(200);

      expect(res.body).toEqual(profile);
      expect(res.headers['cache-control']).toBe('no-store');
      expect(profileService.getProfile).toHaveBeenCalledWith(11);
    });

    it('needs a token', async () => {
      const res = await request(app.getHttpServer())
        .get('/auth/profile')
        .expect(401);

      expect(res.body.errorCode).toBe('AUTH-401');
      expect(profileService.getProfile).not.toHaveBeenCalled();
    });
  });

  describe('PATCH /auth/profile', () => {
    const patch = (body: object) =>
      asDemo(request(app.getHttpServer()).patch('/auth/profile')).send(body);

    it('updates the user from the token, never one named in the request', async () => {
      await patch({
        fullName: 'Kermit T. Frog',
        email: 'kermit@pond.io',
      }).expect(200);

      expect(profileService.updateProfile).toHaveBeenCalledWith(11, {
        fullName: 'Kermit T. Frog',
        email: 'kermit@pond.io',
      });
    });

    it('trims spaces around the values, like register does', async () => {
      await patch({ fullName: '  Kermit  ', email: ' kermit@pond.io ' }).expect(
        200,
      );

      expect(profileService.updateProfile).toHaveBeenCalledWith(11, {
        fullName: 'Kermit',
        email: 'kermit@pond.io',
      });
    });

    it.each([
      ['an invalid email', { email: 'kermit@' }],
      ['a blank full name', { fullName: '   ' }],
      ['a full name over 255 characters', { fullName: 'x'.repeat(256) }],
      ['a role, which cannot be changed here', { role: 'ADMIN' }],
      ['a username, which cannot be changed', { username: 'admin' }],
      [
        'a password, which has its own endpoint',
        { password: 'correct-horse-battery' },
      ],
      ['another user ID', { id: 1 }],
    ])('rejects %s with 422 VAL-422', async (_label, body) => {
      const res = await patch(body).expect(422);

      expect(res.body.errorCode).toBe('VAL-422');
      expect(profileService.updateProfile).not.toHaveBeenCalled();
    });

    it('answers 409 AUTH-409 when the email belongs to another account', async () => {
      profileService.updateProfile.mockRejectedValue(
        new ConflictException(EMAIL_TAKEN),
      );

      const res = await patch({ email: 'taken@pond.io' }).expect(409);

      expect(res.body).toEqual({ errorCode: 'AUTH-409', message: EMAIL_TAKEN });
    });

    it('needs a token', async () => {
      await request(app.getHttpServer())
        .patch('/auth/profile')
        .send({ fullName: 'x' })
        .expect(401);

      expect(profileService.updateProfile).not.toHaveBeenCalled();
    });
  });

  describe('POST /auth/password', () => {
    const post = (body: object) =>
      asDemo(request(app.getHttpServer()).post('/auth/password')).send(body);
    const valid = {
      currentPassword: 'Demo123!',
      newPassword: 'lily-pad-leap-2026',
      refreshToken: 'this-browser',
    };

    it('changes the password of the user in the token and answers 204 with no body', async () => {
      const res = await post(valid).expect(204);

      expect(res.text).toBe('');
      expect(profileService.changePassword).toHaveBeenCalledWith(11, valid);
    });

    it("accepts an old current password shorter than today's 12-character rule", async () => {
      await post({
        currentPassword: 'short',
        newPassword: 'lily-pad-leap-2026',
      }).expect(204);
    });

    it.each([
      ['a missing current password', { newPassword: 'lily-pad-leap-2026' }],
      [
        'a new password under 12 characters',
        { currentPassword: 'Demo123!', newPassword: 'short' },
      ],
      [
        'a new password over 128 characters',
        { currentPassword: 'Demo123!', newPassword: 'x'.repeat(129) },
      ],
      [
        'a blank new password',
        { currentPassword: 'Demo123!', newPassword: ' '.repeat(12) },
      ],
      ['a user ID, which cannot be chosen', { ...valid, userId: 1 }],
    ])('rejects %s with 422 VAL-422', async (_label, body) => {
      const res = await post(body).expect(422);

      expect(res.body.errorCode).toBe('VAL-422');
      expect(profileService.changePassword).not.toHaveBeenCalled();
    });

    it('answers 403 AUTH-403 for a wrong current password', async () => {
      profileService.changePassword.mockRejectedValue(
        new ForbiddenException(WRONG_CURRENT_PASSWORD),
      );

      const res = await post(valid).expect(403);

      expect(res.body).toEqual({
        errorCode: 'AUTH-403',
        message: WRONG_CURRENT_PASSWORD,
      });
    });

    it('needs a token', async () => {
      await request(app.getHttpServer())
        .post('/auth/password')
        .send(valid)
        .expect(401);

      expect(profileService.changePassword).not.toHaveBeenCalled();
    });
  });
});
