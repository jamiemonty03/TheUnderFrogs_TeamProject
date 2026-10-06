import { Test, TestingModule } from '@nestjs/testing';
import { ConflictException, NotFoundException } from '@nestjs/common';
import { UsersService } from './users.service';
import { UsersRepository } from './users.repository';
import { PasswordHasher } from './password-hasher.service';
import * as bcrypt from 'bcryptjs';

jest.mock('bcryptjs');

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

    (bcrypt.hash as jest.Mock).mockResolvedValue('hashedPassword');
    (bcrypt.compare as jest.Mock).mockResolvedValue(true);
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

      const result = await service.createUser(createUserDto);

      expect(result).toEqual(mockUser);
      expect(bcrypt.hash).toHaveBeenCalledWith('password123', 10);
      expect(repository.create).toHaveBeenCalled();
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

  describe('validatePassword', () => {
    it('should return true for matching passwords', async () => {
      const result = await service.validatePassword('password123', 'hashedPassword');

      expect(result).toBe(true);
      expect(bcrypt.compare).toHaveBeenCalledWith('password123', 'hashedPassword');
    });

    it('should return false for non-matching passwords', async () => {
      (bcrypt.compare as jest.Mock).mockResolvedValue(false);

      const result = await service.validatePassword('password123', 'wrongHash');

      expect(result).toBe(false);
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
});
