import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Account {
	accountId: string;
	holderName: string;
	cashBalance: number;
	status: string;
	lastUpdated?: string;
}

@Injectable({ providedIn: 'root' })
export class Accounts {
	private readonly http = inject(HttpClient);

	getAccount(accountId: string): Observable<Account> {
		return this.http.get<Account>(`/api/accounts/${encodeURIComponent(accountId)}`);
	}
}
