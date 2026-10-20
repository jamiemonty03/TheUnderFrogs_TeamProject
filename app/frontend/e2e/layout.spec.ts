import { Page, expect, test } from '@playwright/test';
import { useSavedLogin } from './fixtures';

const scrollsSideways = (page: Page) =>
  page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth);

test.describe('On a desktop', () => {
  test('there is one account avatar, at the right of the bar', async ({ page }) => {
    await useSavedLogin(page);

    await expect(page.getByTestId('account-menu')).toBeVisible();
    await expect(page.getByTestId('account-menu-mobile')).toBeHidden();
  });
});

test.describe('On a phone', () => {
  test.use({ viewport: { width: 375, height: 812 }, isMobile: true, hasTouch: true });

  test('the account avatar sits beside the menu button and opens the account menu', async ({
    page,
  }) => {
    await useSavedLogin(page);

    await expect(page.getByTestId('account-menu')).toBeHidden();
    await page.getByTestId('account-menu-mobile').click();
    await expect(page.getByTestId('logout')).toBeVisible();
  });

  for (const path of ['/dashboard', '/orders', '/positions', '/instruments', '/instruments/AAPL']) {
    test(`${path} does not scroll sideways`, async ({ page }) => {
      await useSavedLogin(page);
      await page.goto(path);
      // Let the tables and charts finish loading before measuring
      await page.waitForLoadState('networkidle');

      expect(await scrollsSideways(page)).toBe(false);
    });
  }
});
