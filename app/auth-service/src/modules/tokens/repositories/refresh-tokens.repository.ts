import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { IsNull, Not, Repository } from 'typeorm';
import { RefreshToken } from '../entities/refresh-token.entity';

class AlreadyRotated extends Error {}

export type NewRefreshToken = Pick<RefreshToken, 'user_id' | 'token_hash' | 'family_id' | 'expires_at'>;

@Injectable()
export class RefreshTokensRepository {
  constructor(
    @InjectRepository(RefreshToken)
    private readonly repository: Repository<RefreshToken>,
  ) {}

  create(token: NewRefreshToken): Promise<RefreshToken> {
    return this.repository.save(this.repository.create(token));
  }

  findByHash(tokenHash: string): Promise<RefreshToken | null> {
    return this.repository.findOne({ where: { token_hash: tokenHash } });
  }

  async countActiveUsers(): Promise<number> {
    const result = await this.repository
      .createQueryBuilder('refresh_token')
      .select('COUNT(DISTINCT refresh_token.user_id)', 'count')
      .where('refresh_token.revoked_at IS NULL')
      .andWhere('refresh_token.expires_at > CURRENT_TIMESTAMP')
      .getRawOne<{ count: string }>();

    return Number(result?.count ?? 0);
  }

  async rotate(currentId: number, next: NewRefreshToken): Promise<RefreshToken | null> {
    try {
      return await this.repository.manager.transaction(async (manager) => {
        const created = await manager.save(manager.create(RefreshToken, next));
        const result = await manager.update(
          RefreshToken,
          { id: currentId, revoked_at: IsNull() },
          { revoked_at: new Date(), replaced_by: created.id },
        );
        if (!result.affected) {
          throw new AlreadyRotated();
        }
        return created;
      });
    } catch (error) {
      if (error instanceof AlreadyRotated) {
        return null;
      }
      throw error;
    }
  }

  async revokeAllForUser(userId: number, keepFamilyId: string | null): Promise<number> {
    const result = await this.repository.update(
      {
        user_id: userId,
        revoked_at: IsNull(),
        ...(keepFamilyId ? { family_id: Not(keepFamilyId) } : {}),
      },
      { revoked_at: new Date() },
    );
    return result.affected ?? 0;
  }

  async revokeFamily(familyId: string): Promise<number> {
    const result = await this.repository.update({ family_id: familyId, revoked_at: IsNull() }, { revoked_at: new Date() });
    return result.affected ?? 0;
  }
}
