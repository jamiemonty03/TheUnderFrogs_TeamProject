import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClientTradeResponse, OrderResponse, PlaceOrderRequest } from './interfaces/orders.interfaces';

@Injectable({ providedIn: 'root' })
export class Orders {
	private readonly http = inject(HttpClient);

	getOrdersByAccount(accountId: string): Observable<OrderResponse[]> {
		return this.http.get<OrderResponse[]>(`/api/orders/account/${accountId}`);
	}

	getTradesByAccount(accountId: string): Observable<ClientTradeResponse[]> {
		return this.http.get<ClientTradeResponse[]>(`/api/client-trades/account/${encodeURIComponent(accountId)}`);
	}

	placeOrder(request: PlaceOrderRequest): Observable<OrderResponse> {
		return this.http.post<OrderResponse>('/api/orders', request);
	}

	cancelOrder(orderId: string): Observable<void> {
		return this.http.delete<void>(`/api/orders/${orderId}`);
	}
}
