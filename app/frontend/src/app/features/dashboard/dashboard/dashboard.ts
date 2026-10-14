import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { Subscription, finalize } from 'rxjs';
import { Account, Accounts } from '../../../services/accounts';
import { Instruments, LatestPrice, LivePrice } from '../../../services/instruments';
import { Position, Positions } from '../../../services/positions';
import { Portfolio } from '../../../services/interfaces/portfolios.interfaces';
import { Portfolios } from '../../../services/portfolios';
import { sortHoldings, summarise, toHoldings } from '../portfolio';

const PRICES_FAILED = "Couldn't load the latest prices. Try again in a moment.";

@Component({
  imports: [
    CurrencyPipe,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSelectModule,
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
  private readonly portfoliosService = inject(Portfolios);
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
  protected readonly portfolios = signal<Portfolio[]>([]);
  protected readonly selectedPortfolioId = signal('ALL');
  protected readonly balance = signal<number | null>(null);
  private readonly positions = signal<Position[]>([]);
  private readonly prices = signal<ReadonlyMap<string, LatestPrice>>(new Map());
  private readonly livePrices = signal<ReadonlyMap<string, LivePrice>>(new Map());
  protected readonly liveStale = signal(false);
  private livePolling?: Subscription;

  protected readonly firstName = computed(() => this.account()?.holderName.trim().split(/\s+/)[0] ?? '');

  protected readonly sort = signal<Sort>({ active: '', direction: '' });
  protected readonly filteredPositions = computed(() => {
    const selected = this.selectedPortfolioId();
    return selected === 'ALL'
      ? this.positions()
      : this.positions().filter((position) => position.portfolioId === selected);
  });
  protected readonly holdings = computed(() =>
    toHoldings(this.filteredPositions(), this.prices(), this.livePrices()),
  );
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
    this.loadPortfolios();
    this.loadPositions();
    this.loadPrices();
  }

  protected selectPortfolio(portfolioId: string): void {
    this.selectedPortfolioId.set(portfolioId);
    this.watchLivePrices(
      this.filteredPositions().filter((position) => position.quantity > 0).map((position) => position.symbol),
    );
  }

  private loadPortfolios(): void {
    this.portfoliosService
      .getPortfolios()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (portfolios) => this.portfolios.set(portfolios), error: () => this.portfolios.set([]) });
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
      .subscribe({
        next: (positions) => {
          this.positions.set(positions);
          this.watchLivePrices(
            positions
              .filter((position) => position.quantity > 0 &&
                (this.selectedPortfolioId() === 'ALL' || position.portfolioId === this.selectedPortfolioId()))
              .map((position) => position.symbol),
          );
        },
        error: () => this.positions.set([]),
      });
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

  private watchLivePrices(symbols: string[]): void {
    this.livePolling?.unsubscribe();
    this.livePolling = this.instruments
      .watchLivePrices(symbols)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ prices, stale }) => {
        this.livePrices.set(prices);
        this.liveStale.set(stale);
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
