import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { OrderSnackbar } from '../features/orders/order-snackbar/order-snackbar';
import { Auth, CurrentUser } from './auth';
import { OrderResponse } from './interfaces/orders.interfaces';
import { OrderNotifier } from './order-notifier';
import { Orders } from './orders';

const order = (orderStatus: OrderResponse['orderStatus'], extra: Partial<OrderResponse> = {}) =>
  ({
    orderId: 'ORDER-7',
    accountId: 'ACC0011',
    symbol: 'AAPL',
    side: 'BUY',
    quantity: 1,
    priceLimit: 340,
    orderStatus,
    ...extra,
  }) as OrderResponse;

describe('OrderNotifier', () => {
  let orders: { getOrdersByAccount: ReturnType<typeof vi.fn> };
  let snackBar: { openFromComponent: ReturnType<typeof vi.fn> };
  let user: ReturnType<typeof signal<CurrentUser | null>>;
  let settled: OrderResponse[];

  function follow(orderId = 'ORDER-7') {
    settled = [];
    TestBed.inject(OrderNotifier)
      .follow(orderId)
      .subscribe((o) => settled.push(o));
  }

  const checksFind = (...found: OrderResponse[][]) =>
    found.forEach((orderList) => orders.getOrdersByAccount.mockReturnValueOnce(of(orderList)));

  beforeEach(() => {
    vi.useFakeTimers();
    orders = { getOrdersByAccount: vi.fn(() => of([order('NEW')])) };
    snackBar = { openFromComponent: vi.fn() };
    user = signal<CurrentUser | null>({
      userId: 11,
      username: 'demo',
      roles: ['TRADER'],
      accountId: 'ACC0011',
    });
    TestBed.configureTestingModule({
      providers: [
        { provide: Orders, useValue: orders },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: Auth, useValue: { currentUser: user.asReadonly() } },
      ],
    });
  });

  afterEach(() => vi.useRealTimers());

  it('says the order filled, as soon as it has', () => {
    checksFind([order('FILLED')]);

    follow();
    vi.advanceTimersByTime(0);

    expect(orders.getOrdersByAccount).toHaveBeenCalledWith('ACC0011');
    expect(snackBar.openFromComponent).toHaveBeenCalledWith(OrderSnackbar, {
      data: { orderId: 'ORDER-7', status: 'FILLED' },
      duration: 5000,
      panelClass: ['order-snackbar-panel'],
    });
    expect(settled).toEqual([order('FILLED')]);
  });

  it('checks every half second while the order is still NEW', () => {
    checksFind([order('NEW')], [order('NEW')], [order('FILLED')]);

    follow();
    vi.advanceTimersByTime(500);
    expect(snackBar.openFromComponent).not.toHaveBeenCalled();

    vi.advanceTimersByTime(500);
    expect(orders.getOrdersByAccount).toHaveBeenCalledTimes(3);
    expect(snackBar.openFromComponent).toHaveBeenCalledOnce();
    expect(settled.map((o) => o.orderStatus)).toEqual(['FILLED']);
  });

  it('says why an order was rejected', () => {
    checksFind([order('REJECTED', { rejectionReason: 'Not enough cash' })]);

    follow();
    vi.advanceTimersByTime(0);

    expect(snackBar.openFromComponent).toHaveBeenCalledWith(OrderSnackbar, {
      data: { orderId: 'ORDER-7', status: 'REJECTED', reason: 'Not enough cash' },
      duration: 7000,
      panelClass: ['order-snackbar-panel'],
    });
  });

  it('says the order is waiting at its limit when ten more checks still find it NEW', () => {
    follow();
    vi.advanceTimersByTime(60_000);

    expect(orders.getOrdersByAccount).toHaveBeenCalledTimes(1 + 10);
    expect(snackBar.openFromComponent).toHaveBeenCalledOnce();
    expect(snackBar.openFromComponent).toHaveBeenCalledWith(OrderSnackbar, {
      data: { orderId: 'ORDER-7', status: 'NEW', priceLimit: 340 },
      duration: 7000,
      panelClass: ['order-snackbar-panel'],
    });
  });

  it('says nothing about an order it cannot find', () => {
    checksFind([order('FILLED', { orderId: 'SOMEONE-ELSE' })]);

    follow();
    vi.advanceTimersByTime(60_000);

    expect(snackBar.openFromComponent).not.toHaveBeenCalled();
    expect(settled).toEqual([]);
  });

  it('says nothing, and does not ask, without a trading account', () => {
    user.set({ userId: 11, username: 'demo', roles: ['TRADER'], accountId: null });

    follow();
    vi.advanceTimersByTime(60_000);

    expect(orders.getOrdersByAccount).not.toHaveBeenCalled();
    expect(snackBar.openFromComponent).not.toHaveBeenCalled();
  });

  it('gives up quietly if the orders cannot be loaded', () => {
    orders.getOrdersByAccount.mockReturnValueOnce(throwError(() => new Error('down')));

    follow();
    vi.advanceTimersByTime(60_000);

    expect(snackBar.openFromComponent).not.toHaveBeenCalled();
    expect(settled).toEqual([]);
  });
});
