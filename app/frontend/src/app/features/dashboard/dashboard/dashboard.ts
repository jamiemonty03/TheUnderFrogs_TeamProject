import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Account, Accounts } from '../../../services/accounts';
import { Instruments, LatestPrice } from '../../../services/instruments';
import { Position, Positions } from '../../../services/positions';
import { sortHoldings, summarise, toHoldings } from '../portfolio';

const PRICES_FAILED = "Couldn't load the latest prices. Try again in a moment.";

@Component({
  imports: [
    CurrencyPipe,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSortModule,
    MatTableModule,
  ],
  selector: 'app-dashboard',
  styleUrl: './dashboard.scss',
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private readonly accounts = inject(Accounts);
  private readonly positionsService = inject(Positions);
  private readonly instruments = inject(Instruments);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly columns = [
    'symbol',
    'name',
    'quantity',
    'averageCost',
    'price',
    'marketValue',
    'unrealisedPnl',
    'pnlPercent',
    'weight',
    'actions',
  ];

  protected readonly accountState = this.accounts.account;
  protected readonly balanceState = this.accounts.balance;
  protected readonly positionsLoading = this.positionsService.loading.asReadonly();
  protected readonly positionsError = this.positionsService.error.asReadonly();
  protected readonly pricesLoading = signal(false);
  protected readonly pricesError = signal<string | null>(null);

  protected readonly account = signal<Account | null>(null);
  protected readonly balance = signal<number | null>(null);
  private readonly positions = signal<Position[]>([]);
  private readonly prices = signal<ReadonlyMap<string, LatestPrice>>(new Map());

  protected readonly firstName = computed(() => this.account()?.holderName.trim().split(/\s+/)[0] ?? '');

  protected readonly sort = signal<Sort>({ active: '', direction: '' });
  protected readonly holdings = computed(() => toHoldings(this.positions(), this.prices()));
  protected readonly rows = computed(() => sortHoldings(this.holdings(), this.sort()));
  protected readonly summary = computed(() => summarise(this.holdings()));

  protected readonly portfolioValue = computed(() => {
    const cash = this.balance();
    return cash === null ? null : cash + this.summary().invested;
  });

  protected readonly valuationLoading = computed(() => this.positionsLoading() || this.pricesLoading());
  protected readonly valuationError = computed(
    () =>
      this.positionsError() ?? (this.holdings().length > 0 ? this.pricesError() : null),
  );

  constructor() {
    this.loadAccount();
    this.loadBalance();
    this.loadPositions();
    this.loadPrices();
  }

  protected loadAccount(): void {
    this.accounts
      .getAccount()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (account) => this.account.set(account), error: () => this.account.set(null) });
  }

  protected loadBalance(): void {
    this.accounts
      .getBalance()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (balance) => this.balance.set(balance), error: () => this.balance.set(null) });
  }

  protected loadPositions(): void {
    this.positionsService
      .getPositions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (positions) => this.positions.set(positions), error: () => this.positions.set([]) });
  }

  protected loadPrices(): void {
    this.pricesLoading.set(true);
    this.pricesError.set(null);
    this.instruments
      .getLatestPrices()
      .pipe(
        finalize(() => this.pricesLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (prices) => this.prices.set(prices),
        error: () => this.pricesError.set(PRICES_FAILED),
      });
  }

  protected retryValuation(): void {
    if (this.positionsError()) {
      this.loadPositions();
    }
    if (this.pricesError()) {
      this.loadPrices();
    }
  }
}
