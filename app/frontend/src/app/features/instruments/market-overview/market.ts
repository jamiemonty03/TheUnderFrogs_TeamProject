import { AssetClass, DayChange, LatestPrice, LivePrice, dayChange } from '../../../services/instruments';

export interface MarketRow {
  symbol: string;
  name: string;
  assetClass: AssetClass;
  price: number | null;
  priceSource: 'live' | 'close' | null;
  change: DayChange | null;
  tradeDate: string | null;
}

export interface IndexTile {
  label: string;
  row: MarketRow | null;
}

export const INDEX_PROXIES = [
  { label: 'S&P 500', symbol: 'SPY' },
  { label: 'NASDAQ', symbol: 'QQQ' },
] as const;

/** A live quote, when there is one, replaces the daily close; the change is still from the previous close. */
export function toMarketRows(
  prices: ReadonlyMap<string, LatestPrice>,
  live: ReadonlyMap<string, LivePrice> = new Map(),
): MarketRow[] {
  return [...prices.values()]
    .map((latest) => {
      const quote = live.get(latest.symbol);
      const price = quote ? quote.price : latest.price;
      return {
        symbol: latest.symbol,
        name: latest.name,
        assetClass: latest.assetClass,
        price,
        priceSource: quote ? 'live' : price === null ? null : 'close',
        change: dayChange({ price, previousClose: latest.previousClose }),
        tradeDate: latest.tradeDate,
      } satisfies MarketRow;
    })
    .sort((a, b) => a.symbol.localeCompare(b.symbol));
}

export function filterRows(rows: MarketRow[], assetClass: AssetClass, query: string): MarketRow[] {
  const needle = query.trim().toLowerCase();
  return rows.filter(
    (row) =>
      row.assetClass === assetClass &&
      (!needle || row.symbol.toLowerCase().includes(needle) || row.name.toLowerCase().includes(needle)),
  );
}

export function movers(rows: MarketRow[]): { hopper: MarketRow | null; sinker: MarketRow | null } {
  const moved = rows.filter((row) => row.change !== null);
  const best = moved.reduce<MarketRow | null>((top, row) => (!top || row.change!.percent > top.change!.percent ? row : top), null);
  const worst = moved.reduce<MarketRow | null>((low, row) => (!low || row.change!.percent < low.change!.percent ? row : low), null);
  return {
    hopper: best && best.change!.percent > 0 ? best : null,
    sinker: worst && worst.change!.percent < 0 ? worst : null,
  };
}

export function indexTiles(rows: MarketRow[]): IndexTile[] {
  return INDEX_PROXIES.map(({ label, symbol }) => ({
    label,
    row: rows.find((row) => row.symbol === symbol) ?? null,
  }));
}

export function latestTradeDate(rows: MarketRow[]): string | null {
  return rows.reduce<string | null>(
    (latest, row) => (row.tradeDate && (!latest || row.tradeDate > latest) ? row.tradeDate : latest),
    null,
  );
}
