import { Injectable, NotFoundException } from '@nestjs/common';
import { TokensRepository } from './tokens.repository';
import { Token } from './entities/token.entity';

@Injectable()
export class TokensService {
  constructor(private readonly tokensRepository: TokensRepository) {}

  async createToken(
    userId: number,
    token: string,
    tokenType: string = 'refresh',
    expiresAt?: Date,
  ): Promise<Token> {
    return this.tokensRepository.create({
      user_id: userId,
      token,
      token_type: tokenType,
      expires_at: expiresAt,
    });
  }

  async getTokenById(id: number): Promise<Token> {
    const token = await this.tokensRepository.findById(id);
    if (!token) {
      throw new NotFoundException(`Token with id ${id} not found`);
    }
    return token;
  }

  async getUserTokens(userId: number): Promise<Token[]> {
    return this.tokensRepository.findByUserId(userId);
  }

  async validateToken(token: string): Promise<Token> {
    const tokenRecord = await this.tokensRepository.findByToken(token);
    if (!tokenRecord) {
      throw new NotFoundException('Token not found');
    }

    if (tokenRecord.expires_at && new Date() > tokenRecord.expires_at) {
      throw new NotFoundException('Token has expired');
    }

    return tokenRecord;
  }

  async revokeToken(id: number): Promise<void> {
    await this.getTokenById(id);
    const success = await this.tokensRepository.revokeToken(id);
    if (!success) {
      throw new NotFoundException(`Failed to revoke token with id ${id}`);
    }
  }

  async deleteToken(id: number): Promise<void> {
    await this.getTokenById(id);
    const success = await this.tokensRepository.delete(id);
    if (!success) {
      throw new NotFoundException(`Failed to delete token with id ${id}`);
    }
  }
}
