import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Auth } from './entities/auth.entity';

@Injectable()
export class AuthRepository {
  constructor(
    @InjectRepository(Auth)
    private readonly repository: Repository<Auth>,
  ) {}

  async create(auth: Partial<Auth>): Promise<Auth> {
    const newAuth = this.repository.create(auth);
    return this.repository.save(newAuth);
  }

  async findById(id: number): Promise<Auth | null> {
    return this.repository.findOne({ where: { id } });
  }

  async findByUserId(userId: number): Promise<Auth | null> {
    return this.repository.findOne({ where: { user_id: userId } });
  }

  async update(id: number, auth: Partial<Auth>): Promise<Auth | null> {
    await this.repository.update(id, auth);
    return this.findById(id);
  }

  async recordLogin(userId: number): Promise<void> {
    const changes = { last_login: new Date().toISOString(), failed_login_attempts: 0 };
    const result = await this.repository.update({ user_id: userId }, changes);
    if (!result.affected) {
      await this.repository.save(this.repository.create({ user_id: userId, ...changes }));
    }
  }

  async delete(id: number): Promise<boolean> {
    const result = await this.repository.delete(id);
    return (result.affected ?? 0) > 0;
  }
}
