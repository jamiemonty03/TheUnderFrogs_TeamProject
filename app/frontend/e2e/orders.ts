import { Page, expect } from '@playwright/test';

/**
 * Places a buy through the order dialog on the Orders page, and returns the new order's ID
 * and the price limit it was placed with (the dialog fills that in with the market price).
 */
export async function buyFromOrdersPage(page: Page, symbol: string, quantity: number) {
  await page.getByTestId('nav-orders').click();
  await page.getByTestId('new-order').click();

  // Step 1: pick the instrument
  await page.getByTestId('buy-instrument').fill(symbol);
  await page.getByTestId(`buy-option-${symbol}`).click();
  await page.getByTestId('buy-instrument-next').click();

  // Step 2: quantity, and keep the price limit the dialog suggested
  await page.getByTestId('buy-quantity').fill(String(quantity));
  const priceLimit = Number(await page.getByTestId('buy-price-limit').inputValue());
  await page.getByTestId('buy-details-next').click();

  // Step 3: place it. The dialog answers "Order <id> was placed with status NEW."
  await page.getByTestId('place-buy-order').click();
  const message = page.getByTestId('buy-result');
  await expect(message).toContainText('was placed');
  const orderId = (await message.textContent())!.trim().split(' ')[1];

  await page.getByTestId('close-order-dialog').click();
  return { orderId, priceLimit };
}

/**
 * Waits for an order to reach a status in the orders list. The fill happens in the background
 * (Kafka, then the trade-executor), and the list doesn't update by itself, so this reloads the
 * page until the status shows, for up to a minute.
 */
export async function waitForOrderStatus(page: Page, orderId: string, status: string) {
  const statusCell = page.getByTestId(`order-${orderId}`).getByTestId('order-status');
  await expect(async () => {
    await page.reload();
    await expect(statusCell).toHaveText(status, { timeout: 3_000 });
  }).toPass({ timeout: 60_000 });
}
