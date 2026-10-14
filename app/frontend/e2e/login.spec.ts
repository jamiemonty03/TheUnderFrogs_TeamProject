import { expect, test } from '@playwright/test';
import { DEMO_USER, logIn } from './fixtures';

test.describe('Logging in', () => {
  test('the demo trader logs in and lands on the dashboard', async ({ page }) => {
    await logIn(page);

    await expect(page).toHaveURL(/\/dashboard$/);
  });

  test('a wrong password shows an error and stays on the login page', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill(DEMO_USER.username);
    await page.getByLabel('Password').fill('definitely-not-the-password');
    await page.getByRole('button', { name: /log in/i }).click();

    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
  });

  test('after logging in, a page that needs the account loads its data', async ({ page }) => {
    await logIn(page);

    await page.getByRole('link', { name: 'Portfolios' }).click();

    await expect(page.getByTestId('portfolios-title')).toHaveText('All holdings');
    await expect(page.getByTestId('portfolio-all')).toBeVisible();
    await expect(page.getByTestId('portfolios-error')).toHaveCount(0);
  });
});
