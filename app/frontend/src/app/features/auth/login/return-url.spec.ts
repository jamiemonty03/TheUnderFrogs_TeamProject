import { DEFAULT_AFTER_LOGIN, safeReturnUrl } from './return-url';

describe('safeReturnUrl', () => {
  it.each(['/orders', '/orders?status=NEW', '/positions/ACC0011', '/dashboard'])(
    'keeps the in-app path %s',
    (url) => {
      expect(safeReturnUrl(url)).toBe(url);
    },
  );

  it.each([
    ['missing', null],
    ['empty', ''],
    ['another site', 'https://evil.example.com/steal'],
    ['a protocol-relative URL', '//evil.example.com'],
    ['a backslash trick browsers treat like //', '/\\evil.example.com'],
    ['a relative path', 'orders'],
    ['a javascript: URL', 'javascript:alert(1)'],
    ['the login page itself', '/login'],
    ['the login page with its own returnUrl', '/login?returnUrl=%2Forders'],
  ])('falls back to /dashboard for %s', (_label, url) => {
    expect(safeReturnUrl(url)).toBe(DEFAULT_AFTER_LOGIN);
  });

  it('defaults to /dashboard', () => {
    expect(DEFAULT_AFTER_LOGIN).toBe('/dashboard');
  });
});
