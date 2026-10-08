import { createPrivateKey, createPublicKey, KeyObject } from 'crypto';

export const MIN_RSA_KEY_BITS = 2048;

export interface SigningKey {
  kid: string;
  privateKey: KeyObject;
  publicKey: KeyObject;
}

export function parsePrivateKey(base64Pem: string): KeyObject {
  let key: KeyObject;
  try {
    key = createPrivateKey(Buffer.from(base64Pem, 'base64').toString('utf8'));
  } catch {
    throw new Error('JWT_PRIVATE_KEY is not a valid base64-encoded PEM private key');
  }
  if (key.asymmetricKeyType !== 'rsa') {
    throw new Error('JWT_PRIVATE_KEY must be an RSA private key');
  }
  if ((key.asymmetricKeyDetails?.modulusLength ?? 0) < MIN_RSA_KEY_BITS) {
    throw new Error(`JWT_PRIVATE_KEY must be at least ${MIN_RSA_KEY_BITS} bits`);
  }
  return key;
}

export function loadSigningKey(base64Pem: string, kid: string): SigningKey {
  const privateKey = parsePrivateKey(base64Pem);
  return { kid, privateKey, publicKey: createPublicKey(privateKey) };
}
