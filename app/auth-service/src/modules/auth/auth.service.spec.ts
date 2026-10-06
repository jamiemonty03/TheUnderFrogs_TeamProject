import { Test, TestingModule } from '@nestjs/testing';
import { UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { AuthService } from './auth.service';
import { AuthRepository } from './auth.repository';
import { UsersService } from '../users/users.service';

describe('AuthService', () => {
  let service: AuthService;
  let usersService: UsersService;
  let authRepository: AuthRepository;
  let jwtService: JwtService;

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
        AuthService,
        {
          provide: UsersService,
          useValue: {
            createUser: jest.fn(),
            verifyCredentials: jest.fn(),
          },
        },
        {
          provide: AuthRepository,
          useValue: {
            update: jest.fn(),
          },
        },
        {
          provide: JwtService,
          useValue: {
            sign: jest.fn().mockReturnValue('jwt_token_here'),
            verify: jest.fn(),
          },
        },
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn((key, defaultValue) => {
              if (key === 'JWT_EXPIRATION') return 86400000;
              return defaultValue;
            }),
          },
        },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    usersService = module.get<UsersService>(UsersService);
    authRepository = module.get<AuthRepository>(AuthRepository);
    jwtService = module.get<JwtService>(JwtService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('register', () => {
    it('should register a new user and return token', async () => {
      const registerDto = {
        username: 'newuser',
        email: 'new@example.com',
        password: 'password123',
      };

      jest.spyOn(usersService, 'createUser').mockResolvedValue(mockUser);

      const result = await service.register(registerDto);

      expect(result).toHaveProperty('access_token');
      expect(result).toHaveProperty('token_type');
      expect(result.token_type).toBe('Bearer');
      expect(result).toHaveProperty('expires_in');
      expect(usersService.createUser).toHaveBeenCalledWith({
        username: registerDto.username,
        email: registerDto.email,
        password: registerDto.password,
        role: 'USER',
      });
    });
  });

  describe('login', () => {
    it('should login user and return token', async () => {
      const loginDto = {
        username: 'testuser',
        password: 'password123',
      };

      jest.spyOn(usersService, 'verifyCredentials').mockResolvedValue(mockUser);
      jest.spyOn(authRepository, 'update').mockResolvedValue(null);

      const result = await service.login(loginDto);

      expect(result).toHaveProperty('access_token');
      expect(result.token_type).toBe('Bearer');
      expect(usersService.verifyCredentials).toHaveBeenCalledWith('testuser', 'password123');
      expect(authRepository.update).toHaveBeenCalled();
    });

    it('should throw UnauthorizedException for invalid password', async () => {
      const loginDto = {
        username: 'testuser',
        password: 'wrongpassword',
      };

      jest.spyOn(usersService, 'verifyCredentials').mockResolvedValue(null);

      await expect(service.login(loginDto)).rejects.toThrow(UnauthorizedException);
      expect(authRepository.update).not.toHaveBeenCalled();
    });

    it('should give the same error for an unknown username as for a wrong password', async () => {
      jest.spyOn(usersService, 'verifyCredentials').mockResolvedValue(null);

      await expect(service.login({ username: 'ghost', password: 'whatever' })).rejects.toThrow('Invalid credentials');
    });

    it('should throw UnauthorizedException if user is inactive', async () => {
      const loginDto = {
        username: 'testuser',
        password: 'password123',
      };

      const inactiveUser = { ...mockUser, is_active: false };

      jest.spyOn(usersService, 'verifyCredentials').mockResolvedValue(inactiveUser);

      await expect(service.login(loginDto)).rejects.toThrow(UnauthorizedException);
    });
  });

  describe('validateToken', () => {
    it('should validate a token successfully', async () => {
      const token = 'valid_token';
      const decoded = { sub: 1, username: 'testuser', roles: ['user'] };

      jest.spyOn(jwtService, 'verify').mockReturnValue(decoded);

      const result = await service.validateToken(token);

      expect(result).toEqual(decoded);
      expect(jwtService.verify).toHaveBeenCalledWith(token);
    });

    it('should throw UnauthorizedException for invalid token', async () => {
      const token = 'invalid_token';

      jest.spyOn(jwtService, 'verify').mockImplementation(() => {
        throw new Error('Invalid token');
      });

      await expect(service.validateToken(token)).rejects.toThrow(UnauthorizedException);
    });
  });
});