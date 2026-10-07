import { generateKeyPairSync, createSign, createVerify } from 'crypto';
import { loadSigningKey, parsePrivateKey } from './signing-key';
import { validationSchema } from './validation';

const toBase64Pem = (pem: string) => Buffer.from(pem).toString('base64');

const rsaKey = (bits: number) =>
  toBase64Pem(
    generateKeyPairSync('rsa', { modulusLength: bits }).privateKey.export({ type: 'pkcs8', format: 'pem' }).toString(),
  );

describe('signing key config', () => {
  const validKey = rsaKey(2048);

  describe('parsePrivateKey', () => {
    it('accepts a 2048-bit RSA key', () => {
      expect(parsePrivateKey(validKey).asymmetricKeyType).toBe('rsa');
    });

    it('rejects a key smaller than 2048 bits', () => {
      expect(() => parsePrivateKey(rsaKey(1024))).toThrow('at least 2048 bits');
    });

    it('rejects a non-RSA key', () => {
      const ec = generateKeyPairSync('ec', { namedCurve: 'P-256' }).privateKey.export({ type: 'pkcs8', format: 'pem' });
      expect(() => parsePrivateKey(toBase64Pem(ec.toString()))).toThrow('must be an RSA private key');
    });

    it('rejects a public key', () => {
      const pub = generateKeyPairSync('rsa', { modulusLength: 2048 }).publicKey.export({ type: 'spki', format: 'pem' });
      expect(() => parsePrivateKey(toBase64Pem(pub.toString()))).toThrow('not a valid');
    });

    it('rejects garbage without echoing it', () => {
      expect(() => parsePrivateKey(toBase64Pem('not-a-key-secret-value'))).toThrow(
        'JWT_PRIVATE_KEY is not a valid base64-encoded PEM private key',
      );
    });
  });

  describe('loadSigningKey', () => {
    it('derives a public key that verifies signatures made with the private key', () => {
      const key = loadSigningKey(validKey, 'auth-test');
      const signature = createSign('RSA-SHA256').update('payload').sign(key.privateKey);

      expect(key.kid).toBe('auth-test');
      expect(key.publicKey.type).toBe('public');
      expect(createVerify('RSA-SHA256').update('payload').verify(key.publicKey, signature)).toBe(true);
    });
  });

  describe('validationSchema', () => {
    const base = {
      DB_HOST: 'auth-db',
      DB_USERNAME: 'postgres',
      DB_PASSWORD: 'pw',
      DB_NAME: 'auth_db',
      JWT_SECRET: 'x'.repeat(32),
      JWT_PRIVATE_KEY: validKey,
      JWT_KEY_ID: 'auth-20261007',
    };

    it('accepts a valid config and applies the token defaults', () => {
      const { error, value } = validationSchema.validate(base);

      expect(error).toBeUndefined();
      expect(value).toMatchObject({ JWT_ISSUER: 'auth-service', JWT_AUDIENCE: 'trading-platform', JWT_ACCESS_TOKEN_TTL: 900 });
    });

    it.each(['JWT_PRIVATE_KEY', 'JWT_KEY_ID'])('requires %s', (name) => {
      const { error } = validationSchema.validate({ ...base, [name]: undefined });
      expect(error?.message).toContain(name);
    });

    it('rejects an invalid key without printing its value', () => {
      const secret = toBase64Pem('super-secret-garbage');
      const { error } = validationSchema.validate({ ...base, JWT_PRIVATE_KEY: secret });

      expect(error?.message).toContain('not a valid base64-encoded PEM private key');
      expect(error?.message).not.toContain(secret);
    });

    it('rejects a key ID with unsafe characters', () => {
      expect(validationSchema.validate({ ...base, JWT_KEY_ID: 'bad kid!' }).error).toBeDefined();
    });

    it.each([30, 7200])('rejects an access token lifetime of %i seconds', (ttl) => {
      expect(validationSchema.validate({ ...base, JWT_ACCESS_TOKEN_TTL: ttl }).error).toBeDefined();
    });
  });
});
