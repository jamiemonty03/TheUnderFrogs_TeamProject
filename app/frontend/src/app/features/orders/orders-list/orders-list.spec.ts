import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { NEVER, Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Auth, CurrentUser } from '../../../services/auth/auth';
import { Instruments } from '../../../services/instruments';
import { LivePrice, LivePriceFeed, TradingInstrument } from '../../../services/interfaces/instruments.interfaces';
import { LivePrices } from '../../../services/live-prices';
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
  let orders: { getOrdersByAccount: ReturnType<typeof vi.fn>; cancelOrder: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };
  // Stands in for the live-price WebSocket: tests push quotes into it
  let liveFeed: Subject<LivePriceFeed>;

  beforeEach(async () => {
    user = signal<CurrentUser | null>({
      userId: 42,
      username: 'trader',
      roles: ['TRADER'],
      accountId: ACCOUNT,
    });
    instruments = { getAllTradableInstruments: vi.fn(() => of([AAPL, TSLA])) };
    orders = {
      getOrdersByAccount: vi.fn(() => of([BUY_ORDER, SELL_ORDER])),
      cancelOrder: vi.fn(() => of(undefined)),
    };
    dialog = { open: vi.fn(() => closedDialog) };
    liveFeed = new Subject<LivePriceFeed>();

    await TestBed.configureTestingModule({
      imports: [OrdersList],
      providers: [
        { provide: Auth, useValue: { currentUser: user.asReadonly() } },
        { provide: Instruments, useValue: instruments },
        { provide: Orders, useValue: orders },
        { provide: MatDialog, useValue: dialog },
        { provide: LivePrices, useValue: { watch: () => liveFeed.asObservable() } },
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

  describe('limit orders', () => {
    const page = () => fixture.nativeElement as HTMLElement;
    const row = (orderId: string) => page().querySelector(`[data-testid="order-${orderId}"]`)!;
    let snackBar: MatSnackBar;

    beforeEach(() => {
      // MatSnackBarModule gives the component its own MatSnackBar, so spy on that one
      snackBar = fixture.debugElement.injector.get(MatSnackBar);
      vi.spyOn(snackBar, 'openFromComponent');
      vi.spyOn(snackBar, 'open');
      fixture.detectChanges();
    });

    afterEach(() => vi.useRealTimers());

    const market = (orderId: string) =>
      row(orderId).querySelector('[data-testid="order-market"]')?.textContent?.replace(/\s+/g, ' ').trim();
    // Like the real feed, each push keeps the other symbols' latest quotes
    const quotes = new Map<string, LivePrice>();
    const push = (symbol: string, bid: number, ask: number) => {
      quotes.set(symbol, { symbol, price: (bid + ask) / 2, bid, ask, quoteTime: '2026-10-19T14:30:00Z' });
      liveFeed.next({ prices: new Map(quotes), stale: false });
      fixture.detectChanges();
    };

    it('shows the daily close, tagged Close, until a live quote arrives', () => {
      expect(market('ORDER-1')).toBe('$182.40 Close');
    });

    it('shows the live ask on a BUY and the live bid on a SELL', () => {
      push('AAPL', 183, 183.1);
      push('TSLA', 251, 251.2);

      expect(market('ORDER-1')).toBe('$183.10 Live');
      expect(market('ORDER-2')).toBe('$251.00 Live');
    });

    it('updates the market price on every quote without reloading orders', () => {
      orders.getOrdersByAccount.mockClear();
      push('AAPL', 183, 183.1);
      push('AAPL', 184, 184.1);

      expect(market('ORDER-1')).toBe('$184.10 Live');
      expect(orders.getOrdersByAccount).not.toHaveBeenCalled();
    });

    it('gives the Cancel button its red accent', () => {
      expect(row('ORDER-2').querySelector('[data-testid="cancel-order"]')?.classList).toContain('orders-table__cancel');
    });

    it("shows each order's limit price next to the market price", () => {
      expect(row('ORDER-1').querySelector('[data-testid="order-limit"]')?.textContent?.trim()).toBe('$180.00');
      expect(component.orders()[0]).toEqual(expect.objectContaining({ limit: '$180.00', price: '$182.40' }));
    });

    it('offers Cancel only on NEW orders', () => {
      expect(row('ORDER-1').querySelector('[data-testid="cancel-order"]')).toBeNull();
      expect(row('ORDER-2').querySelector('[data-testid="cancel-order"]')).not.toBeNull();
    });

    it('cancels a NEW order, confirms it and reloads the list', () => {
      orders.getOrdersByAccount.mockClear();

      row('ORDER-2').querySelector<HTMLButtonElement>('[data-testid="cancel-order"]')!.click();

      expect(orders.cancelOrder).toHaveBeenCalledWith('ORDER-2');
      expect(snackBar.openFromComponent).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ data: { orderId: 'ORDER-2', status: 'CANCELLED' } }),
      );
      expect(orders.getOrdersByAccount).toHaveBeenCalledOnce();
      expect(component.cancelling()).toBeNull();
    });

    it('explains a failed cancel and reloads, as the order may have filled', () => {
      orders.cancelOrder.mockReturnValueOnce(throwError(() => new Error('409')));
      orders.getOrdersByAccount.mockClear();

      component.cancelOrder('ORDER-2');

      expect(snackBar.open).toHaveBeenCalledWith(
        "Couldn't cancel the order. It may already have filled.",
        'Dismiss',
        { duration: 7000 },
      );
      expect(orders.getOrdersByAccount).toHaveBeenCalledOnce();
      expect(component.cancelling()).toBeNull();
    });

    it('tells the user a just-placed order is waiting for its price once polling gives up', () => {
      vi.useFakeTimers();
      const resting = { ...SELL_ORDER, priceLimit: 350 };
      orders.getOrdersByAccount.mockReturnValue(of([resting]));
      dialog.open.mockReturnValueOnce({ afterClosed: () => of(resting) });

      component.openOrderDialog();
      vi.advanceTimersByTime(10 * 500);

      expect(orders.getOrdersByAccount).toHaveBeenCalledTimes(1 + 11);
      expect(snackBar.openFromComponent).toHaveBeenCalledOnce();
      expect(snackBar.openFromComponent).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({ data: { orderId: 'ORDER-2', status: 'NEW', priceLimit: 350 } }),
      );
    });
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
        { provide: LivePrices, useValue: { watch: () => NEVER } },
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
