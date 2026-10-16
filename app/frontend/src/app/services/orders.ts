import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderResponse, PlaceOrderRequest } from './interfaces/orders.interfaces';

@Injectable({ providedIn: 'root' })
export class Orders {
	private readonly http = inject(HttpClient);

	getOrdersByAccount(accountId: string): Observable<OrderResponse[]> {
		return this.http.get<OrderResponse[]>(`/api/orders/account/${accountId}`);
	}

	placeOrder(request: PlaceOrderRequest): Observable<OrderResponse> {
		return this.http.post<OrderResponse>('/api/orders', request);
	}
}
