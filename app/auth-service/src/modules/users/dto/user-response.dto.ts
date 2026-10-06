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
}
