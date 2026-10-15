import { generateKeyPairSync } from 'crypto';
import { jest } from '@jest/globals';
import { INestApplication } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { Test } from '@nestjs/testing';
import { ThrottlerGuard, ThrottlerModule } from '@nestjs/throttler';
import request from 'supertest';
import { App } from 'supertest/types';
import { AppController } from '../app.controller';
import { AppService } from '../app.service';
import { AuthController } from '../modules/auth/controllers/auth.controller';
import { AuthService } from '../modules/auth/services/auth.service';
import { JwksController } from '../modules/tokens/controllers/jwks.controller';
import { SIGNING_KEY } from '../modules/tokens/services/token.service';
import { throttleConfig } from './throttle.config';

describe('rate limits', () => {
  let app: INestApplication<App>;

  const tokens = {
    accessToken: 'a',
    refreshToken: 'r',
    expiresIn: 900,
    mfaRequired: false,
  };
  const resolves = (value: unknown) =>
    jest.fn<() => Promise<unknown>>().mockResolvedValue(value);
  const authService = {
    login: resolves(tokens),
    register: resolves({ ...tokens, accountId: 'ACC0001' }),
    refresh: resolves(tokens),
    logout: resolves(undefined),
  };

  beforeEach(async () => {
    const { privateKey, publicKey } = generateKeyPairSync('rsa', {
      modulusLength: 2048,
    });
    const moduleRef = await Test.createTestingModule({
      imports: [ThrottlerModule.forRoot(throttleConfig)],
      controllers: [AppController, AuthController, JwksController],
      providers: [
        AppService,
        { provide: AuthService, useValue: authService },
        {
          provide: SIGNING_KEY,
          useValue: { kid: 'test-key', privateKey, publicKey },
        },
        { provide: APP_GUARD, useClass: ThrottlerGuard },
      ],
    }).compile();
    app = moduleRef.createNestApplication();
    await app.init();
  });

  afterEach(() => app.close());

  async function statuses(
    times: number,
    send: () => request.Test,
  ): Promise<number[]> {
    const results: number[] = [];
    for (let i = 0; i < times; i++) {
      results.push((await send()).status);
    }
    return results;
  }

  const server = () => request(app.getHttpServer());
  const allowedBefore429 = (results: number[]) =>
    results.findIndex((status) => status === 429);

  it('never rate-limits the health check, so Docker keeps it healthy', async () => {
    const results = await statuses(150, () => server().get('/health'));

    expect(results.every((status) => status === 200)).toBe(true);
  });

  it('never rate-limits the JWKS endpoint that other services fetch keys from', async () => {
    const results = await statuses(150, () =>
      server().get('/.well-known/jwks.json'),
    );

    expect(results.every((status) => status === 200)).toBe(true);
  });

  it('allows 30 refreshes a minute before answering 429', async () => {
    const results = await statuses(31, () =>
      server().post('/auth/refresh').send({ refresh_token: 'r' }),
    );

    expect(allowedBefore429(results)).toBe(30);
  });

  it.each([
    ['login', '/auth/login', { username: 'demo', password: 'Demo123!' }],
    [
      'register',
      '/auth/register',
      { username: 'demo', email: 'd@e.io', password: 'x'.repeat(12) },
    ],
  ])(
    'keeps %s strict at 5 a minute to slow down password guessing',
    async (_name, url, body) => {
      const results = await statuses(6, () => server().post(url).send(body));

      expect(allowedBefore429(results)).toBe(5);
    },
  );

  it('allows 100 requests a minute on routes without their own limit', async () => {
    const results = await statuses(101, () =>
      server().post('/auth/logout').send({ refresh_token: 'r' }),
    );

    expect(allowedBefore429(results)).toBe(100);
  });

  it('counts each route separately, so busy refreshes do not block logging in', async () => {
    await statuses(31, () =>
      server().post('/auth/refresh').send({ refresh_token: 'r' }),
    );

    const login = await server()
      .post('/auth/login')
      .send({ username: 'demo', password: 'x' });

    expect(login.status).toBe(200);
  });
});
