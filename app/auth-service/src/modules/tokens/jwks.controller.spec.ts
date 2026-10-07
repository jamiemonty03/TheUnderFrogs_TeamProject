import { createPublicKey, generateKeyPairSync, JsonWebKey } from 'crypto';
import { INestApplication } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { APP_GUARD } from '@nestjs/core';
import { Test } from '@nestjs/testing';
import * as jsonwebtoken from 'jsonwebtoken';
import request from 'supertest';
import { App } from 'supertest/types';
import { loadSigningKey, SigningKey } from '../../config/signing-key';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { Jwk, Jwks, JwksController } from './jwks.controller';
import { SIGNING_KEY, TokenService } from './token.service';

const newKey = (kid: string): SigningKey => {
  const pem = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey.export({ type: 'pkcs8', format: 'pem' });
  return loadSigningKey(Buffer.from(pem.toString()).toString('base64'), kid);
};

const config = { get: (_name: string, fallback: unknown) => fallback } as unknown as ConfigService;

const publicKeyFrom = (jwks: Jwks, kid: string) => {
  const jwk = jwks.keys.find((key) => key.kid === kid);
  if (!jwk) {
    throw new Error(`no key with kid ${kid}`);
  }
  return createPublicKey({ key: jwk as unknown as JsonWebKey, format: 'jwk' });
};

describe('JWKS', () => {
  const key = newKey('auth-key-1');
  const tokens = new TokenService(key, config);
  const alice = { id: 7, username: 'alice', roles: ['TRADER'], account_id: 'ACC0001' };

  describe('JwksController', () => {
    const jwks = new JwksController(key).getJwks();

    it('publishes one RS256 signing key with the configured kid', () => {
      expect(jwks.keys).toHaveLength(1);
      expect(jwks.keys[0]).toMatchObject({ kty: 'RSA', use: 'sig', alg: 'RS256', kid: 'auth-key-1', e: 'AQAB' });
      expect(jwks.keys[0].n.length).toBeGreaterThan(300);
    });

    it('never exposes any part of the private key', () => {
      const published = jwks.keys[0] as Jwk & Record<string, unknown>;

      for (const privateField of ['d', 'p', 'q', 'dp', 'dq', 'qi']) {
        expect(published).not.toHaveProperty(privateField);
      }
      expect(Object.keys(published).sort()).toEqual(['alg', 'e', 'kid', 'kty', 'n', 'use']);
    });

    it('publishes a key that verifies tokens issued by TokenService', () => {
      const { accessToken } = tokens.issue(alice);
      const { kid } = tokens.decode(accessToken)!.header;

      const claims = jsonwebtoken.verify(accessToken, publicKeyFrom(jwks, kid!), {
        algorithms: ['RS256'],
        issuer: 'auth-service',
        audience: 'trading-platform',
      });

      expect(claims).toMatchObject({ sub: '7', username: 'alice', roles: ['TRADER'], accountId: 'ACC0001' });
    });

    it('publishes a key that rejects tokens signed by any other key', () => {
      const otherTokens = new TokenService(newKey('auth-key-1'), config);
      const { accessToken } = otherTokens.issue(alice);

      expect(() =>
        jsonwebtoken.verify(accessToken, publicKeyFrom(jwks, 'auth-key-1'), { algorithms: ['RS256'] }),
      ).toThrow('invalid signature');
    });
  });

  describe('GET /.well-known/jwks.json', () => {
    let app: INestApplication<App>;

    beforeAll(async () => {
      const moduleRef = await Test.createTestingModule({
        controllers: [JwksController],
        providers: [
          { provide: SIGNING_KEY, useValue: key },
          { provide: TokenService, useValue: { verify: () => { throw new Error('no tokens accepted'); } } },
          { provide: APP_GUARD, useClass: JwtAuthGuard },
        ],
      }).compile();
      app = moduleRef.createNestApplication();
      await app.init();
    });

    afterAll(async () => {
      await app.close();
    });

    it('is public: works without a token even with the auth guard switched on', async () => {
      const response = await request(app.getHttpServer()).get('/.well-known/jwks.json');

      expect(response.status).toBe(200);
      expect(response.headers['content-type']).toContain('application/json');
      expect(response.body.keys[0].kid).toBe('auth-key-1');
    });

    it('lets clients cache the keys for 5 minutes', async () => {
      const response = await request(app.getHttpServer()).get('/.well-known/jwks.json');

      expect(response.headers['cache-control']).toBe('public, max-age=300');
    });

    it('returns keys over HTTP that verify an issued token', async () => {
      const { body } = await request(app.getHttpServer()).get('/.well-known/jwks.json');
      const { accessToken } = tokens.issue(alice);

      expect(() =>
        jsonwebtoken.verify(accessToken, publicKeyFrom(body, 'auth-key-1'), { algorithms: ['RS256'] }),
      ).not.toThrow();
    });
  });
});
