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

  async create(token: Partial<Token>): Promise<Token> {
    const newToken = this.repository.create(token);
    return this.repository.save(newToken);
  }

  async findById(id: number): Promise<Token | null> {
    return this.repository.findOne({ where: { id } });
  }

  async findByUserId(userId: number): Promise<Token[]> {
    return this.repository.find({ where: { user_id: userId, is_revoked: false } });
  }

  async findByToken(token: string): Promise<Token | null> {
    return this.repository.findOne({ where: { token, is_revoked: false } });
  }

  async update(id: number, token: Partial<Token>): Promise<Token | null> {
    await this.repository.update(id, token);
    return this.findById(id);
  }

  async revokeToken(id: number): Promise<boolean> {
    const result = await this.repository.update(id, { is_revoked: true });
    return (result.affected ?? 0) > 0;
  }

  async delete(id: number): Promise<boolean> {
    const result = await this.repository.delete(id);
    return (result.affected ?? 0) > 0;
  }
}
