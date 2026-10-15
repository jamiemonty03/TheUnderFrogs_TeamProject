import { jest } from '@jest/globals';
import { ConflictException, NotFoundException } from '@nestjs/common';
import { EMAIL_TAKEN, ProfileService } from './profile.service';
import { User } from '../../users/entities/user.entity';
import { UsersRepository } from '../../users/repositories/users.repository';
import { UsersService } from '../../users/services/users.service';

const demo = {
  id: 11,
  username: 'demo',
  email: 'demo@toadstock.io',
  full_name: 'Demo Trader',
  account_id: 'ACC0011',
} as User;

describe('ProfileService', () => {
  const usersService = { getUserById: jest.fn<(id: number) => Promise<User>>() };
  const usersRepository = {
    findByEmail: jest.fn<(email: string) => Promise<User | null>>(),
    update: jest.fn<(id: number, changes: Partial<User>) => Promise<User | null>>(),
  };
  const service = new ProfileService(
    usersService as unknown as UsersService,
    usersRepository as unknown as UsersRepository,
  );

  beforeEach(() => {
    jest.clearAllMocks();
    usersService.getUserById.mockResolvedValue(demo);
    usersRepository.findByEmail.mockResolvedValue(null);
    usersRepository.update.mockImplementation(async (_id, changes) => ({ ...demo, ...changes }) as User);
  });

  it('returns the username, email and full name, and nothing else', async () => {
    await expect(service.getProfile(11)).resolves.toEqual({
      username: 'demo',
      email: 'demo@toadstock.io',
      fullName: 'Demo Trader',
    });
  });

  it('returns null for a user who never set a full name', async () => {
    usersService.getUserById.mockResolvedValue({ ...demo, full_name: null } as User);

    await expect(service.getProfile(11)).resolves.toMatchObject({ fullName: null });
  });

  it('changes the full name', async () => {
    const profile = await service.updateProfile(11, { fullName: 'Kermit T. Frog' });

    expect(usersRepository.update).toHaveBeenCalledWith(11, { full_name: 'Kermit T. Frog' });
    expect(profile.fullName).toBe('Kermit T. Frog');
  });

  it('changes the email when nobody else uses it', async () => {
    const profile = await service.updateProfile(11, { email: 'kermit@pond.io' });

    expect(usersRepository.findByEmail).toHaveBeenCalledWith('kermit@pond.io');
    expect(usersRepository.update).toHaveBeenCalledWith(11, { email: 'kermit@pond.io' });
    expect(profile.email).toBe('kermit@pond.io');
  });

  it('changes both at once', async () => {
    await service.updateProfile(11, { fullName: 'Kermit', email: 'kermit@pond.io' });

    expect(usersRepository.update).toHaveBeenCalledWith(11, { full_name: 'Kermit', email: 'kermit@pond.io' });
  });

  it('refuses an email that another account uses', async () => {
    usersRepository.findByEmail.mockResolvedValue({ ...demo, id: 99, email: 'taken@pond.io' } as User);

    await expect(service.updateProfile(11, { email: 'taken@pond.io' })).rejects.toThrow(
      new ConflictException(EMAIL_TAKEN),
    );
    expect(usersRepository.update).not.toHaveBeenCalled();
  });

  it('does not write anything when nothing actually changed', async () => {
    const profile = await service.updateProfile(11, { fullName: 'Demo Trader', email: 'demo@toadstock.io' });

    expect(usersRepository.findByEmail).not.toHaveBeenCalled();
    expect(usersRepository.update).not.toHaveBeenCalled();
    expect(profile).toEqual({ username: 'demo', email: 'demo@toadstock.io', fullName: 'Demo Trader' });
  });

  it('does not write anything for an empty update', async () => {
    await service.updateProfile(11, {});

    expect(usersRepository.update).not.toHaveBeenCalled();
  });

  it('answers 404 when the user no longer exists', async () => {
    usersService.getUserById.mockRejectedValue(new NotFoundException('User with id 11 not found'));

    await expect(service.updateProfile(11, { fullName: 'x' })).rejects.toBeInstanceOf(NotFoundException);
    expect(usersRepository.update).not.toHaveBeenCalled();
  });
});
