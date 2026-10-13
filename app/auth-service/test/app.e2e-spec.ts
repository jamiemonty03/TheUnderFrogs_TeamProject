import { createHash, createPublicKey, JsonWebKey } from 'crypto';
import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { App } from 'supertest/types';
import { DataSource } from 'typeorm';
import * as jsonwebtoken from 'jsonwebtoken';
import { AppModule } from '../src/app.module';
import { AccountsServiceClient } from '../src/modules/auth/services/accounts-service-client';
import { createValidationPipe } from '../src/common/validation/validation.pipe';
import { JsonBodyAdapter } from '../src/common/validation/json-body.adapter';
import { HttpExceptionFilter } from '../src/common/filters/http-exception.filter';

describe('auth-service (e2e)', () => {
  let app: INestApplication<App>;
  let db: DataSource;

  const user = {
    username: `e2e_${Date.now()}`,
    email: `e2e_${Date.now()}@example.com`,
    password: 'correct-horse-battery',
    full_name: 'E2E Tester',
  };
  const accountsServiceClient = {
    createAccount: async ({ userId }: { userId: number }) => ({ accountId: `E2E${userId}` }),
  };

  // Filled in as the journey progresses.
  let accountId: string;
  let accessToken: string;
  let registerRefresh: string;
  let loginRefresh: string;
  let rotatedRefresh: string;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({ imports: [AppModule] })
      .overrideProvider(AccountsServiceClient)
      .useValue(accountsServiceClient)
      .compile();

    // Same wiring as main.ts.
    app = moduleRef.createNestApplication(new JsonBodyAdapter());
    app.useGlobalPipes(createValidationPipe());
    app.useGlobalFilters(new HttpExceptionFilter());
    await app.init();

    db = app.get(DataSource);
  });

  afterAll(async () => {
    // auth and refresh_tokens rows go with the user (ON DELETE CASCADE).
    await db?.query('DELETE FROM users WHERE username = $1', [user.username]);
    await app?.close();
  });

  const http = () => request(app.getHttpServer());

  it('GET /health is public', async () => {
    await http().get('/health').expect(200);
  });

  describe('register', () => {
    it('creates the user and returns tokens and the new account ID', async () => {
      const res = await http().post('/auth/register').send(user).expect(201);

      expect(res.body).toEqual({
        accessToken: expect.any(String),
        refreshToken: expect.any(String),
        expiresIn: 900,
        mfaRequired: false,
        accountId: expect.stringMatching(/^E2E\d+$/),
      });
      accountId = res.body.accountId;
      registerRefresh = res.body.refreshToken;

      const [row] = await db.query('SELECT password_hash, account_id FROM users WHERE username = $1', [user.username]);
      expect(row.account_id).toBe(accountId);
      expect(row.password_hash).toMatch(/^\$argon2id\$/);
    });

    it('rejects the same username again with 409', async () => {
      const res = await http().post('/auth/register').send(user).expect(409);

      expect(res.body.errorCode).toBe('AUTH-409');
    });

    it('rejects a password that is too short with 422', async () => {
      const res = await http()
        .post('/auth/register')
        .send({ ...user, username: `${user.username}_x`, password: 'short' })
        .expect(422);

      expect(res.body.errorCode).toBe('VAL-422');
    });
  });

  describe('login', () => {
    it('rejects a wrong password with a generic 401', async () => {
      const res = await http().post('/auth/login').send({ username: user.username, password: 'wrong-password-123' }).expect(401);

      expect(res.body).toEqual({ errorCode: 'AUTH-401', message: 'Invalid credentials' });
    });

    it('returns an RS256 access token that the published JWKS verifies', async () => {
      const res = await http().post('/auth/login').send({ username: user.username, password: user.password }).expect(200);
      accessToken = res.body.accessToken;
      loginRefresh = res.body.refreshToken;

      const jwks = await http().get('/.well-known/jwks.json').expect(200);
      const [jwk] = jwks.body.keys as (JsonWebKey & { kid: string })[];
      expect(jwk.kid).toBe('e2e-key');

      const claims = jsonwebtoken.verify(accessToken, createPublicKey({ key: jwk, format: 'jwk' }), {
        algorithms: ['RS256'],
      }) as jsonwebtoken.JwtPayload;
      expect(claims).toMatchObject({ username: user.username, roles: ['TRADER'], accountId });
    });

    it('stores only a hash of the refresh token', async () => {
      const rows = await db.query(
        'SELECT token_hash FROM refresh_tokens rt JOIN users u ON u.id = rt.user_id WHERE u.username = $1',
        [user.username],
      );
      const hashes = rows.map((r: { token_hash: string }) => r.token_hash);

      expect(hashes).toContain(createHash('sha256').update(loginRefresh).digest('hex'));
      expect(hashes).not.toContain(loginRefresh);
    });
  });

  describe('me (JwtAuthGuard)', () => {
    it('returns the signed-in user for a valid bearer token', async () => {
      const res = await http().get('/auth/me').set('Authorization', `Bearer ${accessToken}`).expect(200);

      expect(res.body).toEqual({ userId: expect.any(Number), username: user.username, roles: ['TRADER'], accountId });
    });

    it('rejects a request with no token', async () => {
      await http().get('/auth/me').expect(401);
    });

    it('rejects a tampered token', async () => {
      const [header, payload] = accessToken.split('.');
      const forged = Buffer.from(JSON.stringify({ ...JSON.parse(Buffer.from(payload, 'base64url').toString()), roles: ['ADMIN'] })).toString('base64url');

      await http().get('/auth/me').set('Authorization', `Bearer ${header}.${forged}.${accessToken.split('.')[2]}`).expect(401);
    });
  });

  describe('refresh', () => {
    it('swaps a refresh token for a new pair', async () => {
      const res = await http().post('/auth/refresh').send({ refresh_token: loginRefresh }).expect(200);

      expect(res.headers['cache-control']).toBe('no-store');
      expect(res.body.refreshToken).not.toBe(loginRefresh);
      rotatedRefresh = res.body.refreshToken;

      await http().get('/auth/me').set('Authorization', `Bearer ${res.body.accessToken}`).expect(200);
    });

    it('treats reuse of an old refresh token as theft and ends that whole session', async () => {
      await http().post('/auth/refresh').send({ refresh_token: loginRefresh }).expect(401);

      // The token issued by the legitimate refresh is revoked too.
      await http().post('/auth/refresh').send({ refresh_token: rotatedRefresh }).expect(401);
    });
  });

  describe('logout', () => {
    it('revokes the refresh token so it can no longer be used', async () => {
      await http().post('/auth/logout').send({ refresh_token: registerRefresh }).expect(204);

      await http().post('/auth/refresh').send({ refresh_token: registerRefresh }).expect(401);
    });
  });
});
