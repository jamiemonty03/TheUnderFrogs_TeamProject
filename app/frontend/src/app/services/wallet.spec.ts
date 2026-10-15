import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Auth, CurrentUser } from './auth';
import { NoTradingAccountError, Wallet } from './wallet';

describe('Wallet', () => {
  let wallet: Wallet;
  let http: HttpTestingController;
  const user = signal<CurrentUser | null>({
    userId: 11,
    username: 'demo',
    roles: ['TRADER'],
    accountId: 'ACC0011',
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Auth, useValue: { currentUser: user.asReadonly() } },
      ],
    });
    wallet = TestBed.inject(Wallet);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    user.set({ userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' });
  });

  it("starts a top-up for the signed-in user's account and returns Stripe's page", async () => {
    const result = firstValueFrom(wallet.startTopUp(250, '/positions'));
    const request = http.expectOne('/api/accounts/ACC0011/top-ups');
    request.flush({ checkoutUrl: 'https://checkout.stripe.com/c/pay/cs_test_1' });

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ amount: 250, returnTo: '/positions' });
    expect(await result).toBe('https://checkout.stripe.com/c/pay/cs_test_1');
  });

  it('confirms a top-up by its session ID, encoded so it cannot change the path', async () => {
    const result = firstValueFrom(wallet.confirmTopUp('cs_test_a1/../x'));
    const request = http.expectOne('/api/accounts/ACC0011/top-ups/cs_test_a1%2F..%2Fx/confirm');
    request.flush({ amount: 250, cashBalance: 750 });

    expect(request.request.method).toBe('POST');
    expect(await result).toEqual({ amount: 250, cashBalance: 750 });
  });

  it('fails without calling the server when the user has no trading account', async () => {
    user.set({ userId: 11, username: 'demo', roles: ['TRADER'], accountId: null });

    await expect(firstValueFrom(wallet.startTopUp(50, '/dashboard'))).rejects.toBeInstanceOf(
      NoTradingAccountError,
    );
    http.expectNone(() => true);
  });
});
