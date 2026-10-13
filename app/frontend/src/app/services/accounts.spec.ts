import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Account, Accounts } from './accounts';
import { Auth, CurrentUser } from './auth';

const ACCOUNT = 'ACC0013';
// Stands in for the accounts-service response; the real holder name comes from the API.
const RESPONSE: Account = { accountId: ACCOUNT, holderName: 'Test Trader', cashBalance: 25000, status: 'ACTIVE' };

describe('Accounts', () => {
  let service: Accounts;
  let http: HttpTestingController;
  // Stands in for Auth, whose currentUser is read-only and only set by logging in.
  const currentUser = signal<CurrentUser | null>(null);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: Auth, useValue: { currentUser } }],
    });
    service = TestBed.inject(Accounts);
    http = TestBed.inject(HttpTestingController);
    currentUser.set({ userId: 13, username: 'trader', roles: ['TRADER'], accountId: ACCOUNT });
  });

  afterEach(() => http.verify());

  it("loads the signed-in user's account with a GET", async () => {
    const result = firstValueFrom(service.getAccount());

    const req = http.expectOne(`/api/accounts/${ACCOUNT}`);
    expect(req.request.method).toBe('GET');
    req.flush(RESPONSE);

    expect(await result).toEqual(RESPONSE);
    expect(service.account.loading()).toBe(false);
    expect(service.account.error()).toBeNull();
  });

  it("returns the signed-in user's cash balance", async () => {
    const result = firstValueFrom(service.getBalance());

    http.expectOne(`/api/accounts/${ACCOUNT}`).flush(RESPONSE);

    expect(await result).toBe(RESPONSE.cashBalance);
    expect(service.balance.loading()).toBe(false);
    expect(service.balance.error()).toBeNull();
  });

  it('is loading until the response arrives', () => {
    service.getAccount().subscribe();

    expect(service.account.loading()).toBe(true);
    http.expectOne(`/api/accounts/${ACCOUNT}`).flush(RESPONSE);
    expect(service.account.loading()).toBe(false);
  });

  it("shows the server's message when the request fails", async () => {
    const result = firstValueFrom(service.getAccount());

    http
      .expectOne(`/api/accounts/${ACCOUNT}`)
      .flush({ message: 'Account not found' }, { status: 404, statusText: 'Not Found' });

    await expect(result).rejects.toBeTruthy();
    expect(service.account.error()).toBe('Account not found');
  });

  it('falls back to a friendly message when the server gives none', async () => {
    const result = firstValueFrom(service.getAccount());

    http.expectOne(`/api/accounts/${ACCOUNT}`).flush(null, { status: 503, statusText: 'Service Unavailable' });

    await expect(result).rejects.toBeTruthy();
    expect(service.account.error()).toBe("Couldn't load your account. Try again in a moment.");
  });

  it('errors with a clear message when the user has no trading account', async () => {
    currentUser.set({ userId: 1, username: 'admin', roles: ['ADMIN'], accountId: null });

    await expect(firstValueFrom(service.getAccount())).rejects.toBeTruthy();
    await expect(firstValueFrom(service.getBalance())).rejects.toBeTruthy();

    expect(service.account.error()).toBe('No trading account is linked to this user.');
    expect(service.balance.error()).toBe('No trading account is linked to this user.');
  });

  it('keeps loading and error state separate per method', async () => {
    currentUser.set({ userId: 1, username: 'admin', roles: ['ADMIN'], accountId: null });
    await expect(firstValueFrom(service.getAccount())).rejects.toBeTruthy();

    expect(service.account.error()).not.toBeNull();
    expect(service.balance.error()).toBeNull();
  });

  it('clears a previous error once a request succeeds', async () => {
    currentUser.set({ userId: 1, username: 'admin', roles: ['ADMIN'], accountId: null });
    await expect(firstValueFrom(service.getAccount())).rejects.toBeTruthy();

    currentUser.set({ userId: 13, username: 'trader', roles: ['TRADER'], accountId: ACCOUNT });
    const result = firstValueFrom(service.getAccount());
    http.expectOne(`/api/accounts/${ACCOUNT}`).flush(RESPONSE);
    await result;

    expect(service.account.error()).toBeNull();
  });
});
