import { ValidationError, ValidationPipe } from '@nestjs/common';
import { ValidationException } from '../exceptions/custom.exceptions';

export function firstValidationMessage(errors: ValidationError[]): string {
  for (const error of errors) {
    const message = Object.values(error.constraints ?? {})[0] ?? firstValidationMessage(error.children ?? []);
    if (message) {
      return message;
    }
  }
  return '';
}

export function createValidationPipe(): ValidationPipe {
  return new ValidationPipe({
    whitelist: true,
    forbidNonWhitelisted: true,
    transform: true,
    exceptionFactory: (errors) => new ValidationException(firstValidationMessage(errors) || 'Validation failed'),
  });
}
