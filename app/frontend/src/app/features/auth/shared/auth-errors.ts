export const AUTH_ERRORS = {
  tooManyAttempts: 'Too many attempts. Wait a minute and try again.',
  unreachable: "Can't reach the sign-in service.",
  unexpected: 'Something went wrong. Try again.',
} as const;

export const UNREACHABLE_STATUSES = [0, 502, 503, 504];
