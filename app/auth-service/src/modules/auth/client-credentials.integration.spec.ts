import { jest } from '@jest/globals';
import { createPublicKey, generateKeyPairSync, JsonWebKey } from 'crypto';
import { ConfigService } from '@nestjs/config';
import { UnauthorizedException } from '@nestjs/common';
import * as jsonwebtoken from 'jsonwebtoken';
import { AuthService } from './auth.service';
import { AuthRepository } from './auth.repository';
import { AccountsServiceClient } from './services/accounts-service-client';
import { UsersService } from '../users/users.service';
import { PasswordHasher } from '../users/password-hasher.service';
import { TokenService } from '../tokens/token.service';
import { RefreshTokensService } from '../tokens/refresh-tokens.service';
import { JwksController } from '../tokens/jwks.controller';
import { ServiceClientsService } from '../clients/service-clients.service';
import { ServiceClientsRepository } from '../clients/service-clients.repository';
import { ServiceClient } from '../clients/entities/service-client.entity';
import { loadSigningKey } from '../../config/signing-key';

describe('Client credentials end to end (real hasher, TokenService and JWKS)', () => {
  const pem = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey.export({ type: 'pkcs8', format: 'pem' });
  const key = loadSigningKey(Buffer.from(pem.toString()).toString('base64'), 'auth-key-1');
  const config = (overrides: Record<string, unknown> = {}) =>
    ({ get: (name: string, fallback: unknown) => (name in overrides ? overrides[name] : fallback) }) as unknown as ConfigService;
  const hasher = new PasswordHasher();
  const clients: ServiceClient[] = [];
  const repository = { findActiveByClientId: jest.fn(async (id: string) => clients.find((c) => c.client_id === id && c.is_active) ?? null) };

  const authServiceWith = (tokenService: TokenService) =>
    new AuthService(
      {} as UsersService,
      {} as AuthRepository,
      tokenService,
      {} as RefreshTokensService,
      {} as AccountsServiceClient,
      new ServiceClientsService(repository as unknown as ServiceClientsRepository, hasher),
    );
  const authService = authServiceWith(new TokenService(key, config()));
  const request = (client_secret: string, client_id = 'trade-executor') => ({ grant_type: 'client_credentials', client_id, client_secret });

  beforeAll(async () => {
    clients.push(
      { client_id: 'trade-executor', client_secret_hash: await hasher.hash('the-right-client-secret'), roles: ['SERVICE'], is_active: true } as ServiceClient,
      { client_id: 'retired-service', client_secret_hash: await hasher.hash('old-secret'), roles: ['SERVICE'], is_active: false } as ServiceClient,
    );
  });

  it('issues a 5-minute RS256 SERVICE token for the client that the published JWKS verifies', async () => {
    const result = await authService.clientCredentials(request('the-right-client-secret'));

    const jwk = new JwksController(key).getJwks().keys[0];
    const claims = jsonwebtoken.verify(result.access_token, createPublicKey({ key: jwk as unknown as JsonWebKey, format: 'jwk' }), {
      algorithms: ['RS256'],
      issuer: 'auth-service',
      audience: 'trading-platform',
    }) as Record<string, unknown>;

    expect(result).toMatchObject({ token_type: 'Bearer', expires_in: 300 });
    expect(claims).toMatchObject({ sub: 'trade-executor', roles: ['SERVICE'] });
    expect((claims.exp as number) - (claims.iat as number)).toBe(300);
    expect(claims).not.toHaveProperty('username');
    expect(claims).not.toHaveProperty('accountId');
  });

  it('uses JWT_CLIENT_TOKEN_TTL for the lifetime', async () => {
    const result = await authServiceWith(new TokenService(key, config({ JWT_CLIENT_TOKEN_TTL: 120 }))).clientCredentials(
      request('the-right-client-secret'),
    );

    expect(result.expires_in).toBe(120);
  });

  it.each([
    ['a wrong secret', request('a-wrong-client-secret')],
    ['an unknown client', request('the-right-client-secret', 'not-a-client')],
    ['a switched-off client', request('old-secret', 'retired-service')],
  ])('rejects %s with the same generic error', async (_label, body) => {
    await expect(authService.clientCredentials(body)).rejects.toThrow(new UnauthorizedException('Invalid client credentials'));
  });
});
