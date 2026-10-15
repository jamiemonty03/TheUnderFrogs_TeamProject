import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Instruments, LIVE_POLL_MS, LatestPrice } from '../../../services/instruments';
import { MarketOverview } from './market-overview';

const latest = (
  symbol: string,
  name: string,
  assetClass: LatestPrice['assetClass'],
  price: number | null,
  previousClose: number | null,
): LatestPrice => ({ symbol, name, assetClass, price, previousClose, tradeDate: '2026-10-09T00:00:00' });
const PRICES = new Map(
  [
    latest('AAPL', 'Apple Inc.', 'Equity', 189.84, 187.51),
    latest('NVDA', 'NVIDIA Corp.', 'Equity', 131.2, 127.37),
    latest('TSLA', 'Tesla Inc.', 'Equity', 238.75, 243.89),
    latest('SPY', 'SPDR S&P 500 ETF', 'ETF', 571.2, 568.86),
    latest('QQQ', 'Invesco QQQ Trust', 'ETF', 490.1, 485.6),
    latest('AGG', 'iShares Core U.S. Aggregate Bond', 'Bond', null, null),
  ].map((p) => [p.symbol, p]),
);

describe('MarketOverview', () => {
  let fixture: ComponentFixture<MarketOverview>;
  let page: HTMLElement;
  let getLatestPrices: ReturnType<typeof vi.fn<() => Observable<Map<string, LatestPrice>>>>;

  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  async function setUp(...responses: Observable<Map<string, LatestPrice>>[]) {
    getLatestPrices = vi.fn(() => responses.shift() ?? of(PRICES));
    await TestBed.configureTestingModule({
      imports: [MarketOverview],
      providers: [provideRouter([]), { provide: Instruments, useValue: { getLatestPrices } }],
    }).compileComponents();
    fixture = TestBed.createComponent(MarketOverview);
    page = fixture.nativeElement;
    tick(0);
  }

  function tick(ms: number) {
    vi.advanceTimersByTime(ms);
    fixture.detectChanges();
  }

  const text = (selector: string) => page.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  // Tiles are several <p>s, so join their texts with spaces.
  const tile = (id: string) =>
    Array.from(page.querySelectorAll(`[data-testid="tile-${id}"] p`), (p) => p.textContent?.replace(/\s+/g, ' ').trim()).join(' ');
  const rows = () => Array.from(page.querySelectorAll('tr.market__row'));
  const cells = (row: Element) => Array.from(row.querySelectorAll('td')).map((td) => td.textContent?.replace(/\s+/g, ' ').trim());
  const tab = (label: string) =>
    Array.from(page.querySelectorAll<HTMLButtonElement>('.market__tab')).find((b) => b.textContent?.trim() === label)!;

  it('lists stocks with price, change and change %, coloured by direction', async () => {
    await setUp();

    expect(rows().map((r) => cells(r)[0])).toEqual(['AAPL', 'NVDA', 'TSLA']);
    const [symbol, name, price, change, percent] = cells(rows()[0]);
    expect([symbol, name, price, change, percent]).toEqual(['AAPL', 'Apple Inc.', '$189.84', '▲ $2.33', '1.24%']);
    expect(rows()[0].querySelectorAll('td.gain')).toHaveLength(2);
    expect(cells(rows()[2])[3]).toBe('▼ -$5.14');
    expect(rows()[2].querySelectorAll('td.loss')).toHaveLength(2);
  });

  it('switches between stocks, ETFs and bonds', async () => {
    await setUp();

    tab('ETFs').click();
    fixture.detectChanges();
    expect(rows().map((r) => cells(r)[0])).toEqual(['QQQ', 'SPY']);
    expect(tab('ETFs').getAttribute('aria-pressed')).toBe('true');

    tab('Bonds').click();
    fixture.detectChanges();
    expect(cells(rows()[0])).toEqual(['AGG', 'iShares Core U.S. Aggregate Bond', '—', '—', '—', 'Buy']);
  });

  it('filters by symbol or name', async () => {
    await setUp();
    const search = page.querySelector<HTMLInputElement>('input[type="search"]')!;

    search.value = 'nvidia';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(rows().map((r) => cells(r)[0])).toEqual(['NVDA']);

    search.value = 'zzz';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(text('.market__empty')).toBe('No lily pads match "zzz".');
  });

  it('shows the S&P 500 and NASDAQ via SPY and QQQ, plus the top hopper and biggest sinker', async () => {
    await setUp();

    expect(tile('S&P 500')).toBe('S&P 500 571.20 ▲ 0.41% via SPY');
    expect(tile('NASDAQ')).toBe('NASDAQ 490.10 ▲ 0.93% via QQQ');
    expect(tile('hopper')).toBe('Top hopper NVDA NVIDIA Corp. ▲ 3.01%');
    expect(tile('sinker')).toBe('Biggest sinker TSLA Tesla Inc. ▼ -2.11%');
    expect(page.querySelector('[data-testid="tile-sinker"] .loss')).toBeTruthy();
  });

  it('links each Buy button to a new BUY order for that symbol', async () => {
    await setUp();

    const buy = page.querySelector<HTMLAnchorElement>('a[aria-label="Buy NVDA"]')!;
    const url = new URL(buy.href);
    expect(url.pathname).toBe('/orders');
    expect(Object.fromEntries(url.searchParams)).toEqual({ new: 'true', symbol: 'NVDA', side: 'BUY' });
  });

  it('notes that prices are the latest daily close', async () => {
    await setUp();

    expect(text('.market__note')).toBe('Prices are the latest daily close (Oct 9, 2026), refreshed every 30 seconds.');
  });

  it('refreshes every 30 seconds', async () => {
    const moved = new Map(PRICES).set('AAPL', latest('AAPL', 'Apple Inc.', 'Equity', 200, 187.51));
    await setUp(of(PRICES), of(moved));

    expect(getLatestPrices).toHaveBeenCalledTimes(1);
    tick(LIVE_POLL_MS - 1);
    expect(getLatestPrices).toHaveBeenCalledTimes(1);
    tick(1);
    expect(getLatestPrices).toHaveBeenCalledTimes(2);
    expect(cells(rows()[0])[2]).toBe('$200.00');
  });

  it('stops refreshing when the user leaves the page', async () => {
    await setUp();

    fixture.destroy();
    vi.advanceTimersByTime(LIVE_POLL_MS * 3);
    expect(getLatestPrices).toHaveBeenCalledTimes(1);
  });

  it('keeps the last prices and says so when a refresh fails, then recovers', async () => {
    await setUp(of(PRICES), throwError(() => new Error('503')));

    tick(LIVE_POLL_MS);
    expect(text('[role="alert"]')).toContain('showing the last values');
    expect(rows()).toHaveLength(3);

    tick(LIVE_POLL_MS);
    expect(page.querySelector('[role="alert"]')).toBeNull();
  });

  it('says it could not load when the very first request fails', async () => {
    await setUp(throwError(() => new Error('503')));

    expect(text('[role="alert"]')).toBe("Couldn't load market prices. Retrying in 30 seconds.");
    expect(rows()).toHaveLength(0);
  });
});
