export class TokenPayloadDto {
  sub: number;
  username: string;
  roles: string[];
  iat?: number;
  exp?: number;
}
