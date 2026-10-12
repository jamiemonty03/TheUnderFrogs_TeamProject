import { jest } from '@jest/globals';
import { Repository } from 'typeorm';
import { AuthRepository } from './auth.repository';
import { Auth } from './entities/auth.entity';

describe('AuthRepository.recordLogin', () => {
  const typeorm = { update: jest.fn<any>(), create: jest.fn<any>((row) => row), save: jest.fn<any>() };
  const repository = new AuthRepository(typeorm as unknown as Repository<Auth>);

  beforeEach(() => jest.clearAllMocks());

  it('updates the auth row by user_id, not by its own id', async () => {
    typeorm.update.mockResolvedValue({ affected: 1 });

    await repository.recordLogin(5);

    expect(typeorm.update).toHaveBeenCalledWith(
      { user_id: 5 },
      expect.objectContaining({ failed_login_attempts: 0, last_login: expect.any(String) }),
    );
    expect(typeorm.save).not.toHaveBeenCalled();
  });

  it('creates the auth row for a user who has never logged in', async () => {
    typeorm.update.mockResolvedValue({ affected: 0 });

    await repository.recordLogin(9);

    expect(typeorm.save).toHaveBeenCalledWith(expect.objectContaining({ user_id: 9, failed_login_attempts: 0 }));
  });
});
