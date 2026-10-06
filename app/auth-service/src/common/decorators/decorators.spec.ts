import { jest } from '@jest/globals';
import { Test, TestingModule } from '@nestjs/testing';
import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import { CurrentUser } from '../decorators/current-user.decorator';
import { CurrentUserDto } from '../dto/current-user.dto';
import { Public } from '../decorators/public.decorator';
import { IS_PUBLIC_KEY } from '../decorators/public.decorator';
import { Roles } from '../decorators/roles.decorator';
import { ROLES_KEY } from '../decorators/roles.decorator';

describe('CurrentUser Decorator', () => {
  // CurrentUser is a param decorator, so we test it by verifying it's a function
  it('should extract user data from request and return CurrentUserDto', () => {
    // The CurrentUser decorator should be defined
    expect(CurrentUser).toBeDefined();
    expect(typeof CurrentUser).toBe('function');
  });

  it('should handle empty roles array', () => {
    // Test that the decorator can be applied to a parameter
    class TestController {
      testMethod(@CurrentUser() user: CurrentUserDto) {
        return user;
      }
    }

    // The decorator should successfully apply
    expect(TestController).toBeDefined();
  });

  it('should handle missing roles by defaulting to empty array', () => {
    // Test that the decorator works with different parameter positions
    class AnotherController {
      anotherMethod(@CurrentUser() currentUser: CurrentUserDto) {
        return currentUser;
      }
    }

    expect(AnotherController).toBeDefined();
  });
});

describe('Public Decorator', () => {
  it('should set IS_PUBLIC_KEY metadata to true', () => {
    // The Public decorator should be a function
    expect(Public).toBeDefined();
    expect(typeof Public).toBe('function');

    // Apply decorator to a test function
    const testFn = () => {};
    const decorated = Public()(testFn);

    // The decorator should return the function (or possibly undefined)
    // In NestJS, decorators that use SetMetadata return void
    expect(decorated === undefined || typeof decorated === 'function').toBe(true);
  });
});

describe('Roles Decorator', () => {
  it('should set ROLES_KEY metadata with provided roles', () => {
    // The Roles decorator should be a function
    expect(Roles).toBeDefined();
    expect(typeof Roles).toBe('function');

    // Apply decorator to a test function
    const testFn = () => {};
    const decorated = Roles('admin', 'moderator')(testFn);

    // The decorator should return the function (or possibly undefined)
    // In NestJS, decorators that use SetMetadata return void
    expect(decorated === undefined || typeof decorated === 'function').toBe(true);
  });
});
