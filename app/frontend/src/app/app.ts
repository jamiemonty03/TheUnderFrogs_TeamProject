import {
  Component,
  ElementRef,
  afterRenderEffect,
  computed,
  inject,
  viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { NavigationBar } from './shared/navigation-bar/navigation-bar';
import { NewsFooter } from './shared/news-footer/news-footer';
import { AssistantChat } from './features/assistant/assistant-chat';
import { LilyPadAssistant } from './features/assistant/lily-pad-assistant/lily-pad-assistant';

const FULL_SCREEN_PAGES = /^\/(login|register)(?:[/?#]|$)/;

@Component({
  imports: [LilyPadAssistant, NavigationBar, NewsFooter, RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
  host: {
    '[class.with-shell]': 'showShell()',
    '[class.assistant-open]': 'showShell() && assistant.open()',
  },
})
export class App {
  private readonly router = inject(Router);
  protected readonly assistant = inject(AssistantChat);
  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: null },
  );

  readonly showShell = computed(() => {
    const url = this.url();
    return url !== null && !FULL_SCREEN_PAGES.test(url);
  });

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly nav = viewChild(NavigationBar, { read: ElementRef });

  constructor() {
    afterRenderEffect((onCleanup) => {
      const nav = this.nav()?.nativeElement as HTMLElement | undefined;
      if (!nav || !this.assistant.open()) {
        return;
      }
      const bar = nav.firstElementChild ?? nav;
      const update = () => {
        const bottom = Math.max(0, Math.round(bar.getBoundingClientRect().bottom));
        this.host.nativeElement.style.setProperty('--nav-offset', `${bottom}px`);
      };
      update();
      window.addEventListener('scroll', update, { passive: true });
      window.addEventListener('resize', update);
      const resized = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(update);
      resized?.observe(bar);
      onCleanup(() => {
        window.removeEventListener('scroll', update);
        window.removeEventListener('resize', update);
        resized?.disconnect();
      });
    });
  }
}
