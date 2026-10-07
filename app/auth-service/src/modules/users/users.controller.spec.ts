import { jest } from '@jest/globals';
import { UsersController } from './users.controller';
import { UsersService } from './users.service';
import { User } from './entities/user.entity';

describe('UsersController', () => {
  const storedUser: User = {
    id: 1,
    username: 'alice',
    email: 'alice@example.com',
    password_hash: '$argon2id$v=19$m=19456,t=2,p=1$c2FsdA$aGFzaA',
    full_name: 'Alice Johnson',
    roles: ['USER'],
    account_id: 'ACC0001',
    is_active: true,
    failed_attempts: 2,
    locked_until: null,
    version: 0,
    created_at: new Date('2026-10-01T00:00:00Z'),
    updated_at: new Date('2026-10-01T00:00:00Z'),
    updated_by: 'SYSTEM',
  };

  const usersService = {
    createUser: jest.fn().mockResolvedValue(storedUser),
    getAllUsers: jest.fn().mockResolvedValue([storedUser]),
    getUserById: jest.fn().mockResolvedValue(storedUser),
    updateUser: jest.fn().mockResolvedValue(storedUser),
  } as unknown as UsersService;

  const controller = new UsersController(usersService);

  const expectSafe = (response: object) => {
    expect(response).not.toHaveProperty('password_hash');
    expect(response).not.toHaveProperty('failed_attempts');
    expect(response).not.toHaveProperty('locked_until');
    expect(JSON.stringify(response)).not.toContain('$argon2id$');
    expect(response).toMatchObject({
      id: 1,
      username: 'alice',
      email: 'alice@example.com',
      full_name: 'Alice Johnson',
      roles: ['USER'],
      account_id: 'ACC0001',
    });
  };

  it('createUser never returns the password hash', async () => {
    expectSafe(
      await controller.createUser({ username: 'alice', email: 'alice@example.com', password: 'secret-password' }),
    );
  });

  it('getAllUsers never returns password hashes', async () => {
    const users = await controller.getAllUsers();
    expect(users).toHaveLength(1);
    users.forEach(expectSafe);
  });

  it('getUserById never returns the password hash', async () => {
    expectSafe(await controller.getUserById('1'));
  });

  it('updateUser never returns the password hash', async () => {
    expectSafe(await controller.updateUser('1', { email: 'alice@example.com' }));
  });
});
