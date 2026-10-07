import { jest } from '@jest/globals';
// @ts-nocheck
import { Test, TestingModule } from '@nestjs/testing';
import { ConflictException, UnauthorizedException, HttpException, HttpStatus } from '@nestjs/common';
import { AuthService } from './auth.service';
import { AuthRepository } from './auth.repository';
import { UsersService } from '../users/users.service';
import { TokenService } from '../tokens/token.service';
import { RefreshTokensService } from '../tokens/refresh-tokens.service';
import { AccountsServiceClient } from './services/accounts-service-client';

describe('AuthService', () => {
  let service: AuthService;
  let usersService: any;
  let authRepository: any;
  let tokenService: any;
  let refreshTokensService: any;
  let accountsServiceClient: any;

  const mockUser = {
    id: 1,
    username: 'testuser',
    email: 'test@example.com',
    password_hash: 'hashedPassword',
    full_name: 'Test User',
    roles: ['TRADER'],
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
            recordLogin: jest.fn() as any,
          } as any,
        },
        {
          provide: TokenService,
          useValue: {
            issue: jest.fn().mockReturnValue({ accessToken: 'rs256.token.here', expiresIn: 900 }) as any,
            verify: jest.fn() as any,
          } as any,
        },
        {
          provide: RefreshTokensService,
          useValue: {
            issue: jest.fn().mockResolvedValue({ refreshToken: 'opaque-refresh', expiresAt: new Date() }) as any,
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
    tokenService = module.get<any>(TokenService);
    refreshTokensService = module.get<any>(RefreshTokensService);
    accountsServiceClient = module.get<any>(AccountsServiceClient);
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
        role: 'TRADER',
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

  describe('register', () => {
    const registerDto = { username: 'newuser', email: 'new@example.com', password: 'correct-horse-battery' };

    it('creates the user with the TRADER role and returns a token for them', async () => {
      const created = { ...mockUser, id: 9, username: 'newuser', roles: ['TRADER'] };
      (usersService.createUser as jest.Mock).mockResolvedValue(created as any);

      const result = await service.register(registerDto);

      expect(usersService.createUser).toHaveBeenCalledWith(expect.objectContaining({ username: 'newuser', role: 'TRADER' }));
      expect(tokenService.issue).toHaveBeenCalledWith(created);
      expect(refreshTokensService.issue).toHaveBeenCalledWith(9);
      expect(result).toEqual({ accessToken: 'rs256.token.here', refreshToken: 'opaque-refresh', expiresIn: 900, mfaRequired: false });
    });

    it('passes a taken username or email straight through as a conflict', async () => {
      (usersService.createUser as jest.Mock).mockRejectedValue(new ConflictException('User with username newuser already exists') as any);

      await expect(service.register(registerDto)).rejects.toThrow(ConflictException);
      expect(tokenService.issue).not.toHaveBeenCalled();
    });
  });

  describe('login', () => {
    const loginDto = { username: 'testuser', password: 'password123' };

    it('returns an access token, a refresh token, the lifetime and mfaRequired false', async () => {
      (usersService.verifyCredentials as jest.Mock).mockResolvedValue(mockUser as any);

      const result = await service.login(loginDto);

      expect(result).toEqual({ accessToken: 'rs256.token.here', refreshToken: 'opaque-refresh', expiresIn: 900, mfaRequired: false });
      expect(usersService.verifyCredentials).toHaveBeenCalledWith('testuser', 'password123');
      expect(tokenService.issue).toHaveBeenCalledWith(mockUser);
      expect(refreshTokensService.issue).toHaveBeenCalledWith(1);
      expect(authRepository.recordLogin).toHaveBeenCalledWith(1);
    });

    it.each([
      ['an unknown username', null],
      ['a wrong password', null],
      ['an inactive account', { ...mockUser, is_active: false }],
    ])('rejects %s with the same generic error and issues nothing', async (_label, verified) => {
      (usersService.verifyCredentials as jest.Mock).mockResolvedValue(verified as any);

      await expect(service.login(loginDto)).rejects.toThrow(new UnauthorizedException('Invalid credentials'));
      expect(tokenService.issue).not.toHaveBeenCalled();
      expect(refreshTokensService.issue).not.toHaveBeenCalled();
      expect(authRepository.recordLogin).not.toHaveBeenCalled();
    });
  });

  describe('validateToken', () => {
    it('should validate a token successfully', async () => {
      const token = 'valid_token';
      const decoded = { sub: '1', username: 'testuser', roles: ['TRADER'], accountId: 'ACC0001' };

      (tokenService.verify as jest.Mock).mockReturnValue(decoded as any);

      const result = await service.validateToken(token);

      expect(result).toEqual(decoded);
      expect(tokenService.verify).toHaveBeenCalledWith(token);
    });

    it('should throw UnauthorizedException for invalid token', async () => {
      const token = 'invalid_token';

      (tokenService.verify as jest.Mock).mockImplementation(() => {
        throw new UnauthorizedException('Invalid or expired token');
      });

      await expect(service.validateToken(token)).rejects.toThrow(UnauthorizedException);
    });
  });
});
