import { IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';

export class CreateUserDto {
  @IsNotEmpty()
  @IsString()
  @MinLength(3)
  username: string;

  @IsNotEmpty()
  @IsEmail()
  email: string;

  @IsNotEmpty()
  @IsString()
  @MinLength(6)
  password: string;

  @IsString()
  role?: string = 'user';
}

export class UpdateUserDto {
  @IsString()
  @MinLength(3)
  username?: string;

  @IsEmail()
  email?: string;

  @IsString()
  @MinLength(6)
  password?: string;

  @IsString()
  role?: string;
}

export { UserResponseDto, toUserResponse } from './user-response.dto';
