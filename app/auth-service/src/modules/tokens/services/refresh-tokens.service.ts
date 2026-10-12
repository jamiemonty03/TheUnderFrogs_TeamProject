import { Injectable, Logger, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createHash, randomBytes, randomUUID } from 'crypto';
import { NewRefreshToken, RefreshTokensRepository } from '../repositories/refresh-tokens.repository';
import { RefreshToken } from '../entities/refresh-token.entity';

export const INVALID_REFRESH_TOKEN = 'Invalid refresh token';

export interface RotatedRefreshToken {
  userId: number;
  refreshToken: string;
  familyId: string;
}

export interface IssuedRefreshToken {
  refreshToken: string;
  familyId: string;
  expiresAt: Date;
}

export const hashRefreshToken = (refreshToken: string): string =>
  createHash('sha256').update(refreshToken).digest('hex');

@Injectable()
export class RefreshTokensService {
  private readonly logger = new Logger(RefreshTokensService.name);
  private readonly ttlSeconds: number;

  constructor(
    private readonly refreshTokensRepository: RefreshTokensRepository,
    configService: ConfigService,
  ) {
    this.ttlSeconds = configService.get<number>('JWT_REFRESH_TOKEN_TTL', 604800);
  }

  async issue(userId: number, familyId: string = randomUUID()): Promise<IssuedRefreshToken> {
    const { refreshToken, row } = this.newToken(userId, familyId);
    await this.refreshTokensRepository.create(row);
    return { refreshToken, familyId, expiresAt: row.expires_at };
  }

  async rotate(presented: string): Promise<RotatedRefreshToken> {
    const current = presented ? await this.refreshTokensRepository.findByHash(hashRefreshToken(presented)) : null;
    if (!current) {
      throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
    }
    if (current.revoked_at) {
      await this.revokeReusedFamily(current.family_id, current.user_id);
    }
    if (current.expires_at.getTime() <= Date.now()) {
      throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
    }

    const { refreshToken, row } = this.newToken(current.user_id, current.family_id);
    if (!(await this.refreshTokensRepository.rotate(current.id, row))) {
      await this.revokeReusedFamily(current.family_id, current.user_id);
    }
    return { userId: current.user_id, refreshToken, familyId: current.family_id };
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

  async revokeFamilyOf(presented: string): Promise<number> {
    const token = presented ? await this.refreshTokensRepository.findByHash(hashRefreshToken(presented)) : null;
    return token ? this.refreshTokensRepository.revokeFamily(token.family_id) : 0;
  }

  private async revokeReusedFamily(familyId: string, userId: number): Promise<never> {
    const revoked = await this.refreshTokensRepository.revokeFamily(familyId);
    this.logger.warn(`Refresh token reuse detected for user ${userId}: revoked ${revoked} token(s) in family ${familyId}`);
    throw new UnauthorizedException(INVALID_REFRESH_TOKEN);
  }

  private newToken(userId: number, familyId: string) {
    const refreshToken = randomBytes(32).toString('base64url');
    const row: NewRefreshToken = {
      user_id: userId,
      token_hash: hashRefreshToken(refreshToken),
      family_id: familyId,
      expires_at: new Date(Date.now() + this.ttlSeconds * 1000),
    };
    return { refreshToken, row };
  }
}
