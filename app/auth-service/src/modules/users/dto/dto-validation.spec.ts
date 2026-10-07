import { plainToInstance } from 'class-transformer';
import { validate } from 'class-validator';
import { CreateUserDto, UpdateUserDto } from './index';
import { LoginDto, RegisterDto } from '../../auth/dto';

const errorsFor = async <T extends object>(cls: new () => T, body: object) => {
  const errors = await validate(plainToInstance(cls, body), { whitelist: true, forbidNonWhitelisted: true });
  return errors.map((e) => e.property);
};

const valid = {
  username: 'alice',
  email: 'alice@example.com',
  password: 'correct-horse-battery',
};

describe('user DTO validation', () => {
  describe.each([
    ['CreateUserDto', CreateUserDto],
    ['RegisterDto', RegisterDto],
  ])('%s', (_name, cls: new () => object) => {
    it('accepts a valid body', async () => {
      expect(await errorsFor(cls, valid)).toEqual([]);
    });

    it.each([
      ['an 11 character password', 'abcdefghijk'],
      ['a 129 character password', 'a'.repeat(129)],
      ['a whitespace-only password', ' '.repeat(12)],
      ['an empty password', ''],
      ['a non-string password', 123456789012],
    ])('rejects %s', async (_label, password) => {
      expect(await errorsFor(cls, { ...valid, password })).toEqual(['password']);
    });

    it('accepts a 12 and a 128 character password', async () => {
      expect(await errorsFor(cls, { ...valid, password: 'a'.repeat(12) })).toEqual([]);
      expect(await errorsFor(cls, { ...valid, password: 'a'.repeat(128) })).toEqual([]);
    });

    it.each([
      ['username', '   '],
      ['username', 'ab'],
      ['username', 'a'.repeat(51)],
      ['username', 'alice smith'],
      ['email', '   '],
      ['email', 'not-an-email'],
      ['full_name', '   '],
    ])('rejects a bad %s: "%s"', async (field, value) => {
      expect(await errorsFor(cls, { ...valid, [field]: value })).toEqual([field]);
    });

    it('rejects missing fields', async () => {
      expect((await errorsFor(cls, {})).sort()).toEqual(['email', 'password', 'username']);
    });

    it('trims username and email', async () => {
      const dto = plainToInstance(cls, { ...valid, username: '  alice ', email: ' alice@example.com ' }) as CreateUserDto;
      expect(await validate(dto)).toEqual([]);
      expect(dto.username).toBe('alice');
      expect(dto.email).toBe('alice@example.com');
    });

    it('rejects unknown fields such as password_hash', async () => {
      expect(await errorsFor(cls, { ...valid, password_hash: '$2a$10$x' })).toEqual(['password_hash']);
    });
  });

  describe('CreateUserDto role', () => {
    it('accepts USER and ADMIN in any case', async () => {
      expect(await errorsFor(CreateUserDto, { ...valid, role: 'admin' })).toEqual([]);
      expect(plainToInstance(CreateUserDto, { ...valid, role: ' user ' }).role).toBe('USER');
    });

    it('rejects any other role', async () => {
      expect(await errorsFor(CreateUserDto, { ...valid, role: 'SUPERUSER' })).toEqual(['role']);
    });
  });

  describe('RegisterDto', () => {
    it('does not allow choosing a role', async () => {
      expect(await errorsFor(RegisterDto, { ...valid, role: 'ADMIN' })).toEqual(['role']);
    });
  });

  describe('UpdateUserDto', () => {
    it('accepts an empty body and a single field', async () => {
      expect(await errorsFor(UpdateUserDto, {})).toEqual([]);
      expect(await errorsFor(UpdateUserDto, { email: 'new@example.com' })).toEqual([]);
    });

    it('applies the same password policy', async () => {
      expect(await errorsFor(UpdateUserDto, { password: 'short' })).toEqual(['password']);
      expect(await errorsFor(UpdateUserDto, { password: ' '.repeat(20) })).toEqual(['password']);
    });

    it('rejects a blank username', async () => {
      expect(await errorsFor(UpdateUserDto, { username: '   ' })).toEqual(['username']);
    });
  });

  describe('LoginDto', () => {
    it('accepts an existing short password so seed users can still log in', async () => {
      expect(await errorsFor(LoginDto, { username: 'demo', password: 'Demo123!' })).toEqual([]);
    });

    it.each([
      ['a blank username', { username: '   ', password: 'Demo123!' }, 'username'],
      ['a blank password', { username: 'demo', password: '   ' }, 'password'],
      ['a 129 character password', { username: 'demo', password: 'a'.repeat(129) }, 'password'],
    ])('rejects %s', async (_label, body, field) => {
      expect(await errorsFor(LoginDto, body)).toEqual([field]);
    });
  });
});
