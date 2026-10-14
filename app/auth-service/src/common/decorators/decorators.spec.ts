import { ExecutionContext } from '@nestjs/common';
import { ROUTE_ARGS_METADATA } from '@nestjs/common/constants';
import { Reflector } from '@nestjs/core';
import { CurrentUser } from './current-user.decorator';
import { CurrentUserDto } from '../dtos/current-user.dto';
import { Public, IS_PUBLIC_KEY } from './public.decorator';
import { Roles, ROLES_KEY } from './roles.decorator';

function currentUserFrom(user: Record<string, unknown>): CurrentUserDto {
  class TestController {
    handler(@CurrentUser() current: CurrentUserDto) {
      return current;
    }
  }
  const args = Reflect.getMetadata(ROUTE_ARGS_METADATA, TestController, 'handler');
  const { factory } = Object.values(args)[0] as { factory: (data: unknown, ctx: ExecutionContext) => CurrentUserDto };
  const ctx = { switchToHttp: () => ({ getRequest: () => ({ user }) }) } as unknown as ExecutionContext;
  return factory(undefined, ctx);
}

describe('CurrentUser decorator', () => {
  it('maps the verified JWT claims on the request to a CurrentUserDto', () => {
    expect(currentUserFrom({ sub: '11', username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' })).toEqual({
      userId: 11,
      username: 'demo',
      roles: ['TRADER'],
      accountId: 'ACC0011',
    });
  });

  it('defaults roles to an empty array when the token has none', () => {
    expect(currentUserFrom({ sub: '3', username: 'norole' }).roles).toEqual([]);
  });
});

describe('Public decorator', () => {
  it('marks a handler as public under IS_PUBLIC_KEY', () => {
    class TestController {
      @Public()
      open() {}

      closed() {}
    }

    const reflector = new Reflector();
    expect(reflector.get(IS_PUBLIC_KEY, TestController.prototype.open)).toBe(true);
    expect(reflector.get(IS_PUBLIC_KEY, TestController.prototype.closed)).toBeUndefined();
  });
});

describe('Roles decorator', () => {
  it('stores the required roles under ROLES_KEY', () => {
    class TestController {
      @Roles('ADMIN', 'TRADER')
      guarded() {}
    }

    expect(new Reflector().get(ROLES_KEY, TestController.prototype.guarded)).toEqual(['ADMIN', 'TRADER']);
  });
});
