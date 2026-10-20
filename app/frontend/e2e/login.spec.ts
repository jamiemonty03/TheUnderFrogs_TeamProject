import { expect, test } from '@playwright/test';
import { DEMO_USER, useSavedLogin } from './fixtures';

test.describe('Logging in', () => {
  test('the login page loads', async ({ page }) => {
    await page.goto('/login');

    await expect(page.getByTestId('login-username')).toBeVisible();
    await expect(page.getByTestId('login-password')).toBeVisible();
    await expect(page.getByTestId('login-submit')).toBeEnabled();
  });

  test("the app's stylesheet applies, rather than staying print-only", async ({ page }) => {
    await page.goto('/login');

    await expect(page.locator('link[rel="stylesheet"][media="print"]')).toHaveCount(0);
    const loaded = await page.evaluate(() =>
      Array.from(document.styleSheets).some((sheet) =>
        Array.from(sheet.cssRules).some((rule) => rule.cssText.startsWith('.pos')),
      ),
    );
    expect(loaded).toBe(true);
  });

  test('a wrong password shows an error and stays on the login page', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('login-username').fill(DEMO_USER.username);
    await page.getByTestId('login-password').fill('definitely-not-the-password');
    await page.getByTestId('login-submit').click();

    await expect(page.getByTestId('login-error')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test('after logging in, a page that needs the account loads its data', async ({ page }) => {
    await useSavedLogin(page);

    await page.getByTestId('nav-portfolios').click();

    await expect(page.getByTestId('portfolios-title')).toHaveText('All holdings');
    await expect(page.getByTestId('portfolio-all')).toBeVisible();
    await expect(page.getByTestId('portfolios-error')).toHaveCount(0);
  });
});
