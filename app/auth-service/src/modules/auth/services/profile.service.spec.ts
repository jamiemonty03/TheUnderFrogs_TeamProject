import { jest } from '@jest/globals';
import {
  ConflictException,
  ForbiddenException,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import {
  EMAIL_TAKEN,
  PASSWORD_CHANGED_ELSEWHERE,
  ProfileService,
  SAME_PASSWORD,
  WRONG_CURRENT_PASSWORD,
} from './profile.service';
import { RefreshTokensService } from '../../tokens/services/refresh-tokens.service';
import { PasswordHasher } from '../../users/services/password-hasher.service';
import { User } from '../../users/entities/user.entity';
import { UsersRepository } from '../../users/repositories/users.repository';
import { UsersService } from '../../users/services/users.service';

const demo = {
  id: 11,
  username: 'demo',
  email: 'demo@toadstock.io',
  full_name: 'Demo Trader',
  account_id: 'ACC0011',
  password_hash: '$argon2id$current',
} as User;

describe('ProfileService', () => {
  const usersService = {
    getUserById: jest.fn<(id: number) => Promise<User>>(),
  };
  const usersRepository = {
    findByEmail: jest.fn<(email: string) => Promise<User | null>>(),
    update:
      jest.fn<(id: number, changes: Partial<User>) => Promise<User | null>>(),
    updatePasswordHash:
      jest.fn<
        (id: number, currentHash: string, newHash: string) => Promise<boolean>
      >(),
  };
  const passwordHasher = {
    verify: jest.fn<(plain: string, hash: string) => Promise<boolean>>(),
    hash: jest.fn<(plain: string) => Promise<string>>(),
  };
  const refreshTokensService = {
    revokeOtherSessions:
      jest.fn<(userId: number, presented?: string) => Promise<number>>(),
  };
  const service = new ProfileService(
    usersService as unknown as UsersService,
    usersRepository as unknown as UsersRepository,
    passwordHasher as unknown as PasswordHasher,
    refreshTokensService as unknown as RefreshTokensService,
  );

  beforeEach(() => {
    jest.clearAllMocks();
    usersService.getUserById.mockResolvedValue(demo);
    usersRepository.findByEmail.mockResolvedValue(null);
    usersRepository.update.mockImplementation(
      async (_id, changes) => ({ ...demo, ...changes }) as User,
    );
    usersRepository.updatePasswordHash.mockResolvedValue(true);
    passwordHasher.verify.mockResolvedValue(true);
    passwordHasher.hash.mockResolvedValue('$argon2id$new');
    refreshTokensService.revokeOtherSessions.mockResolvedValue(2);
  });

  it('returns the username, email and full name, and nothing else', async () => {
    await expect(service.getProfile(11)).resolves.toEqual({
      username: 'demo',
      email: 'demo@toadstock.io',
      fullName: 'Demo Trader',
    });
  });

  it('returns null for a user who never set a full name', async () => {
    usersService.getUserById.mockResolvedValue({
      ...demo,
      full_name: null,
    } as User);

    await expect(service.getProfile(11)).resolves.toMatchObject({
      fullName: null,
    });
  });

  it('changes the full name', async () => {
    const profile = await service.updateProfile(11, {
      fullName: 'Kermit T. Frog',
    });

    expect(usersRepository.update).toHaveBeenCalledWith(11, {
      full_name: 'Kermit T. Frog',
    });
    expect(profile.fullName).toBe('Kermit T. Frog');
  });

  it('changes the email when nobody else uses it', async () => {
    const profile = await service.updateProfile(11, {
      email: 'kermit@pond.io',
    });

    expect(usersRepository.findByEmail).toHaveBeenCalledWith('kermit@pond.io');
    expect(usersRepository.update).toHaveBeenCalledWith(11, {
      email: 'kermit@pond.io',
    });
    expect(profile.email).toBe('kermit@pond.io');
  });

  it('changes both at once', async () => {
    await service.updateProfile(11, {
      fullName: 'Kermit',
      email: 'kermit@pond.io',
    });

    expect(usersRepository.update).toHaveBeenCalledWith(11, {
      full_name: 'Kermit',
      email: 'kermit@pond.io',
    });
  });

  it('refuses an email that another account uses', async () => {
    usersRepository.findByEmail.mockResolvedValue({
      ...demo,
      id: 99,
      email: 'taken@pond.io',
    } as User);

    await expect(
      service.updateProfile(11, { email: 'taken@pond.io' }),
    ).rejects.toThrow(new ConflictException(EMAIL_TAKEN));
    expect(usersRepository.update).not.toHaveBeenCalled();
  });

  it('does not write anything when nothing actually changed', async () => {
    const profile = await service.updateProfile(11, {
      fullName: 'Demo Trader',
      email: 'demo@toadstock.io',
    });

    expect(usersRepository.findByEmail).not.toHaveBeenCalled();
    expect(usersRepository.update).not.toHaveBeenCalled();
    expect(profile).toEqual({
      username: 'demo',
      email: 'demo@toadstock.io',
      fullName: 'Demo Trader',
    });
  });

  it('does not write anything for an empty update', async () => {
    await service.updateProfile(11, {});

    expect(usersRepository.update).not.toHaveBeenCalled();
  });

  it('answers 404 when the user no longer exists', async () => {
    usersService.getUserById.mockRejectedValue(
      new NotFoundException('User with id 11 not found'),
    );

    await expect(
      service.updateProfile(11, { fullName: 'x' }),
    ).rejects.toBeInstanceOf(NotFoundException);
    expect(usersRepository.update).not.toHaveBeenCalled();
  });

  describe('changePassword', () => {
    const change = {
      currentPassword: 'Demo123!',
      newPassword: 'lily-pad-leap-2026',
      refreshToken: 'this-browser',
    };

    it('checks the current password, saves the new hash only if the old one is unchanged, and signs out other sessions', async () => {
      await service.changePassword(11, change);

      expect(passwordHasher.verify).toHaveBeenCalledWith(
        'Demo123!',
        '$argon2id$current',
      );
      expect(passwordHasher.hash).toHaveBeenCalledWith('lily-pad-leap-2026');
      expect(usersRepository.updatePasswordHash).toHaveBeenCalledWith(
        11,
        '$argon2id$current',
        '$argon2id$new',
      );
      expect(refreshTokensService.revokeOtherSessions).toHaveBeenCalledWith(
        11,
        'this-browser',
      );
    });

    it('refuses a wrong current password with 403 and changes nothing', async () => {
      passwordHasher.verify.mockResolvedValue(false);

      await expect(service.changePassword(11, change)).rejects.toThrow(
        new ForbiddenException(WRONG_CURRENT_PASSWORD),
      );
      expect(passwordHasher.hash).not.toHaveBeenCalled();
      expect(usersRepository.updatePasswordHash).not.toHaveBeenCalled();
      expect(refreshTokensService.revokeOtherSessions).not.toHaveBeenCalled();
    });

    it('refuses a new password that is the same as the current one', async () => {
      await expect(
        service.changePassword(11, {
          currentPassword: 'lily-pad-leap-2026',
          newPassword: 'lily-pad-leap-2026',
        }),
      ).rejects.toMatchObject({ status: 422, message: SAME_PASSWORD });
      expect(usersRepository.updatePasswordHash).not.toHaveBeenCalled();
    });

    it('answers 409 when the password was changed somewhere else at the same moment, and signs nobody out', async () => {
      usersRepository.updatePasswordHash.mockResolvedValue(false);

      await expect(service.changePassword(11, change)).rejects.toThrow(
        new ConflictException(PASSWORD_CHANGED_ELSEWHERE),
      );
      expect(refreshTokensService.revokeOtherSessions).not.toHaveBeenCalled();
    });

    it('signs out every session when no refresh token is sent', async () => {
      await service.changePassword(11, {
        currentPassword: 'Demo123!',
        newPassword: 'lily-pad-leap-2026',
      });

      expect(refreshTokensService.revokeOtherSessions).toHaveBeenCalledWith(
        11,
        undefined,
      );
    });

    it('never writes either password to the logs', async () => {
      const log = jest
        .spyOn(Logger.prototype, 'log')
        .mockImplementation(() => undefined);

      await service.changePassword(11, change);

      const logged = JSON.stringify(log.mock.calls);
      expect(logged).toContain('user ID 11');
      expect(logged).not.toContain('Demo123!');
      expect(logged).not.toContain('lily-pad-leap-2026');
      log.mockRestore();
    });
  });
});
