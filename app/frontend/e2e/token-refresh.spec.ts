import { Page, expect, test } from '@playwright/test';
import { logIn } from './fixtures';

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  user: { userId: number; username: string; roles: string[]; accountId: string | null } | null;
}

const SESSION_KEY = 'auth.session';

function expiredAccessToken(token = 'eyJhbGciOiJSUzI1NiJ9.e30.signature'): string {
  const [header, , signature] = token.split('.');
  const payload = Buffer.from(
    JSON.stringify({ sub: '11', exp: Math.floor(Date.now() / 1000) - 60 }),
  ).toString('base64url');
  return `${header}.${payload}.${signature}`;
}

async function storedSession(page: Page): Promise<StoredSession> {
  return page.evaluate((key) => JSON.parse(sessionStorage.getItem(key) ?? 'null'), SESSION_KEY);
}

async function saveSession(page: Page, session: StoredSession): Promise<void> {
  await page.evaluate(
    ([key, value]) => sessionStorage.setItem(key, value),
    [SESSION_KEY, JSON.stringify(session)],
  );
}

const refreshResponse = (page: Page) =>
  page.waitForResponse(
    (response) =>
      response.url().endsWith('/auth/refresh') && response.request().method() === 'POST',
  );

test.describe('Staying signed in past the 15-minute access token', () => {
  test('an expired access token is refreshed quietly and the page still loads its data', async ({
    page,
  }) => {
    await logIn(page);
    const before = await storedSession(page);
    const expired = expiredAccessToken(before.accessToken);
    await saveSession(page, { ...before, accessToken: expired });

    const refreshed = refreshResponse(page);
    await page.goto('/positions');

    expect((await refreshed).status()).toBe(200);
    await expect(page).toHaveURL(/\/positions$/);
    await expect(page.getByTestId('portfolios-title')).toHaveText('All holdings');
    await expect(page.getByTestId('portfolios-error')).toHaveCount(0);

    const after = await storedSession(page);
    expect(after.accessToken).not.toBe(expired);
    expect(after.refreshToken).not.toBe(before.refreshToken);
    expect(after.user).toEqual(before.user);
  });

  test('a refresh token the server rejects sends you to login with the expired message', async ({
    page,
  }) => {
    await page.goto('/login');
    await saveSession(page, {
      accessToken: expiredAccessToken(),
      refreshToken: 'not-a-real-refresh-token',
      user: { userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' },
    });

    const refreshed = refreshResponse(page);
    await page.goto('/orders');

    expect((await refreshed).status()).toBe(401);
    await expect(page).toHaveURL(/\/login\?returnUrl=(%2F|\/)orders&expired=1$/);
    await expect(page.getByTestId('login-expired')).toHaveText(
      'Your session has expired. Please log in again.',
    );
    expect(await storedSession(page)).toBeNull();
  });
});
