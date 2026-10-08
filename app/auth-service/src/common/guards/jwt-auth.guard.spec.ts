import { jest } from '@jest/globals';
import { Test, TestingModule } from '@nestjs/testing';
import { UnauthorizedException, ExecutionContext } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { generateKeyPairSync } from 'crypto';
import { TokenService } from '../../modules/tokens/token.service';
import { loadSigningKey } from '../../config/signing-key';
import { Reflector } from '@nestjs/core';
import { JwtAuthGuard } from '../guards/jwt-auth.guard';
import { IS_PUBLIC_KEY } from '../decorators/public.decorator';

describe('JwtAuthGuard', () => {
  let guard: JwtAuthGuard;
  let tokenService: TokenService;
  let reflector: Reflector;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        JwtAuthGuard,
        {
          provide: TokenService,
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
    tokenService = module.get<TokenService>(TokenService);
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
      (tokenService.verify as jest.Mock).mockImplementation(() => {
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
      const payload = { sub: '1', username: 'testuser', roles: ['TRADER'], accountId: null };
      const context = createMockExecutionContext('Bearer valid.token.here');
      const request = context.switchToHttp().getRequest();

      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(false);
      (tokenService.verify as jest.Mock).mockReturnValue(payload);

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

describe('JwtAuthGuard with the real TokenService', () => {
  const pem = generateKeyPairSync('rsa', { modulusLength: 2048 }).privateKey.export({ type: 'pkcs8', format: 'pem' });
  const key = loadSigningKey(Buffer.from(pem.toString()).toString('base64'), 'auth-key-1');
  const config = { get: (_name: string, fallback: unknown) => fallback } as unknown as ConfigService;
  const tokenService = new TokenService(key, config);
  const reflector = { getAllAndOverride: () => false } as unknown as Reflector;
  const guard = new JwtAuthGuard(tokenService, reflector);

  it('accepts an RS256 token issued by auth-service and sets request.user', () => {
    const { accessToken } = tokenService.issue({ id: 5, username: 'demo', roles: ['TRADER'], account_id: 'ACC0011' });
    const context = createMockExecutionContext(`Bearer ${accessToken}`);

    expect(guard.canActivate(context)).toBe(true);
    expect(context.switchToHttp().getRequest().user).toMatchObject({ sub: '5', username: 'demo', accountId: 'ACC0011' });
  });

  it('rejects an old HS256 token signed with a shared secret', () => {
    const oldToken = new JwtService().sign(
      { sub: 5, username: 'demo', roles: ['ADMIN'] },
      { secret: 'x'.repeat(32), expiresIn: 3600 },
    );

    expect(() => guard.canActivate(createMockExecutionContext(`Bearer ${oldToken}`))).toThrow(
      expect.objectContaining({ response: expect.objectContaining({ message: 'Invalid or expired token' }) }),
    );
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
