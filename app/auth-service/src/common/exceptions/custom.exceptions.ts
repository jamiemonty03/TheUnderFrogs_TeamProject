import { BadRequestException, NotFoundException, ConflictException, UnauthorizedException, ForbiddenException } from '@nestjs/common';

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

export class ValidationException extends BadRequestException {
  constructor(message: string) {
    super(message);
  }
}

export class AccessForbiddenException extends ForbiddenException {
  constructor(message: string = 'Access forbidden') {
    super(message);
  }
}
