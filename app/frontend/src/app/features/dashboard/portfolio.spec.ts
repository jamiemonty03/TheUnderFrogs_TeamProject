import { LatestPrice, LivePrice } from '../../services/instruments';
import { Position } from '../../services/positions';
import { Holding, marketValue, percentOf, sortHoldings, summarise, toHoldings, unrealisedPnl } from './portfolio';

const ACCOUNT = 'ACC0013';
const position = (symbol: string, quantity: number, averageCost: number): Position => ({
  accountId: ACCOUNT,
  symbol,
  quantity,
  averageCost,
});
const price = (symbol: string, value: number | null, assetClass: LatestPrice['assetClass'] = 'Equity'): LatestPrice => ({
  symbol,
  name: `${symbol} name`,
  assetClass,
  price: value,
  previousClose: null,
  tradeDate: null,
});
const prices = (...list: LatestPrice[]) => new Map(list.map((p) => [p.symbol, p]));

describe('marketValue and unrealisedPnl', () => {
  it('values a holding as quantity × price', () => {
    expect(marketValue(40, 189.84)).toBeCloseTo(7593.6);
  });

  it('returns a gain when the price is above average cost', () => {
    expect(unrealisedPnl(40, 171.2, 189.84)).toBeCloseTo(745.6);
  });

  it('returns a loss when the price is below average cost', () => {
    expect(unrealisedPnl(12, 260, 238.75)).toBeCloseTo(-255);
  });

  it('percentOf returns null for a zero base instead of dividing by zero', () => {
    expect(percentOf(5, 0)).toBeNull();
    expect(percentOf(25, 200)).toBe(12.5);
  });
});

describe('toHoldings', () => {
  it('values every position and works out P&L, P&L % and weight', () => {
    const [aapl, tsla] = toHoldings(
      [position('AAPL', 40, 171.2), position('TSLA', 12, 260)],
      prices(price('AAPL', 189.84), price('TSLA', 238.75)),
    );

    expect(aapl).toMatchObject({ symbol: 'AAPL', name: 'AAPL name', price: 189.84, costBasis: 6848 });
    expect(aapl.marketValue).toBeCloseTo(7593.6);
    expect(aapl.unrealisedPnl).toBeCloseTo(745.6);
    expect(aapl.pnlPercent).toBeCloseTo(10.8879, 3);
    expect(tsla.unrealisedPnl).toBeCloseTo(-255);
    expect(tsla.pnlPercent).toBeCloseTo(-8.1731, 3);
    expect(aapl.weight! + tsla.weight!).toBeCloseTo(100);
    expect(aapl.weight).toBeCloseTo((7593.6 / (7593.6 + 2865)) * 100);
  });

  it('values bond ETFs per unit, the same as any other instrument', () => {
    const [bnd] = toHoldings([position('BND', 40, 73.1)], prices(price('BND', 72.55, 'Bond')));

    expect(bnd.marketValue).toBeCloseTo(2902);
    expect(bnd.unrealisedPnl).toBeCloseTo(-22);
  });

  it('leaves price-based figures empty when a symbol has no price', () => {
    const [aapl, agg] = toHoldings(
      [position('AAPL', 10, 100), position('AGG', 5, 98)],
      prices(price('AAPL', 110), price('AGG', null, 'Bond')),
    );

    expect(agg).toMatchObject({ price: null, marketValue: null, unrealisedPnl: null, pnlPercent: null, weight: null });
    expect(aapl.weight).toBe(100);
  });

  it('drops positions that have been sold down to zero', () => {
    expect(toHoldings([position('AAPL', 0, 100)], prices(price('AAPL', 110)))).toEqual([]);
  });
});

describe('toHoldings with live prices', () => {
  const quote = (symbol: string, value: number): LivePrice => ({
    symbol,
    price: value,
    bid: null,
    ask: null,
    quoteTime: '2026-10-13T14:30:00Z',
  });

  it('values a holding at its live quote when there is one', () => {
    const [aapl] = toHoldings(
      [position('AAPL', 40, 171.2)],
      prices(price('AAPL', 189.84)),
      new Map([['AAPL', quote('AAPL', 200)]]),
    );

    expect(aapl).toMatchObject({ price: 200, priceSource: 'live', marketValue: 8000 });
    expect(aapl.unrealisedPnl).toBeCloseTo(1152);
  });

  it('falls back to the daily close and says so', () => {
    const [aapl, tsla] = toHoldings(
      [position('AAPL', 40, 171.2), position('TSLA', 12, 260)],
      prices(price('AAPL', 189.84), price('TSLA', 238.75)),
      new Map([['AAPL', quote('AAPL', 200)]]),
    );

    expect(aapl.priceSource).toBe('live');
    expect(tsla).toMatchObject({ price: 238.75, priceSource: 'close' });
  });

  it('uses a live quote even when there is no daily close', () => {
    const [agg] = toHoldings([position('AGG', 5, 98)], prices(), new Map([['AGG', quote('AGG', 99)]]));

    expect(agg).toMatchObject({ price: 99, priceSource: 'live', marketValue: 495 });
  });

  it('has no price source when neither is available', () => {
    const [agg] = toHoldings([position('AGG', 5, 98)], prices());

    expect(agg).toMatchObject({ price: null, priceSource: null });
  });
});

describe('summarise', () => {
  it('totals market value and unrealised P&L across holdings', () => {
    const summary = summarise(
      toHoldings(
        [position('AAPL', 40, 171.2), position('TSLA', 12, 260)],
        prices(price('AAPL', 189.84), price('TSLA', 238.75)),
      ),
    );

    expect(summary.invested).toBeCloseTo(10458.6);
    expect(summary.unrealisedPnl).toBeCloseTo(490.6);
    expect(summary.pnlPercent).toBeCloseTo((490.6 / 9968) * 100);
    expect(summary.positions).toBe(2);
    expect(summary.unpriced).toBe(0);
  });

  it('counts unpriced holdings separately and leaves them out of the totals', () => {
    const summary = summarise(
      toHoldings([position('AAPL', 10, 100), position('AGG', 5, 98)], prices(price('AAPL', 110))),
    );

    expect(summary).toMatchObject({ invested: 1100, unrealisedPnl: 100, pnlPercent: 10, positions: 2, unpriced: 1 });
  });

  it('is all zeros for an empty portfolio', () => {
    expect(summarise([])).toEqual({ invested: 0, unrealisedPnl: 0, pnlPercent: null, positions: 0, unpriced: 0 });
  });
});

describe('sortHoldings', () => {
  const holdings: Holding[] = toHoldings(
    [position('TSLA', 12, 260), position('AAPL', 40, 171.2), position('AGG', 5, 98)],
    prices(price('AAPL', 189.84), price('TSLA', 238.75)),
  );
  const symbols = (list: Holding[]) => list.map((h) => h.symbol);

  it('keeps the original order when no sort is active', () => {
    expect(symbols(sortHoldings(holdings, { active: '', direction: '' }))).toEqual(['TSLA', 'AAPL', 'AGG']);
  });

  it('sorts text columns alphabetically', () => {
    expect(symbols(sortHoldings(holdings, { active: 'symbol', direction: 'asc' }))).toEqual(['AAPL', 'AGG', 'TSLA']);
  });

  it('sorts numbers either way and always puts missing values last', () => {
    expect(symbols(sortHoldings(holdings, { active: 'marketValue', direction: 'desc' }))).toEqual([
      'AAPL',
      'TSLA',
      'AGG',
    ]);
    expect(symbols(sortHoldings(holdings, { active: 'marketValue', direction: 'asc' }))).toEqual([
      'TSLA',
      'AAPL',
      'AGG',
    ]);
  });
});
