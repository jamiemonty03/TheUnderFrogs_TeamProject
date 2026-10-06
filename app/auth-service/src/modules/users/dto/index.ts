import { IsNotEmpty, IsOptional } from 'class-validator';
import { IsEmailAddress, IsFullName, IsPassword, IsRole, IsUsername } from './validation';

export class CreateUserDto {
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
  role?: string;
}

export { UserResponseDto, toUserResponse } from './user-response.dto';
export { PASSWORD_MIN_LENGTH, PASSWORD_MAX_LENGTH } from './validation';
