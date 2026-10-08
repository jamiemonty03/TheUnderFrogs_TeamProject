import { jest } from '@jest/globals';
import { ConflictException } from '@nestjs/common';
import { QueryFailedError, Repository } from 'typeorm';
import { UsersRepository } from './users.repository';
import { User } from './entities/user.entity';

describe('UsersRepository', () => {
  const typeorm = { update: jest.fn<any>(), create: jest.fn<any>(), save: jest.fn<any>() };
  const repository = new UsersRepository(typeorm as unknown as Repository<User>);

  beforeEach(() => jest.clearAllMocks());

  describe('create', () => {
    const payload = { username: 'alice', email: 'alice@example.com', password_hash: 'h', roles: ['TRADER'] };
    const uniqueViolation = new QueryFailedError('INSERT', [], Object.assign(new Error('duplicate key'), { code: '23505' }));

    it('turns a unique violation from a simultaneous registration into a conflict', async () => {
      typeorm.create.mockReturnValue(payload);
      typeorm.save.mockRejectedValue(uniqueViolation);

      await expect(repository.create(payload)).rejects.toThrow(new ConflictException('Username or email already exists'));
    });

    it('rethrows any other database error', async () => {
      typeorm.create.mockReturnValue(payload);
      typeorm.save.mockRejectedValue(new Error('connection lost'));

      await expect(repository.create(payload)).rejects.toThrow('connection lost');
    });
  });

  describe('updatePasswordHash', () => {
    it('only replaces the hash if it is still the one that was verified', async () => {
      typeorm.update.mockResolvedValue({ affected: 1 });

      await expect(repository.updatePasswordHash(5, '$2a$10$old', '$argon2id$new')).resolves.toBe(true);
      expect(typeorm.update).toHaveBeenCalledWith(
        { id: 5, password_hash: '$2a$10$old' },
        { password_hash: '$argon2id$new' },
      );
    });

    it('returns false when the hash was already changed by someone else', async () => {
      typeorm.update.mockResolvedValue({ affected: 0 });

      await expect(repository.updatePasswordHash(5, '$2a$10$old', '$argon2id$new')).resolves.toBe(false);
    });
  });
});
