import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ExternalNavigation {
  goTo(url: string): void {
    window.location.assign(url);
  }
}
