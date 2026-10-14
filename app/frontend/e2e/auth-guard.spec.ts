import { expect, test } from '@playwright/test';
import { DEMO_USER, logIn } from './fixtures';

test.describe('Pages that need a login', () => {
  test('a protected page sends you to login, then back to that page once logged in', async ({
    page,
  }) => {
    await page.goto('/orders');

    await expect(page).toHaveURL(/\/login\?returnUrl=(%2F|\/)orders$/);

    await page.getByTestId('login-username').fill(DEMO_USER.username);
    await page.getByTestId('login-password').fill(DEMO_USER.password);
    await page.getByTestId('login-submit').click();

    await expect(page).toHaveURL(/\/orders$/);
  });

  test('logging out returns to login, and the dashboard is then off limits', async ({ page }) => {
    await logIn(page);

    await page.getByTestId('account-menu').click();
    await page.getByTestId('logout').click();
    await expect(page).toHaveURL(/\/login/);

    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/);
  });
});
