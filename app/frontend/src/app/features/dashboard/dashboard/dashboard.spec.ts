import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NEVER, Observable, Subject, of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';
import { Account, Accounts } from '../../../services/accounts';
import { Instruments, LatestPrice, LivePrice, LivePriceFeed } from '../../../services/instruments';
import { LivePrices } from '../../../services/live-prices';
import { Portfolio } from '../../../services/interfaces/portfolios.interfaces';
import { Position, Positions } from '../../../services/positions';
import { Portfolios } from '../../../services/portfolios';
import { Dashboard } from './dashboard';

const ACCOUNT: Account = { accountId: 'ACC0013', holderName: 'Kermit Frog', cashBalance: 12480, status: 'ACTIVE' };
const POSITIONS: Position[] = [
  { accountId: 'ACC0013', portfolioId: 'PF0011', symbol: 'AAPL', quantity: 40, averageCost: 171.2 },
  { accountId: 'ACC0013', portfolioId: 'PF0012', symbol: 'TSLA', quantity: 12, averageCost: 260 },
];
const PORTFOLIOS: Portfolio[] = [
  {
    portfolioId: 'PF0011',
    accountId: 'ACC0013',
    name: 'Main',
    risk: 'MEDIUM',
    benchmark: null,
    defaultPortfolio: true,
  },
  {
    portfolioId: 'PF0012',
    accountId: 'ACC0013',
    name: 'Growth',
    risk: 'HIGH',
    benchmark: null,
    defaultPortfolio: false,
  },
];
const latest = (symbol: string, name: string, price: number): LatestPrice => ({
  symbol,
  name,
  assetClass: 'Equity',
  price,
  previousClose: null,
  tradeDate: null,
});
const PRICES = new Map([
  ['AAPL', latest('AAPL', 'Apple Inc.', 189.84)],
  ['TSLA', latest('TSLA', 'Tesla Inc.', 238.75)],
]);

interface Calls {
  account?: Observable<Account>;
  balance?: Observable<number>;
  positions?: Observable<Position[]>;
  prices?: Observable<Map<string, LatestPrice>>;
  loading?: { account?: boolean; balance?: boolean; positions?: boolean };
}

const live = (symbol: string, price: number): LivePrice => ({
  symbol,
  price,
  bid: price - 0.05,
  ask: price + 0.05,
  quoteTime: '2026-10-13T14:30:00Z',
});

describe('Dashboard', () => {
  let fixture: ComponentFixture<Dashboard>;
  let page: HTMLElement;
  // Stands in for the live-price WebSocket: tests push feeds into it.
  let liveFeed: Subject<LivePriceFeed>;
  let watch: Mock<(symbols?: string[]) => Observable<LivePriceFeed>>;
  let getAccount: Mock<() => Observable<Account>>;
  let getBalance: Mock<() => Observable<number>>;
  let getPositions: Mock<() => Observable<Position[]>>;
  let getLatestPrices: Mock<() => Observable<Map<string, LatestPrice>>>;

  // The real services track loading/error themselves; these fakes do the same for errors.
  async function setUp(calls: Calls = {}) {
    const accountError = signal<string | null>(null);
    const balanceError = signal<string | null>(null);
    const positionsError = signal<string | null>(null);
    const failWith = <T>(error: { set(v: string): void }, call: Observable<T>) =>
      new Observable<T>((sub) =>
        call.subscribe({
          next: (v) => sub.next(v),
          error: (e: Error) => {
            error.set(e.message);
            sub.error(e);
          },
          complete: () => sub.complete(),
        }),
      );

    liveFeed = new Subject<LivePriceFeed>();
    watch = vi.fn(() => liveFeed.asObservable());
    getAccount = vi.fn(() => failWith(accountError, calls.account ?? of(ACCOUNT)));
    getBalance = vi.fn(() => failWith(balanceError, calls.balance ?? of(ACCOUNT.cashBalance)));
    getPositions = vi.fn(() => failWith(positionsError, calls.positions ?? of(POSITIONS)));

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        {
          provide: Accounts,
          useValue: {
            account: { loading: signal(!!calls.loading?.account), error: accountError },
            balance: { loading: signal(!!calls.loading?.balance), error: balanceError },
            getAccount,
            getBalance,
          },
        },
        {
          provide: Positions,
          useValue: {
            loading: signal(!!calls.loading?.positions),
            error: positionsError,
            getPositions,
          },
        },
        { provide: Portfolios, useValue: { getPortfolios: () => of(PORTFOLIOS) } },
        { provide: LivePrices, useValue: { watch } },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
    const instruments = TestBed.inject(Instruments);
    getLatestPrices = vi.fn(() => calls.prices ?? of(PRICES));
    vi.spyOn(instruments, 'getLatestPrices').mockImplementation(getLatestPrices);
    fixture = TestBed.createComponent(Dashboard);
    page = fixture.nativeElement;
    fixture.detectChanges();
  }

  const text = (selector: string) => page.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim();
  const card = (labelId: string) => page.querySelector(`[aria-labelledby="${labelId}"]`)!;
  const rows = () => Array.from(page.querySelectorAll('tr[mat-row]'));
  const cells = (row: Element) => Array.from(row.querySelectorAll('td')).map((td) => td.textContent?.trim());

  it('welcomes the holder by name with their account ID and status', async () => {
    await setUp();

    expect(text('.dashboard__greeting')).toBe('Welcome, Kermit ribbet');
    expect(text('h1')).toBe('Your pond at a glance');
    expect(text('.dashboard__account')).toContain('ACC0013');
    expect(text('.pill--active')).toBe('ACTIVE');
  });

  it('shows portfolio value as cash plus invested, and cash on its own tile', async () => {
    await setUp();

    expect(card('portfolio-label').textContent).toContain('$22,938.60');
    expect(card('cash-label').textContent).toContain('$12,480.00');
  });

  it('defaults to all holdings and filters the dashboard by portfolio', async () => {
    await setUp();

    expect(
      (fixture.componentInstance as unknown as { selectedPortfolioId(): string }).selectedPortfolioId(),
    ).toBe('ALL');
    expect(rows()).toHaveLength(2);

    (fixture.componentInstance as unknown as { selectPortfolio(portfolioId: string): void }).selectPortfolio('PF0012');
    fixture.detectChanges();

    expect(rows()).toHaveLength(1);
    expect(cells(rows()[0])[0]).toBe('TSLA');
    expect(card('invested-label').textContent).toContain('$2,865.00');
  });

  it('portfolio value is just cash when the user holds nothing', async () => {
    await setUp({ positions: of([]) });

    expect(card('portfolio-label').textContent).toContain('$12,480.00');
  });

  it('shows invested, P&L and the number of positions', async () => {
    await setUp();

    expect(card('invested-label').textContent).toContain('$10,458.60');
    expect(card('pnl-label').textContent).toContain('$490.60');
    expect(card('pnl-label').querySelector('.gain')).toBeTruthy();
    expect(card('count-label').textContent).toContain('2');
  });

  it('colours a portfolio-wide loss red', async () => {
    await setUp({ prices: of(new Map([['AAPL', latest('AAPL', 'Apple', 150)], ['TSLA', latest('TSLA', 'Tesla', 200)]])) });

    expect(card('pnl-label').querySelector('.loss')).toBeTruthy();
  });

  it('lists each holding with its valuation', async () => {
    await setUp();

    expect(rows().length).toBe(2);
    const [symbol, name, quantity, avgCost, price, value, pnl, pnlPercent, weight] = cells(rows()[0]);
    expect([symbol, name, quantity, avgCost, value]).toEqual(['AAPL', 'Apple Inc.', '40', '$171.20', '$7,593.60']);
    expect(price).toContain('$189.84');
    expect(pnl).toContain('$745.60');
    expect(pnlPercent).toBe('10.89%');
    expect(weight).toBe('72.6%');
    expect(rows()[1].querySelector('.loss')).toBeTruthy();
  });

  it('links Buy and Sell on each row to a prefilled new order', async () => {
    await setUp();

    const buy = rows()[0].querySelector<HTMLAnchorElement>('a[aria-label="Buy AAPL"]')!;
    const sell = rows()[0].querySelector<HTMLAnchorElement>('a[aria-label="Sell AAPL"]')!;
    expect(buy.getAttribute('href')).toBe('/orders?new=true&symbol=AAPL&side=BUY');
    expect(sell.getAttribute('href')).toBe('/orders?new=true&symbol=AAPL&side=SELL');
  });

  it('links Place order, with no Order history button', async () => {
    await setUp();

    const topActions = Array.from(page.querySelectorAll<HTMLAnchorElement>('.dashboard__welcome .dashboard__actions a'));
    expect(topActions.map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
      ['Place order', '/orders?new=true'],
    ]);
    expect(page.textContent).not.toContain('Order history');
  });

  it('sorts the table when a column header is clicked', async () => {
    await setUp();

    const header = Array.from(page.querySelectorAll<HTMLElement>('th[mat-sort-header]')).find((th) =>
      th.textContent?.includes('Market value'),
    )!;
    header.click();
    fixture.detectChanges();
    expect(rows().map((r) => cells(r)[0])).toEqual(['TSLA', 'AAPL']);

    header.click();
    fixture.detectChanges();
    expect(rows().map((r) => cells(r)[0])).toEqual(['AAPL', 'TSLA']);
  });

  it('shows an empty state when the user holds nothing', async () => {
    await setUp({ positions: of([]) });

    expect(page.querySelector('table')).toBeNull();
    expect(text('.dashboard__empty')).toContain("You don't hold anything yet.");
    expect(card('count-label').textContent).toContain('0');
  });

  it('keeps the rest of the page when the account call fails', async () => {
    await setUp({ account: throwError(() => new Error('Account service is down')) });

    expect(text('.dashboard__welcome [role="alert"]')).toContain('Account service is down');
    expect(page.querySelector('h1')).toBeNull();
    expect(card('portfolio-label').textContent).toContain('$22,938.60');
    expect(rows().length).toBe(2);
  });

  it('keeps the rest of the page when the positions call fails', async () => {
    await setUp({ positions: throwError(() => new Error("Couldn't load your positions.")) });

    expect(text('[aria-labelledby="positions-title"] [role="alert"]')).toContain("Couldn't load your positions.");
    expect(card('invested-label').textContent).toContain('Unavailable');
    expect(text('.dashboard__greeting')).toBe('Welcome, Kermit ribbet');
    expect(text('h1')).toBe('Your pond at a glance');
    expect(card('portfolio-label').querySelector('[role="alert"]')).toBeTruthy();
    expect(card('cash-label').textContent).toContain('$12,480.00');
  });

  it('still lists holdings without valuations when prices fail', async () => {
    await setUp({ prices: throwError(() => new Error('503')) });

    expect(text('[aria-labelledby="positions-title"] [role="alert"]')).toContain("Couldn't load the latest prices.");
    expect(rows().length).toBe(2);
    expect(cells(rows()[0])[5]).toBe('—');
    expect(card('pnl-label').textContent).toContain('Unavailable');
    expect(card('count-label').textContent).toContain('2');
  });

  const retry = (container: Element) => {
    container.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    fixture.detectChanges();
  };

  describe('loading states', () => {
    it('shows a spinner while the account loads', async () => {
      await setUp({ loading: { account: true } });

      expect(text('.dashboard__welcome .dashboard__status')).toBe('Loading your account…');
      expect(page.querySelector('.dashboard__greeting')).toBeNull();
    });

    it('shows loading on the portfolio and cash cards while the balance loads', async () => {
      await setUp({ loading: { balance: true } });

      expect(text('[aria-labelledby="portfolio-label"] .dashboard__status')).toBe('Loading…');
      expect(text('[aria-labelledby="cash-label"] .dashboard__status')).toBe('Loading…');
    });

    it('shows loading on every valuation card while prices are in flight', async () => {
      await setUp({ prices: NEVER });

      for (const id of ['portfolio-label', 'invested-label', 'pnl-label', 'allocation-title']) {
        expect(card(id).querySelector('.dashboard__status'), id).toBeTruthy();
      }
      expect(text('[aria-labelledby="positions-title"] .dashboard__status')).toBe('Loading your positions…');
      expect(page.querySelector('table')).toBeNull();
    });

    it('shows loading on the positions count while positions load', async () => {
      await setUp({ loading: { positions: true } });

      expect(text('[aria-labelledby="count-label"] .dashboard__status')).toBe('Loading…');
    });
  });

  describe('errors and retries', () => {
    it('retries the account call from the welcome banner', async () => {
      await setUp({ account: throwError(() => new Error('Account service is down')) });

      retry(page.querySelector('.dashboard__welcome')!);

      expect(getAccount).toHaveBeenCalledTimes(2);
    });

    it('marks cash unavailable and retries when the balance call fails', async () => {
      await setUp({ balance: throwError(() => new Error('Balance service is down')) });

      expect(card('portfolio-label').querySelector('[role="alert"]')?.textContent).toContain('Balance service is down');
      expect(card('cash-label').textContent).toContain('Unavailable');

      retry(card('portfolio-label'));

      expect(getBalance).toHaveBeenCalledTimes(2);
    });

    it('retries only the failed positions call from the portfolio card', async () => {
      await setUp({ positions: throwError(() => new Error("Couldn't load your positions.")) });

      retry(card('portfolio-label'));

      expect(getPositions).toHaveBeenCalledTimes(2);
      expect(getLatestPrices).toHaveBeenCalledTimes(1);
    });

    it('retries only the failed prices call from the portfolio card', async () => {
      await setUp({ prices: throwError(() => new Error('503')) });

      retry(card('portfolio-label'));

      expect(getLatestPrices).toHaveBeenCalledTimes(2);
      expect(getPositions).toHaveBeenCalledTimes(1);
    });

    it('retries positions from the positions table', async () => {
      await setUp({ positions: throwError(() => new Error("Couldn't load your positions.")) });

      retry(card('positions-title'));

      expect(getPositions).toHaveBeenCalledTimes(2);
    });

    it('retries prices from the positions table', async () => {
      await setUp({ prices: throwError(() => new Error('503')) });

      retry(card('positions-title'));

      expect(getLatestPrices).toHaveBeenCalledTimes(2);
    });
  });

  it('notes holdings left out of the totals when they have no price', async () => {
    await setUp({ prices: of(new Map([['AAPL', latest('AAPL', 'Apple Inc.', 189.84)]])) });

    expect(card('portfolio-label').textContent).toContain('Excludes 1 unpriced');
    expect(card('invested-label').textContent).toContain('excludes 1 unpriced');
    expect(cells(rows()[1])[4]).toBe('—');
  });

  it('filters by portfolio when one is picked from the dropdown', async () => {
    await setUp();

    page.querySelector<HTMLElement>('[data-testid="dashboard-portfolio-filter"] .mat-mdc-select-trigger')!.click();
    fixture.detectChanges();
    const growth = Array.from(document.querySelectorAll<HTMLElement>('mat-option')).find((o) =>
      o.textContent?.includes('Growth'),
    )!;
    growth.click();
    fixture.detectChanges();

    expect(rows().map((r) => cells(r)[0])).toEqual(['TSLA']);
    expect(watch).toHaveBeenLastCalledWith(['TSLA']);
  });

  describe('live prices', () => {
    const push = (quotes: LivePrice[], stale = false) => {
      liveFeed.next({ prices: new Map(quotes.map((q) => [q.symbol, q])), stale });
      fixture.detectChanges();
    };
    const priceCell = (row: number) => rows()[row].querySelectorAll('td')[4];

    it("watches live quotes for the user's holdings and values them over the daily close", async () => {
      await setUp();
      push([live('AAPL', 200)]);

      expect(watch).toHaveBeenCalledWith(['AAPL', 'TSLA']);
      expect(priceCell(0).textContent).toContain('$200.00');
      expect(priceCell(0).querySelector('.price-tag--live')?.textContent?.trim()).toBe('Live');
      expect(priceCell(1).querySelector('.price-tag')?.textContent?.trim()).toBe('Close');
      // 40 × 200 + 12 × 238.75
      expect(card('invested-label').textContent).toContain('$10,865.00');
    });

    it('updates the page on every pushed quote', async () => {
      await setUp();
      push([live('AAPL', 200)]);
      expect(priceCell(0).textContent).toContain('$200.00');

      push([live('AAPL', 210)]);
      expect(priceCell(0).textContent).toContain('$210.00');
    });

    it('keeps the last live values and shows a note while the feed is stale', async () => {
      await setUp();
      push([live('AAPL', 200)]);

      push([live('AAPL', 200)], true);
      expect(priceCell(0).textContent).toContain('$200.00');
      expect(page.querySelector('.dashboard__stale')?.textContent).toContain('Live prices paused');
      expect(page.querySelector('[aria-labelledby="positions-title"] [role="alert"]')).toBeNull();

      push([live('AAPL', 205)]);
      expect(priceCell(0).textContent).toContain('$205.00');
      expect(page.querySelector('.dashboard__stale')).toBeNull();
    });

    it('watches only the selected portfolio', async () => {
      await setUp();

      (fixture.componentInstance as unknown as { selectPortfolio(portfolioId: string): void }).selectPortfolio('PF0012');

      expect(watch).toHaveBeenLastCalledWith(['TSLA']);
    });

    it('asks for no symbols when the user holds nothing', async () => {
      await setUp({ positions: of([]) });

      expect(watch).toHaveBeenCalledWith([]);
    });
  });
});
