import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { Account, Accounts } from '../../../services/accounts';
import { Instruments, LatestPrice } from '../../../services/instruments';
import { Position, Positions } from '../../../services/positions';
import { Dashboard } from './dashboard';

const ACCOUNT: Account = { accountId: 'ACC0013', holderName: 'Kermit Frog', cashBalance: 12480, status: 'ACTIVE' };
const POSITIONS: Position[] = [
  { accountId: 'ACC0013', symbol: 'AAPL', quantity: 40, averageCost: 171.2 },
  { accountId: 'ACC0013', symbol: 'TSLA', quantity: 12, averageCost: 260 },
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
}

describe('Dashboard', () => {
  let fixture: ComponentFixture<Dashboard>;
  let page: HTMLElement;

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

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        provideRouter([]),
        {
          provide: Accounts,
          useValue: {
            account: { loading: signal(false), error: accountError },
            balance: { loading: signal(false), error: balanceError },
            getAccount: () => failWith(accountError, calls.account ?? of(ACCOUNT)),
            getBalance: () => failWith(balanceError, calls.balance ?? of(ACCOUNT.cashBalance)),
          },
        },
        {
          provide: Positions,
          useValue: {
            loading: signal(false),
            error: positionsError,
            getPositions: () => failWith(positionsError, calls.positions ?? of(POSITIONS)),
          },
        },
        { provide: Instruments, useValue: { getLatestPrices: () => calls.prices ?? of(PRICES) } },
      ],
    }).compileComponents();
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

    expect(text('h1')).toBe('Welcome, Kermit');
    expect(text('.dashboard__account')).toContain('ACC0013');
    expect(text('.pill--active')).toBe('ACTIVE');
  });

  it('shows portfolio value as cash plus invested, and cash on its own tile', async () => {
    await setUp();

    expect(card('portfolio-label').textContent).toContain('$22,938.60');
    expect(card('cash-label').textContent).toContain('$12,480.00');
  });

  it('portfolio value is just cash when the user holds nothing', async () => {
    await setUp({ positions: of([]) });

    expect(card('portfolio-label').textContent).toContain('$12,480.00');
  });

  it('shows invested, unrealised P&L and the number of positions', async () => {
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
    expect([symbol, name, quantity, avgCost, price, value]).toEqual([
      'AAPL',
      'Apple Inc.',
      '40',
      '$171.20',
      '$189.84',
      '$7,593.60',
    ]);
    expect(pnl).toContain('$745.60');
    expect(pnlPercent).toBe('10.89%');
    expect(weight).toBe('72.6%');
    expect(rows()[1].querySelector('.loss')).toBeTruthy();
  });

  it('links Buy and Sell on each row to a prefilled new order', async () => {
    await setUp();

    const buy = rows()[0].querySelector<HTMLAnchorElement>('a[aria-label="Buy AAPL"]')!;
    const sell = rows()[0].querySelector<HTMLAnchorElement>('a[aria-label="Sell AAPL"]')!;
    expect(buy.getAttribute('href')).toBe('/orders/new?symbol=AAPL&side=BUY');
    expect(sell.getAttribute('href')).toBe('/orders/new?symbol=AAPL&side=SELL');
  });

  it('links Place order and Order history', async () => {
    await setUp();

    const actions = Array.from(page.querySelectorAll<HTMLAnchorElement>('.dashboard__actions a'));
    expect(actions.map((a) => [a.textContent?.trim(), a.getAttribute('href')])).toEqual([
      ['Place order', '/orders/new'],
      ['Order history', '/orders'],
    ]);
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
    expect(text('h1')).toBe('Welcome, Kermit');
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
});
