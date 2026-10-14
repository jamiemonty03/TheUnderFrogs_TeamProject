import { Page, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';

/** The seeded demo trader. Override with E2E_USERNAME / E2E_PASSWORD. */
export const DEMO_USER = {
  username: process.env['E2E_USERNAME'] ?? 'demo',
  password: process.env['E2E_PASSWORD'] ?? 'Demo123!',
};

/** Logs in through the login page, and waits until the app has moved off it. */
export async function logIn(page: Page, user = DEMO_USER): Promise<void> {
  await page.goto('/login');
  await page.getByTestId('login-username').fill(user.username);
  await page.getByTestId('login-password').fill(user.password);
  await page.getByTestId('login-submit').click();
  await expect(page).not.toHaveURL(/\/login/);
}

/** Where auth.setup.ts saves the logged-in session. */
export const SAVED_SESSION = 'playwright/.auth/session.json';

/**
 * Starts the test already logged in, using the session saved by auth.setup.ts, instead of
 * going through the login form again (auth-service only allows 5 logins a minute).
 */
export async function useSavedLogin(page: Page): Promise<void> {
  const session = readFileSync(SAVED_SESSION, 'utf8');
  // Put the session in sessionStorage before the app starts, just as the login page would
  await page.addInitScript((saved) => sessionStorage.setItem('auth.session', saved), session);
  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/dashboard$/);
}

/** The account's cash, read straight from accounts-service rather than from the page. */
export async function cashBalance(page: Page): Promise<number> {
  // After login the app keeps its token and account ID in sessionStorage, so reuse them
  const session = await page.evaluate(() =>
    JSON.parse(sessionStorage.getItem('auth.session') ?? '{}'),
  );
  const response = await page.request.get(`/api/accounts/${session.user.accountId}`, {
    headers: { Authorization: `Bearer ${session.accessToken}` },
  });
  expect(response.ok()).toBe(true);
  const account = await response.json();
  return account.cashBalance;
}
