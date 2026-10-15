import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { LivePrice, LivePriceFeed } from './interfaces/instruments.interfaces';
import { LIVE_PRICES_PATH, LivePrices, RECONNECT_MS } from './live-prices';

class FakeSocket {
  static opened: FakeSocket[] = [];
  readyState = 0;
  onopen?: (e: Event) => void;
  onmessage?: (e: MessageEvent) => void;
  onclose?: (e: CloseEvent) => void;
  onerror?: (e: Event) => void;

  constructor(readonly url: string) {
    FakeSocket.opened.push(this);
  }

  close(): void {
    this.readyState = 3;
  }

  open(): void {
    this.readyState = 1;
    this.onopen?.(new Event('open'));
  }

  push(quotes: LivePrice[]): void {
    this.onmessage?.(new MessageEvent('message', { data: JSON.stringify(quotes) }));
  }

  drop(): void {
    this.readyState = 3;
    this.onclose?.(new CloseEvent('close', { wasClean: false }));
  }
}

const quote = (symbol: string, price: number): LivePrice => ({
  symbol,
  price,
  bid: null,
  ask: null,
  quoteTime: '2026-10-13T14:30:00Z',
});
const pricesOf = (feed: LivePriceFeed) => Object.fromEntries([...feed.prices].map(([s, q]) => [s, q.price]));

describe('LivePrices', () => {
  let service: LivePrices;
  const socket = () => FakeSocket.opened.at(-1)!;

  beforeEach(() => {
    FakeSocket.opened = [];
    vi.stubGlobal('WebSocket', FakeSocket);
    vi.useFakeTimers();
    service = TestBed.inject(LivePrices);
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('connects to the live-price stream and merges each message into the prices so far', () => {
    const feeds: LivePriceFeed[] = [];
    const sub = service.watch().subscribe((feed) => feeds.push(feed));

    expect(socket().url).toBe(`ws://${location.host}${LIVE_PRICES_PATH}`);
    socket().open();
    socket().push([quote('AAPL', 200), quote('SPY', 570)]);
    socket().push([quote('AAPL', 201)]);

    expect(feeds.map(pricesOf)).toEqual([
      { AAPL: 200, SPY: 570 },
      { AAPL: 201, SPY: 570 },
    ]);
    expect(feeds.every((f) => !f.stale)).toBe(true);
    sub.unsubscribe();
  });

  it('gives each watcher only its symbols, and stays quiet when others move', () => {
    const feeds: LivePriceFeed[] = [];
    const sub = service.watch(['AAPL']).subscribe((feed) => feeds.push(feed));
    socket().open();

    socket().push([quote('AAPL', 200), quote('SPY', 570)]);
    socket().push([quote('SPY', 571)]);
    socket().push([quote('AAPL', 202)]);

    expect(feeds.map(pricesOf)).toEqual([{ AAPL: 200 }, { AAPL: 202 }]);
    sub.unsubscribe();
  });

  it('shares one connection between watchers and closes it after the last one leaves', () => {
    const first = service.watch(['AAPL']).subscribe();
    const second = service.watch(['SPY']).subscribe();
    socket().open();
    expect(FakeSocket.opened).toHaveLength(1);

    first.unsubscribe();
    expect(socket().readyState).toBe(1);
    second.unsubscribe();
    expect(socket().readyState).toBe(3);
  });

  it('keeps the last prices and marks the feed stale when the connection drops, then reconnects', () => {
    const feeds: LivePriceFeed[] = [];
    const sub = service.watch(['AAPL']).subscribe((feed) => feeds.push(feed));
    socket().open();
    socket().push([quote('AAPL', 200)]);

    socket().drop();
    expect(feeds.at(-1)).toEqual({ prices: new Map([['AAPL', quote('AAPL', 200)]]), stale: true });

    vi.advanceTimersByTime(RECONNECT_MS);
    expect(FakeSocket.opened).toHaveLength(2);
    socket().open();
    socket().push([quote('AAPL', 205)]);

    expect(feeds.map((f) => [f.stale, f.prices.get('AAPL')?.price])).toEqual([
      [false, 200],
      [true, 200],
      [false, 205],
    ]);
    sub.unsubscribe();
  });

  it('with an empty list completes without connecting', () => {
    let completed = false;
    service.watch([]).subscribe({ complete: () => (completed = true) });

    expect(completed).toBe(true);
    expect(FakeSocket.opened).toHaveLength(0);
  });
});
