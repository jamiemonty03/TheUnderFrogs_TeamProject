import { expect, test } from '@playwright/test';
import { cashBalance, useSavedLogin } from './fixtures';
import { finishBuy, waitForOrderStatus } from './orders';

test('search for an instrument, read its chart, then buy it from its page', async ({ page }) => {
  test.setTimeout(90_000);

  await useSavedLogin(page);
  const cashBefore = await cashBalance(page);

  await page.getByTestId('nav-search').fill('AAPL');
  await page.getByTestId('search-option-AAPL').click();

  await expect(page).toHaveURL(/\/instruments\/AAPL$/);
  await expect(page.getByTestId('instrument-symbol')).toHaveText('AAPL');
  await expect(page.getByTestId('instrument-price')).toHaveText(/^\$[\d,]+\.\d{2}$/);
  await expect(page.getByTestId('price-chart').locator('canvas')).toBeVisible();

  await page.getByTestId('mode-candles').click();
  await expect(page.getByTestId('mode-candles')).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByTestId('chart-ohlc')).toContainText(/O[\d,.]+H[\d,.]+L[\d,.]+C[\d,.]+/);

  await page.getByTestId('instrument-buy').click();
  await expect(page).toHaveURL(/\/orders\?new=true&symbol=AAPL&side=BUY$/);
  await expect(page.getByTestId('buy-instrument')).toHaveValue(/AAPL/);

  const { orderId, priceLimit } = await finishBuy(page, 1);
  await waitForOrderStatus(page, orderId, 'FILLED');

  const spent = Math.round((cashBefore - (await cashBalance(page))) * 100) / 100;
  expect(spent).toBeGreaterThan(0);
  expect(spent).toBeLessThanOrEqual(priceLimit);
});
