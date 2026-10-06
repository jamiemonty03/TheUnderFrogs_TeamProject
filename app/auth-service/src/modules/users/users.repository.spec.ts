import { Repository } from 'typeorm';
import { UsersRepository } from './users.repository';
import { User } from './entities/user.entity';

describe('UsersRepository', () => {
  const typeorm = { update: jest.fn() };
  const repository = new UsersRepository(typeorm as unknown as Repository<User>);

  beforeEach(() => jest.clearAllMocks());

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
