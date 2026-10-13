import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Auth, CurrentUser } from './auth';
import { Position, Positions } from './positions';

const ACCOUNT = 'ACC0013';
const AAPL: Position = { accountId: ACCOUNT, symbol: 'AAPL', quantity: 50, averageCost: 182.4 };

describe('Positions', () => {
  let service: Positions;
  let http: HttpTestingController;
  // Stands in for Auth, whose currentUser is read-only and only set by logging in.
  const currentUser = signal<CurrentUser | null>(null);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: Auth, useValue: { currentUser } }],
    });
    service = TestBed.inject(Positions);
    http = TestBed.inject(HttpTestingController);
    currentUser.set({ userId: 13, username: 'trader', roles: ['TRADER'], accountId: ACCOUNT });
  });

  afterEach(() => http.verify());

  it("loads the signed-in account's positions with a GET", async () => {
    const result = firstValueFrom(service.getPositions());

    const req = http.expectOne(`/api/positions/${ACCOUNT}`);
    expect(req.request.method).toBe('GET');
    req.flush([AAPL]);

    expect(await result).toEqual([AAPL]);
  });

  it('loads one position by symbol', async () => {
    const result = firstValueFrom(service.getPosition('AAPL'));

    const req = http.expectOne(`/api/positions/${ACCOUNT}/AAPL`);
    expect(req.request.method).toBe('GET');
    req.flush(AAPL);

    expect(await result).toEqual(AAPL);
  });

  it('returns quantity 0 when the user does not hold the symbol (404)', async () => {
    const result = firstValueFrom(service.getPosition('TSLA'));

    http
      .expectOne(`/api/positions/${ACCOUNT}/TSLA`)
      .flush({ errorCode: 'POS-404', message: 'Position not found' }, { status: 404, statusText: 'Not Found' });

    expect(await result).toEqual({ accountId: ACCOUNT, symbol: 'TSLA', quantity: 0, averageCost: 0 });
    expect(service.error()).toBeNull();
  });

  it("sets error to the server's message when a request fails", async () => {
    const result = firstValueFrom(service.getPositions());

    http
      .expectOne(`/api/positions/${ACCOUNT}`)
      .flush({ errorCode: 'ACC-403', message: 'Account not active' }, { status: 403, statusText: 'Forbidden' });

    await expect(result).rejects.toBeTruthy();
    expect(service.error()).toBe('Account not active');
    expect(service.loading()).toBe(false);
  });

  it('falls back to a plain message when the server sends no error body', async () => {
    const result = firstValueFrom(service.getPositions());

    http.expectOne(`/api/positions/${ACCOUNT}`).flush(null, { status: 500, statusText: 'Server Error' });

    await expect(result).rejects.toBeTruthy();
    expect(service.error()).toBe("Couldn't load your positions. Try again in a moment.");
  });

  it('makes no request when the user has no trading account', async () => {
    currentUser.set({ userId: 1, username: 'admin', roles: ['ADMIN'], accountId: null });

    await expect(firstValueFrom(service.getPositions())).rejects.toBeTruthy();

    http.expectNone(() => true);
    expect(service.error()).toBe('No trading account is linked to this user.');
  });

  it('is loading only while a request is in flight', async () => {
    const result = firstValueFrom(service.getPositions());
    expect(service.loading()).toBe(true);

    http.expectOne(`/api/positions/${ACCOUNT}`).flush([]);
    await result;

    expect(service.loading()).toBe(false);
  });
});
