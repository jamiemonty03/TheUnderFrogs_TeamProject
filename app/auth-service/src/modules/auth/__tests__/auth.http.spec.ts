import { jest } from '@jest/globals';
import { ConflictException, INestApplication, ServiceUnavailableException, UnauthorizedException } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';
import { AuthController } from '../controllers/auth.controller';
import { AuthService, REGISTRATION_FAILED } from '../services/auth.service';
import { createValidationPipe } from '../../../common/validation/validation.pipe';
import { JsonBodyAdapter } from '../../../common/validation/json-body.adapter';
import { HttpExceptionFilter } from '../../../common/filters/http-exception.filter';

describe('Auth endpoints (HTTP)', () => {
  let app: INestApplication<App>;
  const authService = { register: jest.fn<any>(), login: jest.fn<any>(), clientCredentials: jest.fn<any>(), refresh: jest.fn<any>(), logout: jest.fn<any>() };

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

    it('returns 503 AUTH-503 when the account could not be created', async () => {
      authService.register.mockRejectedValue(new ServiceUnavailableException(REGISTRATION_FAILED));

      const response = await request(app.getHttpServer()).post('/auth/register').send(body);

      expect(response.status).toBe(503);
      expect(response.body).toEqual({ errorCode: 'AUTH-503', message: REGISTRATION_FAILED });
    });

    it('returns 201 with the tokens and the new account ID', async () => {
      authService.register.mockResolvedValue({ ...tokens, accountId: 'ACC0012' });

      const response = await request(app.getHttpServer()).post('/auth/register').send(body);

      expect(response.status).toBe(201);
      expect(response.body).toEqual({ ...tokens, accountId: 'ACC0012' });
    });
  });

  describe('POST /auth/refresh', () => {
    it('returns 200 with a new token pair and tells clients not to cache it', async () => {
      authService.refresh.mockResolvedValue(tokens);

      const response = await request(app.getHttpServer()).post('/auth/refresh').send({ refresh_token: 'old-refresh' });

      expect(response.status).toBe(200);
      expect(response.body).toEqual(tokens);
      expect(response.headers['cache-control']).toBe('no-store');
      expect(authService.refresh).toHaveBeenCalledWith({ refresh_token: 'old-refresh' });
    });

    it('returns 401 AUTH-401 for an invalid, expired or reused refresh token', async () => {
      authService.refresh.mockRejectedValue(new UnauthorizedException('Invalid refresh token'));

      const response = await request(app.getHttpServer()).post('/auth/refresh').send({ refresh_token: 'reused' });

      expect(response.status).toBe(401);
      expect(response.body).toEqual({ errorCode: 'AUTH-401', message: 'Invalid refresh token' });
    });

    it.each([
      ['a missing refresh_token', {}],
      ['a blank refresh_token', { refresh_token: '   ' }],
    ])('rejects %s with 422 VAL-422', async (_label, body) => {
      const response = await request(app.getHttpServer()).post('/auth/refresh').send(body);

      expect(response.status).toBe(422);
      expect(response.body.errorCode).toBe('VAL-422');
      expect(authService.refresh).not.toHaveBeenCalled();
    });
  });

  describe('POST /auth/logout', () => {
    it('returns 204 with no body and needs no access token', async () => {
      authService.logout.mockResolvedValue(undefined);

      const response = await request(app.getHttpServer()).post('/auth/logout').send({ refresh_token: 'current-refresh' });

      expect(response.status).toBe(204);
      expect(response.text).toBe('');
      expect(authService.logout).toHaveBeenCalledWith({ refresh_token: 'current-refresh' });
    });

    it('rejects a missing refresh_token with 422 VAL-422 and a clear message', async () => {
      const response = await request(app.getHttpServer()).post('/auth/logout').send({});

      expect(response.status).toBe(422);
      expect(response.body).toEqual({ errorCode: 'VAL-422', message: 'refresh_token should not be empty' });
      expect(authService.logout).not.toHaveBeenCalled();
    });
  });

  describe('POST /auth/token', () => {
    const clientToken = { access_token: 'client.jwt', token_type: 'Bearer', expires_in: 300 };
    const credentials = { grant_type: 'client_credentials', client_id: 'trade-executor', client_secret: 'the-secret' };

    it('returns 200 with the OAuth2 token response and tells clients not to cache it', async () => {
      authService.clientCredentials.mockResolvedValue(clientToken);

      const response = await request(app.getHttpServer()).post('/auth/token').send(credentials);

      expect(response.status).toBe(200);
      expect(response.body).toEqual(clientToken);
      expect(response.headers['cache-control']).toBe('no-store');
      expect(authService.clientCredentials).toHaveBeenCalledWith(credentials);
    });

    it('accepts the form-encoded body that OAuth2 clients send', async () => {
      authService.clientCredentials.mockResolvedValue(clientToken);

      const response = await request(app.getHttpServer()).post('/auth/token').type('form').send(credentials);

      expect(response.status).toBe(200);
      expect(authService.clientCredentials).toHaveBeenCalledWith(credentials);
    });

    it('gives identical 401 responses for a wrong secret and an unknown client', async () => {
      authService.clientCredentials.mockRejectedValue(new UnauthorizedException('Invalid client credentials'));

      const wrongSecret = await request(app.getHttpServer()).post('/auth/token').send({ ...credentials, client_secret: 'wrong' });
      const unknownClient = await request(app.getHttpServer()).post('/auth/token').send({ ...credentials, client_id: 'nobody' });

      expect(wrongSecret.status).toBe(401);
      expect(wrongSecret.body).toEqual({ errorCode: 'AUTH-401', message: 'Invalid client credentials' });
      expect(unknownClient.text).toBe(wrongSecret.text);
    });

    it.each([
      ['an unsupported grant type', { ...credentials, grant_type: 'password' }, /grant_type must be client_credentials/],
      ['a missing client secret', { grant_type: 'client_credentials', client_id: 'trade-executor' }, /client_secret/],
      ['a blank client ID', { ...credentials, client_id: '   ' }, /client_id/],
    ])('rejects %s with 422 VAL-422 before checking credentials', async (_label, body, message) => {
      const response = await request(app.getHttpServer()).post('/auth/token').send(body);

      expect(response.status).toBe(422);
      expect(response.body).toEqual({ errorCode: 'VAL-422', message: expect.stringMatching(message) });
      expect(authService.clientCredentials).not.toHaveBeenCalled();
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
