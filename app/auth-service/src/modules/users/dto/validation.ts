import { applyDecorators } from '@nestjs/common';
import { Transform } from 'class-transformer';
import { IsEmail, IsString, Matches, MaxLength, MinLength } from 'class-validator';

export const PASSWORD_MIN_LENGTH = 12;
export const PASSWORD_MAX_LENGTH = 128;
export const ROLES = ['USER', 'ADMIN'];

export const Trim = () => Transform(({ value }) => (typeof value === 'string' ? value.trim() : value));

export const NotBlank = () => Matches(/\S/, { message: '$property must not be blank' });

export const IsPassword = () =>
  applyDecorators(IsString(), NotBlank(), MinLength(PASSWORD_MIN_LENGTH), MaxLength(PASSWORD_MAX_LENGTH));

export const IsUsername = () =>
  applyDecorators(
    Trim(),
    IsString(),
    MinLength(3),
    MaxLength(50),
    Matches(/^[A-Za-z0-9._-]+$/, {
      message: '$property may only contain letters, numbers, dots, underscores and hyphens',
    }),
  );

export const IsEmailAddress = () => applyDecorators(Trim(), IsString(), IsEmail(), MaxLength(255));

export const IsFullName = () => applyDecorators(Trim(), IsString(), NotBlank(), MaxLength(255));

export const IsRole = () =>
  applyDecorators(
    Transform(({ value }) => (typeof value === 'string' ? value.trim().toUpperCase() : value)),
    IsString(),
    Matches(new RegExp(`^(${ROLES.join('|')})$`), { message: `$property must be one of: ${ROLES.join(', ')}` }),
  );
