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
  @IsString()
  @MinLength(3)
  username: string;

  @ApiProperty({
    description: 'User email address',
    format: 'email',
    example: 'john@example.com',
  })
  @IsNotEmpty()
  @IsEmail()
  email: string;

  @ApiProperty({
    description: 'Account password (minimum 6 characters)',
    minLength: 6,
    example: 'Password1234',
  })
  @IsNotEmpty()
  @IsString()
  @MinLength(6)
  password: string;

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

export { UserResponseDto } from './user-response.dto';
