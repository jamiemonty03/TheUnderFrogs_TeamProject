import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Auth, CurrentUser } from '../../../services/auth/auth';
import { Instruments } from '../../../services/instruments';
import { TradingInstrument } from '../../../services/interfaces/instruments.interfaces';
import { OrderResponse } from '../../../services/interfaces/orders.interfaces';
import { Orders } from '../../../services/orders';
import { OrderSnackbar } from '../order-snackbar/order-snackbar';
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
    user = signal<CurrentUser | null>({
      userId: 42,
      username: 'trader',
      roles: ['TRADER'],
      accountId: ACCOUNT,
    });
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
    })
      .overrideProvider(MatDialog, { useValue: dialog })
      .compileComponents();

    fixture = TestBed.createComponent(OrdersList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the current account orders and maps market prices', () => {
    expect(orders.getOrdersByAccount).toHaveBeenCalledWith(ACCOUNT);
    expect(instruments.getAllTradableInstruments).toHaveBeenCalledOnce();
    expect(component.orders()).toEqual([
      expect.objectContaining({ symbol: 'AAPL', price: '$182.40', status: 'FILLED' }),
      expect.objectContaining({ symbol: 'TSLA', price: '$250.00', status: 'NEW' }),
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
    expect(component.pagedOrders()[0].symbol).toBe('TSLA');
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

  it('tags each row with its order ID, so an order can be found in the table', () => {
    const rows = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('tr[data-testid^="order-"]'),
    ).map((row) => row.getAttribute('data-testid'));

    expect(rows).toEqual(['order-ORDER-1', 'order-ORDER-2']);
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

describe('OrdersList opened from a Buy or Sell link', () => {
  it('opens the order dialog on that instrument, side and portfolio', async () => {
    const dialog = { open: vi.fn(() => ({ afterClosed: () => of(undefined) })) };
    await TestBed.configureTestingModule({
      imports: [OrdersList],
      providers: [
        {
          provide: Auth,
          useValue: {
            currentUser: signal<CurrentUser | null>({
              userId: 42,
              username: 'trader',
              roles: ['TRADER'],
              accountId: ACCOUNT,
            }).asReadonly(),
          },
        },
        { provide: Instruments, useValue: { getAllTradableInstruments: vi.fn(() => of([AAPL])) } },
        { provide: Orders, useValue: { getOrdersByAccount: vi.fn(() => of([])) } },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: { new: 'true', symbol: 'AAPL', side: 'SELL', portfolioId: 'PF0013' },
            },
          },
        },
      ],
    })
      .overrideProvider(MatDialog, { useValue: dialog })
      .compileComponents();

    TestBed.createComponent(OrdersList).detectChanges();

    expect(dialog.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ data: { symbol: 'AAPL', side: 'SELL', portfolioId: 'PF0013' } }),
    );
  });
});

describe('OrdersList after an order is placed', () => {
  let orders: { getOrdersByAccount: ReturnType<typeof vi.fn> };
  let snackBar: { openFromComponent: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };
  let component: OrdersList;

  const placed = (status: OrderResponse['orderStatus'], extra: Partial<OrderResponse> = {}) => ({
    ...BUY_ORDER,
    orderId: 'ORDER-9',
    orderStatus: status,
    ...extra,
  });

  async function setUp(placedOrder: OrderResponse | undefined) {
    orders = { getOrdersByAccount: vi.fn(() => of([BUY_ORDER])) };
    snackBar = { openFromComponent: vi.fn() };
    dialog = { open: vi.fn(() => ({ afterClosed: () => of(placedOrder) })) };
    await TestBed.configureTestingModule({
      imports: [OrdersList],
      providers: [
        {
          provide: Auth,
          useValue: {
            currentUser: signal<CurrentUser | null>({
              userId: 42,
              username: 'trader',
              roles: ['TRADER'],
              accountId: ACCOUNT,
            }).asReadonly(),
          },
        },
        { provide: Instruments, useValue: { getAllTradableInstruments: vi.fn(() => of([AAPL])) } },
        { provide: Orders, useValue: orders },
      ],
    })
      .overrideProvider(MatDialog, { useValue: dialog })
      .overrideProvider(MatSnackBar, { useValue: snackBar })
      .compileComponents();

    const fixture = TestBed.createComponent(OrdersList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  const returns = (...statuses: OrderResponse['orderStatus'][]) =>
    statuses.forEach((status) =>
      orders.getOrdersByAccount.mockReturnValueOnce(of([placed(status)])),
    );

  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('says the order filled, briefly', async () => {
    await setUp(placed('NEW'));
    returns('FILLED');

    component.openOrderDialog();

    expect(snackBar.openFromComponent).toHaveBeenCalledWith(OrderSnackbar, {
      data: { orderId: 'ORDER-9', status: 'FILLED' },
      duration: 5000,
      panelClass: ['order-snackbar-panel'],
    });
  });

  it('says the order was rejected, with the reason, for longer', async () => {
    await setUp(placed('NEW'));
    orders.getOrdersByAccount.mockReturnValueOnce(
      of([placed('REJECTED', { rejectionReason: 'Not enough cash' })]),
    );

    component.openOrderDialog();

    expect(snackBar.openFromComponent).toHaveBeenCalledWith(OrderSnackbar, {
      data: { orderId: 'ORDER-9', status: 'REJECTED', reason: 'Not enough cash' },
      duration: 7000,
      panelClass: ['order-snackbar-panel'],
    });
  });

  it('checks again every half second while the order is still NEW', async () => {
    await setUp(placed('NEW'));
    returns('NEW', 'NEW', 'FILLED');

    component.openOrderDialog();
    expect(snackBar.openFromComponent).not.toHaveBeenCalled();

    vi.advanceTimersByTime(500);
    expect(snackBar.openFromComponent).not.toHaveBeenCalled();

    vi.advanceTimersByTime(500);
    expect(snackBar.openFromComponent).toHaveBeenCalledOnce();
    expect(orders.getOrdersByAccount).toHaveBeenCalledTimes(4);
  });

  it('gives up quietly after ten retries if the order never leaves NEW', async () => {
    await setUp(placed('NEW'));
    orders.getOrdersByAccount.mockReturnValue(of([placed('NEW')]));

    component.openOrderDialog();
    vi.advanceTimersByTime(60_000);

    expect(orders.getOrdersByAccount).toHaveBeenCalledTimes(1 + 1 + 10);
    expect(snackBar.openFromComponent).not.toHaveBeenCalled();
  });

  it('says nothing when the dialog closes without an order', async () => {
    await setUp(undefined);

    component.openOrderDialog();
    vi.advanceTimersByTime(5_000);

    expect(orders.getOrdersByAccount).toHaveBeenCalledTimes(2);
    expect(snackBar.openFromComponent).not.toHaveBeenCalled();
  });

  it('shows the raw time when an order has one it cannot read', async () => {
    await setUp(undefined);
    orders.getOrdersByAccount.mockReturnValueOnce(of([{ ...BUY_ORDER, createdAt: 'yesterday' }]));

    component.openOrderDialog();

    expect(component.orders()[0].placed).toBe('yesterday');
  });
});

describe('OrdersList opened from a link with odd parameters', () => {
  it('ignores a side or values it does not understand', async () => {
    const dialog = { open: vi.fn(() => ({ afterClosed: () => of(undefined) })) };
    await TestBed.configureTestingModule({
      imports: [OrdersList],
      providers: [
        {
          provide: Auth,
          useValue: {
            currentUser: signal<CurrentUser | null>({
              userId: 42,
              username: 'trader',
              roles: ['TRADER'],
              accountId: ACCOUNT,
            }).asReadonly(),
          },
        },
        { provide: Instruments, useValue: { getAllTradableInstruments: vi.fn(() => of([])) } },
        { provide: Orders, useValue: { getOrdersByAccount: vi.fn(() => of([])) } },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParams: { new: 'true', symbol: ['AAPL', 'TSLA'], side: 'HOLD' } },
          },
        },
      ],
    })
      .overrideProvider(MatDialog, { useValue: dialog })
      .compileComponents();

    TestBed.createComponent(OrdersList).detectChanges();

    expect(dialog.open).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        data: { symbol: undefined, side: undefined, portfolioId: undefined },
      }),
    );
  });
});
