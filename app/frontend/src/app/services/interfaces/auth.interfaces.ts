export interface CurrentUser {
  userId: number;
  username: string;
  roles: string[];
  accountId: string | null;
}

export interface RegisterDetails {
  fullName: string;
  username: string;
  email: string;
  password: string;
}
