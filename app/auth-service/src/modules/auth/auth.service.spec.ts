import { jest } from '@jest/globals';
// @ts-nocheck
import { Test, TestingModule } from '@nestjs/testing';
import { UnauthorizedException, HttpException, HttpStatus } from '@nestjs/common';
import { AuthService } from './auth.service';
import { AuthRepository } from './auth.repository';
import { UsersService } from '../users/users.service';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { AccountsServiceClient } from './services/accounts-service-client';

describe('AuthService', () => {
  let service: AuthService;
  let usersService: any;
  let authRepository: any;
  let jwtService: any;
  let accountsServiceClient: any;
  let configService: any;

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

  const mockAuth = {
    id: 1,
    user_id: 1,
    last_login: new Date().toISOString(),
    is_2fa_enabled: false,
    failed_login_attempts: 0,
    created_at: new Date(),
    updated_at: new Date(),
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
            updateUser: jest.fn(),
            deleteUser: jest.fn(),
          },
        },
        {
          provide: AuthRepository,
          useValue: {
            update: jest.fn() as any,
          } as any,
        },
        {
          provide: JwtService,
          useValue: {
            sign: jest.fn().mockReturnValue('jwt_token_here') as any,
            verify: jest.fn() as any,
          } as any,
        },
        {
          provide: ConfigService,
          useValue: {
            get: jest.fn((key, defaultValue) => {
              if (key === 'JWT_EXPIRATION') return 86400000;
              return defaultValue;
            }) as any,
          } as any,
        },
        {
          provide: AccountsServiceClient,
          useValue: {
            createAccount: jest.fn() as any,
            getAccountByUserId: jest.fn() as any,
          } as any,
        },
      ],
    }).compile();

    service = module.get<AuthService>(AuthService);
    usersService = module.get<any>(UsersService);
    authRepository = module.get<any>(AuthRepository);
    jwtService = module.get<any>(JwtService);
    accountsServiceClient = module.get<any>(AccountsServiceClient);
    configService = module.get<any>(ConfigService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe.skip('register', () => {
    it('should successfully register a user with an account', async () => {
      const registerDto = {
        username: 'newuser',
        email: 'new@example.com',
        password: 'password123',
      };

      const newUser = { ...mockUser, id: 2, username: 'newuser', email: 'new@example.com', account_id: null };
      const mockAccount = {
        accountId: 'ACC-000002',
        userId: 2,
        holderName: 'newuser',
        cashBalance: '0',
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      };

      (usersService.createUser as jest.Mock).mockResolvedValue(newUser as any);
      (accountsServiceClient.createAccount as jest.Mock).mockResolvedValue(mockAccount as any);
      (usersService.updateUser as jest.Mock).mockResolvedValue({ ...newUser, account_id: 'ACC-000002' } as any);

      const result = await service.register(registerDto);

      expect(result).toHaveProperty('access_token');
      expect(result.token_type).toBe('Bearer');
      expect(usersService.createUser).toHaveBeenCalledWith({
        username: registerDto.username,
        email: registerDto.email,
        password: registerDto.password,
        role: 'USER',
      });
      expect(accountsServiceClient.createAccount).toHaveBeenCalledWith({
        userId: 2,
        holderName: 'newuser',
      });
      expect(usersService.updateUser).toHaveBeenCalledWith(2, { account_id: 'ACC-000002' });
    });

    it('should include accountId in token payload on successful registration', async () => {
      const registerDto = {
        username: 'newuser',
        email: 'new@example.com',
        password: 'password123',
      };

      const newUser = { ...mockUser, id: 42, username: 'newuser', email: 'new@example.com', account_id: null };
      const mockAccount = {
        accountId: 'ACC-000042',
        userId: 42,
        holderName: 'newuser',
        cashBalance: '0',
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      };

      (usersService.createUser as jest.Mock).mockResolvedValue(newUser as any);
      (accountsServiceClient.createAccount as jest.Mock).mockResolvedValue(mockAccount as any);
      (usersService.updateUser as jest.Mock).mockResolvedValue({ ...newUser, account_id: 'ACC-000042' } as any);

      await service.register(registerDto);

      const signCall = (jwtService.sign as jest.Mock).mock.calls[0];
      expect(signCall[0]).toMatchObject({
        sub: 42,
        username: 'newuser',
        roles: ['user'],
        accountId: 'ACC-000042',
      });
    });

    it('should rollback user creation if account creation fails', async () => {
      const registerDto = {
        username: 'newuser',
        email: 'new@example.com',
        password: 'password123',
      };

      const newUser = { ...mockUser, id: 3, username: 'newuser', email: 'new@example.com', account_id: null };

      (usersService.createUser as jest.Mock).mockResolvedValue(newUser as any);
      (accountsServiceClient.createAccount as jest.Mock).mockRejectedValue(
        new Error('Accounts service is down') as any,
      );
      (usersService.deleteUser as jest.Mock).mockResolvedValue(undefined as any);

      await expect(service.register(registerDto)).rejects.toThrow(HttpException);

      expect(usersService.deleteUser).toHaveBeenCalledWith(3);
      expect(usersService.updateUser).not.toHaveBeenCalled();
    });

    it('should throw HttpException with error code REGISTRATION-500 on account creation failure', async () => {
      const registerDto = {
        username: 'newuser',
        email: 'new@example.com',
        password: 'password123',
      };

      const newUser = { ...mockUser, id: 4, username: 'newuser', email: 'new@example.com', account_id: null };

      (usersService.createUser as jest.Mock).mockResolvedValue(newUser as any);
      (accountsServiceClient.createAccount as jest.Mock).mockRejectedValue(
        new Error('Service error') as any,
      );
      (usersService.deleteUser as jest.Mock).mockResolvedValue(undefined as any);

      try {
        await service.register(registerDto);
        fail('Should have thrown an exception');
      } catch (error) {
        expect(error).toBeInstanceOf(HttpException);
        expect(error.getStatus()).toBe(HttpStatus.INTERNAL_SERVER_ERROR);
        const response = error.getResponse() as any;
        expect(response.error_code).toBe('REGISTRATION-500');
      }
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
      const decoded = { sub: 1, username: 'testuser', roles: ['user'], accountId: 'ACC-000001' };

      (jwtService.verify as jest.Mock).mockReturnValue(decoded as any);

      const result = await service.validateToken(token);

      expect(result).toEqual(decoded);
      expect(jwtService.verify).toHaveBeenCalledWith(token);
    });

    it('should throw UnauthorizedException for invalid token', async () => {
      const token = 'invalid_token';

      (jwtService.verify as jest.Mock).mockImplementation(() => {
        throw new Error('Invalid token');
      });

      await expect(service.validateToken(token)).rejects.toThrow(UnauthorizedException);
    });
  });
});
