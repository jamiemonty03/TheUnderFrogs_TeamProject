import { User } from '../entities/user.entity';
import { ApiProperty } from '@nestjs/swagger';

export class UserResponseDto {
  @ApiProperty({
    description: 'User ID',
    type: 'integer',
    example: 1,
  })
  id: number;

  @ApiProperty({
    description: 'Username',
    example: 'johndoe',
  })
  username: string;

  @ApiProperty({
    description: 'User email address',
    format: 'email',
    example: 'john@example.com',
  })
  email: string;
  full_name: string | null;
  roles: string[];
  account_id: string | null;

  @ApiProperty({
    description: 'User role',
    example: 'user',
  })
  role: string;

  @ApiProperty({
    description: 'User account status',
    type: 'boolean',
    example: true,
  })
  is_active: boolean;

  @ApiProperty({
    description: 'Account creation timestamp',
    format: 'date-time',
    example: '2024-10-06T10:30:00Z',
  })
  created_at: Date;

  @ApiProperty({
    description: 'Last update timestamp',
    format: 'date-time',
    example: '2024-10-06T10:30:00Z',
  })
  updated_at: Date;

  @ApiProperty({
    description: 'Version for optimistic locking',
    type: 'integer',
    example: 1,
  })
  version: number;
}

export function toUserResponse(user: User): UserResponseDto {
  return {
    id: user.id,
    username: user.username,
    email: user.email,
    full_name: user.full_name,
    roles: user.roles,
    account_id: user.account_id,
    is_active: user.is_active,
    version: user.version,
    created_at: user.created_at,
    updated_at: user.updated_at,
  };
}
