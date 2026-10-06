import { Test, TestingModule } from '@nestjs/testing';
import { ConflictException, NotFoundException } from '@nestjs/common';
import { UsersService } from './users.service';
import { UsersRepository } from './users.repository';
import { PasswordHasher } from './password-hasher.service';
import { User } from './entities/user.entity';
import * as bcrypt from 'bcryptjs';

describe('UsersService', () => {
  let service: UsersService;
  let repository: UsersRepository;
  let hasher: jest.Mocked<PasswordHasher>;

  const mockUser = {
    id: 1,
    username: 'testuser',
    email: 'test@example.com',
    password_hash: 'hashedPassword',
    full_name: 'Test User',
    roles: ['USER'],
    account_id: null,
    is_active: true,
    failed_attempts: 0,
    locked_until: null,
    version: 0,
    created_at: new Date(),
    updated_at: new Date(),
    updated_by: 'SYSTEM',
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        UsersService,
        {
          provide: UsersRepository,
          useValue: {
            create: jest.fn(),
            findById: jest.fn(),
            findByUsername: jest.fn(),
            findByEmail: jest.fn(),
            findAll: jest.fn(),
            update: jest.fn(),
            updatePasswordHash: jest.fn(),
            delete: jest.fn(),
          },
        },
        {
          provide: PasswordHasher,
          useValue: {
            hash: jest.fn(),
            verify: jest.fn(),
            needsRehash: jest.fn(),
          },
        },
      ],
    }).compile();

    service = module.get<UsersService>(UsersService);
    repository = module.get<UsersRepository>(UsersRepository);
    hasher = module.get(PasswordHasher);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('createUser', () => {
    it('should create a new user', async () => {
      const createUserDto = {
        username: 'newuser',
        email: 'new@example.com',
        password: 'password123',
      };

      jest.spyOn(repository, 'findByUsername').mockResolvedValue(null);
      jest.spyOn(repository, 'findByEmail').mockResolvedValue(null);
      jest.spyOn(repository, 'create').mockResolvedValue(mockUser);
      hasher.hash.mockResolvedValue('$argon2id$new-hash');

      const result = await service.createUser(createUserDto);

      expect(result).toEqual(mockUser);
      expect(hasher.hash).toHaveBeenCalledWith('password123');
      expect(repository.create).toHaveBeenCalledWith(
        expect.objectContaining({ password_hash: '$argon2id$new-hash' }),
      );
      expect(repository.create).not.toHaveBeenCalledWith(expect.objectContaining({ password: expect.anything() }));
    });

    it('should throw ConflictException if username exists', async () => {
      const createUserDto = {
        username: 'testuser',
        email: 'new@example.com',
        password: 'password123',
      };

      jest.spyOn(repository, 'findByUsername').mockResolvedValue(mockUser);

      await expect(service.createUser(createUserDto)).rejects.toThrow(ConflictException);
    });

    it('should throw ConflictException if email exists', async () => {
      const createUserDto = {
        username: 'newuser',
        email: 'test@example.com',
        password: 'password123',
      };

      jest.spyOn(repository, 'findByUsername').mockResolvedValue(null);
      jest.spyOn(repository, 'findByEmail').mockResolvedValue(mockUser);

      await expect(service.createUser(createUserDto)).rejects.toThrow(ConflictException);
    });
  });

  describe('getUserById', () => {
    it('should return a user by id', async () => {
      jest.spyOn(repository, 'findById').mockResolvedValue(mockUser);

      const result = await service.getUserById(1);

      expect(result).toEqual(mockUser);
      expect(repository.findById).toHaveBeenCalledWith(1);
    });

    it('should throw NotFoundException if user not found', async () => {
      jest.spyOn(repository, 'findById').mockResolvedValue(null);

      await expect(service.getUserById(999)).rejects.toThrow(NotFoundException);
    });
  });

  describe('updateUser', () => {
    it('hashes a new password with the password hasher', async () => {
      jest.spyOn(repository, 'findById').mockResolvedValue(mockUser);
      jest.spyOn(repository, 'update').mockResolvedValue(mockUser);
      hasher.hash.mockResolvedValue('$argon2id$new-hash');

      await service.updateUser(1, { password: 'a-new-long-password' });

      expect(hasher.hash).toHaveBeenCalledWith('a-new-long-password');
      expect(repository.update).toHaveBeenCalledWith(1, { password_hash: '$argon2id$new-hash' });
    });

    it('does not touch the password hash when no password is given', async () => {
      jest.spyOn(repository, 'findById').mockResolvedValue(mockUser);
      jest.spyOn(repository, 'update').mockResolvedValue(mockUser);

      await service.updateUser(1, { email: 'new@example.com', role: 'admin' });

      expect(hasher.hash).not.toHaveBeenCalled();
      expect(repository.update).toHaveBeenCalledWith(1, { email: 'new@example.com', roles: ['ADMIN'] });
    });
  });

  describe('verifyCredentials', () => {
    const bcryptHash = '$2a$10$tEN44im3u450Nu8rjYmV2./rNJUmlmQsl8jIAmoJjbOWRMXILuxfW';
    const argonHash = '$argon2id$v=19$m=19456,t=2,p=1$c2FsdHNhbHQ$aGFzaA';

    it('returns null for an unknown username and still runs a password check', async () => {
      jest.spyOn(repository, 'findByUsername').mockResolvedValue(null);
      hasher.hash.mockResolvedValue(argonHash);
      hasher.verify.mockResolvedValue(false);

      await expect(service.verifyCredentials('ghost', 'Demo123!')).resolves.toBeNull();
      expect(hasher.verify).toHaveBeenCalledWith('Demo123!', argonHash);
      expect(repository.updatePasswordHash).not.toHaveBeenCalled();
    });

    it('returns null for a wrong password and does not rehash', async () => {
      jest.spyOn(repository, 'findByUsername').mockResolvedValue({ ...mockUser, password_hash: bcryptHash });
      hasher.verify.mockResolvedValue(false);

      await expect(service.verifyCredentials('testuser', 'wrong')).resolves.toBeNull();
      expect(hasher.hash).not.toHaveBeenCalled();
      expect(repository.updatePasswordHash).not.toHaveBeenCalled();
    });

    it('rehashes a bcrypt hash to argon2id after a successful login', async () => {
      jest.spyOn(repository, 'findByUsername').mockResolvedValue({ ...mockUser, password_hash: bcryptHash });
      hasher.verify.mockResolvedValue(true);
      hasher.needsRehash.mockReturnValue(true);
      hasher.hash.mockResolvedValue(argonHash);
      jest.spyOn(repository, 'updatePasswordHash').mockResolvedValue(true);

      const user = await service.verifyCredentials('testuser', 'Demo123!');

      expect(hasher.hash).toHaveBeenCalledWith('Demo123!');
      expect(repository.updatePasswordHash).toHaveBeenCalledWith(1, bcryptHash, argonHash);
      expect(user?.password_hash).toBe(argonHash);
    });

    it('does not rehash a current argon2id hash', async () => {
      jest.spyOn(repository, 'findByUsername').mockResolvedValue({ ...mockUser, password_hash: argonHash });
      hasher.verify.mockResolvedValue(true);
      hasher.needsRehash.mockReturnValue(false);

      const user = await service.verifyCredentials('testuser', 'Demo123!');

      expect(user?.id).toBe(1);
      expect(hasher.hash).not.toHaveBeenCalled();
      expect(repository.updatePasswordHash).not.toHaveBeenCalled();
    });

    it('keeps the old hash when another login already replaced it', async () => {
      jest.spyOn(repository, 'findByUsername').mockResolvedValue({ ...mockUser, password_hash: bcryptHash });
      hasher.verify.mockResolvedValue(true);
      hasher.needsRehash.mockReturnValue(true);
      hasher.hash.mockResolvedValue(argonHash);
      jest.spyOn(repository, 'updatePasswordHash').mockResolvedValue(false);

      const user = await service.verifyCredentials('testuser', 'Demo123!');

      expect(user?.password_hash).toBe(bcryptHash);
    });

    it('still logs the user in if the rehash fails', async () => {
      jest.spyOn(repository, 'findByUsername').mockResolvedValue({ ...mockUser, password_hash: bcryptHash });
      hasher.verify.mockResolvedValue(true);
      hasher.needsRehash.mockReturnValue(true);
      hasher.hash.mockResolvedValue(argonHash);
      jest.spyOn(repository, 'updatePasswordHash').mockRejectedValue(new Error('db down'));

      const user = await service.verifyCredentials('testuser', 'Demo123!');

      expect(user?.id).toBe(1);
      expect(user?.password_hash).toBe(bcryptHash);
    });
  });

  describe('with the real PasswordHasher', () => {
    let realService: UsersService;
    let stored: User | undefined;
    const repo = {
      findByUsername: jest.fn(async (username: string) => (stored?.username === username ? { ...stored } : null)),
      findByEmail: jest.fn(async () => null),
      create: jest.fn(async (payload: Partial<User>) => {
        stored = { ...mockUser, ...payload } as User;
        return { ...stored };
      }),
      updatePasswordHash: jest.fn(async (_id: number, current: string, next: string) => {
        if (!stored || stored.password_hash !== current) {
          return false;
        }
        stored = { ...stored, password_hash: next };
        return true;
      }),
    };

    beforeEach(() => {
      stored = undefined;
      realService = new UsersService(repo as unknown as UsersRepository, new PasswordHasher());
    });

    it('stores new passwords as salted argon2id hashes, never in plain text', async () => {
      await realService.createUser({ username: 'newuser', email: 'new@example.com', password: 'correct-horse-battery' });

      expect(stored?.password_hash.startsWith('$argon2id$')).toBe(true);
      expect(stored?.password_hash).not.toContain('correct-horse-battery');
    });

    it('logs a seed BCrypt user in, upgrades them to argon2id, and the new hash still works', async () => {
      stored = { ...mockUser, username: 'demo', password_hash: await bcrypt.hash('Demo123!', 4) };

      expect(await realService.verifyCredentials('demo', 'Demo123!')).not.toBeNull();
      expect(stored?.password_hash.startsWith('$argon2id$')).toBe(true);

      expect(await realService.verifyCredentials('demo', 'Demo123!')).not.toBeNull();
      expect(await realService.verifyCredentials('demo', 'wrong-password')).toBeNull();
      expect(repo.updatePasswordHash).toHaveBeenCalledTimes(1);
    });
  });
});
