import { jest } from '@jest/globals';
// @ts-nocheck
import { Test, TestingModule } from '@nestjs/testing';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';

describe('AuthController', () => {
  let controller: AuthController;
  let authService: AuthService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [AuthController],
      providers: [
        {
          provide: AuthService,
          useValue: {
            register: jest.fn(),
            login: jest.fn(),
          },
        },
      ],
    }).compile();

    controller = module.get<AuthController>(AuthController);
    authService = module.get<AuthService>(AuthService);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('POST /auth/register', () => {
    it('should call authService.register with the provided DTO', async () => {
      const registerDto = {
        username: 'newuser',
        email: 'new@example.com',
        password: 'password123',
      };

      const mockToken = {
        accessToken: 'jwt.token',
        refreshToken: 'opaque-refresh',
        expiresIn: 900,
        mfaRequired: false,
      };

      // @ts-expect-error - Mock compatibility
      (authService.register as jest.Mock).mockResolvedValue(mockToken as any);

      const result = await controller.register(registerDto);

      expect(authService.register).toHaveBeenCalledWith(registerDto);
      expect(result).toEqual(mockToken);
    });
  });

  describe('POST /auth/login', () => {
    it('should call authService.login with the provided DTO', async () => {
      const loginDto = {
        username: 'testuser',
        password: 'password123',
      };

      const mockToken = {
        accessToken: 'jwt.token',
        refreshToken: 'opaque-refresh',
        expiresIn: 900,
        mfaRequired: false,
      };

      // @ts-expect-error - Mock compatibility
      (authService.login as jest.Mock).mockResolvedValue(mockToken as any);

      const result = await controller.login(loginDto);

      expect(authService.login).toHaveBeenCalledWith(loginDto);
      expect(result).toEqual(mockToken);
    });
  });

  describe('GET /auth/me', () => {
    it('should return current user information', async () => {
      const mockCurrentUser: any = {
        userId: 1,
        username: 'testuser',
        roles: ['user'],
        accountId: 'ACC-000001',
      };

      const result = await controller.getCurrentUser(mockCurrentUser);

      expect(result).toEqual(mockCurrentUser);
    });

    it('should handle user without accountId', async () => {
      const mockCurrentUser: any = {
        userId: 2,
        username: 'admin',
        roles: ['admin'],
        accountId: undefined,
      };

      const result = await controller.getCurrentUser(mockCurrentUser);

      expect(result).toEqual(mockCurrentUser);
    });
  });
});
