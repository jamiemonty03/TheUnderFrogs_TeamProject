import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-config';
import type { ProfileChanges, UserProfile } from './interfaces/profile.interfaces';

export type { ProfileChanges, UserProfile } from './interfaces/profile.interfaces';

@Injectable({ providedIn: 'root' })
export class Profile {
  private readonly http = inject(HttpClient);
  private readonly url = `${inject(API_BASE_URL)}/auth/profile`;

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(this.url);
  }

  updateProfile(changes: ProfileChanges): Observable<UserProfile> {
    return this.http.patch<UserProfile>(this.url, changes);
  }
}
