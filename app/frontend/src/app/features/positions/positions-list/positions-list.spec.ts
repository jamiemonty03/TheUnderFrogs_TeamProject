import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Accounts } from '../../../services/accounts';
import { Instruments, LatestPrice } from '../../../services/instruments';
import { LivePrices } from '../../../services/live-prices';
import { LivePriceFeed } from '../../../services/interfaces/instruments.interfaces';
import { Portfolio } from '../../../services/interfaces/portfolios.interfaces';
import { Portfolios } from '../../../services/portfolios';
import { Position } from '../../../services/interfaces/positions.interfaces';
import { Positions } from '../../../services/positions';
import { PositionsList, REFRESH_MS } from './positions-list';

const MAIN: Portfolio = {
  portfolioId: 'PF0011',
  accountId: 'ACC0011',
  name: 'Main',
  risk: 'MEDIUM',
  benchmark: null,
  defaultPortfolio: true,
};
const GROWTH: Portfolio = {
  portfolioId: 'PF0012',
  accountId: 'ACC0011',
  name: 'Growth',
  risk: 'HIGH',
  benchmark: 'Nasdaq-100',
  defaultPortfolio: false,
};
const position = (
  portfolioId: string,
  symbol: string,
  quantity: number,
  averageCost: number,
): Position => ({
  accountId: 'ACC0011',
  portfolioId,
  symbol,
  quantity,
  averageCost,
});
const ALL = [
  position('PF0011', 'AAPL', 10, 100),
  position('PF0012', 'NVDA', 2, 500),
  position('PF0012', 'AAPL', 4, 150),
];
const IN_GROWTH = ALL.filter((p) => p.portfolioId === 'PF0012');
const quote = (
  symbol: string,
  name: string,
  price: number,
  previousClose: number,
): LatestPrice => ({
  symbol,
  name,
  assetClass: 'Equity',
  price,
  previousClose,
  tradeDate: '2026-10-13',
});
const AAPL = quote('AAPL', 'Apple Inc.', 120, 100);
const NVDA = quote('NVDA', 'NVIDIA Corp.', 450, 500);
const PRICES = new Map([
  ['AAPL', AAPL],
  ['NVDA', NVDA],
]);

describe('PositionsList (Portfolios page)', () => {
  let fixture: ComponentFixture<PositionsList>;
  let portfolios: {
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    getPortfolios: ReturnType<typeof vi.fn>;
    createPortfolio: ReturnType<typeof vi.fn>;
    updatePortfolio: ReturnType<typeof vi.fn>;
    deletePortfolio: ReturnType<typeof vi.fn>;
  };
  let positions: {
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    getPositions: ReturnType<typeof vi.fn>;
  };
  let instruments: { getLatestPrices: ReturnType<typeof vi.fn> };
  let liveFeed: { watch: ReturnType<typeof vi.fn> };
  let accounts: { getAccount: ReturnType<typeof vi.fn> };

  const el = (testId: string): HTMLElement | null =>
    fixture.nativeElement.querySelector(`[data-testid="${testId}"]`);
  const text = (testId: string): string => el(testId)?.textContent?.trim() ?? '';
  const click = async (testId: string) => {
    el(testId)!.click();
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const rowCount = () => fixture.nativeElement.querySelectorAll('tr.mat-mdc-row').length;
  const column = (name: string): string[] =>
    Array.from(
      fixture.nativeElement.querySelectorAll(
        `td.mat-column-${name}.mat-mdc-cell`,
      ) as NodeListOf<HTMLElement>,
    ).map((td) => td.textContent?.replace(/\s+/g, ' ').trim() ?? '');
  const reload = async () => {
    fixture.componentInstance.refresh();
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const headers = () =>
    Array.from(fixture.nativeElement.querySelectorAll('th') as NodeListOf<HTMLElement>).map((th) =>
      th.textContent?.trim(),
    );

  beforeEach(async () => {
    portfolios = {
      loading: signal(false),
      error: signal<string | null>(null),
      getPortfolios: vi.fn(() => of([MAIN, GROWTH])),
      createPortfolio: vi.fn(() =>
        of({ ...GROWTH, portfolioId: 'PF0013', name: 'Income', risk: 'LOW' }),
      ),
      updatePortfolio: vi.fn(() => of({ ...GROWTH, name: 'Long-term growth' })),
      deletePortfolio: vi.fn(() => of(undefined)),
    };
    positions = {
      loading: signal(false),
      error: signal<string | null>(null),
      getPositions: vi.fn((portfolioId?: string) => of(portfolioId === 'PF0012' ? IN_GROWTH : ALL)),
    };
    instruments = { getLatestPrices: vi.fn(() => of(PRICES)) };
    liveFeed = { watch: vi.fn(() => of({ prices: new Map(), stale: false })) };
    accounts = {
      getAccount: vi.fn(() =>
        of({
          accountId: 'ACC0011',
          holderName: 'Demo Trader',
          cashBalance: 97949.15,
          status: 'ACTIVE',
        }),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [PositionsList],
      providers: [
        provideRouter([]),
        { provide: Portfolios, useValue: portfolios },
        { provide: Positions, useValue: positions },
        { provide: Instruments, useValue: instruments },
        { provide: LivePrices, useValue: liveFeed },
        { provide: Accounts, useValue: accounts },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PositionsList);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('opens on all holdings, across every portfolio, with a portfolio column', () => {
    expect(positions.getPositions).toHaveBeenCalledWith(undefined);
    expect(text('portfolios-title')).toBe('All holdings');
    expect(rowCount()).toBe(3);
    expect(headers()).toContain('Portfolio');
    expect(text('holding-count')).toBe('3');
  });

  it('adds up the total cost of the holdings shown', () => {
    // 10 x 100 + 2 x 500 + 4 x 150
    expect(text('total-cost')).toBe('$2,600.00');
  });

  it('values each holding at the latest price, with P&L, day change and weight', () => {
    // Sorted by market value, largest first: Main AAPL 1,200, NVDA 900, Growth AAPL 480 (total 2,580)
    expect(column('symbol')).toEqual(['AAPL', 'NVDA', 'AAPL']);
    expect(column('name')).toEqual(['Apple Inc.', 'NVIDIA Corp.', 'Apple Inc.']);
    expect(column('assetClass')).toEqual(['Equity', 'Equity', 'Equity']);
    expect(column('price')).toEqual(['$120.00 Close', '$450.00 Close', '$120.00 Close']);
    expect(column('marketValue')).toEqual(['$1,200.00', '$900.00', '$480.00']);
    expect(column('pnl')).toEqual(['$200.00', '-$100.00', '-$120.00']);
    expect(column('pnlPercent')).toEqual(['20.00%', '-10.00%', '-20.00%']);
    expect(column('dayChange')).toEqual(['20.00%', '-10.00%', '20.00%']);
    expect(column('weight')).toEqual(['46.5%', '34.9%', '18.6%']);
  });

  it('uses the live price when there is one, as the dashboard does', async () => {
    const live = new Map([
      ['AAPL', { symbol: 'AAPL', price: 130, bid: null, ask: null, quoteTime: '' }],
    ]);
    liveFeed.watch.mockReturnValue(of({ prices: live, stale: false }));
    positions.getPositions.mockReturnValue(of([position('PF0011', 'AAPL', 10, 100)]));

    await click('portfolio-PF0011');

    expect(column('price')).toEqual(['$130.00 Live']);
    expect(column('pnl')).toEqual(['$300.00']);
    expect(liveFeed.watch).toHaveBeenLastCalledWith(['AAPL']);
  });

  it('flashes P&L, P&L % and day change, not the price, as a live price moves', async () => {
    const feed = new Subject<LivePriceFeed>();
    liveFeed.watch.mockReturnValue(feed.asObservable());
    positions.getPositions.mockReturnValue(of([position('PF0011', 'AAPL', 10, 100)]));
    await click('portfolio-PF0011');
    const tick = (price: number) => {
      feed.next({ prices: new Map([['AAPL', { symbol: 'AAPL', price, bid: null, ask: null, quoteTime: '' }]]), stale: false });
      fixture.detectChanges();
    };
    const row = () => fixture.nativeElement.querySelector('tr.mat-mdc-row') as HTMLElement;
    const flashing = (direction: 'up' | 'down') =>
      Array.from(row().querySelectorAll(`.price-flash--${direction}`)).map((el) => (el.closest('td') as HTMLElement).className);

    tick(130);
    const firstRow = row();
    tick(131);

    expect(row()).toBe(firstRow); // the row is updated in place, not rebuilt
    expect(flashing('up')).toEqual([
      expect.stringContaining('mat-column-pnl '),
      expect.stringContaining('mat-column-pnlPercent'),
      expect.stringContaining('mat-column-dayChange'),
    ]);
    expect(row().querySelector('td.mat-column-price .price-flash')).toBeNull();

    tick(129);
    expect(flashing('down')).toHaveLength(3);
    expect(flashing('up')).toHaveLength(0);
  });

  it('colours gains green and losses red', () => {
    const pnlCells = fixture.nativeElement.querySelectorAll('td.mat-column-pnl.mat-mdc-cell');
    expect(pnlCells[0].classList).toContain('pos');
    expect(pnlCells[1].classList).toContain('neg');
  });

  it('adds up market value, P&L and day change in the tiles', () => {
    expect(text('market-value')).toBe('$2,580.00');
    // 2,580 - 2,600
    expect(text('pnl')).toBe('-$20.00');
    // 10 x 20 - 2 x 50 + 4 x 20
    expect(text('day-change')).toBe('$180.00');
    expect(el('day-change')!.classList).toContain('pos');
  });

  it('shows the shared cash balance', () => {
    expect(accounts.getAccount).toHaveBeenCalled();
    expect(text('cash')).toBe('$97,949.15');
  });

  it("Buy and Sell go to the Orders page's dialog, with the holding's symbol and portfolio", () => {
    const href = (testId: string) => el(testId)!.getAttribute('href');

    expect(href('buy-AAPL')).toBe('/orders?new=true&symbol=AAPL&side=BUY&portfolioId=PF0011');
    expect(href('sell-NVDA')).toBe('/orders?new=true&symbol=NVDA&side=SELL&portfolioId=PF0012');
  });

  it('shows a dash for cash when the account cannot be loaded', async () => {
    accounts.getAccount.mockReturnValue(throwError(() => new Error('down')));

    await reload();

    expect(text('cash')).toBe('-');
  });

  it('shows a dash where a holding has no price, and leaves it out of the totals', async () => {
    instruments.getLatestPrices.mockReturnValue(of(new Map([['AAPL', AAPL]])));

    await reload();

    expect(column('symbol')).toEqual(['AAPL', 'AAPL', 'NVDA']);
    expect(column('marketValue')).toEqual(['$1,200.00', '$480.00', '-']);
    expect(column('name')[2]).toBe('-');
    expect(text('market-value')).toBe('$1,680.00');
  });

  it('says so when prices cannot be loaded', async () => {
    expect(el('prices-failed')).toBeNull();
    instruments.getLatestPrices.mockReturnValue(throwError(() => new Error('down')));

    await reload();

    expect(text('prices-failed')).toContain("Prices couldn't be loaded");
  });

  it('sorts by a column when its header is clicked', async () => {
    const symbolHeader = fixture.nativeElement.querySelector('th.mat-column-symbol') as HTMLElement;

    symbolHeader.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(column('symbol')).toEqual(['AAPL', 'AAPL', 'NVDA']);
  });

  it('refreshes holdings, prices and cash every 30 seconds', () => {
    vi.useFakeTimers();
    try {
      fixture.destroy();
      fixture = TestBed.createComponent(PositionsList);
      fixture.detectChanges();
      positions.getPositions.mockClear();
      instruments.getLatestPrices.mockClear();
      accounts.getAccount.mockClear();

      vi.advanceTimersByTime(REFRESH_MS);

      expect(positions.getPositions).toHaveBeenCalledTimes(1);
      expect(instruments.getLatestPrices).toHaveBeenCalledTimes(1);
      expect(accounts.getAccount).toHaveBeenCalledTimes(1);
    } finally {
      vi.useRealTimers();
    }
  });

  it("each ticker links to that instrument's page", () => {
    expect(el('symbol-NVDA')!.getAttribute('href')).toBe('/instruments/NVDA');
  });

  it('lists every portfolio in the sidebar, marking Main', () => {
    expect(text('portfolio-PF0011')).toContain('Main');
    expect(text('portfolio-PF0012')).toContain('Growth');
    expect(text('portfolio-PF0012')).toContain('High risk');
  });

  it("selecting a portfolio shows only that portfolio's holdings", async () => {
    await click('portfolio-PF0012');

    expect(positions.getPositions).toHaveBeenLastCalledWith('PF0012');
    expect(text('portfolios-title')).toBe('Growth');
    expect(rowCount()).toBe(2);
    expect(headers()).not.toContain('Portfolio');
    expect(el('portfolio-PF0012')!.getAttribute('aria-pressed')).toBe('true');
  });

  describe('allocation', () => {
    const weighting = () =>
      el('allocation')!.querySelector('[role="img"]')!.getAttribute('aria-label');

    it('shows how all holdings are weighted, with AAPL from both portfolios as one', () => {
      expect(weighting()).toBe('Portfolio weighting: AAPL 65.1%, NVDA 34.9%');
      expect(el('allocation')!.querySelectorAll('li')).toHaveLength(2);
    });

    it("follows the selected portfolio's holdings", async () => {
      await click('portfolio-PF0012');

      expect(weighting()).toBe('Portfolio weighting: NVDA 65.2%, AAPL 34.8%');
    });

    it('is hidden when there is nothing held', async () => {
      positions.getPositions.mockReturnValue(of([]));

      await click('portfolio-PF0011');

      expect(el('allocation')).toBeNull();
    });
  });

  it('shows an empty state when a portfolio has no holdings', async () => {
    positions.getPositions.mockReturnValue(of([]));

    await click('portfolio-PF0011');

    expect(el('holdings-table')).toBeNull();
    expect(text('holdings-empty')).toContain('No holdings in Main yet.');
  });

  it('creates a portfolio with a trimmed name and then shows it', async () => {
    await click('new-portfolio');
    const name = el('portfolio-name') as HTMLInputElement;
    name.value = '  Income  ';
    name.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    await click('save-portfolio');

    expect(portfolios.createPortfolio).toHaveBeenCalledWith({
      name: 'Income',
      risk: 'MEDIUM',
      benchmark: '',
    });
    expect(positions.getPositions).toHaveBeenLastCalledWith('PF0013');
    expect(el('portfolio-form')).toBeNull();
  });

  it('does not create a portfolio without a name', async () => {
    await click('new-portfolio');

    await click('save-portfolio');

    expect(portfolios.createPortfolio).not.toHaveBeenCalled();
    expect(el('portfolio-form')).not.toBeNull();
  });

  it('edits the selected portfolio, starting from its current details', async () => {
    await click('portfolio-PF0012');
    await click('edit-portfolio');
    const name = el('portfolio-name') as HTMLInputElement;
    expect(name.value).toBe('Growth');

    name.value = 'Long-term growth';
    name.dispatchEvent(new Event('input'));
    await click('save-portfolio');

    expect(portfolios.updatePortfolio).toHaveBeenCalledWith('PF0012', {
      name: 'Long-term growth',
      risk: 'HIGH',
      benchmark: 'Nasdaq-100',
    });
  });

  it('offers no delete for Main', async () => {
    await click('portfolio-PF0011');

    expect(el('edit-portfolio')).not.toBeNull();
    expect(el('delete-portfolio')).toBeNull();
  });

  it('asks before deleting, says the holdings move to Main, then goes back to all holdings', async () => {
    await click('portfolio-PF0012');
    await click('delete-portfolio');

    expect(fixture.nativeElement.textContent).toContain('Its 2 holdings move into Main.');
    expect(portfolios.deletePortfolio).not.toHaveBeenCalled();

    await click('confirm-delete');

    expect(portfolios.deletePortfolio).toHaveBeenCalledWith('PF0012');
    expect(text('portfolios-title')).toBe('All holdings');
    expect(positions.getPositions).toHaveBeenLastCalledWith(undefined);
  });

  it("shows a service's error message", async () => {
    portfolios.error.set("There's already a portfolio called Growth");
    fixture.detectChanges();

    expect(text('portfolios-error')).toBe("There's already a portfolio called Growth");
  });
});
