import { jest } from '@jest/globals';
import { createHash } from 'crypto';
import { ConfigService } from '@nestjs/config';
import { IsNull, Repository } from 'typeorm';
import { hashRefreshToken, RefreshTokensService } from './refresh-tokens.service';
import { NewRefreshToken, RefreshTokensRepository } from '../repositories/refresh-tokens.repository';
import { RefreshToken } from '../entities/refresh-token.entity';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

describe('RefreshTokensService', () => {
  let rows: RefreshToken[];
  const repository = {
    create: jest.fn(async (row: NewRefreshToken) => {
      const saved = { id: rows.length + 1, revoked_at: null, replaced_by: null, created_at: new Date(), ...row } as RefreshToken;
      rows.push(saved);
      return saved;
    }),
    findByHash: jest.fn(async (hash: string) => rows.find((r) => r.token_hash === hash) ?? null),
    revokeFamily: jest.fn(async (familyId: string) => {
      const active = rows.filter((r) => r.family_id === familyId && !r.revoked_at);
      active.forEach((r) => (r.revoked_at = new Date()));
      return active.length;
    }),
  };
  const config = (ttl?: number) =>
    ({ get: (name: string, fallback: unknown) => (name === 'JWT_REFRESH_TOKEN_TTL' && ttl ? ttl : fallback) }) as unknown as ConfigService;
  const service = new RefreshTokensService(repository as unknown as RefreshTokensRepository, config());

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

      expect(rows[0].token_hash).toBe(createHash('sha256').update(refreshToken).digest('hex'));
      expect(JSON.stringify(rows)).not.toContain(refreshToken);
    });

    it('starts a new family for each login', async () => {
      const first = await service.issue(5);
      const second = await service.issue(5);

      expect(first.familyId).toMatch(UUID);
      expect(second.familyId).not.toBe(first.familyId);
      expect(rows.map((r) => r.family_id)).toEqual([first.familyId, second.familyId]);
    });

    it('can add a token to an existing family', async () => {
      const { familyId } = await service.issue(5);

      await service.issue(5, familyId);

      expect(rows.map((r) => r.family_id)).toEqual([familyId, familyId]);
    });

    it('starts active and expires after 7 days by default', async () => {
      jest.useFakeTimers({ now: new Date('2026-10-12T10:00:00Z') });

      const { expiresAt } = await service.issue(5);

      expect(expiresAt).toEqual(new Date('2026-10-19T10:00:00Z'));
      expect(rows[0]).toMatchObject({ user_id: 5, expires_at: expiresAt, revoked_at: null, replaced_by: null });
    });

    it('uses JWT_REFRESH_TOKEN_TTL', async () => {
      jest.useFakeTimers({ now: new Date('2026-10-12T10:00:00Z') });
      const shortLived = new RefreshTokensService(repository as unknown as RefreshTokensRepository, config(3600));

      expect((await shortLived.issue(5)).expiresAt).toEqual(new Date('2026-10-12T11:00:00Z'));
    });
  });

  describe('findValid', () => {
    it('finds an active token by its plain value', async () => {
      const { refreshToken } = await service.issue(5);

      expect((await service.findValid(refreshToken))?.user_id).toBe(5);
    });

    it.each([
      ['an unknown token', 'not-a-real-refresh-token'],
      ['an empty token', ''],
    ])('returns null for %s', async (_label, token) => {
      await service.issue(5);

      expect(await service.findValid(token)).toBeNull();
    });

    it('returns null for the stored hash, so a leaked database row cannot be used as a token', async () => {
      await service.issue(5);

      expect(await service.findValid(rows[0].token_hash)).toBeNull();
    });

    it('returns null once the token has expired', async () => {
      jest.useFakeTimers({ now: new Date('2026-10-12T10:00:00Z') });
      const { refreshToken } = await service.issue(5);
      jest.setSystemTime(new Date('2026-10-19T10:00:00Z'));

      expect(await service.findValid(refreshToken)).toBeNull();
    });

    it('returns null for a revoked token', async () => {
      const { refreshToken, familyId } = await service.issue(5);
      await service.revokeFamily(familyId);

      expect(await service.findValid(refreshToken)).toBeNull();
    });
  });

  describe('revokeFamily', () => {
    it('revokes every active token in the family and leaves other families alone', async () => {
      const a = await service.issue(5);
      await service.issue(5, a.familyId);
      const other = await service.issue(5);

      expect(await service.revokeFamily(a.familyId)).toBe(2);
      expect(await service.findValid(other.refreshToken)).not.toBeNull();
    });
  });

  it('hashRefreshToken is deterministic', () => {
    expect(hashRefreshToken('abc')).toBe(hashRefreshToken('abc'));
    expect(hashRefreshToken('abc')).toHaveLength(64);
  });
});

describe('RefreshTokensRepository', () => {
  const typeorm = { update: jest.fn<any>(), findOne: jest.fn<any>(), create: jest.fn<any>((row) => row), save: jest.fn<any>(async (row) => row) };
  const repository = new RefreshTokensRepository(typeorm as unknown as Repository<RefreshToken>);

  beforeEach(() => jest.clearAllMocks());

  it('looks a token up by hash whether or not it is revoked, so reuse can be detected', async () => {
    await repository.findByHash('h'.repeat(64));

    expect(typeorm.findOne).toHaveBeenCalledWith({ where: { token_hash: 'h'.repeat(64) } });
  });

  it('revokes only the still-active tokens of one family and reports how many', async () => {
    typeorm.update.mockResolvedValue({ affected: 3 });

    expect(await repository.revokeFamily('family-1')).toBe(3);
    expect(typeorm.update).toHaveBeenCalledWith(
      { family_id: 'family-1', revoked_at: IsNull() },
      { revoked_at: expect.any(Date) },
    );
  });
});
