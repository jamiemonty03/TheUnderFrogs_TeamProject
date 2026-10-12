export interface Jwks {
  keys: Array<{
'kty': string;
'use': string;
'kid': string;
'alg': string;

'n': string;

'e': string;
}>;
}
