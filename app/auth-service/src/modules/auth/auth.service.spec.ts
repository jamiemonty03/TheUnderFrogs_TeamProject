import { jest } from '@jest/globals';
// @ts-nocheck
import { Test, TestingModule } from '@nestjs/testing';
import { ConflictException, ServiceUnavailableException, UnauthorizedException } from '@nestjs/common';
import { AuthService, REGISTRATION_FAILED } from './auth.service';
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
            linkAccount: jest.fn(),
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

  describe('register', () => {
    const registerDto = { username: 'zed', email: 'zed@example.com', password: 'correct-horse-battery', full_name: 'Zed Smith' };
    const created = { ...mockUser, id: 12, username: 'zed', full_name: 'Zed Smith', roles: ['TRADER'], account_id: null };
    const linked = { ...created, account_id: 'ACC0012' };

    beforeEach(() => {
      (usersService.createUser as jest.Mock).mockResolvedValue(created as any);
      (accountsServiceClient.createAccount as jest.Mock).mockResolvedValue({ accountId: 'ACC0012', userId: 12 } as any);
      (usersService.linkAccount as jest.Mock).mockResolvedValue(linked as any);
    });

    it('creates the user with the TRADER role', async () => {
      await service.register(registerDto);

      expect(usersService.createUser).toHaveBeenCalledWith(expect.objectContaining({ username: 'zed', role: 'TRADER' }));
    });

    it('creates an account in accounts-service for the new user, named after their full name', async () => {
      await service.register(registerDto);

      expect(accountsServiceClient.createAccount).toHaveBeenCalledWith({ userId: 12, holderName: 'Zed Smith' });
    });

    it('names the account after the username when there is no full name', async () => {
      (usersService.createUser as jest.Mock).mockResolvedValue({ ...created, full_name: null } as any);

      await service.register({ ...registerDto, full_name: undefined });

      expect(accountsServiceClient.createAccount).toHaveBeenCalledWith({ userId: 12, holderName: 'zed' });
    });

    it('links the account to the user and issues tokens that carry the account ID', async () => {
      const result = await service.register(registerDto);

      expect(usersService.linkAccount).toHaveBeenCalledWith(12, 'ACC0012');
      expect(tokenService.issue).toHaveBeenCalledWith(linked);
      expect(refreshTokensService.issue).toHaveBeenCalledWith(12);
      expect(result).toEqual({ accessToken: 'rs256.token.here', refreshToken: 'opaque-refresh', expiresIn: 900, mfaRequired: false });
    });

    it('passes a taken username or email straight through as a conflict without creating an account', async () => {
      (usersService.createUser as jest.Mock).mockRejectedValue(new ConflictException('User with username zed already exists') as any);

      await expect(service.register(registerDto)).rejects.toThrow(ConflictException);
      expect(accountsServiceClient.createAccount).not.toHaveBeenCalled();
      expect(tokenService.issue).not.toHaveBeenCalled();
    });

    it.each([
      ['accounts-service is down', new ServiceUnavailableException('Account service is unavailable, please try again')],
      ['the account ID already exists', new ConflictException('Account ACC0012 already exists')],
    ])('deletes the new user and returns 503 when %s', async (_label, failure) => {
      (accountsServiceClient.createAccount as jest.Mock).mockRejectedValue(failure as any);

      await expect(service.register(registerDto)).rejects.toThrow(new ServiceUnavailableException(REGISTRATION_FAILED));
      expect(usersService.deleteUser).toHaveBeenCalledWith(12);
      expect(usersService.linkAccount).not.toHaveBeenCalled();
      expect(tokenService.issue).not.toHaveBeenCalled();
      expect(refreshTokensService.issue).not.toHaveBeenCalled();
    });

    it('deletes the new user and returns 503 when the account cannot be linked', async () => {
      (usersService.linkAccount as jest.Mock).mockRejectedValue(new Error('connection lost') as any);

      await expect(service.register(registerDto)).rejects.toThrow(new ServiceUnavailableException(REGISTRATION_FAILED));
      expect(usersService.deleteUser).toHaveBeenCalledWith(12);
      expect(tokenService.issue).not.toHaveBeenCalled();
    });

    it('still returns 503 when deleting the unfinished user also fails', async () => {
      (accountsServiceClient.createAccount as jest.Mock).mockRejectedValue(new ServiceUnavailableException() as any);
      (usersService.deleteUser as jest.Mock).mockRejectedValue(new Error('database down') as any);

      await expect(service.register(registerDto)).rejects.toThrow(new ServiceUnavailableException(REGISTRATION_FAILED));
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
