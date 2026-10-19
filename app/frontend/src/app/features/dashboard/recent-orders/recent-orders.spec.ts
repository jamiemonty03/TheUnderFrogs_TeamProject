import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { Auth } from '../../../services/auth/auth';
import { OrderResponse } from '../../../services/interfaces/orders.interfaces';
import { Orders } from '../../../services/orders';
import { RecentOrders, latestOrders } from './recent-orders';

const order = (orderId: string, createdAt: string, overrides: Partial<OrderResponse> = {}): OrderResponse => ({
  orderId,
  accountId: 'ACC1',
  symbol: 'AAPL',
  side: 'BUY',
  quantity: 10,
  priceLimit: 189.84,
  orderStatus: 'FILLED',
  version: 1,
  createdAt,
  lastUpdated: null,
  updatedBy: null,
  ...overrides,
});

const ORDERS = [
  order('O1', '2026-10-10T09:00:00'),
  order('O2', '2026-10-14T09:00:00', { side: 'SELL', symbol: 'TSLA', orderStatus: 'REJECTED' }),
  order('O3', '2026-10-12T09:00:00'),
  order('O4', '2026-10-15T09:00:00'),
  order('O5', '2026-10-13T09:00:00'),
];

describe('latestOrders', () => {
  it('keeps the four newest orders, newest first', () => {
    expect(latestOrders(ORDERS).map((o) => o.orderId)).toEqual(['O4', 'O2', 'O5', 'O3']);
  });
});

describe('RecentOrders', () => {
  let fixture: ComponentFixture<RecentOrders>;
  let page: HTMLElement;

  async function render(orders: Observable<OrderResponse[]>, accountId: string | null = 'ACC1') {
    await TestBed.configureTestingModule({
      imports: [RecentOrders],
      providers: [
        provideRouter([]),
        { provide: Auth, useValue: { currentUser: signal(accountId ? { accountId } : null) } },
        { provide: Orders, useValue: { getOrdersByAccount: () => orders } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(RecentOrders);
    fixture.detectChanges();
    page = fixture.nativeElement;
  }

  it('lists the latest four orders', async () => {
    await render(of(ORDERS));

    const rows = page.querySelectorAll('[data-testid="recent-order"]');
    expect(rows).toHaveLength(4);
    expect(rows[1].textContent).toContain('Sell');
    expect(rows[1].textContent).toContain('TSLA');
    expect(rows[1].textContent).toContain('REJECTED');
    expect(rows[0].textContent).toContain('10 @ $189.84');
  });

  it('says when there are no orders', async () => {
    await render(of([]));

    expect(page.textContent).toContain('No orders yet.');
  });

  it('shows an error when orders fail to load', async () => {
    await render(throwError(() => new Error('down')));

    expect(page.querySelector('[role="alert"]')!.textContent).toContain("Couldn't load your orders");
  });

  it('shows an error when the user has no account', async () => {
    await render(of(ORDERS), null);

    expect(page.querySelector('[role="alert"]')!.textContent).toContain('No trading account');
  });
});
