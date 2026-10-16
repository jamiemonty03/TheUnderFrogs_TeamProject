import { Page, expect, test } from '@playwright/test';
import { useSavedLogin } from './fixtures';
import { buyFromOrdersPage, waitForOrderStatus } from './orders';

async function aaplInMain(page: Page): Promise<number> {
  await page.locator('[data-testid^="portfolio-PF"]').filter({ hasText: 'Main' }).click();
  await expect(page.getByTestId('portfolios-title')).toHaveText('Main');
  const quantity = page.getByTestId('holding-AAPL').getByTestId('holding-quantity');
  await expect(quantity).toBeVisible();
  return Number(await quantity.textContent());
}

test('create a portfolio, buy into it, then delete it and the holding moves to Main', async ({
  page,
}) => {
  test.setTimeout(120_000); // includes a buy, which fills in the background

  await useSavedLogin(page);
  await page.getByTestId('nav-portfolios').click();

  const name = `E2E test ${Date.now()}`; // unique, so re-runs don't clash
  await page.getByTestId('new-portfolio').click();
  await page.getByTestId('portfolio-name').fill(name);
  await page.getByTestId('save-portfolio').click();
  await expect(page.getByTestId('portfolios-title')).toHaveText(name);
  await expect(page.getByTestId('holdings-empty')).toBeVisible();

  const card = page.locator('[data-testid^="portfolio-PF"][aria-pressed="true"]');
  const portfolioId = (await card.getAttribute('data-testid'))!.replace('portfolio-', '');

  const mainBefore = await aaplInMain(page);

  const { orderId } = await buyFromOrdersPage(page, 'AAPL', 1, portfolioId);
  await waitForOrderStatus(page, orderId, 'FILLED');

  await page.getByTestId('nav-portfolios').click();
  await page.getByTestId(`portfolio-${portfolioId}`).click();
  await expect(page.getByTestId('holding-AAPL').getByTestId('holding-quantity')).toHaveText('1');

  await page.getByTestId('delete-portfolio').click();
  await page.getByTestId('confirm-delete').click();
  await expect(page.getByTestId('portfolios-title')).toHaveText('All holdings');
  await expect(page.getByTestId(`portfolio-${portfolioId}`)).toHaveCount(0);

  expect(await aaplInMain(page)).toBe(mainBefore + 1);
});
