import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';
import {
  Instruments,
  LIVE_POLL_MS,
  LatestPrice,
  LivePrice,
  LivePriceFeed,
  PricedAsset,
  dayChange,
} from './instruments';

describe('Instruments', () => {
  let service: Instruments;
  let http: HttpTestingController;

  const aapl: PricedAsset = {
    symbol: 'AAPL',
    name: 'Apple Inc.',
    price: 336.64,
    previousClose: 340.42,
    tradeDate: '2026-10-09T00:00:00',
  };
  const spy: PricedAsset = {
    symbol: 'SPY',
    name: 'State Street SPDR S&P 500 ETF T',
    price: 773.5,
    previousClose: 761.69,
    tradeDate: '2026-10-09T00:00:00',
  };
  const agg: PricedAsset = {
    symbol: 'AGG',
    name: 'iShares Core U.S. Aggregate Bon',
    price: null,
    previousClose: null,
    tradeDate: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [Instruments, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Instruments);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('getLatestPrices merges stocks, ETFs and bonds into one map keyed by symbol', () => {
    let result: Map<string, LatestPrice> | undefined;
    service.getLatestPrices().subscribe((prices) => (result = prices));

    http.expectOne('/api/stocks').flush([aapl]);
    http.expectOne('/api/etfs').flush([spy]);
    http.expectOne('/api/bonds').flush([agg]);

    expect(result?.size).toBe(3);
    expect(result?.get('AAPL')).toEqual({ ...aapl, assetClass: 'Equity' });
    expect(result?.get('SPY')?.assetClass).toBe('ETF');
    expect(result?.get('AGG')).toEqual({ ...agg, assetClass: 'Bond' });
  });

  it('getLivePrices asks the live-prices endpoint for the given symbols', () => {
    let result: unknown;
    service.getLivePrices(['AAPL', 'SPY']).subscribe((quotes) => (result = quotes));

    const req = http.expectOne((r) => r.url === '/api/instruments/live-prices');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('symbols')).toBe('AAPL,SPY');
    const quotes = [
      { symbol: 'AAPL', price: 189.85, bid: 189.8, ask: 189.9, quoteTime: '2026-10-13T14:30:00Z' },
    ];
    req.flush(quotes);

    expect(result).toEqual(quotes);
  });

  it('getLivePrices with no symbols asks for every tracked symbol', () => {
    service.getLivePrices().subscribe();

    const req = http.expectOne((r) => r.url === '/api/instruments/live-prices');
    expect(req.request.params.has('symbols')).toBe(false);
    req.flush([]);
  });

  it('getLivePrices with an empty list makes no request', () => {
    let result: LivePrice[] | undefined;
    service.getLivePrices([]).subscribe((quotes) => (result = quotes));

    http.expectNone(() => true);
    expect(result).toEqual([]);
  });

  describe('watchLivePrices', () => {
    const quote = (symbol: string, price: number): LivePrice => ({
      symbol,
      price,
      bid: null,
      ask: null,
      quoteTime: '2026-10-13T14:30:00Z',
    });
    const nextPoll = () => http.expectOne((r) => r.url === '/api/instruments/live-prices');

    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('polls now and then every LIVE_POLL_MS, merging each poll into the prices so far', () => {
      const feeds: LivePriceFeed[] = [];
      const sub = service.watchLivePrices(['AAPL', 'SPY']).subscribe((feed) => feeds.push(feed));

      vi.advanceTimersByTime(0);
      const first = nextPoll();
      expect(first.request.params.get('symbols')).toBe('AAPL,SPY');
      first.flush([quote('AAPL', 200), quote('SPY', 570)]);

      vi.advanceTimersByTime(LIVE_POLL_MS);
      nextPoll().flush([quote('AAPL', 201)]);

      expect(feeds).toHaveLength(2);
      expect(feeds[1].stale).toBe(false);
      expect(feeds[1].prices.get('AAPL')?.price).toBe(201);
      expect(feeds[1].prices.get('SPY')?.price).toBe(570);
      sub.unsubscribe();
    });

    it('keeps the last prices and marks the feed stale when a poll fails, until one succeeds', () => {
      const feeds: LivePriceFeed[] = [];
      const sub = service.watchLivePrices(['AAPL']).subscribe((feed) => feeds.push(feed));

      vi.advanceTimersByTime(0);
      nextPoll().flush([quote('AAPL', 200)]);
      vi.advanceTimersByTime(LIVE_POLL_MS);
      nextPoll().flush(null, { status: 503, statusText: 'Service Unavailable' });
      vi.advanceTimersByTime(LIVE_POLL_MS);
      nextPoll().flush([quote('AAPL', 205)]);

      expect(feeds.map((f) => [f.stale, f.prices.get('AAPL')?.price])).toEqual([
        [false, 200],
        [true, 200],
        [false, 205],
      ]);
      sub.unsubscribe();
    });

    it('with no symbols watches every tracked symbol', () => {
      const sub = service.watchLivePrices().subscribe();

      vi.advanceTimersByTime(0);
      const req = nextPoll();
      expect(req.request.params.has('symbols')).toBe(false);
      req.flush([]);
      sub.unsubscribe();
    });

    it('with an empty list never polls', () => {
      let completed = false;
      service.watchLivePrices([]).subscribe({ complete: () => (completed = true) });

      vi.advanceTimersByTime(LIVE_POLL_MS * 2);
      http.expectNone(() => true);
      expect(completed).toBe(true);
    });
  });

  it('getLatestPrices errors if any of the three lookups fails', () => {
    let error: unknown;
    service.getLatestPrices().subscribe({ error: (e) => (error = e) });

    http.expectOne('/api/stocks').flush([aapl]);
    http.expectOne('/api/etfs').flush(null, { status: 503, statusText: 'Service Unavailable' });
    http.match('/api/bonds');

    expect(error).toBeTruthy();
  });

  it('getInstrument, getStock, getEtf and getBond load one instrument by symbol', () => {
    service.getInstrument('AAPL').subscribe();
    service.getStock('AAPL').subscribe();
    service.getEtf('SPY').subscribe();
    service.getBond('AGG').subscribe();

    http.expectOne('/api/instruments/AAPL').flush({});
    http.expectOne('/api/stocks/AAPL').flush({});
    http.expectOne('/api/etfs/SPY').flush({});
    http.expectOne('/api/bonds/AGG').flush({});
  });

  it('getHistory asks for about a year of daily closes by default', () => {
    service.getHistory('AAPL').subscribe();
    service.getHistory('AAPL', 21).subscribe();

    http.expectOne('/api/instruments/AAPL/history?days=252').flush([]);
    http.expectOne('/api/instruments/AAPL/history?days=21').flush([]);
  });

  it('getNews can ask for one symbol only', () => {
    service.getNews(5, 'AAPL').subscribe();

    http.expectOne('/api/news?limit=5&symbol=AAPL').flush([]);
  });

  it('encodes the symbol in the URL', () => {
    service.getStock('BRK/B').subscribe();

    http.expectOne('/api/stocks/BRK%2FB').flush({});
  });

  it('getNews requests the configured number of recent headlines', () => {
    let result: unknown;
    service.getNews().subscribe((news) => (result = news));

    const request = http.expectOne((req) => req.url === '/api/news');
    expect(request.request.params.get('limit')).toBe('5');
    request.flush([]);

    expect(result).toEqual([]);
  });
});

describe('dayChange', () => {
  it('returns the amount and percent moved since the previous close', () => {
    const change = dayChange({ price: 110, previousClose: 100 });

    expect(change?.amount).toBeCloseTo(10);
    expect(change?.percent).toBeCloseTo(10);
  });

  it('returns a negative change when the price fell', () => {
    const change = dayChange({ price: 336.64, previousClose: 340.42 });

    expect(change?.amount).toBeCloseTo(-3.78);
    expect(change?.percent).toBeCloseTo(-1.1104, 3);
  });

  it('returns null when either close is missing', () => {
    expect(dayChange({ price: null, previousClose: 100 })).toBeNull();
    expect(dayChange({ price: 100, previousClose: null })).toBeNull();
    expect(dayChange({ price: 100, previousClose: 0 })).toBeNull();
  });
});
