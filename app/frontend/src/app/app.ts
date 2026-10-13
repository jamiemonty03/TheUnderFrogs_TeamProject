import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { NavigationBar } from './shared/navigation-bar/navigation-bar';

const FULL_SCREEN_PAGES = /^\/(login|register)(?:[/?#]|$)/;

@Component({
  imports: [NavigationBar, RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  private readonly router = inject(Router);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  readonly showShell = computed(() => !FULL_SCREEN_PAGES.test(this.url()));
}
