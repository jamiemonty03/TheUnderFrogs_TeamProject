import { Page, expect } from '@playwright/test';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';

/** The seeded demo admin. Override with E2E_USERNAME / E2E_PASSWORD. */
export const DEMO_USER = {
  username: process.env['E2E_USERNAME'] ?? 'demo',
  password: process.env['E2E_PASSWORD'] ?? 'Demo123!',
};

export async function logIn(page: Page, user = DEMO_USER): Promise<void> {
  await page.goto('/login');
  await page.getByTestId('login-username').fill(user.username);
  await page.getByTestId('login-password').fill(user.password);
  await page.getByTestId('login-submit').click();
  await expect(page).not.toHaveURL(/\/login/);
}

export const SAVED_SESSION = 'playwright/.auth/session.json';

export async function saveSession(page: Page): Promise<void> {
  const session = await page.evaluate(() => sessionStorage.getItem('auth.session'));
  mkdirSync(dirname(SAVED_SESSION), { recursive: true });
  writeFileSync(SAVED_SESSION, session ?? '');
}

export async function useSavedLogin(page: Page): Promise<void> {
  const saved = existsSync(SAVED_SESSION) ? readFileSync(SAVED_SESSION, 'utf8') : '';
  if (!stillValid(saved)) {
    await logIn(page);
    await saveSession(page);
    return;
  }
  await page.addInitScript((session) => {
    if (!sessionStorage.getItem('e2e.seeded')) {
      sessionStorage.setItem('auth.session', session);
      sessionStorage.setItem('e2e.seeded', 'yes');
    }
  }, saved);
  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/dashboard$/);
}

function stillValid(saved: string): boolean {
  try {
    const token: string = JSON.parse(saved).accessToken;
    const payload = JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString());
    return payload.exp * 1000 > Date.now() + 60_000;
  } catch {
    return false;
  }
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
