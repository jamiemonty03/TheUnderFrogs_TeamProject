import { jest } from '@jest/globals';
import { createHash } from 'crypto';
import { ConfigService } from '@nestjs/config';
import { IsNull, Repository } from 'typeorm';
import { hashRefreshToken, INVALID_REFRESH_TOKEN, RefreshTokensService } from './refresh-tokens.service';
import { Logger, UnauthorizedException } from '@nestjs/common';
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
    rotate: jest.fn(async (currentId: number, next: NewRefreshToken) => {
      const current = rows.find((r) => r.id === currentId);
      if (!current || current.revoked_at) {
        return null;
      }
      const created = { id: rows.length + 1, revoked_at: null, replaced_by: null, created_at: new Date(), ...next } as RefreshToken;
      rows.push(created);
      current.revoked_at = new Date();
      current.replaced_by = created.id;
      return created;
    }),
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

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

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

  describe('rotate', () => {
    const invalid = new UnauthorizedException(INVALID_REFRESH_TOKEN);

    it('issues a new token in the same family and retires the old one, recording what replaced it', async () => {
      const first = await service.issue(5);

      const second = await service.rotate(first.refreshToken);

      expect(second).toMatchObject({ userId: 5, familyId: first.familyId });
      expect(second.refreshToken).not.toBe(first.refreshToken);
      expect(rows[0]).toMatchObject({ revoked_at: expect.any(Date), replaced_by: 2 });
      expect(rows[1]).toMatchObject({ family_id: first.familyId, revoked_at: null, token_hash: hashRefreshToken(second.refreshToken) });
    });

    it('makes the old token stop working while the new one works', async () => {
      const first = await service.issue(5);
      const second = await service.rotate(first.refreshToken);

      expect(await service.findValid(first.refreshToken)).toBeNull();
      expect(await service.findValid(second.refreshToken)).not.toBeNull();
    });

    it('can be chained: each new token can be used once', async () => {
      const first = await service.issue(5);
      const second = await service.rotate(first.refreshToken);
      const third = await service.rotate(second.refreshToken);

      expect(rows.map((r) => [r.id, r.replaced_by, r.revoked_at === null])).toEqual([
        [1, 2, false],
        [2, 3, false],
        [3, null, true],
      ]);
      expect(third.familyId).toBe(first.familyId);
    });

    it('treats an already-used token as theft: revokes the whole family and refuses', async () => {
      jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);
      const first = await service.issue(5);
      const second = await service.rotate(first.refreshToken);

      await expect(service.rotate(first.refreshToken)).rejects.toThrow(invalid);

      expect(await service.findValid(second.refreshToken)).toBeNull();
      expect(rows.every((r) => r.revoked_at !== null)).toBe(true);
    });

    it('does not touch other families when reuse is detected', async () => {
      jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);
      const stolen = await service.issue(5);
      const otherDevice = await service.issue(5);
      await service.rotate(stolen.refreshToken);

      await expect(service.rotate(stolen.refreshToken)).rejects.toThrow(invalid);

      expect(await service.findValid(otherDevice.refreshToken)).not.toBeNull();
    });

    it('logs reuse without logging the token', async () => {
      const warn = jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);
      const first = await service.issue(5);
      await service.rotate(first.refreshToken);

      await expect(service.rotate(first.refreshToken)).rejects.toThrow();

      expect(warn).toHaveBeenCalledWith(expect.stringContaining('reuse detected for user 5'));
      expect(JSON.stringify(warn.mock.calls)).not.toContain(first.refreshToken);
    });

    it('revokes the family when it loses a race with another refresh of the same token', async () => {
      jest.spyOn(Logger.prototype, 'warn').mockImplementation(() => undefined);
      const first = await service.issue(5);
      repository.rotate.mockResolvedValueOnce(null);

      await expect(service.rotate(first.refreshToken)).rejects.toThrow(invalid);

      expect(repository.revokeFamily).toHaveBeenCalledWith(first.familyId);
    });

    it.each([
      ['an unknown token', 'not-a-real-refresh-token'],
      ['an empty token', ''],
    ])('refuses %s', async (_label, token) => {
      await expect(service.rotate(token)).rejects.toThrow(invalid);
      expect(repository.rotate).not.toHaveBeenCalled();
    });

    it('refuses an expired token without rotating it', async () => {
      jest.useFakeTimers({ now: new Date('2026-10-12T10:00:00Z') });
      const first = await service.issue(5);
      jest.setSystemTime(new Date('2026-10-19T10:00:01Z'));

      await expect(service.rotate(first.refreshToken)).rejects.toThrow(invalid);
      expect(repository.rotate).not.toHaveBeenCalled();
    });

    it('gives the new token a fresh 7-day lifetime', async () => {
      jest.useFakeTimers({ now: new Date('2026-10-12T10:00:00Z') });
      const first = await service.issue(5);
      jest.setSystemTime(new Date('2026-10-15T10:00:00Z'));

      await service.rotate(first.refreshToken);

      expect(rows[1].expires_at).toEqual(new Date('2026-10-22T10:00:00Z'));
    });
  });

  describe('revokeFamilyOf', () => {
    it('revokes the whole family of the presented token, including tokens issued after it', async () => {
      const first = await service.issue(5);
      const second = await service.rotate(first.refreshToken);

      expect(await service.revokeFamilyOf(second.refreshToken)).toBe(1);
      expect(rows.every((r) => r.revoked_at !== null)).toBe(true);
      await expect(service.rotate(second.refreshToken)).rejects.toThrow(UnauthorizedException);
    });

    it('works with an already-used token from the chain too', async () => {
      const first = await service.issue(5);
      const second = await service.rotate(first.refreshToken);

      await service.revokeFamilyOf(first.refreshToken);

      expect(await service.findValid(second.refreshToken)).toBeNull();
    });

    it('leaves other sign-ins alone', async () => {
      const laptop = await service.issue(5);
      const phone = await service.issue(5);

      await service.revokeFamilyOf(laptop.refreshToken);

      expect(await service.findValid(phone.refreshToken)).not.toBeNull();
    });

    it.each([
      ['an unknown token', 'not-a-real-refresh-token'],
      ['an empty token', ''],
    ])('does nothing for %s', async (_label, token) => {
      await service.issue(5);

      expect(await service.revokeFamilyOf(token)).toBe(0);
      expect(repository.revokeFamily).not.toHaveBeenCalled();
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
  const manager = {
    create: jest.fn<any>((_entity, row) => row),
    save: jest.fn<any>(async (row) => ({ id: 99, ...row })),
    update: jest.fn<any>(),
  };
  const typeorm = {
    update: jest.fn<any>(),
    findOne: jest.fn<any>(),
    create: jest.fn<any>((row) => row),
    save: jest.fn<any>(async (row) => row),
    manager: { transaction: jest.fn<any>(async (work: (m: typeof manager) => unknown) => work(manager)) },
  };
  const repository = new RefreshTokensRepository(typeorm as unknown as Repository<RefreshToken>);

  beforeEach(() => jest.clearAllMocks());

  it('looks a token up by hash whether or not it is revoked, so reuse can be detected', async () => {
    await repository.findByHash('h'.repeat(64));

    expect(typeorm.findOne).toHaveBeenCalledWith({ where: { token_hash: 'h'.repeat(64) } });
  });

  it('rotates inside one transaction: saves the new token and retires the old one only if it is still active', async () => {
    const next = { user_id: 5, token_hash: 'n'.repeat(64), family_id: 'family-1', expires_at: new Date() };
    manager.update.mockResolvedValue({ affected: 1 });

    const created = await repository.rotate(7, next);

    expect(created).toMatchObject({ id: 99, ...next });
    expect(typeorm.manager.transaction).toHaveBeenCalledTimes(1);
    expect(manager.update).toHaveBeenCalledWith(
      RefreshToken,
      { id: 7, revoked_at: IsNull() },
      { revoked_at: expect.any(Date), replaced_by: 99 },
    );
  });

  it('returns null and rolls back when the old token was already retired by someone else', async () => {
    manager.update.mockResolvedValue({ affected: 0 });

    await expect(
      repository.rotate(7, { user_id: 5, token_hash: 'n'.repeat(64), family_id: 'family-1', expires_at: new Date() }),
    ).resolves.toBeNull();
  });

  it('passes on unexpected database errors', async () => {
    manager.update.mockRejectedValue(new Error('connection lost'));

    await expect(
      repository.rotate(7, { user_id: 5, token_hash: 'n'.repeat(64), family_id: 'family-1', expires_at: new Date() }),
    ).rejects.toThrow('connection lost');
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
