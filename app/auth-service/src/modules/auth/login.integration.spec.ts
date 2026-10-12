import { jest } from '@jest/globals';
import { createPublicKey, generateKeyPairSync, JsonWebKey } from 'crypto';
import { readFileSync } from 'fs';
import { join } from 'path';
import { load } from 'js-yaml';
import { ConfigService } from '@nestjs/config';
import * as jsonwebtoken from 'jsonwebtoken';
import { AuthService } from './auth.service';
import { AuthRepository } from './auth.repository';
import { UsersService } from '../users/users.service';
import { TokenService } from '../tokens/token.service';
import { RefreshTokensService, hashRefreshToken } from '../tokens/refresh-tokens.service';
import { TokensRepository } from '../tokens/tokens.repository';
import { JwksController } from '../tokens/jwks.controller';
import { AccountsServiceClient } from './services/accounts-service-client';
import { loadSigningKey } from '../../config/signing-key';

describe('Login end to end (real TokenService, RefreshTokensService and JWKS)', () => {
  const pem = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey.export({ type: 'pkcs8', format: 'pem' });
  const key = loadSigningKey(Buffer.from(pem.toString()).toString('base64'), 'auth-key-1');
  const config = { get: (_name: string, fallback: unknown) => fallback } as unknown as ConfigService;

  const demo = {
    id: 11,
    username: 'demo',
    roles: ['TRADER'],
    account_id: 'ACC0011',
    is_active: true,
    password_hash: '$argon2id$v=19$m=19456,t=2,p=1$c2FsdA$aGFzaA',
    email: 'demo.trader@example.com',
  };
  const storedHashes: string[] = [];
  const usersService = { verifyCredentials: jest.fn<any>(), createUser: jest.fn<any>() };
  const tokensRepository = {
    create: jest.fn(async (row: { token_hash: string }) => {
      storedHashes.push(row.token_hash);
      return row;
    }),
  };
  const authRepository = { recordLogin: jest.fn<any>() };

  const tokenService = new TokenService(key, config);
  const authService = new AuthService(
    usersService as unknown as UsersService,
    authRepository as unknown as AuthRepository,
    tokenService,
    new RefreshTokensService(tokensRepository as unknown as TokensRepository, config),
    {} as AccountsServiceClient,
  );
  const jwks = new JwksController(key).getJwks();

  it('returns an RS256 access token with the agreed claims that the published JWKS verifies', async () => {
    usersService.verifyCredentials.mockResolvedValue(demo);

    const result = await authService.login({ username: 'demo', password: 'Demo123!' });

    const header = jsonwebtoken.decode(result.accessToken, { complete: true })!.header;
    const jwk = jwks.keys.find((k) => k.kid === header.kid)!;
    const claims = jsonwebtoken.verify(result.accessToken, createPublicKey({ key: jwk as unknown as JsonWebKey, format: 'jwk' }), {
      algorithms: ['RS256'],
      issuer: 'auth-service',
      audience: 'trading-platform',
    }) as Record<string, unknown>;

    expect(header).toMatchObject({ alg: 'RS256', kid: 'auth-key-1' });
    expect(claims).toMatchObject({
      sub: '11',
      username: 'demo',
      roles: ['TRADER'],
      accountId: 'ACC0011',
      iss: 'auth-service',
      aud: 'trading-platform',
    });
    expect((claims.exp as number) - (claims.iat as number)).toBe(900);
    expect(Object.keys(claims).sort()).toEqual(['accountId', 'aud', 'exp', 'iat', 'iss', 'roles', 'sub', 'username']);
  });

  it('returns a refresh token whose hash, and only its hash, was stored', async () => {
    usersService.verifyCredentials.mockResolvedValue(demo);

    const { refreshToken, expiresIn, mfaRequired } = await authService.login({ username: 'demo', password: 'Demo123!' });

    expect(storedHashes).toContain(hashRefreshToken(refreshToken));
    expect(storedHashes).not.toContain(refreshToken);
    expect(expiresIn).toBe(900);
    expect(mfaRequired).toBe(false);
  });

  it('matches the AuthTokens schema in the OpenAPI spec served at /docs', async () => {
    usersService.verifyCredentials.mockResolvedValue(demo);
    const spec = load(readFileSync(join(process.cwd(), 'docs', 'auth-service.yaml'), 'utf8')) as {
      components: { schemas: Record<string, { required: string[]; properties: Record<string, { type: string }> }> };
    };
    const schema = spec.components.schemas.AuthTokens;

    const result = await authService.login({ username: 'demo', password: 'Demo123!' });

    expect(Object.keys(result).sort()).toEqual([...schema.required].sort());
    for (const [field, value] of Object.entries(result)) {
      expect(typeof value === 'number' ? 'integer' : typeof value).toBe(schema.properties[field].type);
    }
  });
});
