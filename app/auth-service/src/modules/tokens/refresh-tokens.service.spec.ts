import { jest } from '@jest/globals';
import { createHash } from 'crypto';
import { ConfigService } from '@nestjs/config';
import { Repository } from 'typeorm';
import { hashRefreshToken, RefreshTokensService } from './refresh-tokens.service';
import { TokensRepository } from './tokens.repository';
import { Token } from './entities/token.entity';

describe('RefreshTokensService', () => {
  let rows: Token[];
  const repository = {
    create: jest.fn(async (row: Pick<Token, 'user_id' | 'token_hash' | 'expires_at'>) => {
      const saved = { id: rows.length + 1, token_type: 'refresh', is_revoked: false, ...row } as Token;
      rows.push(saved);
      return saved;
    }),
    findActiveByHash: jest.fn(async (hash: string) => rows.find((r) => r.token_hash === hash && !r.is_revoked) ?? null),
    revoke: jest.fn(async (id: number) => {
      const row = rows.find((r) => r.id === id && !r.is_revoked);
      if (row) {
        row.is_revoked = true;
      }
      return !!row;
    }),
  };
  const config = (ttl?: number) =>
    ({ get: (name: string, fallback: unknown) => (name === 'JWT_REFRESH_TOKEN_TTL' && ttl ? ttl : fallback) }) as unknown as ConfigService;
  const service = new RefreshTokensService(repository as unknown as TokensRepository, config());

  beforeEach(() => {
    rows = [];
    jest.clearAllMocks();
  });

  afterEach(() => jest.useRealTimers());

  describe('issue', () => {
    it('returns a 32-byte random URL-safe token', async () => {
      const { refreshToken } = await service.issue(5);

      expect(refreshToken).toMatch(/^[A-Za-z0-9_-]{43}$/);
      expect(Buffer.from(refreshToken, 'base64url')).toHaveLength(32);
    });

    it('stores only the SHA-256 hash, never the token itself', async () => {
      const { refreshToken } = await service.issue(5);

      expect(rows).toHaveLength(1);
      expect(rows[0].token_hash).toBe(createHash('sha256').update(refreshToken).digest('hex'));
      expect(JSON.stringify(rows)).not.toContain(refreshToken);
    });

    it('stores the token against the user', async () => {
      await service.issue(5);

      expect(rows[0].user_id).toBe(5);
    });

    it('expires after 7 days by default', async () => {
      jest.useFakeTimers({ now: new Date('2026-10-07T10:00:00Z') });

      const { expiresAt } = await service.issue(5);

      expect(expiresAt).toEqual(new Date('2026-10-14T10:00:00Z'));
      expect(rows[0].expires_at).toEqual(expiresAt);
    });

    it('uses the configured lifetime', async () => {
      jest.useFakeTimers({ now: new Date('2026-10-07T10:00:00Z') });
      const shortLived = new RefreshTokensService(repository as unknown as TokensRepository, config(3600));

      expect((await shortLived.issue(5)).expiresAt).toEqual(new Date('2026-10-07T11:00:00Z'));
    });

    it('gives a different token every time', async () => {
      const tokens = await Promise.all([1, 2, 3, 4, 5].map(() => service.issue(5)));

      expect(new Set(tokens.map((t) => t.refreshToken)).size).toBe(5);
    });
  });

  describe('findValid', () => {
    it('finds an issued token by its plain value', async () => {
      const { refreshToken } = await service.issue(5);

      expect((await service.findValid(refreshToken))?.user_id).toBe(5);
    });

    it('returns null for an unknown token', async () => {
      await service.issue(5);

      expect(await service.findValid('not-a-real-refresh-token')).toBeNull();
    });

    it('returns null for an empty token without querying', async () => {
      expect(await service.findValid('')).toBeNull();
      expect(repository.findActiveByHash).not.toHaveBeenCalled();
    });

    it('returns null for the stored hash, so a leaked database row cannot be used as a token', async () => {
      await service.issue(5);

      expect(await service.findValid(rows[0].token_hash)).toBeNull();
    });

    it('returns null once the token has expired', async () => {
      jest.useFakeTimers({ now: new Date('2026-10-07T10:00:00Z') });
      const { refreshToken } = await service.issue(5);
      jest.setSystemTime(new Date('2026-10-14T10:00:00Z'));

      expect(await service.findValid(refreshToken)).toBeNull();
    });

    it('returns null for a revoked token', async () => {
      const { refreshToken } = await service.issue(5);
      await service.revoke(rows[0].id);

      expect(await service.findValid(refreshToken)).toBeNull();
    });
  });

  describe('revoke', () => {
    it('returns false when the token was already revoked', async () => {
      await service.issue(5);

      expect(await service.revoke(1)).toBe(true);
      expect(await service.revoke(1)).toBe(false);
    });
  });

  it('hashRefreshToken is deterministic', () => {
    expect(hashRefreshToken('abc')).toBe(hashRefreshToken('abc'));
    expect(hashRefreshToken('abc')).toHaveLength(64);
  });
});

describe('TokensRepository', () => {
  const typeorm = { update: jest.fn(), findOne: jest.fn(), create: jest.fn((row) => row), save: jest.fn(async (row) => row) };
  const repository = new TokensRepository(typeorm as unknown as Repository<Token>);

  beforeEach(() => jest.clearAllMocks());

  it('only looks up tokens that are not revoked', async () => {
    await repository.findActiveByHash('h'.repeat(64));

    expect(typeorm.findOne).toHaveBeenCalledWith({ where: { token_hash: 'h'.repeat(64), is_revoked: false } });
  });

  it('only revokes a token that is still active', async () => {
    typeorm.update.mockResolvedValue({ affected: 0 } as never);

    expect(await repository.revoke(3)).toBe(false);
    expect(typeorm.update).toHaveBeenCalledWith({ id: 3, is_revoked: false }, { is_revoked: true });
  });
});
