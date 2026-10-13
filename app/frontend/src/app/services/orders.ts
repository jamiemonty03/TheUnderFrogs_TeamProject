import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type ApiOrderStatus = 'NEW' | 'FILLED' | 'CANCELLED' | 'REJECTED';
export type ApiOrderSide = 'BUY' | 'SELL';

export interface OrderResponse {
	orderId: string;
	accountId: string;
	symbol: string;
	side: ApiOrderSide;
	quantity: number;
	priceLimit: number;
	orderStatus: ApiOrderStatus;
	version: number;
	createdAt: string;
	lastUpdated: string | null;
	updatedBy: string | null;
}

@Injectable({ providedIn: 'root' })
export class Orders {
	constructor(private readonly http: HttpClient) {}

	getOrdersByAccount(accountId: string): Observable<OrderResponse[]> {
		return this.http.get<OrderResponse[]>(`/api/orders/account/${accountId}`);
	}
}
