import { jest } from '@jest/globals';
import { Test, TestingModule } from '@nestjs/testing';
import { ForbiddenException, ExecutionContext } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { RolesGuard } from '../guards/roles.guard';

describe('RolesGuard', () => {
  let guard: RolesGuard;
  let reflector: Reflector;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        RolesGuard,
        {
          provide: Reflector,
          useValue: {
            getAllAndOverride: jest.fn(),
          },
        },
      ],
    }).compile();

    guard = module.get<RolesGuard>(RolesGuard);
    reflector = module.get<Reflector>(Reflector);
  });

  describe('canActivate', () => {
    it('should allow access when no roles are required', () => {
      const context = createMockExecutionContext(
        { sub: 1, username: 'testuser', roles: ['user'] },
        null,
      );
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(null);

      const result = guard.canActivate(context);

      expect(result).toBe(true);
    });

    it('should allow access when user has required role', () => {
      const context = createMockExecutionContext(
        { sub: 1, username: 'testuser', roles: ['admin'] },
        ['admin'],
      );
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(['admin']);

      const result = guard.canActivate(context);

      expect(result).toBe(true);
    });

    it('should allow access when user has one of multiple required roles', () => {
      const context = createMockExecutionContext(
        { sub: 1, username: 'testuser', roles: ['moderator'] },
        ['admin', 'moderator'],
      );
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue([
        'admin',
        'moderator',
      ]);

      const result = guard.canActivate(context);

      expect(result).toBe(true);
    });

    it('should throw AUTH-403 when user lacks required role', () => {
      const context = createMockExecutionContext(
        { sub: 1, username: 'testuser', roles: ['user'] },
        ['admin'],
      );
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(['admin']);

      expect(() => guard.canActivate(context)).toThrow(
        expect.objectContaining({
          response: expect.objectContaining({
            error_code: 'AUTH-403',
            message: expect.stringContaining('Required roles: admin'),
          }),
        }),
      );
    });

    it('should throw AUTH-403 when user has none of multiple required roles', () => {
      const context = createMockExecutionContext(
        { sub: 1, username: 'testuser', roles: ['user'] },
        ['admin', 'moderator'],
      );
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue([
        'admin',
        'moderator',
      ]);

      expect(() => guard.canActivate(context)).toThrow(
        expect.objectContaining({
          response: expect.objectContaining({
            error_code: 'AUTH-403',
          }),
        }),
      );
    });

    it('should throw AUTH-403 when user object is missing', () => {
      const context = createMockExecutionContext(null, ['admin']);
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(['admin']);

      expect(() => guard.canActivate(context)).toThrow(
        expect.objectContaining({
          response: expect.objectContaining({
            error_code: 'AUTH-403',
            message: 'User not found in request',
          }),
        }),
      );
    });

    it('should throw AUTH-403 when user has no roles array', () => {
      const context = createMockExecutionContext(
        { sub: 1, username: 'testuser' },
        ['admin'],
      );
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue(['admin']);

      expect(() => guard.canActivate(context)).toThrow(
        expect.objectContaining({
          response: expect.objectContaining({
            error_code: 'AUTH-403',
          }),
        }),
      );
    });

    it('should allow access when required roles array is empty', () => {
      const context = createMockExecutionContext(
        { sub: 1, username: 'testuser', roles: ['user'] },
        [],
      );
      (reflector.getAllAndOverride as jest.Mock).mockReturnValue([]);

      const result = guard.canActivate(context);

      expect(result).toBe(true);
    });
  });
});

function createMockExecutionContext(
  user: any,
  requiredRoles: string[] | null,
): ExecutionContext {
  const request = {
    user,
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
