import { expect, test } from '@playwright/test';
import { cashBalance, useSavedLogin } from './fixtures';
import { sellFromHolding, waitForOrderStatus } from './orders';

test('a SELL from a holding fills, and the cash goes up', async ({ page }) => {
  test.setTimeout(90_000);

  await useSavedLogin(page);
  const cashBefore = await cashBalance(page);

  const orderId = await sellFromHolding(page, 'AAPL', 1);
  await waitForOrderStatus(page, orderId, 'FILLED');

  expect(await cashBalance(page)).toBeGreaterThan(cashBefore);
});
