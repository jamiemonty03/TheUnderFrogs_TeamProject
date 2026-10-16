import { expect, test } from '@playwright/test';
import { cashBalance, useSavedLogin } from './fixtures';
import { buyFromOrdersPage, waitForOrderStatus } from './orders';

// Note: each run really buys 1 share for the demo account
test('a BUY shows as FILLED in the orders list, and the cash goes down', async ({ page }) => {
  test.setTimeout(90_000); // the fill goes through Kafka and the trade-executor, so allow longer

  await useSavedLogin(page);
  const cashBefore = await cashBalance(page);

  const { orderId, priceLimit } = await buyFromOrdersPage(page, 'AAPL', 1);
  await waitForOrderStatus(page, orderId, 'FILLED');

  // Round to cents: subtracting money in JavaScript can leave a tiny leftover (346.19000000000023)
  const spent = Math.round((cashBefore - (await cashBalance(page))) * 100) / 100;
  expect(spent).toBeGreaterThan(0); // the cash went down
  expect(spent).toBeLessThanOrEqual(priceLimit); // by no more than the limit for 1 share
});
