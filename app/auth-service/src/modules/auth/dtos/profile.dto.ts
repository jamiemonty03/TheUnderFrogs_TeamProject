import { ApiProperty } from '@nestjs/swagger';
import { IsOptional } from 'class-validator';
import { IsEmailAddress, IsFullName } from '../../users/dtos/validation';

export class ProfileResponseDto {
  @ApiProperty({ example: 'demo' })
  username: string;

  @ApiProperty({ example: 'demo@toadstock.io' })
  email: string;

  @ApiProperty({ example: 'Demo Trader', nullable: true })
  fullName: string | null;
}

export class UpdateProfileDto {
  @ApiProperty({ description: 'Full name', maxLength: 255, example: 'Demo Trader', required: false })
  @IsOptional()
  @IsFullName()
  fullName?: string;

  @ApiProperty({ description: 'Email address', format: 'email', example: 'demo@toadstock.io', required: false })
  @IsOptional()
  @IsEmailAddress()
  email?: string;
}
