import { Sort } from '@angular/material/sort';
import { LatestPrice, LivePrice } from '../../services/instruments';
import { Position } from '../../services/positions';

export interface Holding {
  symbol: string;
  name: string;
  quantity: number;
  averageCost: number;
  price: number | null;
  priceSource: 'live' | 'close' | null;
  costBasis: number;
  marketValue: number | null;
  unrealisedPnl: number | null;
  pnlPercent: number | null;
  dailyPnl: number | null;
  dailyPnlPercent: number | null;
  dailyCostBasis: number | null;
  weight: number | null;
}

export interface PortfolioSummary {
  invested: number;
  unrealisedPnl: number;
  pnlPercent: number | null;
  dailyPnl: number;
  dailyPnlPercent: number | null;
  positions: number;
  unpriced: number;
}

export function marketValue(quantity: number, price: number): number {
  return quantity * price;
}

export function unrealisedPnl(quantity: number, averageCost: number, price: number): number {
  return marketValue(quantity, price) - quantity * averageCost;
}

export function percentOf(part: number, whole: number): number | null {
  return whole === 0 ? null : (part / whole) * 100;
}

export function toHoldings(
  positions: Position[],
  prices: ReadonlyMap<string, LatestPrice>,
  live: ReadonlyMap<string, LivePrice> = new Map(),
): Holding[] {
  const held = positions
    .filter((position) => position.quantity > 0)
    .map((position) => {
      const latest = prices.get(position.symbol);
      const quote = live.get(position.symbol);
      const close = latest?.price ?? null;
      const price = quote ? quote.price : close;
      const priceSource: Holding['priceSource'] = quote ? 'live' : close === null ? null : 'close';
      const costBasis = position.quantity * position.averageCost;
      const value = price === null ? null : marketValue(position.quantity, price);
      const pnl = price === null ? null : unrealisedPnl(position.quantity, position.averageCost, price);
      const dailyPnl = price === null || latest?.previousClose == null
        ? null
        : marketValue(position.quantity, price - latest.previousClose);
      const dailyCost = latest?.previousClose == null ? null : marketValue(position.quantity, latest.previousClose);
      return {
        symbol: position.symbol,
        name: latest?.name ?? '',
        quantity: position.quantity,
        averageCost: position.averageCost,
        price,
        priceSource,
        costBasis,
        marketValue: value,
        unrealisedPnl: pnl,
        pnlPercent: pnl === null ? null : percentOf(pnl, costBasis),
        dailyPnl,
        dailyPnlPercent: dailyPnl === null || dailyCost === null ? null : percentOf(dailyPnl, dailyCost),
        dailyCostBasis: dailyCost,
        weight: null as number | null,
      };
    });

  const totalMarketValue = held.reduce(
    (accumulatedValue, holding) => accumulatedValue + (holding.marketValue ?? 0),
    0,
  );
  return held.map((holding) => ({
    ...holding,
    weight:
      holding.marketValue === null ? null : percentOf(holding.marketValue, totalMarketValue),
  }));
}

export function summarise(holdings: Holding[]): PortfolioSummary {
  let invested = 0;
  let pnl = 0;
  let pricedCost = 0;
  let dailyPnl = 0;
  let dailyCost = 0;
  let unpriced = 0;
  for (const holding of holdings) {
    if (holding.marketValue === null || holding.unrealisedPnl === null) {
      unpriced++;
      continue;
    }
    invested += holding.marketValue;
    pnl += holding.unrealisedPnl;
    pricedCost += holding.costBasis;
    if (holding.dailyPnl !== null) {
      dailyPnl += holding.dailyPnl;
      dailyCost += holding.dailyCostBasis ?? 0;
    }
  }
  return {
    invested,
    unrealisedPnl: pnl,
    pnlPercent: percentOf(pnl, pricedCost),
    dailyPnl,
    dailyPnlPercent: percentOf(dailyPnl, dailyCost),
    positions: holdings.length,
    unpriced,
  };
}

export function sortHoldings(holdings: Holding[], sort: Sort): Holding[] {
  const key = sort.active as keyof Holding;
  if (!sort.direction || !key) {
    return holdings;
  }
  const directionMultiplier = sort.direction === 'asc' ? 1 : -1;
  return [...holdings].sort((firstHolding, secondHolding) => {
    const firstValue = firstHolding[key];
    const secondValue = secondHolding[key];
    if (firstValue === null || secondValue === null) {
      return firstValue === secondValue ? 0 : firstValue === null ? 1 : -1;
    }
    return typeof firstValue === 'string'
      ? firstValue.localeCompare(secondValue as string) * directionMultiplier
      : ((firstValue as number) - (secondValue as number)) * directionMultiplier;
  });
}
