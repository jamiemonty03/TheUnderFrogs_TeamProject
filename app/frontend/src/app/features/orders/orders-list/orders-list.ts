import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatTableModule } from '@angular/material/table';
import { Auth } from '../../../services/auth/auth';
import { Instruments } from '../../../services/instruments';
import { Orders } from '../../../services/orders';
import { OrderResponse } from '../../../services/interfaces/orders.interfaces';
import { OrderDialog, OrderDialogData } from '../order-dialog/order-dialog';
import { forkJoin } from 'rxjs';
import { OrderFilter, OrderRow } from './orders-list.interfaces';

@Component({
  imports: [CommonModule, MatButtonModule, MatChipsModule, MatDialogModule, MatPaginatorModule, MatTableModule],
  selector: 'app-orders-list',
  styleUrl: './orders-list.scss',
  templateUrl: './orders-list.html',
})
export class OrdersList implements OnInit {
  private readonly dialog = inject(MatDialog);
  private readonly route = inject(ActivatedRoute, { optional: true });
  private readonly auth = inject(Auth);
  private readonly instrumentsApi = inject(Instruments);
  private readonly ordersApi = inject(Orders);

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

  readonly displayedColumns: string[] = ['placed', 'symbol', 'side', 'quantity', 'price', 'status'];

  readonly visibleOrders = computed(() => {
    const filter = this.activeFilter();
    const orders = this.orders();
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
      const { symbol, side } = this.route.snapshot.queryParams;
      this.openOrderDialog({
        symbol: typeof symbol === 'string' ? symbol : undefined,
        side: side === 'BUY' || side === 'SELL' ? side : undefined,
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
    }).afterClosed().subscribe(() => {
      this.loadOrders();
    });
  }

  private loadOrders(): void {
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
      },
      error: () => {
        this.orders.set([]);
        this.loadError.set('Could not load orders from backend.');
        this.isLoading.set(false);
      },
    });
  }

  private toOrderRow(order: OrderResponse, marketPrices: Map<string, number>): OrderRow {
    return {
      placed: this.formatDate(order.createdAt),
      symbol: order.symbol,
      side: order.side,
      quantity: order.quantity,
      price: this.formatPrice(marketPrices.get(order.symbol)),
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
