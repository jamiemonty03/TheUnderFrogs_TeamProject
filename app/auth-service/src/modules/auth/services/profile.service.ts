import { ConflictException, Injectable, NotFoundException } from '@nestjs/common';
import { ProfileResponseDto, UpdateProfileDto } from '../dtos';
import { User } from '../../users/entities/user.entity';
import { UsersRepository } from '../../users/repositories/users.repository';
import { UsersService } from '../../users/services/users.service';

export const EMAIL_TAKEN = 'That email is already used by another account';

export function toProfile(user: User): ProfileResponseDto {
  return { username: user.username, email: user.email, fullName: user.full_name ?? null };
}

@Injectable()
export class ProfileService {
  constructor(
    private readonly usersService: UsersService,
    private readonly usersRepository: UsersRepository,
  ) {}

  async getProfile(userId: number): Promise<ProfileResponseDto> {
    return toProfile(await this.usersService.getUserById(userId));
  }

  async updateProfile(userId: number, dto: UpdateProfileDto): Promise<ProfileResponseDto> {
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
    return toProfile(updated);
  }
}
