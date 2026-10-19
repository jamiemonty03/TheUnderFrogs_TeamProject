import { Component, computed, inject, signal, viewChild } from '@angular/core';
import {
  MatAutocompleteModule,
  MatAutocompleteSelectedEvent,
  MatAutocompleteTrigger,
} from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../services/auth';
import { Instruments } from '../../services/instruments';
import { Theme } from '../../services/theme';
import { TradingInstrument } from '../../services/interfaces/instruments.interfaces';

const MAX_SUGGESTIONS = 8;

@Component({
  imports: [RouterLink, RouterLinkActive, MatAutocompleteModule, MatButtonModule, MatMenuModule],
  selector: 'app-navigation-bar',
  styleUrl: './navigation-bar.scss',
  templateUrl: './navigation-bar.html',
})
export class NavigationBar {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly instrumentsApi = inject(Instruments);
  private readonly searchTrigger = viewChild(MatAutocompleteTrigger);

  private readonly instruments = signal<TradingInstrument[]>([]);
  private instrumentsRequested = false;
  protected readonly themes = inject(Theme);
  protected readonly logo = computed(() =>
    this.themes.theme() === 'light'
      ? '/brand/toadstock-logo-light.svg'
      : '/brand/toadstock-logo-dark.svg',
  );
  readonly query = signal('');

  readonly suggestions = computed(() => {
    const text = this.query().toLowerCase().trim();
    if (!text) {
      return [];
    }
    return this.instruments()
      .filter(
        (i) =>
          i.symbol.toLowerCase().includes(text) ||
          i.name.toLowerCase().includes(text) ||
          i.type.toLowerCase().includes(text),
      )
      .slice(0, MAX_SUGGESTIONS);
  });

  isMenuOpen = false;

  readonly username = computed(() => this.auth.currentUser()?.username ?? '');
  readonly initial = computed(() => this.username().charAt(0).toUpperCase());
  readonly role = computed(() =>
    this.auth.currentUser()?.roles.includes('ADMIN') ? 'Admin' : 'Trader',
  );

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
  }

  closeMenu(): void {
    this.isMenuOpen = false;
  }

  openProfile(): void {
    this.closeMenu();
    void import('../../features/profile/profile-dialog/profile-dialog').then(({ ProfileDialog }) =>
      this.dialog.open(ProfileDialog, {
        width: '480px',
        maxWidth: '95vw',
        panelClass: 'profile-dialog-panel',
      }),
    );
  }

  openWallet(): void {
    this.closeMenu();
    void import('../../features/wallet/wallet-dialog/wallet-dialog').then(({ WalletDialog }) =>
      this.dialog.open(WalletDialog, {
        width: '440px',
        maxWidth: '95vw',
        panelClass: 'wallet-dialog-panel',
      }),
    );
  }

  loadInstruments(): void {
    if (this.instrumentsRequested) {
      return;
    }
    this.instrumentsRequested = true;
    this.instrumentsApi.getAllTradableInstruments().subscribe({
      next: (instruments) => this.instruments.set(instruments),
      error: () => (this.instrumentsRequested = false),
    });
  }

  selectSuggestion(event: MatAutocompleteSelectedEvent): void {
    this.openInstrument(String(event.option.value));
  }

  search(): void {
    if (this.searchTrigger()?.activeOption) {
      return;
    }
    const typed = this.query().trim().toUpperCase();
    if (!typed) {
      return;
    }
    const exact = this.instruments().find((i) => i.symbol === typed);
    this.openInstrument(exact?.symbol ?? this.suggestions()[0]?.symbol ?? typed);
  }

  private openInstrument(symbol: string): void {
    this.query.set('');
    this.searchTrigger()?.closePanel();
    this.closeMenu();
    void this.router.navigate(['/instruments', symbol]);
  }

  logout(): void {
    this.closeMenu();
    this.auth.logout().subscribe({
      complete: () => void this.router.navigate(['/login'], { replaceUrl: true }),
    });
  }
}
