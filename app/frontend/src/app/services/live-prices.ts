import { Injectable } from '@angular/core';
import { EMPTY, Observable, catchError, defer, distinctUntilChanged, endWith, map, repeat, scan, shareReplay } from 'rxjs';
import { webSocket } from 'rxjs/webSocket';

import { LivePrice, LivePriceFeed } from './interfaces/instruments.interfaces';

export const LIVE_PRICES_PATH = '/api/instruments/live-prices/stream';
export const RECONNECT_MS = 5_000;

/**
 * Live quotes pushed from the instruments service over a WebSocket.
 * Every watcher shares one connection: it opens with the first watcher and
 * closes after the last one unsubscribes. The server sends every held quote
 * on connect, then each new quote as it arrives.
 *
 *   inject(LivePrices).watch(['AAPL', 'SPY']).subscribe(({ prices, stale }) => ...)
 */
@Injectable({ providedIn: 'root' })
export class LivePrices {
  private readonly feed$: Observable<LivePriceFeed> = defer(() => webSocket<LivePrice[]>(socketUrl())).pipe(
    catchError(() => EMPTY),
    endWith(null),
    repeat({ delay: RECONNECT_MS }),
    scan(
      (feed: LivePriceFeed, quotes: LivePrice[] | null) => {
        if (quotes === null) {
          return { prices: feed.prices, stale: true };
        }
        const prices = new Map(feed.prices);
        quotes.forEach((q) => prices.set(q.symbol, q));
        return { prices, stale: false };
      },
      { prices: new Map<string, LivePrice>(), stale: false },
    ),
    shareReplay({ bufferSize: 1, refCount: true }),
  );

  watch(symbols?: string[]): Observable<LivePriceFeed> {
    if (!symbols) {
      return this.feed$;
    }
    if (symbols.length === 0) {
      return EMPTY;
    }
    return this.feed$.pipe(
      map(({ prices, stale }) => ({
        prices: new Map(symbols.filter((s) => prices.has(s)).map((s) => [s, prices.get(s)!])),
        stale,
      })),
      distinctUntilChanged((a, b) => a.stale === b.stale && symbols.every((s) => a.prices.get(s) === b.prices.get(s))),
    );
  }
}

function socketUrl(): string {
  const scheme = location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${scheme}//${location.host}${LIVE_PRICES_PATH}`;
}
