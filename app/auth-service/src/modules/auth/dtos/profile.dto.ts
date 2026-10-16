import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';
import {
  IsEmailAddress,
  IsFullName,
  IsPassword,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
} from '../../users/dtos/validation';

export class ProfileResponseDto {
  @ApiProperty({ example: 'demo' })
  username: string;

  @ApiProperty({ example: 'demo@toadstock.io' })
  email: string;

  @ApiProperty({ example: 'Demo Trader', nullable: true })
  fullName: string | null;
}

export class UpdateProfileDto {
  @ApiProperty({
    description: 'Full name',
    maxLength: 255,
    example: 'Demo Trader',
    required: false,
  })
  @IsOptional()
  @IsFullName()
  fullName?: string;

  @ApiProperty({
    description: 'Email address',
    format: 'email',
    example: 'demo@toadstock.io',
    required: false,
  })
  @IsOptional()
  @IsEmailAddress()
  email?: string;
}

export class ChangePasswordDto {
  @ApiProperty({ description: 'Your current password' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(1024)
  currentPassword: string;

  @ApiProperty({
    description: `New password (${PASSWORD_MIN_LENGTH} to ${PASSWORD_MAX_LENGTH} characters)`,
    minLength: PASSWORD_MIN_LENGTH,
    maxLength: PASSWORD_MAX_LENGTH,
  })
  @IsNotEmpty()
  @IsPassword()
  newPassword: string;

  @ApiProperty({
    description:
      "This browser's refresh token, so this session stays signed in while the others are signed out",
    required: false,
  })
  @IsOptional()
  @IsString()
  @MaxLength(512)
  refreshToken?: string;
}
