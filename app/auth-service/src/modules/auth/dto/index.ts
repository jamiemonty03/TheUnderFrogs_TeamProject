import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';
import {
  IsEmailAddress,
  IsFullName,
  IsPassword,
  IsUsername,
  NotBlank,
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  Trim,
} from '../../users/dto/validation';
import { ApiProperty } from '@nestjs/swagger';

export class LoginDto {
  @ApiProperty({
    description: 'Username or email',
    example: 'johndoe',
  })
  @IsNotEmpty()
  @Trim()
  @IsString()
  @NotBlank()
  @MaxLength(50)
  username: string;

  @ApiProperty({
    description: 'Account password',
    example: 'MySecurePass123',
  })
  @IsNotEmpty()
  @IsString()
  @NotBlank()
  @MaxLength(PASSWORD_MAX_LENGTH)
  password: string;
}

export class RegisterDto {
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
}

export class TokenResponseDto {
  @ApiProperty({
    description: 'JWT access token',
  })
  access_token: string;

  @ApiProperty({
    description: 'Token type (always Bearer)',
    enum: ['Bearer'],
    example: 'Bearer',
  })
  token_type: string;

  @ApiProperty({
    description: 'Token expiration time in seconds',
    type: 'integer',
    example: 86400,
  })
  expires_in: number;
}

export { TokenPayloadDto } from './token-payload.dto';
