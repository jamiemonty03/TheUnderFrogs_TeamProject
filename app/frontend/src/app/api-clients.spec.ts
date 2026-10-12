import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { provideApiClients } from './api-clients';
import { AuthenticationService } from './api/auth/services/authentication.service';
import { AuthTokens } from './api/auth/models/auth-tokens';
import { environment } from '../environments/environment';
import { AccountsService } from './api/accounts/services/accounts.service';
import { OrdersService } from './api/orders/services/orders.service';
import { PositionQueriesService } from './api/positions/services/position-queries.service';
import { InstrumentsService } from './api/instruments/services/instruments.service';
import { StocksService } from './api/stocks/services/stocks.service';
import { EtFsService } from './api/etfs/services/et-fs.service';
import { BondsService } from './api/bonds/services/bonds.service';
import { Observable } from 'rxjs';

describe('Generated auth client', () => {
  const tokens: AuthTokens = {
    accessToken: 'access',
    refreshToken: 'refresh',
    expiresIn: 900,
    mfaRequired: false,
  };

  let httpController: HttpTestingController | undefined;

  function setUp(baseUrls = environment.apiBaseUrls) {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideApiClients(baseUrls)],
    });
    httpController = TestBed.inject(HttpTestingController);
    return { auth: TestBed.inject(AuthenticationService), http: httpController };
  }

  afterEach(() => {
    httpController?.verify();
    httpController = undefined;
  });

  it('calls /auth/login on the same origin by default, never a protocol-relative //auth URL', async () => {
    const { auth, http } = setUp();

    const result = firstValueFrom(auth.login({ body: { username: 'demo', password: 'Demo123!' } }));
    const request = http.expectOne('/auth/login');
    request.flush(tokens);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ username: 'demo', password: 'Demo123!' });
    expect(await result).toEqual(tokens);
  });

  it('uses the configured base URL', async () => {
    const { auth, http } = setUp({ ...environment.apiBaseUrls, auth: 'https://auth.example.com' });

    const result = firstValueFrom(auth.refresh({ body: { refresh_token: 'refresh' } }));
    http.expectOne('https://auth.example.com/auth/refresh').flush(tokens);

    expect((await result).accessToken).toBe('access');
  });

  it('sends logout to /auth/logout and expects no content', async () => {
    const { auth, http } = setUp();

    const result = firstValueFrom(auth.logout({ body: { refresh_token: 'refresh' } }));
    const request = http.expectOne('/auth/logout');
    request.flush(null, { status: 204, statusText: 'No Content' });

    expect(request.request.method).toBe('POST');
    expect(await result).toBeNull();
  });

  it('defaults every environment to same-origin URLs served through the proxy', () => {
    expect(environment.apiBaseUrls).toEqual({
      auth: '',
      accounts: '/api',
      orders: '/api',
      positions: '/api',
      instruments: '/api',
      stocks: '/api',
      etfs: '/api',
      bonds: '/api',
    });
  });
});

describe('Generated service clients', () => {
  let http: HttpTestingController;

  function inject<T>(service: new (...args: never[]) => T, baseUrls = environment.apiBaseUrls): T {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideApiClients(baseUrls)],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(service);
  }

  afterEach(() => http.verify());

  const calls: [string, () => Observable<unknown>, string][] = [
    [
      'accounts',
      () => inject(AccountsService).getAccount({ accountId: 'ACC0011' }),
      '/api/accounts/ACC0011',
    ],
    ['orders', () => inject(OrdersService).getOrderById({ orderId: 'ord-1' }), '/api/orders/ord-1'],
    [
      'positions',
      () => inject(PositionQueriesService).getPositionsByAccount({ accountId: 'ACC0011' }),
      '/api/positions/ACC0011',
    ],
    ['instruments', () => inject(InstrumentsService).getInstruments(), '/api/instruments'],
    ['stocks', () => inject(StocksService).getStock({ symbol: 'AAPL' }), '/api/stocks/AAPL'],
    ['etfs', () => inject(EtFsService).getEtf({ symbol: 'SPY' }), '/api/etfs/SPY'],
    ['bonds', () => inject(BondsService).getBond({ symbol: 'UST10Y' }), '/api/bonds/UST10Y'],
  ];

  it.each(calls)(
    '%s client calls the service under the /api context path',
    async (_name, call, url) => {
      const result = firstValueFrom(call());
      const request = http.expectOne(url);
      request.flush({});

      expect(request.request.method).toBe('GET');
      await result;
    },
  );

  it('places an order with a typed body', async () => {
    const orders = inject(OrdersService);
    const body = {
      accountId: 'ACC0011',
      symbol: 'AAPL',
      side: 'BUY' as const,
      quantity: 1,
      priceLimit: 1000,
      idempotencyKey: 'key-1',
    };

    const result = firstValueFrom(orders.placeOrder({ body }));
    const request = http.expectOne('/api/orders');
    request.flush({
      ...body,
      orderId: 'ord-1',
      orderStatus: 'NEW',
      createdAt: '2026-10-12T15:23:58.713',
    });

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect((await result).orderStatus).toBe('NEW');
  });

  it('uses a configured base URL for a Java service', async () => {
    const accounts = inject(AccountsService, {
      ...environment.apiBaseUrls,
      accounts: 'https://accounts.example.com/api',
    });

    const result = firstValueFrom(accounts.getAccount({ accountId: 'ACC0011' }));
    http.expectOne('https://accounts.example.com/api/accounts/ACC0011').flush({});

    await result;
  });
});
