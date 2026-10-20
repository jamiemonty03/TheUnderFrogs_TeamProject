import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  EMPTY,
  Observable,
  catchError,
  concatMap,
  last,
  map,
  take,
  takeWhile,
  tap,
  timer,
} from 'rxjs';
import { OrderSnackbar } from '../features/orders/order-snackbar/order-snackbar';
import { Auth } from './auth';
import { OrderResponse } from './interfaces/orders.interfaces';
import { Orders } from './orders';

export const CHECK_EVERY_MS = 500;
export const MORE_CHECKS = 10;

@Injectable({ providedIn: 'root' })
export class OrderNotifier {
  private readonly orders = inject(Orders);
  private readonly auth = inject(Auth);
  private readonly snackBar = inject(MatSnackBar);

  follow(orderId: string): Observable<OrderResponse> {
    const accountId = this.auth.currentUser()?.accountId;
    if (!accountId) {
      return EMPTY;
    }
    return timer(0, CHECK_EVERY_MS).pipe(
      take(1 + MORE_CHECKS),
      concatMap(() => this.orders.getOrdersByAccount(accountId)),
      map((orders) => orders.find((order) => order.orderId === orderId)),
      takeWhile((order) => order?.orderStatus === 'NEW', true),
      last(),
      concatMap((order) => (order ? [order] : [])),
      tap((order) => this.tell(order)),
      catchError(() => EMPTY),
    );
  }

  private tell(order: OrderResponse): void {
    const panelClass = ['order-snackbar-panel'];
    switch (order.orderStatus) {
      case 'FILLED':
        this.snackBar.openFromComponent(OrderSnackbar, {
          data: { orderId: order.orderId, status: 'FILLED' },
          duration: 5000,
          panelClass,
        });
        break;
      case 'REJECTED':
        this.snackBar.openFromComponent(OrderSnackbar, {
          data: { orderId: order.orderId, status: 'REJECTED', reason: order.rejectionReason },
          duration: 7000,
          panelClass,
        });
        break;
      case 'NEW':
        this.snackBar.openFromComponent(OrderSnackbar, {
          data: { orderId: order.orderId, status: 'NEW', priceLimit: order.priceLimit },
          duration: 7000,
          panelClass,
        });
        break;
    }
  }
}
