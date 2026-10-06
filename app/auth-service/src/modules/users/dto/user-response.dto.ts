import { User } from '../entities/user.entity';

export class UserResponseDto {
  id: number;
  username: string;
  email: string;
  full_name: string | null;
  roles: string[];
  account_id: string | null;
  is_active: boolean;
  version: number;
  created_at: Date;
  updated_at: Date;
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
