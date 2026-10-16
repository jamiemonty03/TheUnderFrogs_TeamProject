import {
  ConflictException,
  ForbiddenException,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import {
  ChangePasswordDto,
  ProfileResponseDto,
  UpdateProfileDto,
} from '../dtos';
import { ValidationException } from '../../../common/exceptions/custom.exceptions';
import { RefreshTokensService } from '../../tokens/services/refresh-tokens.service';
import { AccountsServiceClient } from './accounts-service-client';
import { User } from '../../users/entities/user.entity';
import { UsersRepository } from '../../users/repositories/users.repository';
import { PasswordHasher } from '../../users/services/password-hasher.service';
import { UsersService } from '../../users/services/users.service';

export const EMAIL_TAKEN = 'That email is already used by another account';
export const WRONG_CURRENT_PASSWORD = 'Your current password is incorrect';
export const SAME_PASSWORD =
  'Choose a new password that is different from your current one';
export const PASSWORD_CHANGED_ELSEWHERE =
  'Your password was just changed somewhere else. Try again.';

export function toProfile(user: User): ProfileResponseDto {
  return {
    username: user.username,
    email: user.email,
    fullName: user.full_name ?? null,
  };
}

@Injectable()
export class ProfileService {
  private readonly logger = new Logger(ProfileService.name);

  constructor(
    private readonly usersService: UsersService,
    private readonly usersRepository: UsersRepository,
    private readonly passwordHasher: PasswordHasher,
    private readonly refreshTokensService: RefreshTokensService,
    private readonly accountsServiceClient: AccountsServiceClient,
  ) {}

  async getProfile(userId: number): Promise<ProfileResponseDto> {
    return toProfile(await this.usersService.getUserById(userId));
  }

  async updateProfile(
    userId: number,
    dto: UpdateProfileDto,
  ): Promise<ProfileResponseDto> {
    const user = await this.usersService.getUserById(userId);
    const changes: Partial<User> = {};

    if (dto.email !== undefined && dto.email !== user.email) {
      const owner = await this.usersRepository.findByEmail(dto.email);
      if (owner && owner.id !== userId) {
        throw new ConflictException(EMAIL_TAKEN);
      }
      changes.email = dto.email;
    }
    if (dto.fullName !== undefined && dto.fullName !== user.full_name) {
      changes.full_name = dto.fullName;
    }
    if (Object.keys(changes).length === 0) {
      return toProfile(user);
    }

    const updated = await this.usersRepository.update(userId, changes);
    if (!updated) {
      throw new NotFoundException('User not found');
    }
    if (changes.full_name && updated.account_id) {
      await this.syncHolderName(userId, updated.account_id, changes.full_name);
    }
    return toProfile(updated);
  }

  private async syncHolderName(
    userId: number,
    accountId: string,
    fullName: string,
  ): Promise<void> {
    try {
      await this.accountsServiceClient.updateHolderName(accountId, fullName);
    } catch {
      this.logger.warn(
        `Profile saved for user ID ${userId}, but account ${accountId} still has the old holder name`,
      );
    }
  }

  async changePassword(userId: number, dto: ChangePasswordDto): Promise<void> {
    const user = await this.usersService.getUserById(userId);
    if (
      !(await this.passwordHasher.verify(
        dto.currentPassword,
        user.password_hash,
      ))
    ) {
      throw new ForbiddenException(WRONG_CURRENT_PASSWORD);
    }
    if (dto.newPassword === dto.currentPassword) {
      throw new ValidationException(SAME_PASSWORD);
    }

    const newHash = await this.passwordHasher.hash(dto.newPassword);
    if (
      !(await this.usersRepository.updatePasswordHash(
        userId,
        user.password_hash,
        newHash,
      ))
    ) {
      throw new ConflictException(PASSWORD_CHANGED_ELSEWHERE);
    }

    const signedOut = await this.refreshTokensService.revokeOtherSessions(
      userId,
      dto.refreshToken,
    );
    this.logger.log(
      `Password changed for user ID ${userId}; signed out ${signedOut} other session token(s)`,
    );
  }
}
