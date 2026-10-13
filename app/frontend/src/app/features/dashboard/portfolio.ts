import { Sort } from '@angular/material/sort';
import { LatestPrice } from '../../services/instruments';
import { Position } from '../../services/positions';

export interface Holding {
  symbol: string;
  name: string;
  quantity: number;
  averageCost: number;
  price: number | null;
  costBasis: number;
  marketValue: number | null;
  unrealisedPnl: number | null;
  pnlPercent: number | null;
  weight: number | null;
}

export interface PortfolioSummary {
  invested: number;
  unrealisedPnl: number;
  pnlPercent: number | null;
  positions: number;
  unpriced: number;
}

// Every instrument, bonds included, is valued as quantity × price: our bonds are bond ETFs priced per unit.
export function marketValue(quantity: number, price: number): number {
  return quantity * price;
}

export function unrealisedPnl(quantity: number, averageCost: number, price: number): number {
  return marketValue(quantity, price) - quantity * averageCost;
}

export function percentOf(part: number, whole: number): number | null {
  return whole === 0 ? null : (part / whole) * 100;
}

// Weight is each holding's share of the total market value of priced holdings (cash excluded).
export function toHoldings(positions: Position[], prices: ReadonlyMap<string, LatestPrice>): Holding[] {
  const held = positions
    .filter((p) => p.quantity > 0)
    .map((p) => {
      const latest = prices.get(p.symbol);
      const price = latest?.price ?? null;
      const costBasis = p.quantity * p.averageCost;
      const value = price === null ? null : marketValue(p.quantity, price);
      const pnl = price === null ? null : unrealisedPnl(p.quantity, p.averageCost, price);
      return {
        symbol: p.symbol,
        name: latest?.name ?? '',
        quantity: p.quantity,
        averageCost: p.averageCost,
        price,
        costBasis,
        marketValue: value,
        unrealisedPnl: pnl,
        pnlPercent: pnl === null ? null : percentOf(pnl, costBasis),
        weight: null as number | null,
      };
    });

  const total = held.reduce((sum, h) => sum + (h.marketValue ?? 0), 0);
  return held.map((h) => ({
    ...h,
    weight: h.marketValue === null ? null : percentOf(h.marketValue, total),
  }));
}

// Unpriced holdings are left out of the totals and counted in `unpriced` instead.
export function summarise(holdings: Holding[]): PortfolioSummary {
  let invested = 0;
  let pnl = 0;
  let pricedCost = 0;
  let unpriced = 0;
  for (const h of holdings) {
    if (h.marketValue === null || h.unrealisedPnl === null) {
      unpriced++;
      continue;
    }
    invested += h.marketValue;
    pnl += h.unrealisedPnl;
    pricedCost += h.costBasis;
  }
  return {
    invested,
    unrealisedPnl: pnl,
    pnlPercent: percentOf(pnl, pricedCost),
    positions: holdings.length,
    unpriced,
  };
}

// Missing values always sort last, whichever way the column is sorted.
export function sortHoldings(holdings: Holding[], sort: Sort): Holding[] {
  const key = sort.active as keyof Holding;
  if (!sort.direction || !key) {
    return holdings;
  }
  const dir = sort.direction === 'asc' ? 1 : -1;
  return [...holdings].sort((a, b) => {
    const x = a[key];
    const y = b[key];
    if (x === null || y === null) {
      return x === y ? 0 : x === null ? 1 : -1;
    }
    return typeof x === 'string' ? x.localeCompare(y as string) * dir : ((x as number) - (y as number)) * dir;
  });
}
