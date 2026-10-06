import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';
import {
  IsEmailAddress,
  IsFullName,
  IsPassword,
  IsUsername,
  NotBlank,
  PASSWORD_MAX_LENGTH,
  Trim,
} from '../../users/dto/validation';

export class LoginDto {
  @IsNotEmpty()
  @Trim()
  @IsString()
  @NotBlank()
  @MaxLength(50)
  username: string;

  @IsNotEmpty()
  @IsString()
  @NotBlank()
  @MaxLength(PASSWORD_MAX_LENGTH)
  password: string;
}

export class RegisterDto {
  @IsNotEmpty()
  @IsUsername()
  username: string;

  @IsNotEmpty()
  @IsEmailAddress()
  email: string;

  @IsNotEmpty()
  @IsPassword()
  password: string;

  @IsOptional()
  @IsFullName()
  full_name?: string;
}

export class TokenResponseDto {
  access_token: string;
  token_type: string;
  expires_in: number;
}

export { TokenPayloadDto } from './token-payload.dto';
