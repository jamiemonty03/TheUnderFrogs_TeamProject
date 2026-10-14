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

  await page.getByTestId('place-buy-order').click();
  const orderId = await readOrderId(page, 'buy-result');

  await page.getByTestId('close-order-dialog').click();
  return { orderId, priceLimit };
}

export async function sellFromHolding(page: Page, symbol: string, quantity: number) {
  await page.getByTestId('nav-portfolios').click();
  await page.getByTestId(`sell-${symbol}`).first().click();

  await expect(page.getByTestId('sell-instrument-next')).toBeEnabled();
  await page.getByTestId('sell-instrument-next').click();

  await page.getByTestId('sell-quantity').fill(String(quantity));
  await page.getByTestId('sell-details-next').click();

  await page.getByTestId('place-sell-order').click();
  const orderId = await readOrderId(page, 'sell-result');

  await page.getByTestId('close-order-dialog').click();
  return orderId;
}

async function readOrderId(page: Page, resultTestId: string): Promise<string> {
  const message = page.getByTestId(resultTestId);
  await expect(message).toContainText('was placed');
  return (await message.textContent())!.trim().split(' ')[1];
}

export async function waitForOrderStatus(page: Page, orderId: string, status: string) {
  await page.getByTestId('nav-orders').click();
  const statusCell = page.getByTestId(`order-${orderId}`).getByTestId('order-status');
  await expect(async () => {
    await page.reload();
    await expect(statusCell).toHaveText(status, { timeout: 3_000 });
  }).toPass({ timeout: 60_000 });
}
