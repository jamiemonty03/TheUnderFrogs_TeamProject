import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Accounts } from './accounts';

describe('Accounts', () => {
  let service: Accounts;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Accounts);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads an account, including its cash', async () => {
    const result = firstValueFrom(service.getAccount('ACC0011'));

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
});
