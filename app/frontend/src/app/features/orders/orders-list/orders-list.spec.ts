import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Auth, CurrentUser } from '../../../services/auth/auth';
import { Instruments } from '../../../services/instruments';
import { TradingInstrument } from '../../../services/interfaces/instruments.interfaces';
import { OrderResponse } from '../../../services/interfaces/orders.interfaces';
import { Orders } from '../../../services/orders';
import { OrdersList } from './orders-list';

const ACCOUNT = 'ACC0042';
const AAPL: TradingInstrument = {
  symbol: 'AAPL',
  name: 'Apple Inc.',
  type: 'Stock',
  marketPrice: 182.4,
  lastUpdated: '2026-10-14T10:00:00Z',
};
const TSLA: TradingInstrument = {
  symbol: 'TSLA',
  name: 'Tesla Inc.',
  type: 'Stock',
  marketPrice: 250,
  lastUpdated: '2026-10-14T10:00:00Z',
};
const BUY_ORDER: OrderResponse = {
  orderId: 'ORDER-1',
  accountId: ACCOUNT,
  symbol: 'AAPL',
  side: 'BUY',
  quantity: 2,
  priceLimit: 180,
  orderStatus: 'FILLED',
  version: 1,
  createdAt: '2026-10-14T10:00:00Z',
  lastUpdated: null,
  updatedBy: null,
};
const SELL_ORDER: OrderResponse = {
  ...BUY_ORDER,
  orderId: 'ORDER-2',
  symbol: 'TSLA',
  side: 'SELL',
  quantity: 3,
  orderStatus: 'NEW',
};

const closedDialog = { afterClosed: () => of(undefined) };

describe('OrdersList', () => {
  let fixture: ComponentFixture<OrdersList>;
  let component: OrdersList;
  let user: ReturnType<typeof signal<CurrentUser | null>>;
  let instruments: { getAllTradableInstruments: ReturnType<typeof vi.fn> };
  let orders: { getOrdersByAccount: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    user = signal<CurrentUser | null>({ userId: 42, username: 'trader', roles: ['TRADER'], accountId: ACCOUNT });
    instruments = { getAllTradableInstruments: vi.fn(() => of([AAPL, TSLA])) };
    orders = { getOrdersByAccount: vi.fn(() => of([BUY_ORDER, SELL_ORDER])) };
    dialog = { open: vi.fn(() => closedDialog) };

    await TestBed.configureTestingModule({
      imports: [OrdersList],
      providers: [
        { provide: Auth, useValue: { currentUser: user.asReadonly() } },
        { provide: Instruments, useValue: instruments },
        { provide: Orders, useValue: orders },
        { provide: MatDialog, useValue: dialog },
      ],
    }).overrideProvider(MatDialog, { useValue: dialog }).compileComponents();

    fixture = TestBed.createComponent(OrdersList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the current account orders and maps market prices', () => {
    expect(orders.getOrdersByAccount).toHaveBeenCalledWith(ACCOUNT);
    expect(instruments.getAllTradableInstruments).toHaveBeenCalledOnce();
    expect(component.orders()).toEqual([
      expect.objectContaining({ id: 'ORDER-1', symbol: 'AAPL', price: '$182.40', status: 'FILLED', canCancel: false }),
      expect.objectContaining({ id: 'ORDER-2', symbol: 'TSLA', price: '$250.00', status: 'NEW', canCancel: true }),
    ]);
    expect(component.isLoading()).toBe(false);
    expect(component.loadError()).toBeNull();
  });

  it('filters orders, counts statuses, and resets the page', () => {
    component.pageIndex.set(2);
    component.setFilter('NEW');

    expect(component.activeFilter()).toBe('NEW');
    expect(component.pageIndex()).toBe(0);
    expect(component.visibleOrders()).toHaveLength(1);
    expect(component.getFilterCount('ALL')).toBe(2);
    expect(component.getFilterCount('FILLED')).toBe(1);
    expect(component.getFilterCount('REJECTED')).toBe(0);
  });

  it('updates pagination state and returns the current page', () => {
    component.pageSize.set(1);
    component.setPage({ pageIndex: 1, pageSize: 1, length: 2 });

    expect(component.pageIndex()).toBe(1);
    expect(component.pageSize()).toBe(1);
    expect(component.pagedOrders()[0].id).toBe('ORDER-2');
  });

  it('opens the order dialog and reloads orders after it closes', () => {
    component.openOrderDialog();

    expect(dialog.open).toHaveBeenCalledWith(expect.anything(), {
      width: '920px',
      maxWidth: '95vw',
      panelClass: 'order-dialog-panel',
    });
    expect(orders.getOrdersByAccount).toHaveBeenCalledTimes(2);
  });

  it('shows N/A for an order without a matching instrument price', () => {
    orders.getOrdersByAccount.mockReturnValueOnce(of([{ ...BUY_ORDER, symbol: 'UNKNOWN' }]));
    component.ngOnInit();

    expect(component.orders()[0].price).toBe('N/A');
  });

  it('handles order loading failures', () => {
    orders.getOrdersByAccount.mockReturnValueOnce(throwError(() => new Error('failed')));
    component.ngOnInit();

    expect(component.orders()).toEqual([]);
    expect(component.isLoading()).toBe(false);
    expect(component.loadError()).toBe('Could not load orders from backend.');
  });

  it('does not request orders without an account', () => {
    user.set(null);
    orders.getOrdersByAccount.mockClear();
    instruments.getAllTradableInstruments.mockClear();
    component.ngOnInit();

    expect(orders.getOrdersByAccount).not.toHaveBeenCalled();
    expect(instruments.getAllTradableInstruments).not.toHaveBeenCalled();
    expect(component.orders()).toEqual([]);
    expect(component.loadError()).toBe('No account is associated with the current user.');
  });
});
