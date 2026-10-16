import { Injectable } from '@nestjs/common';
import * as argon2 from 'argon2';
import * as bcrypt from 'bcryptjs';

const ARGON2_OPTIONS = {
  type: argon2.argon2id,
  memoryCost: 19456,
  timeCost: 2,
  parallelism: 1,
} as const;

const ARGON2ID_PREFIX = '$argon2id$';
const BCRYPT_PATTERN = /^\$2[aby]\$\d{2}\$/;

@Injectable()
export class PasswordHasher {
  hash(plain: string): Promise<string> {
    return argon2.hash(plain, ARGON2_OPTIONS);
  }

  async verify(plain: string, hash: string): Promise<boolean> {
    if (!plain || !hash) {
      return false;
    }
    try {
      if (hash.startsWith(ARGON2ID_PREFIX)) {
        return await argon2.verify(hash, plain);
      }
      if (BCRYPT_PATTERN.test(hash)) {
        return await bcrypt.compare(plain, hash);
      }
      return false;
    } catch {
      return false;
    }
  }

  needsRehash(hash: string): boolean {
    if (!hash.startsWith(ARGON2ID_PREFIX)) {
      return true;
    }
    try {
      return argon2.needsRehash(hash, ARGON2_OPTIONS);
    } catch {
      return true;
    }
  }
}
