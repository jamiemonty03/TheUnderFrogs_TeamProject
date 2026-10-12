import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createHash, randomBytes } from 'crypto';
import { TokensRepository } from './tokens.repository';
import { Token } from './entities/token.entity';

export interface IssuedRefreshToken {
  refreshToken: string;
  expiresAt: Date;
}

export const hashRefreshToken = (refreshToken: string): string =>
  createHash('sha256').update(refreshToken).digest('hex');

@Injectable()
export class RefreshTokensService {
  private readonly ttlSeconds: number;

  constructor(
    private readonly tokensRepository: TokensRepository,
    configService: ConfigService,
  ) {
    this.ttlSeconds = configService.get<number>('JWT_REFRESH_TOKEN_TTL', 604800);
  }

  async issue(userId: number): Promise<IssuedRefreshToken> {
    const refreshToken = randomBytes(32).toString('base64url');
    const expiresAt = new Date(Date.now() + this.ttlSeconds * 1000);
    await this.tokensRepository.create({ user_id: userId, token_hash: hashRefreshToken(refreshToken), expires_at: expiresAt });
    return { refreshToken, expiresAt };
  }

  async findValid(refreshToken: string): Promise<Token | null> {
    if (!refreshToken) {
      return null;
    }
    const token = await this.tokensRepository.findActiveByHash(hashRefreshToken(refreshToken));
    if (!token || token.expires_at.getTime() <= Date.now()) {
      return null;
    }
    return token;
  }

  revoke(id: number): Promise<boolean> {
    return this.tokensRepository.revoke(id);
  }
}
