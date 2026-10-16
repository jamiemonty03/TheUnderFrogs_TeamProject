import { ThrottlerModuleOptions } from '@nestjs/throttler';

export const ONE_MINUTE_MS = 60000;

export const throttleConfig: ThrottlerModuleOptions = [
  { ttl: ONE_MINUTE_MS, limit: 100 },
];

export const PASSWORD_THROTTLE = { default: { limit: 5, ttl: ONE_MINUTE_MS } };

export const REFRESH_THROTTLE = { default: { limit: 30, ttl: ONE_MINUTE_MS } };
