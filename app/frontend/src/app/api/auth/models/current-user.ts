export interface CurrentUser {
  accountId?: string | null;
  roles: Array<string>;
  userId: number;
  username: string;
}
