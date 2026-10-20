import { expect, test } from '@playwright/test';
import { useSavedLogin } from './fixtures';

// The assistant's answer is mocked so the test never spends the free AI quota
const ANSWER = {
  reply: 'MSFT is down 2.06% (daily close, 16 Oct). A related headline: Azure growth slows.',
  aiSummary: true,
  checksPassed: true,
  model: 'openai/gpt-oss-120b',
  sources: [
    {
      type: 'NEWS',
      symbol: 'MSFT',
      label: 'Azure growth slows',
      url: 'https://news.example.com/azure',
      asOf: '2026-10-19T13:05:00Z',
    },
  ],
  disclaimer: 'Information only, not financial advice.',
};

test('the lily pad opens the assistant, which answers with its sources without covering the page', async ({
  page,
}) => {
  let question: unknown;
  await page.route('**/api/assistant/chat', async (route) => {
    question = route.request().postDataJSON();
    await route.fulfill({ json: ANSWER });
  });
  await page.setViewportSize({ width: 1440, height: 900 });
  await useSavedLogin(page);

  const pad = page.getByTestId('assistant-lily-pad');
  await expect(pad).toBeVisible();
  await pad.click();

  const panel = page.getByTestId('assistant-panel');
  await expect(panel).toBeVisible();
  const panelBox = (await panel.boundingBox())!;
  const pageBox = (await page.locator('main').boundingBox())!;
  expect(pageBox.x + pageBox.width).toBeLessThanOrEqual(panelBox.x + 1);

  await page.getByTestId('assistant-input').fill('Why is Microsoft down today?');
  await page.getByTestId('assistant-send').click();

  await expect(page.getByTestId('assistant-message').last()).toContainText('MSFT is down 2.06%');
  await expect(page.getByTestId('assistant-badge')).toContainText('Checked against our data');
  await expect(page.getByTestId('assistant-source').getByRole('link')).toHaveAttribute(
    'href',
    'https://news.example.com/azure',
  );
  expect(question).toEqual({ message: 'Why is Microsoft down today?' });

  await page.keyboard.press('Escape');
  await expect(panel).toBeHidden();
});
