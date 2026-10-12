import { AuthTokens } from '../models/auth-tokens';
export type RegisterResponse = AuthTokens & {

'accountId': string;
};

