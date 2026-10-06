import { NotFoundException, ConflictException, UnauthorizedException, ForbiddenException, UnprocessableEntityException } from '@nestjs/common';

export class ResourceNotFoundException extends NotFoundException {
  constructor(resource: string) {
    super(`${resource} not found`);
  }
}

export class ResourceConflictException extends ConflictException {
  constructor(message: string) {
    super(message);
  }
}

export class InvalidCredentialsException extends UnauthorizedException {
  constructor() {
    super('Invalid credentials');
  }
}

export const VALIDATION_ERROR_CODE = 'VAL-422';

export class ValidationException extends UnprocessableEntityException {
  constructor(message: string) {
    super({ errorCode: VALIDATION_ERROR_CODE, message });
  }
}

export class AccessForbiddenException extends ForbiddenException {
  constructor(message: string = 'Access forbidden') {
    super(message);
  }
}
