import { LatestPrice } from '../../../services/instruments';
import { filterRows, indexTiles, latestTradeDate, movers, toMarketRows } from './market';

const latest = (
  symbol: string,
  price: number | null,
  previousClose: number | null,
  assetClass: LatestPrice['assetClass'] = 'Equity',
  tradeDate: string | null = '2026-10-09T00:00:00',
): LatestPrice => ({ symbol, name: `${symbol} Corp.`, assetClass, price, previousClose, tradeDate });
const prices = (...list: LatestPrice[]) => new Map(list.map((p) => [p.symbol, p]));

describe('toMarketRows', () => {
  it('works out the change from the previous close and sorts by symbol', () => {
    const rows = toMarketRows(prices(latest('TSLA', 95, 100), latest('AAPL', 110, 100)));

    expect(rows.map((r) => r.symbol)).toEqual(['AAPL', 'TSLA']);
    expect(rows[0].change?.amount).toBeCloseTo(10);
    expect(rows[0].change?.percent).toBeCloseTo(10);
    expect(rows[1].change?.percent).toBeCloseTo(-5);
  });

  it('leaves the change empty when a close is missing', () => {
    expect(toMarketRows(prices(latest('AGG', null, null, 'Bond')))[0].change).toBeNull();
  });

  it('uses a live quote over the close and measures the change from the previous close', () => {
    const live = new Map([['AAPL', { symbol: 'AAPL', price: 105, bid: null, ask: null, quoteTime: '2026-10-13T14:30:00Z' }]]);
    const [aapl, tsla] = toMarketRows(prices(latest('AAPL', 110, 100), latest('TSLA', 95, 100)), live);

    expect(aapl.price).toBe(105);
    expect(aapl.priceSource).toBe('live');
    expect(aapl.change?.amount).toBeCloseTo(5);
    expect(tsla.priceSource).toBe('close');
  });

  it('marks a row with no price as having no source', () => {
    expect(toMarketRows(prices(latest('AGG', null, null, 'Bond')))[0].priceSource).toBeNull();
  });
});

describe('filterRows', () => {
  const rows = toMarketRows(
    prices(latest('AAPL', 1, 1), latest('NVDA', 1, 1), latest('SPY', 1, 1, 'ETF'), latest('TLT', 1, 1, 'Bond')),
  );

  it('keeps only the chosen asset class', () => {
    expect(filterRows(rows, 'ETF', '').map((r) => r.symbol)).toEqual(['SPY']);
  });

  it('matches the search against symbol or name, ignoring case', () => {
    expect(filterRows(rows, 'Equity', 'nv').map((r) => r.symbol)).toEqual(['NVDA']);
    expect(filterRows(rows, 'Equity', ' aapl corp ').map((r) => r.symbol)).toEqual(['AAPL']);
    expect(filterRows(rows, 'Equity', 'zzz')).toEqual([]);
  });
});

describe('movers', () => {
  it('picks the biggest % riser as top hopper and the biggest % faller as biggest sinker', () => {
    const { hopper, sinker } = movers(
      toMarketRows(prices(latest('NVDA', 103, 100), latest('AAPL', 101, 100), latest('TSLA', 98, 100), latest('KO', 99, 100))),
    );

    expect(hopper?.symbol).toBe('NVDA');
    expect(sinker?.symbol).toBe('TSLA');
  });

  it('has no hopper when nothing rose and no sinker when nothing fell', () => {
    expect(movers(toMarketRows(prices(latest('KO', 99, 100)))).hopper).toBeNull();
    expect(movers(toMarketRows(prices(latest('KO', 101, 100)))).sinker).toBeNull();
    expect(movers([])).toEqual({ hopper: null, sinker: null });
  });

  it('ignores rows without a change', () => {
    const { hopper, sinker } = movers(toMarketRows(prices(latest('AGG', null, null))));
    expect(hopper).toBeNull();
    expect(sinker).toBeNull();
  });
});

describe('indexTiles', () => {
  it('uses SPY for the S&P 500 and QQQ for the NASDAQ', () => {
    const tiles = indexTiles(toMarketRows(prices(latest('SPY', 571, 569, 'ETF'))));

    expect(tiles.map((t) => [t.label, t.row?.symbol ?? null])).toEqual([
      ['S&P 500', 'SPY'],
      ['NASDAQ', null],
    ]);
  });
});

describe('latestTradeDate', () => {
  it('returns the newest close date, or null when there is none', () => {
    const rows = toMarketRows(
      prices(latest('AAPL', 1, 1, 'Equity', '2026-10-08T00:00:00'), latest('KO', 1, 1, 'Equity', '2026-10-09T00:00:00'), latest('AGG', 1, 1, 'Bond', null)),
    );

    expect(latestTradeDate(rows)).toBe('2026-10-09T00:00:00');
    expect(latestTradeDate([])).toBeNull();
  });
});
