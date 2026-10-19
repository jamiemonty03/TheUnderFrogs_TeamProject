import { expect, test } from '@playwright/test';
import { cashBalance } from './fixtures';

test('a new trader registers and lands on their own empty dashboard', async ({ page }) => {
  const stamp = Date.now();
  const username = `e2e_${stamp}`;

  await page.goto('/register');
  await page.getByLabel('Full name').fill('Erin Twoe');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Email').fill(`e2e+${stamp}@example.com`);
  await page.getByLabel('Password').fill('E2e-Journey-Pass1');
  await page.getByRole('button', { name: 'Create trading account' }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByText('Welcome, Erin')).toBeVisible();
  await expect(page.getByText("You don't hold anything yet.").first()).toBeVisible();

  expect(await cashBalance(page)).toBeGreaterThanOrEqual(0);
});

test('registering with a username that is taken shows why, and stays on the form', async ({
  page,
}) => {
  await page.goto('/register');
  await page.getByLabel('Full name').fill('Not Demo');
  await page.getByLabel('Username').fill('demo');
  await page.getByLabel('Email').fill(`e2e+taken${Date.now()}@example.com`);
  await page.getByLabel('Password').fill('E2e-Journey-Pass1');
  await page.getByRole('button', { name: 'Create trading account' }).click();

  await expect(page.getByRole('alert')).toBeVisible();
  await expect(page).toHaveURL(/\/register$/);
});
