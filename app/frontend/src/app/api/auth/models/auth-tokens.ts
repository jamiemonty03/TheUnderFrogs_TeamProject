export interface AuthTokens {

  accessToken: string;

  expiresIn: number;

  mfaRequired: boolean;

  refreshToken: string;
}
