import { Injectable, signal } from '@angular/core';

export type ThemeName = 'light' | 'dark';

export const THEME_KEY = 'toadstock.theme';

export const DEFAULT_THEME: ThemeName = 'dark';

function storedChoice(): ThemeName | null {
  try {
    const value = localStorage.getItem(THEME_KEY);
    return value === 'light' || value === 'dark' ? value : null;
  } catch {
    return null;
  }
}

function apply(theme: ThemeName): void {
  document.documentElement.dataset['theme'] = theme;
}

export function applyInitialTheme(): void {
  apply(storedChoice() ?? DEFAULT_THEME);
}

@Injectable({ providedIn: 'root' })
export class Theme {
  private readonly current = signal<ThemeName>(storedChoice() ?? DEFAULT_THEME);

  readonly theme = this.current.asReadonly();

  constructor() {
    apply(this.theme());
  }

  toggle(): void {
    this.set(this.theme() === 'dark' ? 'light' : 'dark');
  }

  set(theme: ThemeName): void {
    this.current.set(theme);
    apply(theme);
    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch {
      return;
    }
  }
}
