import { jest } from '@jest/globals';
import { ConflictException, INestApplication, UnauthorizedException } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';
import { createValidationPipe } from '../../common/validation/validation.pipe';
import { JsonBodyAdapter } from '../../common/validation/json-body.adapter';
import { HttpExceptionFilter } from '../../common/filters/http-exception.filter';

describe('Auth endpoints (HTTP)', () => {
  let app: INestApplication<App>;
  const authService = { register: jest.fn<any>(), login: jest.fn<any>() };

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      controllers: [AuthController],
      providers: [{ provide: AuthService, useValue: authService }],
    }).compile();

    app = moduleRef.createNestApplication(new JsonBodyAdapter());
    app.useGlobalPipes(createValidationPipe());
    app.useGlobalFilters(new HttpExceptionFilter());
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  beforeEach(() => jest.clearAllMocks());

  const tokens = { accessToken: 'rs256.jwt', refreshToken: 'opaque-refresh', expiresIn: 900, mfaRequired: false };

  describe('POST /auth/register', () => {
    const body = { username: 'alice', email: 'alice@example.com', password: 'correct-horse-battery' };

    it.each([
      ['username', 'User with username alice already exists'],
      ['email', 'User with email alice@example.com already exists'],
      ['username or email (simultaneous registration)', 'Username or email already exists'],
    ])('returns 409 AUTH-409 when the %s is taken', async (_label, message) => {
      authService.register.mockRejectedValue(new ConflictException(message));

      const response = await request(app.getHttpServer()).post('/auth/register').send(body);

      expect(response.status).toBe(409);
      expect(response.body).toEqual({ errorCode: 'AUTH-409', message });
    });

    it('returns 201 with the token response for a new user', async () => {
      authService.register.mockResolvedValue(tokens);

      const response = await request(app.getHttpServer()).post('/auth/register').send(body);

      expect(response.status).toBe(201);
      expect(response.body).toEqual(tokens);
    });
  });

  describe('POST /auth/login', () => {
    it('returns 200 with accessToken, refreshToken, expiresIn and mfaRequired', async () => {
      authService.login.mockResolvedValue(tokens);

      const response = await request(app.getHttpServer()).post('/auth/login').send({ username: 'demo', password: 'Demo123!' });

      expect(response.status).toBe(200);
      expect(response.body).toEqual(tokens);
    });

    it('gives byte-for-byte identical responses for a wrong username and a wrong password', async () => {
      authService.login.mockRejectedValue(new UnauthorizedException('Invalid credentials'));

      const wrongUser = await request(app.getHttpServer()).post('/auth/login').send({ username: 'ghost', password: 'Demo123!' });
      const wrongPassword = await request(app.getHttpServer()).post('/auth/login').send({ username: 'demo', password: 'Wrong123!' });

      expect(wrongUser.status).toBe(401);
      expect(wrongUser.body).toEqual({ errorCode: 'AUTH-401', message: 'Invalid credentials' });
      expect(wrongPassword.status).toBe(wrongUser.status);
      expect(wrongPassword.text).toBe(wrongUser.text);
    });
  });
});
