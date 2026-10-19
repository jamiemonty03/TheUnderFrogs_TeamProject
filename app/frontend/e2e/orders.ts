import { Page, expect } from '@playwright/test';

export async function buyFromOrdersPage(
  page: Page,
  symbol: string,
  quantity: number,
  portfolioId?: string,
) {
  await page.getByTestId('nav-orders').click();
  await page.getByTestId('new-order').click();

  await page.getByTestId('buy-instrument').fill(symbol);
  await page.getByTestId(`buy-option-${symbol}`).click();
  await page.getByTestId('buy-instrument-next').click();

  if (portfolioId) {
    await page.getByTestId('buy-portfolio').click();
    await page.getByTestId(`buy-portfolio-${portfolioId}`).click();
  }
  await page.getByTestId('buy-quantity').fill(String(quantity));
  const priceLimit = Number(await page.getByTestId('buy-price-limit').inputValue());
  await page.getByTestId('buy-details-next').click();

  const orderId = await placeOrder(page, 'place-buy-order');
  return { orderId, priceLimit };
}

export async function sellFromHolding(page: Page, symbol: string, quantity: number) {
  await page.getByTestId('nav-portfolios').click();
  await page.getByTestId(`sell-${symbol}`).first().click();

  await expect(page.getByTestId('sell-instrument-next')).toBeEnabled();
  await page.getByTestId('sell-instrument-next').click();

  await page.getByTestId('sell-quantity').fill(String(quantity));
  await page.getByTestId('sell-details-next').click();

  return placeOrder(page, 'place-sell-order');
}

async function placeOrder(page: Page, placeTestId: string): Promise<string> {
  const placed = page.waitForResponse(
    (response) => response.url().endsWith('/api/orders') && response.request().method() === 'POST',
  );
  await page.getByTestId(placeTestId).click();

  const response = await placed;
  expect(response.ok()).toBe(true);
  const { orderId } = await response.json();
  await expect(page.getByTestId('close-order-dialog')).toHaveCount(0);
  return orderId;
}

export async function waitForOrderStatus(page: Page, orderId: string, status: string) {
  await page.getByTestId('nav-orders').click();
  const statusCell = page.getByTestId(`order-${orderId}`).getByTestId('order-status');
  await expect(async () => {
    await page.reload();
    await expect(statusCell).toHaveText(status, { timeout: 3_000 });
  }).toPass({ timeout: 60_000 });
}
