import { jest } from '@jest/globals';
import { createPublicKey, generateKeyPairSync, JsonWebKey } from 'crypto';
import { Logger, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as jsonwebtoken from 'jsonwebtoken';
import { readFileSync } from 'fs';
import { join } from 'path';
import { load } from 'js-yaml';
import { AuthService } from '../services/auth.service';
import { AuthRepository } from '../repositories/auth.repository';
import { AccountsServiceClient } from '../services/accounts-service-client';
import { UsersService } from '../../users/services/users.service';
import { TokenService } from '../../tokens/services/token.service';
import { RefreshTokensService } from '../../tokens/services/refresh-tokens.service';
import { NewRefreshToken, RefreshTokensRepository } from '../../tokens/repositories/refresh-tokens.repository';
import { RefreshToken } from '../../tokens/entities/refresh-token.entity';
import { JwksController } from '../../tokens/controllers/jwks.controller';
import { ServiceClientsService } from '../../clients/services/service-clients.service';
import { loadSigningKey } from '../../../config/signing-key';

describe('Refresh and logout end to end (real AuthService, TokenService, RefreshTokensService and JWKS)', () => {
  const pem = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey.export({ type: 'pkcs8', format: 'pem' });
  const key = loadSigningKey(Buffer.from(pem.toString()).toString('base64'), 'auth-key-1');
  const config = { get: (_name: string, fallback: unknown) => fallback } as unknown as ConfigService;
  const invalid = new UnauthorizedException('Invalid refresh token');

  let rows: RefreshToken[];
  const repository: Pick<RefreshTokensRepository, 'create' | 'findByHash' | 'rotate' | 'revokeFamily'> = {
    create: async (row: NewRefreshToken) => {
      const saved = { id: rows.length + 1, revoked_at: null, replaced_by: null, created_at: new Date(), ...row } as RefreshToken;
      rows.push(saved);
      return saved;
    },
    findByHash: async (hash: string) => rows.find((r) => r.token_hash === hash) ?? null,
    rotate: async (currentId: number, next: NewRefreshToken) => {
      const current = rows.find((r) => r.id === currentId);
      if (!current || current.revoked_at) {
        return null;
      }
      const created = await repository.create(next);
      current.revoked_at = new Date();
      current.replaced_by = created.id;
      return created;
    },
    revokeFamily: async (familyId: string) => {
      const active = rows.filter((r) => r.family_id === familyId && !r.revoked_at);
      active.forEach((r) => (r.revoked_at = new Date()));
      return active.length;
    },
  };

  let demo: { id: number; username: string; roles: string[]; account_id: string | null; is_active: boolean };
  const usersService = {
    verifyCredentials: jest.fn(async () => demo),
    getUserById: jest.fn(async () => demo),
    getUserByUsername: jest.fn(async () => demo),
    recordFailedLogin: jest.fn(async () => undefined),
    recordSuccessfulLogin: jest.fn(async () => undefined),
  };
  const authService = new AuthService(
    usersService as unknown as UsersService,
    { recordLogin: jest.fn() } as unknown as AuthRepository,
    new TokenService(key, config),
    new RefreshTokensService(repository as unknown as RefreshTokensRepository, config),
    {} as AccountsServiceClient,
    {} as ServiceClientsService,
  );
  const publicKey = createPublicKey({ key: new JwksController(key).getJwks().keys[0] as unknown as JsonWebKey, format: 'jwk' });
  const verify = (token: string) =>
    jsonwebtoken.verify(token, publicKey, { algorithms: ['RS256'], issuer: 'auth-service', audience: 'trading-platform' }) as Record<string, unknown>;
  const login = () => authService.login({ username: 'demo', password: 'Demo123!' });

  beforeEach(() => {
    rows = [];
    demo = { id: 11, username: 'demo', roles: ['TRADER'], account_id: 'ACC0011', is_active: true };
    jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);
  });

  afterEach(() => jest.restoreAllMocks());

  it('a valid refresh token returns a new access token (verified with the JWKS) and a new refresh token, and the old one stops working', async () => {
    const session = await login();

    const refreshed = await authService.refresh({ refresh_token: session.refreshToken });

    expect(verify(refreshed.accessToken)).toMatchObject({ sub: '11', username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' });
    expect(refreshed).toMatchObject({ expiresIn: 900, mfaRequired: false });
    expect(refreshed.refreshToken).not.toBe(session.refreshToken);
    expect(rows.find((r) => r.id === 1)?.revoked_at).not.toBeNull();
    await expect(authService.refresh({ refresh_token: session.refreshToken })).rejects.toThrow(invalid);
  });

  it('reusing an old refresh token revokes its whole family and returns AUTH-401', async () => {
    const session = await login();
    const second = await authService.refresh({ refresh_token: session.refreshToken });
    const third = await authService.refresh({ refresh_token: second.refreshToken });

    await expect(authService.refresh({ refresh_token: session.refreshToken })).rejects.toThrow(invalid);

    await expect(authService.refresh({ refresh_token: third.refreshToken })).rejects.toThrow(invalid);
    expect(rows.every((r) => r.revoked_at !== null)).toBe(true);
  });

  it('after logout the refresh token is rejected, while another sign-in keeps working', async () => {
    const laptop = await login();
    const phone = await login();

    await authService.logout({ refresh_token: laptop.refreshToken });

    await expect(authService.refresh({ refresh_token: laptop.refreshToken })).rejects.toThrow(invalid);
    await expect(authService.refresh({ refresh_token: phone.refreshToken })).resolves.toBeDefined();
  });

  it('logging in again after logout starts a fresh, working session', async () => {
    const first = await login();
    await authService.logout({ refresh_token: first.refreshToken });

    const again = await login();

    await expect(authService.refresh({ refresh_token: again.refreshToken })).resolves.toBeDefined();
  });

  it('a refresh picks up changes to the user, such as a new role', async () => {
    const session = await login();
    demo = { ...demo, roles: ['ADMIN'] };

    const refreshed = await authService.refresh({ refresh_token: session.refreshToken });

    expect(verify(refreshed.accessToken).roles).toEqual(['ADMIN']);
  });

  it('a deactivated user cannot refresh, and their sign-in is revoked', async () => {
    const session = await login();
    demo = { ...demo, is_active: false };

    await expect(authService.refresh({ refresh_token: session.refreshToken })).rejects.toThrow(invalid);
    expect(rows.every((r) => r.revoked_at !== null)).toBe(true);
  });

  it('only hashes of refresh tokens are ever stored', async () => {
    const session = await login();
    const refreshed = await authService.refresh({ refresh_token: session.refreshToken });

    const stored = JSON.stringify(rows);
    expect(stored).not.toContain(session.refreshToken);
    expect(stored).not.toContain(refreshed.refreshToken);
    expect(rows.every((r) => /^[0-9a-f]{64}$/.test(r.token_hash))).toBe(true);
  });

  describe('OpenAPI spec served at /docs', () => {
    type Operation = { security?: unknown; responses: Record<string, unknown> };
    const spec = load(readFileSync(join(process.cwd(), 'docs', 'auth-service.yaml'), 'utf8')) as {
      paths: Record<string, { post: Operation }>;
      components: { schemas: Record<string, { required: string[]; properties: Record<string, { type: string }> }> };
    };

    it('documents the refresh response as AuthTokens, matching what refresh returns', async () => {
      const session = await login();
      const schema = spec.components.schemas.AuthTokens;

      const refreshed = await authService.refresh({ refresh_token: session.refreshToken });

      expect(Object.keys(refreshed).sort()).toEqual([...schema.required].sort());
      for (const [field, value] of Object.entries(refreshed)) {
        expect(typeof value === 'number' ? 'integer' : typeof value).toBe(schema.properties[field].type);
      }
    });

    it('documents logout as public, returning 204', () => {
      const logout = spec.paths['/auth/logout'].post;

      expect(logout.security).toBeUndefined();
      expect(Object.keys(logout.responses).sort()).toEqual(['204', '422']);
    });
  });
});
