import { jest } from '@jest/globals';
import { Test, TestingModule } from '@nestjs/testing';
import { UnauthorizedException, ExecutionContext } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { Reflector } from '@nestjs/core';
import { JwtAuthGuard } from '../guards/jwt-auth.guard';
import { IS_PUBLIC_KEY } from '../decorators/public.decorator';

describe('JwtAuthGuard', () => {
  let guard: JwtAuthGuard;
  let jwtService: JwtService;
  let reflector: Reflector;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        JwtAuthGuard,
        {
          provide: JwtService,
          useValue: {
            verify: jest.fn(),
          },
        },
        {
          provide: Reflector,
          useValue: {
            getAllAndOverride: jest.fn(),
          },
        },
      ],
    }).compile();

    guard = module.get<JwtAuthGuard>(JwtAuthGuard);
    jwtService = module.get<JwtService>(JwtService);
    reflector = module.get<Reflector>(Reflector);
  });

  describe('canActivate', () => {
    it('should allow access to public routes without token', () => {
      const context = createMockExecutionContext(null);
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(true);

      const result = guard.canActivate(context);

      expect(result).toBe(true);
    });

    it('should throw AUTH-401 when token is missing', () => {
      const context = createMockExecutionContext(null);
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(false);

      expect(() => guard.canActivate(context)).toThrow(
        expect.objectContaining({
          response: expect.objectContaining({
            error_code: 'AUTH-401',
            message: 'Missing authentication token',
          }),
        }),
      );
    });

    it('should throw AUTH-401 when token is invalid', () => {
      const context = createMockExecutionContext(
        'Bearer invalid.token.here',
      );
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(false);
      (jwtService.verify as jest.Mock).mockImplementation(() => {
        throw new Error('Invalid token');
      });

      expect(() => guard.canActivate(context)).toThrow(
        expect.objectContaining({
          response: expect.objectContaining({
            error_code: 'AUTH-401',
            message: 'Invalid or expired token',
          }),
        }),
      );
    });

    it('should allow access with valid token', () => {
      const payload = { sub: 1, username: 'testuser', roles: ['user'] };
      const context = createMockExecutionContext('Bearer valid.token.here');
      const request = context.switchToHttp().getRequest();

      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(false);
      (jwtService.verify as jest.Mock).mockReturnValue(payload);

      const result = guard.canActivate(context);

      expect(result).toBe(true);
      expect(request.user).toEqual(payload);
    });

    it('should throw AUTH-401 when token format is invalid', () => {
      const context = createMockExecutionContext('InvalidBearerFormat');
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(false);

      expect(() => guard.canActivate(context)).toThrow(
        expect.objectContaining({
          response: expect.objectContaining({
            error_code: 'AUTH-401',
          }),
        }),
      );
    });

    it('should throw AUTH-401 when scheme is not Bearer', () => {
      const context = createMockExecutionContext('Basic base64token');
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(false);

      expect(() => guard.canActivate(context)).toThrow(
        expect.objectContaining({
          response: expect.objectContaining({
            error_code: 'AUTH-401',
          }),
        }),
      );
    });
  });
});

function createMockExecutionContext(
  authorizationHeader: string | null,
): ExecutionContext {
  const request = {
    headers: {
      authorization: authorizationHeader,
    },
  };

  const context = {
    switchToHttp: () => ({
      getRequest: () => request,
    }),
    getHandler: () => jest.fn(),
    getClass: () => jest.fn(),
  } as unknown as ExecutionContext;

  return context;
}
