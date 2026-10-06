import { IsNotEmpty, IsOptional } from 'class-validator';
import { IsEmailAddress, IsFullName, IsPassword, IsRole, IsUsername } from './validation';
import { IsEmail, IsNotEmpty, IsString, MinLength, IsOptional } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

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
    description: 'Account password (minimum 6 characters)',
    minLength: 6,
    example: 'Password1234',
  })
  @IsNotEmpty()
  @IsPassword()
  password: string;

  @IsOptional()
  @IsFullName()
  full_name?: string;

  @IsOptional()
  @IsRole()
  role?: string;
}

export class UpdateUserDto {
  @IsOptional()
  @IsUsername()
  username?: string;

  @IsOptional()
  @IsEmailAddress()
  email?: string;

  @IsOptional()
  @IsPassword()
  password?: string;

  @IsOptional()
  @IsFullName()
  full_name?: string;

  @IsOptional()
  @IsRole()
  @ApiProperty({
    description: 'User role',
    default: 'user',
    example: 'user',
    required: false,
  })
  @IsString()
  @IsOptional()
  role?: string = 'user';
}

export class UpdateUserDto {
  @ApiProperty({
    description: 'Username',
    minLength: 3,
    example: 'johndoe',
    required: true,
  })
  @IsString()
  @MinLength(3)
  @IsNotEmpty()
  username: string;

  @ApiProperty({
    description: 'User email address',
    format: 'email',
    example: 'john@example.com',
    required: false,
  })
  @IsEmail()
  @IsOptional()
  email?: string;

  @ApiProperty({
    description: 'Account password (minimum 6 characters)',
    minLength: 6,
    example: 'Password1234',
    required: true,
  })
  
  @IsString()
  @IsNotEmpty()
  @MinLength(6)
  
  password: string;

  @ApiProperty({
    description: 'User role',
    example: 'user',
    required: false,
  })
  @IsString()
  @IsOptional()
  role?: string;
}

export { UserResponseDto, toUserResponse } from './user-response.dto';
export { PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH } from './validation';
