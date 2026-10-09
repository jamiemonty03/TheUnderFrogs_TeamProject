import { jest } from '@jest/globals';
import { createPublicKey, generateKeyPairSync, JsonWebKey } from 'crypto';
import { ConfigService } from '@nestjs/config';
import * as jsonwebtoken from 'jsonwebtoken';
import { AuthService } from './auth.service';
import { AuthRepository } from './auth.repository';
import { UsersService } from '../users/users.service';
import { TokenService } from '../tokens/token.service';
import { RefreshTokensService } from '../tokens/refresh-tokens.service';
import { TokensRepository } from '../tokens/tokens.repository';
import { JwksController } from '../tokens/jwks.controller';
import { AccountsServiceClient } from './services/accounts-service-client';
import { loadSigningKey } from '../../config/signing-key';

describe('Register end to end (real TokenService, RefreshTokensService and JWKS)', () => {
  const pem = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey.export({ type: 'pkcs8', format: 'pem' });
  const key = loadSigningKey(Buffer.from(pem.toString()).toString('base64'), 'auth-key-1');
  const config = { get: (_name: string, fallback: unknown) => fallback } as unknown as ConfigService;

  const created = { id: 26, username: 'zed', full_name: 'Zed Smith', roles: ['TRADER'], account_id: null, is_active: true };
  const usersService = {
    createUser: jest.fn<any>().mockResolvedValue(created),
    linkAccount: jest.fn<any>(async (id: number, accountId: string) => ({ ...created, id, account_id: accountId })),
    deleteUser: jest.fn<any>(),
  };
  const accountsServiceClient = { createAccount: jest.fn<any>().mockResolvedValue({ accountId: 'ACC0012', userId: 26 }) };
  const tokensRepository = { create: jest.fn(async (row: object) => row) };

  const authService = new AuthService(
    usersService as unknown as UsersService,
    { recordLogin: jest.fn() } as unknown as AuthRepository,
    new TokenService(key, config),
    new RefreshTokensService(tokensRepository as unknown as TokensRepository, config),
    accountsServiceClient as unknown as AccountsServiceClient,
  );
  const jwks = new JwksController(key).getJwks();

  it('returns the generated account ID and an RS256 token, verified with the JWKS, that carries the same account ID', async () => {
    const result = await authService.register({
      username: 'zed',
      email: 'zed@example.com',
      password: 'correct-horse-battery',
      full_name: 'Zed Smith',
    });

    const jwk = jwks.keys[0];
    const claims = jsonwebtoken.verify(result.accessToken, createPublicKey({ key: jwk as unknown as JsonWebKey, format: 'jwk' }), {
      algorithms: ['RS256'],
      issuer: 'auth-service',
      audience: 'trading-platform',
    }) as Record<string, unknown>;

    expect(result.accountId).toBe('ACC0012');
    expect(claims).toMatchObject({ sub: '26', username: 'zed', roles: ['TRADER'], accountId: 'ACC0012' });
    expect(accountsServiceClient.createAccount).toHaveBeenCalledWith({ userId: 26, holderName: 'Zed Smith' });
    expect(usersService.deleteUser).not.toHaveBeenCalled();
  });
});
