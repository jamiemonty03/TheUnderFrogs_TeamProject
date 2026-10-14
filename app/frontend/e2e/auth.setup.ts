import { test as setup } from '@playwright/test';
import { SAVED_SESSION, logIn } from './fixtures';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';

// Runs before the other tests: logs in through the real login page once, then saves the
// session the app keeps in sessionStorage, so other tests can start already logged in.
setup('log in once and save the session', async ({ page }) => {
  await logIn(page);

  const session = await page.evaluate(() => sessionStorage.getItem('auth.session'));
  mkdirSync(dirname(SAVED_SESSION), { recursive: true });
  writeFileSync(SAVED_SESSION, session ?? '');
});
