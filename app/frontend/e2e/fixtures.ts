import { Page, expect } from '@playwright/test';

export const DEMO_USER = {
  username: process.env['E2E_USERNAME'] ?? 'demo',
  password: process.env['E2E_PASSWORD'] ?? 'Demo123!',
};

export async function logIn(page: Page, user = DEMO_USER): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Username').fill(user.username);
  await page.getByLabel('Password').fill(user.password);
  await page.getByRole('button', { name: /log in/i }).click();
  await expect(page).not.toHaveURL(/\/login/);
}
