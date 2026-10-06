import { IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class LoginDto {
  @ApiProperty({
    description: 'Username or email',
    example: 'johndoe',
  })
  @IsNotEmpty()
  @IsString()
  username: string;

  @ApiProperty({
    description: 'Account password',
    example: 'MySecurePass123',
  })
  @IsNotEmpty()
  @IsString()
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
    example: 'MySecurePass123',
  })
  @IsNotEmpty()
  @IsString()
  @MinLength(6)
  password: string;
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
