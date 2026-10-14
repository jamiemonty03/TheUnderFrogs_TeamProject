import { expect, test as setup } from '@playwright/test';
import { SAVED_SESSION, logIn } from './fixtures';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';

setup(
  'the demo trader logs in, lands on the dashboard, and the session is saved',
  async ({ page }) => {
    await logIn(page);
    await expect(page).toHaveURL(/\/dashboard$/);

    const session = await page.evaluate(() => sessionStorage.getItem('auth.session'));
    mkdirSync(dirname(SAVED_SESSION), { recursive: true });
    writeFileSync(SAVED_SESSION, session ?? '');
  },
);
