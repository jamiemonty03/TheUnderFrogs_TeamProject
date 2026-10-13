export const DEFAULT_AFTER_LOGIN = '/dashboard';

export function safeReturnUrl(returnUrl: string | null | undefined): string {
  if (
    !returnUrl ||
    !returnUrl.startsWith('/') ||
    returnUrl.startsWith('//') ||
    returnUrl.startsWith('/\\') ||
    returnUrl === '/login' ||
    returnUrl.startsWith('/login?') ||
    returnUrl.startsWith('/login/')
  ) {
    return DEFAULT_AFTER_LOGIN;
  }
  return returnUrl;
}
