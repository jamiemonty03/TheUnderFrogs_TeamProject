import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Instruments } from '../../../services/instruments';
import {
  BondResponse,
  EtfResponse,
  InstrumentSummary,
  LivePrice,
  StockResponse,
} from '../../../services/interfaces/instruments.interfaces';
import { Portfolios } from '../../../services/portfolios';
import { Positions } from '../../../services/positions';
import { InstrumentsDetail, factsFor } from './instruments-detail';

const summary = (
  symbol: string,
  assetClass: InstrumentSummary['assetClass'],
): InstrumentSummary => ({
  symbol,
  name: `${symbol} name`,
  assetClass,
  currency: 'USD',
  exchange: 'NASDAQ',
  tradable: true,
});

const AAPL = {
  symbol: 'AAPL',
  name: 'Apple Inc.',
  price: 346.19,
  previousClose: 340.98,
  sector: 'Technology',
  industry: 'Consumer Electronics',
  trailingPe: 39.1507,
  trailingEps: 8.76,
  dividendRate: null,
  marketCap: 5005219725312,
} as unknown as StockResponse;

const DIA = {
  symbol: 'DIA',
  name: 'SPDR Dow Jones',
  price: 460,
  previousClose: 455,
  category: 'Large Value',
  fundFamily: 'State Street Investment Management',
  netExpenseRatio: 0.16,
  ytdReturn: 6.94214,
} as unknown as EtfResponse;

const AGG = {
  symbol: 'AGG',
  name: 'iShares Core U.S. Aggregate Bond',
  price: 94.7,
  previousClose: 94.9,
  category: 'Intermediate Core Bond',
  netExpenseRatio: 0.03,
  ytdReturn: -2.84047,
  distributionYield: 0.0419,
  yieldToMaturity: null,
} as unknown as BondResponse;

describe('InstrumentsDetail', () => {
  let fixture: ComponentFixture<InstrumentsDetail>;
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let instruments: Record<string, ReturnType<typeof vi.fn>>;
  let live: BehaviorSubject<{ prices: Map<string, LivePrice>; stale: boolean }>;

  const el = (testId: string) =>
    fixture.nativeElement.querySelector(`[data-testid="${testId}"]`) as HTMLElement | null;
  const text = (testId: string) => el(testId)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  const render = async () => {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    params = new BehaviorSubject(convertToParamMap({ id: 'aapl' }));
    live = new BehaviorSubject<{ prices: Map<string, LivePrice>; stale: boolean }>({
      prices: new Map<string, LivePrice>(),
      stale: false,
    });
    instruments = {
      getInstrument: vi.fn((symbol: string) =>
        symbol === 'DIA'
          ? of(summary('DIA', 'ETF'))
          : symbol === 'AGG'
            ? of(summary('AGG', 'Bond'))
            : of(summary(symbol, 'Equity')),
      ),
      getStock: vi.fn(() => of(AAPL)),
      getEtf: vi.fn(() => of(DIA)),
      getBond: vi.fn(() => of(AGG)),
      watchLivePrices: vi.fn(() => live),
      getNews: vi.fn(() =>
        of([
          {
            headline: 'Apple beats on earnings',
            articleUrl: 'https://example.com/apple',
            publishedAt: '2026-10-14T13:05:00Z',
            updatedAt: '2026-10-14T13:05:00Z',
            symbols: ['AAPL'],
          },
        ]),
      ),
      getHistory: vi.fn(() =>
        of([
          { date: '2026-10-13', close: 341.27, movingAvg20: 336.28, movingAvg50: 323.77 },
          { date: '2026-10-14', close: 342.96, movingAvg20: 336.81, movingAvg50: 324.44 },
        ]),
      ),
    };

    await TestBed.configureTestingModule({
      imports: [InstrumentsDetail],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: params } },
        { provide: Instruments, useValue: instruments },
        {
          provide: Positions,
          useValue: {
            getPositions: vi.fn(() =>
              of([
                {
                  accountId: 'ACC0011',
                  portfolioId: 'PF0011',
                  symbol: 'AAPL',
                  quantity: 12,
                  averageCost: 345.92,
                },
                {
                  accountId: 'ACC0011',
                  portfolioId: 'PF0013',
                  symbol: 'AAPL',
                  quantity: 2,
                  averageCost: 300,
                },
                {
                  accountId: 'ACC0011',
                  portfolioId: 'PF0011',
                  symbol: 'KO',
                  quantity: 4,
                  averageCost: 88,
                },
              ]),
            ),
          },
        },
        {
          provide: Portfolios,
          useValue: {
            getPortfolios: vi.fn(() =>
              of([
                { portfolioId: 'PF0011', name: 'Main', defaultPortfolio: true },
                { portfolioId: 'PF0013', name: 'Golden Apple', defaultPortfolio: false },
              ]),
            ),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(InstrumentsDetail);
    await render();
  });

  it('shows the instrument, its latest close and the day change', () => {
    expect(instruments['getInstrument']).toHaveBeenCalledWith('AAPL');
    expect(text('instrument-symbol')).toBe('AAPL');
    expect(text('instrument-meta')).toBe('Equity · NASDAQ · USD');
    expect(text('instrument-price')).toBe('$346.19');
    expect(fixture.nativeElement.textContent).toContain('Close');
    expect(text('instrument-change')).toBe('+$5.21 (+1.53%) today');
  });

  it("shows a stock's key facts, with a dash for missing ones", () => {
    expect(instruments['getStock']).toHaveBeenCalledWith('AAPL');
    expect(text('fact-Sector')).toBe('Technology');
    expect(text('fact-P/E (trailing)')).toBe('39.15');
    expect(text('fact-EPS (trailing)')).toBe('$8.76');
    expect(text('fact-Market cap')).toBe('$5.01T');
  });

  it('uses the live price when there is one', async () => {
    live.next({
      prices: new Map([['AAPL', { symbol: 'AAPL', price: 351.5 } as LivePrice]]),
      stale: false,
    });
    await render();

    expect(text('instrument-price')).toBe('$351.50');
    expect(fixture.nativeElement.textContent).toContain('Live');
  });

  it('lists your holdings of it in each portfolio, with a Sell for each', () => {
    expect(text('holding-Main')).toContain('12 at $345.92');
    expect(text('holding-Golden Apple')).toContain('2 at $300.00');
    expect(el('holding-Main')!.querySelector('a')!.getAttribute('href')).toBe(
      '/orders?new=true&symbol=AAPL&side=SELL&portfolioId=PF0011',
    );
  });

  it('Buy and Sell open the order dialog for it, like the dashboard', () => {
    expect(el('instrument-buy')!.getAttribute('href')).toBe(
      '/orders?new=true&symbol=AAPL&side=BUY',
    );
    expect(el('instrument-sell')!.getAttribute('href')).toBe(
      '/orders?new=true&symbol=AAPL&side=SELL',
    );
  });

  it("shows an ETF's key facts from the ETF endpoint", async () => {
    params.next(convertToParamMap({ id: 'DIA' }));
    await render();

    expect(instruments['getEtf']).toHaveBeenCalledWith('DIA');
    expect(text('fact-Fund family')).toBe('State Street Investment Management');
    expect(text('fact-Expense ratio')).toBe('0.16%');
    expect(text('fact-YTD return')).toBe('6.94%');
    expect(text('instrument-no-holdings')).toBe("You don't hold any DIA.");
  });

  it("shows a bond's key facts from the bond endpoint", async () => {
    params.next(convertToParamMap({ id: 'AGG' }));
    await render();

    expect(instruments['getBond']).toHaveBeenCalledWith('AGG');
    expect(text('fact-Distribution yield')).toBe('4.19%');
    expect(text('fact-Expense ratio')).toBe('0.03%');
    expect(text('fact-YTD return')).toBe('-2.84%');
  });

  it('shows the price history chart', () => {
    expect(instruments['getHistory']).toHaveBeenCalledWith('AAPL');
    expect(el('price-chart')).not.toBeNull();
    expect(text('chart-change')).toBe('+$1.69 (+0.50%) over 1Y');
  });

  it('says so when the price history cannot be loaded, and the rest of the page still works', async () => {
    instruments['getHistory'].mockReturnValue(throwError(() => new Error('down')));
    params.next(convertToParamMap({ id: 'AAPL' }));
    await render();

    expect(text('history-error')).toBe("Price history couldn't be loaded.");
    expect(text('fact-Sector')).toBe('Technology');
  });

  it('shows the latest news for this instrument, opening each article in a new tab', () => {
    expect(instruments['getNews']).toHaveBeenCalledWith(5, 'AAPL');
    const link = el('instrument-news')!.querySelector('a')!;
    expect(link.textContent?.trim()).toBe('Apple beats on earnings');
    expect(link.getAttribute('href')).toBe('https://example.com/apple');
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('says when there is no recent news, or it cannot be loaded, and the rest of the page still works', async () => {
    instruments['getNews'].mockReturnValue(of([]));
    params.next(convertToParamMap({ id: 'DIA' }));
    await render();
    expect(text('news-empty')).toBe('No recent news for DIA.');

    instruments['getNews'].mockReturnValue(throwError(() => new Error('down')));
    params.next(convertToParamMap({ id: 'AAPL' }));
    await render();
    expect(text('news-error')).toBe("News couldn't be loaded.");
    expect(text('fact-Sector')).toBe('Technology');
  });

  it('says so when the symbol does not exist', async () => {
    instruments['getInstrument'].mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 404 })),
    );
    params.next(convertToParamMap({ id: 'NOPE' }));
    await render();

    expect(text('instrument-not-found')).toContain("We couldn't find an instrument called NOPE.");
  });

  it('shows an error with a retry when the instrument cannot be loaded', async () => {
    instruments['getInstrument'].mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );
    params.next(convertToParamMap({ id: 'AAPL' }));
    await render();
    expect(text('instrument-error')).toContain("Couldn't load AAPL.");

    instruments['getInstrument'].mockReturnValue(of(summary('AAPL', 'Equity')));
    (el('instrument-error')!.querySelector('button') as HTMLButtonElement).click();
    await render();

    expect(text('instrument-symbol')).toBe('AAPL');
  });

  it('shows the key facts error, not a crash, for a type it does not know', async () => {
    instruments['getInstrument'].mockReturnValue(
      of({ ...summary('XYZ', 'Equity'), assetClass: 'Crypto' as never }),
    );
    params.next(convertToParamMap({ id: 'XYZ' }));
    await render();

    expect(text('facts-error')).toContain("Key facts couldn't be loaded");
    expect(text('instrument-symbol')).toBe('XYZ');
  });

  it('still lets you trade when the key facts fail to load', async () => {
    instruments['getStock'].mockReturnValue(throwError(() => new Error('down')));
    params.next(convertToParamMap({ id: 'AAPL' }));
    await render();

    expect(text('facts-error')).toContain("Key facts couldn't be loaded");
    expect(el('instrument-buy')).not.toBeNull();
  });
});

describe('factsFor', () => {
  it('shows a dash for every missing value', () => {
    const facts = factsFor('Equity', { symbol: 'X', name: 'X' } as unknown as StockResponse);

    expect(facts.map((f) => f.value)).toEqual(['-', '-', '-', '-', '-']);
  });
});
