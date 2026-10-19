import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Orders } from './orders';

describe('Orders', () => {
  let service: Orders;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(Orders);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('cancels an order with a DELETE', async () => {
    const result = firstValueFrom(service.cancelOrder('ORDER-1'));

    const req = http.expectOne('/api/orders/ORDER-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });

    await expect(result).resolves.toBeNull();
  });
});
