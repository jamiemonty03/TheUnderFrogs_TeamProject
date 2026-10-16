import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';
import { Auth } from '../../../services/auth/auth';
import { Instruments } from '../../../services/instruments';
import { PricePoint } from '../../../services/interfaces/instruments.interfaces';
import { ClientTradeResponse } from '../../../services/interfaces/orders.interfaces';
import { Orders } from '../../../services/orders';
import { Position } from '../../../services/positions';
import { Performance, historyDaysSince, holdingsHistory, rangeChange, returnHistory, valueChange } from './performance';

const point = (date: string, close: number): PricePoint => ({ date, close, movingAvg20: null, movingAvg50: null });
const trade = (
  symbol: string,
  tradeType: 'BUY' | 'SELL',
  quantity: number,
  price: number,
  tradeDate: string,
): ClientTradeResponse => ({
  tradeId: 1,
  accountId: 'ACC1',
  symbol,
  tradeType,
  quantity,
  price,
  tradeDate,
  version: 0,
  createdAt: `${tradeDate}T10:00:00`,
  lastUpdated: null,
  updatedBy: null,
});

const position = (symbol: string, quantity: number, portfolioId = 'PF1'): Position => ({
  accountId: 'ACC1',
  portfolioId,
  symbol,
  quantity,
  averageCost: 1,
});

describe('holdingsHistory', () => {
  it('values the held quantity of each symbol at every close', () => {
    const histories = new Map([
      ['AAPL', [point('2026-10-01', 100), point('2026-10-02', 110)]],
      ['TSLA', [point('2026-10-01', 200), point('2026-10-02', 190)]],
    ]);

    expect(holdingsHistory([position('AAPL', 2), position('TSLA', 1)], histories)).toEqual([
      { date: '2026-10-01', value: 400 },
      { date: '2026-10-02', value: 410 },
    ]);
  });

  it('adds up a symbol held in more than one portfolio', () => {
    const histories = new Map([['AAPL', [point('2026-10-01', 100)]]]);

    expect(holdingsHistory([position('AAPL', 2), position('AAPL', 3, 'PF2')], histories)).toEqual([
      { date: '2026-10-01', value: 500 },
    ]);
  });

  it('carries a missing close forward and waits until every symbol has a price', () => {
    const histories = new Map([
      ['AAPL', [point('2026-10-01', 100), point('2026-10-02', 110), point('2026-10-03', 120)]],
      ['TSLA', [point('2026-10-02', 200)]],
    ]);

    expect(holdingsHistory([position('AAPL', 1), position('TSLA', 1)], histories)).toEqual([
      { date: '2026-10-02', value: 310 },
      { date: '2026-10-03', value: 320 },
    ]);
  });

  it('ignores closed positions and symbols with no history', () => {
    const histories = new Map([['AAPL', [point('2026-10-01', 100)]]]);

    expect(holdingsHistory([position('AAPL', 0), position('MSFT', 5)], histories)).toEqual([]);
  });
});

describe('valueChange', () => {
  it('measures the move from the first value', () => {
    expect(valueChange([{ date: 'd1', value: 200 }, { date: 'd2', value: 220 }])).toEqual({ amount: 20, percent: 10 });
  });

  it('needs two points', () => {
    expect(valueChange([{ date: 'd1', value: 200 }])).toBeNull();
  });
});

describe('returnHistory', () => {
  it('starts on the first trade and tracks gain from price moves only', () => {
    const histories = new Map([
      ['AAPL', [point('2026-09-30', 90), point('2026-10-01', 100), point('2026-10-02', 110), point('2026-10-05', 105)]],
    ]);

    expect(returnHistory([trade('AAPL', 'BUY', 2, 100, '2026-10-01')], histories)).toEqual([
      { date: '2026-10-01', value: 200, gain: 0, bought: 200 },
      { date: '2026-10-02', value: 220, gain: 20, bought: 200 },
      { date: '2026-10-05', value: 210, gain: 10, bought: 200 },
    ]);
  });

  it("doesn't count a buy as a gain", () => {
    const histories = new Map([
      ['AAPL', [point('2026-10-01', 100), point('2026-10-02', 100)]],
      ['TSLA', [point('2026-10-01', 200), point('2026-10-02', 200)]],
    ]);
    const points = returnHistory(
      [trade('AAPL', 'BUY', 1, 100, '2026-10-01'), trade('TSLA', 'BUY', 5, 200, '2026-10-02')],
      histories,
    );

    expect(points.map((p) => p.value)).toEqual([100, 1100]);
    expect(points.map((p) => p.gain)).toEqual([0, 0]);
  });

  it('keeps the profit locked in by a sale', () => {
    const histories = new Map([['AAPL', [point('2026-10-01', 100), point('2026-10-02', 150), point('2026-10-03', 50)]]]);
    const points = returnHistory(
      [trade('AAPL', 'BUY', 2, 100, '2026-10-01'), trade('AAPL', 'SELL', 2, 150, '2026-10-02')],
      histories,
    );

    expect(points.map((p) => p.gain)).toEqual([0, 100, 100]);
    expect(points.at(-1)!.value).toBe(0);
  });

  it('values a symbol at its trade price until it has a close', () => {
    const histories = new Map([['AAPL', [point('2026-10-02', 110)]]]);

    expect(returnHistory([trade('AAPL', 'BUY', 1, 100, '2026-10-01')], histories)).toEqual([
      { date: '2026-10-01', value: 100, gain: 0, bought: 100 },
      { date: '2026-10-02', value: 110, gain: 10, bought: 100 },
    ]);
  });

  it('returns nothing without trades', () => {
    expect(returnHistory([], new Map())).toEqual([]);
  });
});

describe('rangeChange', () => {
  const points = [
    { date: 'd1', value: 200, gain: 0, bought: 200 },
    { date: 'd2', value: 1220, gain: 20, bought: 1200 },
    { date: 'd3', value: 1260, gain: 60, bought: 1200 },
  ];

  it('measures all time against everything bought', () => {
    expect(rangeChange(points, true)).toEqual({ amount: 60, percent: 5 });
  });

  it('measures a range against the starting value plus what was bought since', () => {
    expect(rangeChange(points.slice(1), false)).toEqual({ amount: 40, percent: (40 / 1220) * 100 });
  });
});

describe('historyDaysSince', () => {
  it('asks for roughly the trading days since the date', () => {
    expect(historyDaysSince('2026-01-08', new Date('2026-10-16'))).toBe(Math.ceil((281 * 5) / 7) + 10);
  });

  it('stays within what the backend serves', () => {
    expect(historyDaysSince('2026-10-15', new Date('2026-10-16'))).toBe(21);
    expect(historyDaysSince('2010-01-01', new Date('2026-10-16'))).toBe(1260);
  });
});

describe('Performance', () => {
  let fixture: ComponentFixture<Performance>;
  let page: HTMLElement;
  let getTrades: Mock<(accountId: string) => Observable<ClientTradeResponse[]>>;
  let getHistory: Mock<(symbol: string, days?: number) => Observable<PricePoint[]>>;

  const HISTORY = [point('2026-10-01', 100), point('2026-10-02', 110)];

  async function render(
    options: {
      positions?: Position[];
      trades?: Observable<ClientTradeResponse[]>;
      history?: Observable<PricePoint[]>;
      accountId?: string | null;
    } = {},
  ) {
    getTrades = vi.fn(() => options.trades ?? of([trade('AAPL', 'BUY', 2, 100, '2026-10-01')]));
    getHistory = vi.fn(() => options.history ?? of(HISTORY));
    const accountId = options.accountId === undefined ? 'ACC1' : options.accountId;
    await TestBed.configureTestingModule({
      imports: [Performance],
      providers: [
        { provide: Auth, useValue: { currentUser: signal(accountId ? { accountId } : null) } },
        { provide: Orders, useValue: { getTradesByAccount: getTrades } },
        { provide: Instruments, useValue: { getHistory } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Performance);
    fixture.componentRef.setInput('positions', options.positions ?? [position('AAPL', 2)]);
    await settle();
    page = fixture.nativeElement;
  }

  async function settle() {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  async function click(selector: string) {
    (page.querySelector(selector) as HTMLButtonElement).click();
    await settle();
  }

  const changeText = () =>
    page.querySelector('[data-testid="performance-change"]')!.textContent!.replace(/\s+/g, ' ');

  describe('current holdings', () => {
    it('is the default view and charts today\'s holdings over the past year', async () => {
      await render();

      expect(page.querySelector('[data-testid="performance-view-holdings"]')!.getAttribute('aria-pressed')).toBe('true');
      expect(changeText()).toContain('+$20.00 (+10.00%) over 1Y');
      expect(page.querySelector('[data-testid="performance-chart"]')).not.toBeNull();
      expect(page.querySelector('[data-testid="performance-range-All"]')).toBeNull();
      expect(getTrades).not.toHaveBeenCalled();
    });

    it('says when nothing is held', async () => {
      await render({ positions: [] });

      expect(page.textContent).toContain("You don't hold anything yet.");
    });

    it('offers a retry when history fails to load', async () => {
      await render({ history: throwError(() => new Error('down')) });

      expect(page.querySelector('[role="alert"]')!.textContent).toContain("Couldn't load price history");

      getHistory.mockImplementation(() => of(HISTORY));
      await click('[role="alert"] button');

      expect(page.querySelector('[role="alert"]')).toBeNull();
      expect(page.querySelector('[data-testid="performance-chart"]')).not.toBeNull();
    });
  });

  describe('trade history', () => {
    it('shows the all-time return since the first trade', async () => {
      await render();
      await click('[data-testid="performance-view-trades"]');

      expect(getTrades).toHaveBeenCalledWith('ACC1');
      expect(changeText()).toContain('+$20.00 (+10.00%) all time');
      expect(page.querySelector('[data-testid="performance-range-All"]')).not.toBeNull();
      expect(page.textContent).toContain('since your first trade on 1 Oct 2026');
    });

    it('only fetches trades once when switching back and forth', async () => {
      await render();
      await click('[data-testid="performance-view-trades"]');
      await click('[data-testid="performance-view-holdings"]');
      await click('[data-testid="performance-view-trades"]');

      expect(getTrades).toHaveBeenCalledTimes(1);
    });

    it('says when there are no trades yet', async () => {
      await render({ trades: of([]) });
      await click('[data-testid="performance-view-trades"]');

      expect(page.textContent).toContain('No trades yet.');
    });

    it('offers a retry when trades fail to load', async () => {
      await render({ trades: throwError(() => new Error('down')) });
      await click('[data-testid="performance-view-trades"]');

      expect(page.querySelector('[role="alert"]')!.textContent).toContain("Couldn't load your trade history");

      getTrades.mockImplementation(() => of([trade('AAPL', 'BUY', 2, 100, '2026-10-01')]));
      await click('[role="alert"] button');

      expect(page.querySelector('[role="alert"]')).toBeNull();
      expect(page.querySelector('[data-testid="performance-chart"]')).not.toBeNull();
    });

    it('shows an error when the user has no account', async () => {
      await render({ accountId: null });
      await click('[data-testid="performance-view-trades"]');

      expect(page.querySelector('[role="alert"]')!.textContent).toContain('No trading account');
    });
  });
});
