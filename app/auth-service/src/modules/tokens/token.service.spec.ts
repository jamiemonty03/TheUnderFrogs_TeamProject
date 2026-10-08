import { jest } from '@jest/globals';
import { createHmac, createSign, generateKeyPairSync } from 'crypto';
import { UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { loadSigningKey, SigningKey } from '../../config/signing-key';
import { TokenService, TokenSubject } from './token.service';

const newKey = (kid: string): SigningKey => {
  const pem = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey.export({ type: 'pkcs8', format: 'pem' });
  return loadSigningKey(Buffer.from(pem.toString()).toString('base64'), kid);
};

const config = (overrides: Record<string, unknown> = {}) =>
  ({ get: (name: string, fallback: unknown) => (name in overrides ? overrides[name] : fallback) }) as unknown as ConfigService;

const b64url = (value: object | string) =>
  Buffer.from(typeof value === 'string' ? value : JSON.stringify(value)).toString('base64url');

describe('TokenService', () => {
  const key = newKey('auth-key-1');
  const service = new TokenService(key, config());
  const alice: TokenSubject = { id: 7, username: 'alice', roles: ['TRADER'], account_id: 'ACC0001' };
  const now = () => Math.floor(Date.now() / 1000);

  const claimsFor = (overrides: Record<string, unknown> = {}) => ({
    sub: '7',
    username: 'alice',
    roles: ['ADMIN'],
    accountId: 'ACC0001',
    iss: 'auth-service',
    aud: 'trading-platform',
    iat: now(),
    exp: now() + 900,
    ...overrides,
  });

  const signRs256 = (header: object, claims: object, privateKey = key.privateKey) => {
    const input = `${b64url(header)}.${b64url(claims)}`;
    return `${input}.${createSign('RSA-SHA256').update(input).sign(privateKey).toString('base64url')}`;
  };

  afterEach(() => jest.useRealTimers());

  describe('issue', () => {
    it('signs with RS256 and puts the key ID in the header', () => {
      const { accessToken } = service.issue(alice);

      expect(service.decode(accessToken)?.header).toMatchObject({ alg: 'RS256', kid: 'auth-key-1', typ: 'JWT' });
    });

    it('includes the agreed claims', () => {
      const claims = service.decode(service.issue(alice).accessToken)?.claims;

      expect(claims).toMatchObject({
        sub: '7',
        username: 'alice',
        roles: ['TRADER'],
        accountId: 'ACC0001',
        iss: 'auth-service',
        aud: 'trading-platform',
      });
    });

    it('expires after 15 minutes by default and reports expiresIn', () => {
      const { accessToken, expiresIn } = service.issue(alice);
      const claims = service.decode(accessToken)!.claims;

      expect(expiresIn).toBe(900);
      expect(claims.exp - claims.iat).toBe(900);
    });

    it('uses the configured issuer, audience and lifetime', () => {
      const custom = new TokenService(key, config({ JWT_ISSUER: 'iss-x', JWT_AUDIENCE: 'aud-y', JWT_ACCESS_TOKEN_TTL: 300 }));
      const { accessToken, expiresIn } = custom.issue(alice);
      const claims = custom.decode(accessToken)!.claims;

      expect(expiresIn).toBe(300);
      expect(claims).toMatchObject({ iss: 'iss-x', aud: 'aud-y' });
      expect(claims.exp - claims.iat).toBe(300);
    });

    it('keeps a null accountId for users without a trading account', () => {
      const claims = service.decode(service.issue({ ...alice, account_id: null }).accessToken)!.claims;

      expect(claims.accountId).toBeNull();
    });

    it('never puts the password hash or other user fields in the token', () => {
      const user = { ...alice, password_hash: '$argon2id$secret', email: 'alice@example.com' } as TokenSubject;
      const payload = Buffer.from(service.issue(user).accessToken.split('.')[1], 'base64url').toString();

      expect(payload).not.toContain('argon2id');
      expect(payload).not.toContain('email');
    });
  });

  describe('verify', () => {
    it('returns the claims of a valid token', () => {
      expect(service.verify(service.issue(alice).accessToken)).toMatchObject({ sub: '7', roles: ['TRADER'] });
    });

    it('rejects an expired token', () => {
      jest.useFakeTimers({ now: new Date('2026-10-07T10:00:00Z') });
      const { accessToken } = service.issue(alice);
      jest.setSystemTime(new Date('2026-10-07T10:15:01Z'));

      expect(() => service.verify(accessToken)).toThrow(UnauthorizedException);
    });

    it('rejects a token whose claims were changed after signing', () => {
      const [header, , signature] = service.issue(alice).accessToken.split('.');
      const forged = `${header}.${b64url(claimsFor({ roles: ['ADMIN'] }))}.${signature}`;

      expect(() => service.verify(forged)).toThrow(UnauthorizedException);
    });

    it('rejects a token signed by a different private key with the same kid', () => {
      const attacker = newKey('auth-key-1');
      const forged = signRs256({ alg: 'RS256', typ: 'JWT', kid: 'auth-key-1' }, claimsFor(), attacker.privateKey);

      expect(() => service.verify(forged)).toThrow(UnauthorizedException);
    });

    it('rejects an HS256 token signed with the public key as the secret', () => {
      const publicPem = key.publicKey.export({ type: 'spki', format: 'pem' }).toString();
      const input = `${b64url({ alg: 'HS256', typ: 'JWT', kid: 'auth-key-1' })}.${b64url(claimsFor())}`;
      const forged = `${input}.${createHmac('sha256', publicPem).update(input).digest('base64url')}`;

      expect(() => service.verify(forged)).toThrow(UnauthorizedException);
    });

    it('rejects an unsigned alg "none" token', () => {
      const forged = `${b64url({ alg: 'none', typ: 'JWT', kid: 'auth-key-1' })}.${b64url(claimsFor())}.`;

      expect(() => service.verify(forged)).toThrow(UnauthorizedException);
    });

    it.each([
      ['an unknown kid', { kid: 'other-key' }, {}],
      ['no kid', { kid: undefined }, {}],
      ['the wrong issuer', {}, { iss: 'someone-else' }],
      ['the wrong audience', {}, { aud: 'another-app' }],
    ])('rejects a correctly signed token with %s', (_label, header, claims) => {
      const token = signRs256({ alg: 'RS256', typ: 'JWT', kid: 'auth-key-1', ...header }, claimsFor(claims));

      expect(() => service.verify(token)).toThrow(UnauthorizedException);
    });

    it('accepts a token built by hand with the right key, kid, issuer and audience', () => {
      const token = signRs256({ alg: 'RS256', typ: 'JWT', kid: 'auth-key-1' }, claimsFor());

      expect(service.verify(token).username).toBe('alice');
    });

    it.each(['', 'not-a-jwt', 'a.b.c'])('rejects garbage: "%s"', (token) => {
      expect(() => service.verify(token)).toThrow(UnauthorizedException);
    });

    it('gives the same message for every failure', () => {
      const failures = ['garbage', signRs256({ alg: 'RS256', kid: 'other' }, claimsFor())].map((token) => {
        try {
          service.verify(token);
          return 'no error';
        } catch (error) {
          return (error as Error).message;
        }
      });

      expect(new Set(failures)).toEqual(new Set(['Invalid or expired token']));
    });
  });

  describe('decode', () => {
    it('reads header and claims without checking the signature', () => {
      const forged = signRs256({ alg: 'RS256', kid: 'other' }, claimsFor(), newKey('x').privateKey);

      expect(service.decode(forged)).toMatchObject({ header: { kid: 'other' }, claims: { username: 'alice' } });
    });

    it('still reads an expired token', () => {
      const expired = signRs256({ alg: 'RS256', kid: 'auth-key-1' }, claimsFor({ exp: now() - 60 }));

      expect(service.decode(expired)?.claims.username).toBe('alice');
    });

    it.each(['', 'not-a-jwt', 'a.b.c'])('returns null for garbage: "%s"', (token) => {
      expect(service.decode(token)).toBeNull();
    });
  });
});
