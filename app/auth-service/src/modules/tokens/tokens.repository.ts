import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Token } from './entities/token.entity';

@Injectable()
export class TokensRepository {
  constructor(
    @InjectRepository(Token)
    private readonly repository: Repository<Token>,
  ) {}

  async create(token: Pick<Token, 'user_id' | 'token_hash' | 'expires_at'>): Promise<Token> {
    return this.repository.save(this.repository.create(token));
  }

  async findActiveByHash(tokenHash: string): Promise<Token | null> {
    return this.repository.findOne({ where: { token_hash: tokenHash, is_revoked: false } });
  }

  async revoke(id: number): Promise<boolean> {
    const result = await this.repository.update({ id, is_revoked: false }, { is_revoked: true });
    return (result.affected ?? 0) > 0;
  }
}
