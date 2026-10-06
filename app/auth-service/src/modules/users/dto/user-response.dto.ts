export class UserResponseDto {
  id: number;
  username: string;
  email: string;
  role: string;
  is_active: boolean;
  version: number;
  created_at: Date;
  updated_at: Date;
}
