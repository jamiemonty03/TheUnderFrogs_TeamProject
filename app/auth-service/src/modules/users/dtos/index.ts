import { IsNotEmpty, IsOptional, IsString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';
import {
  DEFAULT_ROLE,
  IsEmailAddress,
  IsFullName,
  IsPassword,
  IsRole,
  IsUsername,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  ROLES,
} from './validation';

export class CreateUserDto {
  @ApiProperty({
    description: 'Unique username for the account',
    minLength: 3,
    maxLength: 50,
    example: 'johndoe',
  })
  @IsNotEmpty()
  @IsUsername()
  username: string;

  @ApiProperty({
    description: 'User email address',
    format: 'email',
    example: 'john@example.com',
  })
  @IsNotEmpty()
  @IsEmailAddress()
  email: string;

  @ApiProperty({
    description: `Account password (${PASSWORD_MIN_LENGTH} to ${PASSWORD_MAX_LENGTH} characters)`,
    minLength: PASSWORD_MIN_LENGTH,
    maxLength: PASSWORD_MAX_LENGTH,
    example: 'correct-horse-battery',
  })
  @IsNotEmpty()
  @IsPassword()
  password: string;

  @ApiProperty({
    description: 'Full name',
    maxLength: 255,
    example: 'John Doe',
    required: false,
  })
  @IsOptional()
  @IsFullName()
  full_name?: string;

  @ApiProperty({
    description: 'User role',
    enum: ROLES,
    default: DEFAULT_ROLE,
    example: DEFAULT_ROLE,
    required: false,
  })
  @IsOptional()
  @IsRole()
  role?: string;
}

export class UpdateUserDto {
  @ApiProperty({
    description: 'Username',
    minLength: 3,
    maxLength: 50,
    example: 'johndoe',
    required: false,
  })
  @IsOptional()
  @IsUsername()
  username?: string;

  @ApiProperty({
    description: 'User email address',
    format: 'email',
    example: 'john@example.com',
    required: false,
  })
  @IsOptional()
  @IsEmailAddress()
  email?: string;

  @ApiProperty({
    description: `Account password (${PASSWORD_MIN_LENGTH} to ${PASSWORD_MAX_LENGTH} characters)`,
    minLength: PASSWORD_MIN_LENGTH,
    maxLength: PASSWORD_MAX_LENGTH,
    example: 'correct-horse-battery',
    required: false,
  })
  @IsOptional()
  @IsPassword()
  password?: string;

  @ApiProperty({
    description: 'Full name',
    maxLength: 255,
    example: 'John Doe',
    required: false,
  })
  @IsOptional()
  @IsFullName()
  full_name?: string;

  @ApiProperty({
    description: 'User role',
    enum: ROLES,
    example: DEFAULT_ROLE,
    required: false,
  })
  @IsOptional()
  @IsRole()
  role?: string;

  @ApiProperty({
    description: 'Account ID',
    required: false,
  })
  @IsOptional()
  @IsString()
  account_id?: string;
}

export { UserResponseDto, toUserResponse } from './user-response.dto';
export { DEFAULT_ROLE, PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH } from './validation';
