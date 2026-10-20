import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { OrderResponse, PlaceOrderRequest } from './interfaces/orders.interfaces';
import { Orders } from './orders';

const ORDER: OrderResponse = {
  orderId: 'ORDER-1',
  accountId: 'ACC0011',
  symbol: 'AAPL',
  side: 'BUY',
  quantity: 2,
  priceLimit: 180,
  orderStatus: 'NEW',
  version: 0,
  createdAt: '2026-10-14T10:00:00Z',
  lastUpdated: null,
  updatedBy: null,
};

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

  it("loads an account's orders with a GET", async () => {
    const result = firstValueFrom(service.getOrdersByAccount('ACC0011'));

    const request = http.expectOne('/api/orders/account/ACC0011');
    expect(request.request.method).toBe('GET');
    request.flush([ORDER]);

    expect(await result).toEqual([ORDER]);
  });

  it("loads an account's trades, escaping the account ID in the URL", async () => {
    const result = firstValueFrom(service.getTradesByAccount('ACC 0011/x'));

    const request = http.expectOne('/api/client-trades/account/ACC%200011%2Fx');
    expect(request.request.method).toBe('GET');
    request.flush([]);

    expect(await result).toEqual([]);
  });

  it('places an order by POSTing the request as it is', async () => {
    const order: PlaceOrderRequest = {
      accountId: 'ACC0011',
      symbol: 'AAPL',
      side: 'BUY',
      quantity: 2,
      priceLimit: 180,
      idempotencyKey: 'key-1',
    };
    const result = firstValueFrom(service.placeOrder(order));

    const request = http.expectOne('/api/orders');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(order);
    request.flush(ORDER);

    expect(await result).toEqual(ORDER);
  });

  it('cancels an order with a DELETE', async () => {
    const result = firstValueFrom(service.cancelOrder('ORDER-1'));

    const request = http.expectOne('/api/orders/ORDER-1');
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });

    await expect(result).resolves.toBeNull();
  });
});
