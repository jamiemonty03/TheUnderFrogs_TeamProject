import { jest } from '@jest/globals';
import { Test, TestingModule } from '@nestjs/testing';
import { ConflictException, NotFoundException } from '@nestjs/common';
import { UsersService } from './users.service';
import { UsersRepository } from './users.repository';
import * as bcrypt from 'bcryptjs';

jest.mock('bcryptjs');

describe('UsersService', () => {
  let service: UsersService;
  let repository: UsersRepository;

  const mockUser = {
    id: 1,
    username: 'testuser',
    email: 'test@example.com',
    password: 'hashedPassword',
    role: 'user',
    is_active: true,
    version: 0,
    created_at: new Date(),
    updated_at: new Date(),
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
            delete: jest.fn(),
          },
        },
      ],
    }).compile();

    service = module.get<UsersService>(UsersService);
    repository = module.get<UsersRepository>(UsersRepository);

    // Setup bcrypt mocks
    (bcrypt.hash as any) = jest.fn().mockResolvedValue('hashedPassword');
    (bcrypt.compare as any) = jest.fn().mockResolvedValue(true);
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
});