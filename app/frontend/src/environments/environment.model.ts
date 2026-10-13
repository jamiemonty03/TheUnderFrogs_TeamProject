export interface DevLogin {
  username: string;
  password: string;
}

export interface Environment {
  production: boolean;
  apiBaseUrl: string;
  devLogin: DevLogin | null;
}
