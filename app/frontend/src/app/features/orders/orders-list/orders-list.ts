import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { Auth } from '../../../services/auth/auth';
import { Instruments } from '../../../services/instruments';
import { LivePrices } from '../../../services/live-prices';
import { Orders } from '../../../services/orders';
import { OrderResponse } from '../../../services/interfaces/orders.interfaces';
import { OrderDialog, OrderDialogData } from '../order-dialog/order-dialog';
import { OrderSnackbar } from '../order-snackbar/order-snackbar';
import { forkJoin } from 'rxjs';
import { OrderFilter, OrderRow } from './orders-list.interfaces';

@Component({
  imports: [CommonModule, MatButtonModule, MatChipsModule, MatDialogModule, MatPaginatorModule, MatSnackBarModule, MatTableModule],
  selector: 'app-orders-list',
  styleUrl: './orders-list.scss',
  templateUrl: './orders-list.html',
})
export class OrdersList implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly route = inject(ActivatedRoute, { optional: true });
  private readonly auth = inject(Auth);
  private readonly instrumentsApi = inject(Instruments);
  private readonly ordersApi = inject(Orders);
  private readonly liveFeed = inject(LivePrices);

  readonly currentUser = this.auth.currentUser;
  readonly accountId = computed(() => this.currentUser()?.accountId ?? '');

  readonly filters: OrderFilter[] = ['ALL', 'NEW', 'FILLED', 'REJECTED', 'CANCELLED'];

  readonly activeFilter = signal<OrderFilter>('ALL');
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly pageSizeOptions = [10, 25, 50];
  readonly orders = signal<OrderRow[]>([]);
  readonly isLoading = signal(false);
  readonly loadError = signal<string | null>(null);

  readonly displayedColumns: string[] = ['placed', 'symbol', 'side', 'quantity', 'limit', 'price', 'status', 'actions'];
  readonly cancelling = signal<string | null>(null);

  private readonly livePrices = toSignal(this.liveFeed.watch(), {
    initialValue: { prices: new Map(), stale: false },
  });

  // Orders with MARKET swapped for the live quote an order is checked against: the ask for a BUY, the bid for a SELL
  readonly liveOrders = computed(() => {
    const prices = this.livePrices().prices;
    return this.orders().map((order) => {
      const quote = prices.get(order.symbol);
      const live = order.side === 'BUY' ? (quote?.ask ?? quote?.price) : (quote?.bid ?? quote?.price);
      return live != null && live > 0 ? { ...order, price: this.formatPrice(live), live: true } : { ...order, live: false };
    });
  });

  readonly visibleOrders = computed(() => {
    const filter = this.activeFilter();
    const orders = this.liveOrders();
    if (filter === 'ALL') {
      return orders;
    }
    return orders.filter((order) => order.status === filter);
  });

  readonly pagedOrders = computed(() => {
    const start = this.pageIndex() * this.pageSize();
    return this.visibleOrders().slice(start, start + this.pageSize());
  });

  ngOnInit(): void {
    this.loadOrders();
    if (this.route?.snapshot.queryParams['new'] === 'true') {
      const { symbol, side, portfolioId } = this.route.snapshot.queryParams;
      this.openOrderDialog({
        symbol: typeof symbol === 'string' ? symbol : undefined,
        side: side === 'BUY' || side === 'SELL' ? side : undefined,
        portfolioId: typeof portfolioId === 'string' ? portfolioId : undefined,
      });
    }
  }

  setFilter(filter: OrderFilter): void {
    this.activeFilter.set(filter);
    this.pageIndex.set(0);
  }

  setPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  getFilterCount(filter: OrderFilter): number {
    const orders = this.orders();
    if (filter === 'ALL') {
      return orders.length;
    }
    return orders.filter((order) => order.status === filter).length;
  }

  openOrderDialog(data?: OrderDialogData): void {
    this.dialog.open(OrderDialog, {
      width: '920px',
      maxWidth: '95vw',
      panelClass: 'order-dialog-panel',
      ...(data ? { data } : {}),
    }).afterClosed().subscribe((order: OrderResponse | undefined) => {
      this.loadOrders(order?.orderId);
    });
  }

  cancelOrder(orderId: string): void {
    this.cancelling.set(orderId);
    this.ordersApi.cancelOrder(orderId).subscribe({
      next: () => {
        this.cancelling.set(null);
        this.snackBar.openFromComponent(OrderSnackbar, {
          data: { orderId, status: 'CANCELLED' },
          duration: 5000,
          panelClass: ['order-snackbar-panel'],
        });
        this.loadOrders();
      },
      error: () => {
        this.cancelling.set(null);
        this.snackBar.open("Couldn't cancel the order. It may already have filled.", 'Dismiss', { duration: 7000 });
        this.loadOrders();
      },
    });
  }

  private loadOrders(orderIdToNotify?: string, attempt = 0): void {
    const accountId = this.accountId();
    if (!accountId) {
      this.orders.set([]);
      this.loadError.set('No account is associated with the current user.');
      return;
    }

    this.isLoading.set(true);
    this.loadError.set(null);

    forkJoin({
      orders: this.ordersApi.getOrdersByAccount(accountId),
      instruments: this.instrumentsApi.getAllTradableInstruments(),
    }).subscribe({
      next: ({ orders, instruments }) => {
        const marketPrices = new Map(instruments.map((instrument) => [instrument.symbol, instrument.marketPrice]));
        const newestFirst = [...orders].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
        this.orders.set(newestFirst.map((row) => this.toOrderRow(row, marketPrices)));
        this.pageIndex.set(0);
        this.isLoading.set(false);
        this.notifyForOrder(orders.find((order) => order.orderId === orderIdToNotify), orderIdToNotify, attempt);
      },
      error: () => {
        this.orders.set([]);
        this.loadError.set('Could not load orders from backend.');
        this.isLoading.set(false);
      },
    });
  }

  private notifyForOrder(order: OrderResponse | undefined, orderId: string | undefined, attempt: number): void {
    if (!order || !orderId) {
      return;
    }
    if (order.orderStatus === 'NEW' && attempt < 10) {
      window.setTimeout(() => this.loadOrders(orderId, attempt + 1), 500);
      return;
    }
    if (order.orderStatus === 'FILLED') {
      this.snackBar.openFromComponent(OrderSnackbar, {
        data: { orderId: order.orderId, status: 'FILLED' },
        duration: 5000,
        panelClass: ['order-snackbar-panel'],
      });
    } else if (order.orderStatus === 'REJECTED') {
      this.snackBar.openFromComponent(OrderSnackbar, {
        data: {
          orderId: order.orderId,
          status: 'REJECTED',
          reason: order.rejectionReason,
        },
        duration: 7000,
        panelClass: ['order-snackbar-panel'],
      });
    } else if (order.orderStatus === 'NEW') {
      this.snackBar.openFromComponent(OrderSnackbar, {
        data: { orderId: order.orderId, status: 'NEW', priceLimit: order.priceLimit },
        duration: 7000,
        panelClass: ['order-snackbar-panel'],
      });
    }
  }

  private toOrderRow(order: OrderResponse, marketPrices: Map<string, number>): OrderRow {
    return {
      id: order.orderId,
      placed: this.formatDate(order.createdAt),
      symbol: order.symbol,
      side: order.side,
      quantity: order.quantity,
      price: this.formatPrice(marketPrices.get(order.symbol)),
      limit: this.formatPrice(order.priceLimit),
      status: order.orderStatus,
    };
  }

  private formatPrice(value: number | undefined): string {
    return value == null ? 'N/A' : `$${Number(value).toFixed(2)}`;
  }

  private formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return new Intl.DateTimeFormat('en-GB', {
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(date).replace(',', '');
  }
}
