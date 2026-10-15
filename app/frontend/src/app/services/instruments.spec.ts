import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing'; 
import {
  Instruments,
  LatestPrice,
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
