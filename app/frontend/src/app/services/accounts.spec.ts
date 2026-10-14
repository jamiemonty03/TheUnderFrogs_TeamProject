import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Accounts } from './accounts';
import { Auth, CurrentUser } from './auth';

describe('Accounts', () => {
  let service: Accounts;
  let http: HttpTestingController;
  const currentUser = signal<CurrentUser | null>(null);

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Auth, useValue: { currentUser } },
      ],
    });
    service = TestBed.inject(Accounts);
    http = TestBed.inject(HttpTestingController);
    currentUser.set({ userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' });
  });

  afterEach(() => http.verify());

  it("loads the signed-in user's account", async () => {
    const result = firstValueFrom(service.getAccount());

    const req = http.expectOne('/api/accounts/ACC0011');
    expect(req.request.method).toBe('GET');
    req.flush({
      accountId: 'ACC0011',
      holderName: 'Demo Trader',
      cashBalance: 97949.15,
      status: 'ACTIVE',
    });

    expect((await result).cashBalance).toBe(97949.15);
  });

  it('makes no request when the user has no trading account', async () => {
    currentUser.set({ userId: 1, username: 'admin', roles: ['ADMIN'], accountId: null });

    await expect(firstValueFrom(service.getAccount())).rejects.toThrow(
      'No trading account is linked to this user.',
    );
    http.expectNone(() => true);
  });
});
