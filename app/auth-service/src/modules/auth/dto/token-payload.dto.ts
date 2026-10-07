export class TokenPayloadDto {
  sub: number;
  username: string;
  roles: string[];
  accountId?: string;
  iat?: number;
  exp?: number;
}
