import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Auth } from '../../../services/auth/auth';
import { OrderResponse } from '../../../services/interfaces/orders.interfaces';
import { Orders } from '../../../services/orders';

export const RECENT_ORDER_COUNT = 4;
const LOAD_FAILED = "Couldn't load your orders. Try again in a moment.";

export function latestOrders(orders: OrderResponse[], count = RECENT_ORDER_COUNT): OrderResponse[] {
  return [...orders].sort((first, second) => second.createdAt.localeCompare(first.createdAt)).slice(0, count);
}

@Component({
  imports: [CurrencyPipe, DatePipe, DecimalPipe, RouterLink],
  selector: 'app-recent-orders',
  styleUrl: './recent-orders.scss',
  templateUrl: './recent-orders.html',
})
export class RecentOrders {
  private readonly auth = inject(Auth);
  private readonly ordersApi = inject(Orders);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly orders = signal<OrderResponse[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    const accountId = this.auth.currentUser()?.accountId;
    if (!accountId) {
      this.error.set('No trading account is linked to this user.');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.ordersApi
      .getOrdersByAccount(accountId)
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (orders) => this.orders.set(latestOrders(orders)),
        error: () => this.error.set(LOAD_FAILED),
      });
  }
}
