import { expect, test as setup } from '@playwright/test';
import { logIn, saveSession } from './fixtures';

setup(
  'the demo trader logs in, lands on the dashboard, and the session is saved',
  async ({ page }) => {
    await logIn(page);
    await expect(page).toHaveURL(/\/dashboard$/);

    await saveSession(page);
  },
);
