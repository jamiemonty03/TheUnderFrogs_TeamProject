import { Injectable, signal } from '@angular/core';

export interface CurrentUser {
  userId: number;
  username: string;
  roles: string[];
  accountId: string | null;
}

@Injectable({ providedIn: 'root' })
export class Auth {
  readonly currentUser = signal<CurrentUser | null>(null);
}
