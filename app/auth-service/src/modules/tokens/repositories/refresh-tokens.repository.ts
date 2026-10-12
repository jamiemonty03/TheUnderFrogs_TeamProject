import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { IsNull, Repository } from 'typeorm';
import { RefreshToken } from '../entities/refresh-token.entity';

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

  async revokeFamily(familyId: string): Promise<number> {
    const result = await this.repository.update({ family_id: familyId, revoked_at: IsNull() }, { revoked_at: new Date() });
    return result.affected ?? 0;
  }
}
