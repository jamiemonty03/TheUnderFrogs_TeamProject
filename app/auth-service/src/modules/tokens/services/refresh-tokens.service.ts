import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createHash, randomBytes, randomUUID } from 'crypto';
import { RefreshTokensRepository } from '../repositories/refresh-tokens.repository';
import { RefreshToken } from '../entities/refresh-token.entity';

export interface IssuedRefreshToken {
  refreshToken: string;
  familyId: string;
  expiresAt: Date;
}

export const hashRefreshToken = (refreshToken: string): string =>
  createHash('sha256').update(refreshToken).digest('hex');

@Injectable()
export class RefreshTokensService {
  private readonly ttlSeconds: number;

  constructor(
    private readonly refreshTokensRepository: RefreshTokensRepository,
    configService: ConfigService,
  ) {
    this.ttlSeconds = configService.get<number>('JWT_REFRESH_TOKEN_TTL', 604800);
  }

  async issue(userId: number, familyId: string = randomUUID()): Promise<IssuedRefreshToken> {
    const refreshToken = randomBytes(32).toString('base64url');
    const expiresAt = new Date(Date.now() + this.ttlSeconds * 1000);
    await this.refreshTokensRepository.create({
      user_id: userId,
      token_hash: hashRefreshToken(refreshToken),
      family_id: familyId,
      expires_at: expiresAt,
    });
    return { refreshToken, familyId, expiresAt };
  }

  async findValid(refreshToken: string): Promise<RefreshToken | null> {
    if (!refreshToken) {
      return null;
    }
    const token = await this.refreshTokensRepository.findByHash(hashRefreshToken(refreshToken));
    if (!token || token.revoked_at || token.expires_at.getTime() <= Date.now()) {
      return null;
    }
    return token;
  }

  revokeFamily(familyId: string): Promise<number> {
    return this.refreshTokensRepository.revokeFamily(familyId);
  }
}
