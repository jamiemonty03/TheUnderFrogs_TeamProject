import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderResponse } from './interfaces/orders.interfaces';

@Injectable({ providedIn: 'root' })
export class Orders {
	constructor(private readonly http: HttpClient) {}

	getOrdersByAccount(accountId: string): Observable<OrderResponse[]> {
		return this.http.get<OrderResponse[]>(`/api/orders/account/${accountId}`);
	}
}
