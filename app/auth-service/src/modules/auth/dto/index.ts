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
    description: 'RS256 JWT access token',
  })
  accessToken: string;

  @ApiProperty({
    description: 'Opaque refresh token',
  })
  refreshToken: string;

  @ApiProperty({
    description: 'Access token lifetime in seconds',
    type: 'integer',
    example: 900,
  })
  expiresIn: number;

  @ApiProperty({
    description: 'Whether a second factor is needed before the tokens can be used',
    example: false,
  })
  mfaRequired: boolean;
}

export class RegisterResponseDto extends TokenResponseDto {
  @ApiProperty({
    description: 'Trading account created for the new user in accounts-service',
    example: 'ACC0012',
  })
  accountId: string;
}

