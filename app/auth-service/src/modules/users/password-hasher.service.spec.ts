import * as argon2 from 'argon2';
import * as bcrypt from 'bcryptjs';
import { PasswordHasher } from './password-hasher.service';

describe('PasswordHasher', () => {
  const hasher = new PasswordHasher();
  const password = 'correct-horse-battery';

  describe('hash', () => {
    it('produces an argon2id hash with the OWASP parameters', async () => {
      const hash = await hasher.hash(password);
      const [, type, version, params] = hash.split('$');
      expect(type).toBe('argon2id');
      expect(version).toBe('v=19');
      expect(params.split(',').sort()).toEqual(['m=19456', 'p=1', 't=2']);
      expect(hash).not.toContain(password);
    });

    it('salts every hash so the same password gives different hashes', async () => {
      const first = await hasher.hash(password);
      const second = await hasher.hash(password);
      expect(first).not.toEqual(second);
    });
  });

  describe('verify', () => {
    it('accepts the right password for an argon2id hash', async () => {
      const hash = await hasher.hash(password);
      await expect(hasher.verify(password, hash)).resolves.toBe(true);
    });

    it('rejects the wrong password for an argon2id hash', async () => {
      const hash = await hasher.hash(password);
      await expect(hasher.verify('wrong-password', hash)).resolves.toBe(false);
    });

    it.each(['$2a$', '$2b$', '$2y$'])('accepts the right password for a %s bcrypt hash', async (prefix) => {
      const hash = (await bcrypt.hash(password, 4)).replace(/^\$2[aby]\$/, prefix);
      await expect(hasher.verify(password, hash)).resolves.toBe(true);
    });

    it('rejects the wrong password for a bcrypt hash', async () => {
      const hash = await bcrypt.hash(password, 4);
      await expect(hasher.verify('wrong-password', hash)).resolves.toBe(false);
    });

    it('verifies the existing seed user hash', async () => {
      const demoHash = '$2a$10$tEN44im3u450Nu8rjYmV2./rNJUmlmQsl8jIAmoJjbOWRMXILuxfW';
      await expect(hasher.verify('Demo123!', demoHash)).resolves.toBe(true);
    });

    it.each([
      ['a plain-text password', password],
      ['an argon2i hash', '$argon2i$v=19$m=19456,t=2,p=1$c2FsdHNhbHQ$aGFzaA'],
      ['a malformed argon2id hash', '$argon2id$garbage'],
      ['an empty hash', ''],
    ])('rejects %s', async (_label, hash) => {
      await expect(hasher.verify(password, hash)).resolves.toBe(false);
    });

    it('rejects an empty password', async () => {
      const hash = await hasher.hash(password);
      await expect(hasher.verify('', hash)).resolves.toBe(false);
    });
  });

  describe('needsRehash', () => {
    it('is false for a current argon2id hash', async () => {
      expect(hasher.needsRehash(await hasher.hash(password))).toBe(false);
    });

    it('is true for a bcrypt hash', async () => {
      expect(hasher.needsRehash(await bcrypt.hash(password, 4))).toBe(true);
    });

    it('is true for an argon2id hash with weaker parameters', async () => {
      const weak = await argon2.hash(password, { type: argon2.argon2id, memoryCost: 8192, timeCost: 1, parallelism: 1 });
      expect(hasher.needsRehash(weak)).toBe(true);
    });

    it('is true for an unknown hash format', () => {
      expect(hasher.needsRehash('not-a-hash')).toBe(true);
    });
  });
});
