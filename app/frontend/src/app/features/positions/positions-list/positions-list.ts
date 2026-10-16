import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { Accounts } from '../../../services/accounts';
import { Instruments, LatestPrice, LivePrice } from '../../../services/instruments';
import { LivePrices } from '../../../services/live-prices';
import { Portfolio, PortfolioRisk } from '../../../services/interfaces/portfolios.interfaces';
import { Portfolios } from '../../../services/portfolios';
import { HoldingRow, Position } from '../../../services/interfaces/positions.interfaces';
import { Positions } from '../../../services/positions';
import { summarise, toHoldings } from '../../dashboard/portfolio';

export const ALL_HOLDINGS = 'ALL';
export const REFRESH_MS = 30_000;

type FormMode = 'closed' | 'create' | 'edit';

type SortKey = keyof HoldingRow;

@Component({
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
  ],
  selector: 'app-positions-list',
  styleUrl: './positions-list.scss',
  templateUrl: './positions-list.html',
})
export class PositionsList implements OnInit {
  protected readonly portfoliosService = inject(Portfolios);
  protected readonly positionsService = inject(Positions);
  private readonly instruments = inject(Instruments);
  private readonly liveFeed = inject(LivePrices);
  private readonly accounts = inject(Accounts);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  protected readonly risks: PortfolioRisk[] = ['LOW', 'MEDIUM', 'HIGH'];

  protected readonly portfolios = signal<Portfolio[]>([]);
  protected readonly selectedId = signal<string>(ALL_HOLDINGS);
  protected readonly positions = signal<Position[]>([]);
  protected readonly prices = signal<Map<string, LatestPrice>>(new Map());
  protected readonly pricesFailed = signal(false);
  private readonly livePrices = signal<ReadonlyMap<string, LivePrice>>(new Map());
  private liveWatch?: Subscription;
  private watchedSymbols = '';
  protected readonly cash = signal<number | null>(null);
  protected readonly sort = signal<Sort>({ active: 'marketValue', direction: 'desc' });
  protected readonly formMode = signal<FormMode>('closed');
  protected readonly confirmingDelete = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(/.*\S.*/)]],
    risk: ['MEDIUM' as PortfolioRisk, Validators.required],
    benchmark: ['', Validators.maxLength(100)],
  });

  protected readonly showingAll = computed(() => this.selectedId() === ALL_HOLDINGS);

  protected readonly selectedPortfolio = computed(
    () => this.portfolios().find((p) => p.portfolioId === this.selectedId()) ?? null,
  );

  protected readonly title = computed(() => this.selectedPortfolio()?.name ?? 'All holdings');

  protected readonly columns = computed(() => [
    'symbol',
    'name',
    'assetClass',
    ...(this.showingAll() ? ['portfolio'] : []),
    'quantity',
    'averageCost',
    'price',
    'marketValue',
    'pnl',
    'pnlPercent',
    'dayChange',
    'weight',
    'actions',
  ]);

  private readonly heldPositions = computed(() => this.positions().filter((p) => p.quantity > 0));

  // The dashboard's own calculations, so both pages show the same numbers
  private readonly holdings = computed(() =>
    toHoldings(this.heldPositions(), this.prices(), this.livePrices()),
  );

  private readonly summary = computed(() => summarise(this.holdings()));

  private readonly unsortedRows = computed<HoldingRow[]>(() => {
    const names = new Map(this.portfolios().map((p) => [p.portfolioId, p.name]));
    const held = this.heldPositions();
    return this.holdings().map((h, i) => ({
      symbol: h.symbol,
      name: h.name,
      assetClass: this.prices().get(h.symbol)?.assetClass ?? '',
      portfolioId: held[i].portfolioId,
      portfolioName: names.get(held[i].portfolioId ?? '') ?? 'Main',
      quantity: h.quantity,
      averageCost: h.averageCost,
      totalCost: h.costBasis,
      price: h.price,
      priceSource: h.priceSource,
      marketValue: h.marketValue,
      pnl: h.unrealisedPnl,
      pnlPercent: h.pnlPercent,
      dayChange: h.dailyPnl,
      dayPercent: h.dailyPnlPercent,
      weight: h.weight,
    }));
  });

  protected readonly rows = computed<HoldingRow[]>(() => {
    const { active, direction } = this.sort();
    if (!direction) {
      return this.unsortedRows();
    }
    const key = active as SortKey;
    const factor = direction === 'asc' ? 1 : -1;
    return [...this.unsortedRows()].sort((a, b) => compare(a[key], b[key], factor));
  });

  protected readonly totalCost = computed(() => sum(this.rows().map((r) => r.totalCost)));

  protected readonly marketValue = computed(() => this.summary().invested);

  protected readonly pnl = computed(() => this.summary().unrealisedPnl);

  protected readonly pnlPercent = computed(() => this.summary().pnlPercent);

  protected readonly dayChange = computed(() => this.summary().dailyPnl);

  protected readonly dayPercent = computed(() => this.summary().dailyPnlPercent);

  protected readonly allPriced = computed(() => this.rows().every((r) => r.price !== null));

  protected readonly pricesAsOf = computed(() => {
    const dates = this.rows()
      .map((r) => this.prices().get(r.symbol)?.tradeDate)
      .filter((d): d is string => !!d)
      .sort();
    return dates.length ? dates[dates.length - 1] : null;
  });

  protected readonly symbolCount = computed(
    () => new Set(this.rows().map((row) => row.symbol)).size,
  );

  protected readonly error = computed(
    () => this.portfoliosService.error() ?? this.positionsService.error(),
  );

  ngOnInit(): void {
    this.loadPortfolios();
    this.refresh();
    interval(REFRESH_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.refresh());
  }

  /** Reloads holdings, prices and cash, so fills and new prices show up without a page reload. */
  refresh(): void {
    this.loadPositions();
    this.loadPrices();
    this.loadCash();
  }

  protected tone(value: number | null): string {
    if (value === null || value === 0) {
      return '';
    }
    return value > 0 ? 'pos' : 'neg';
  }

  protected select(portfolioId: string): void {
    if (portfolioId === this.selectedId()) {
      return;
    }
    this.selectedId.set(portfolioId);
    this.closeForm();
    this.confirmingDelete.set(false);
    this.loadPositions();
  }

  protected openCreate(): void {
    this.confirmingDelete.set(false);
    this.form.reset({ name: '', risk: 'MEDIUM', benchmark: '' });
    this.formMode.set('create');
  }

  protected openEdit(): void {
    const portfolio = this.selectedPortfolio();
    if (!portfolio) {
      return;
    }
    this.confirmingDelete.set(false);
    this.form.reset({
      name: portfolio.name,
      risk: portfolio.risk,
      benchmark: portfolio.benchmark ?? '',
    });
    this.formMode.set('edit');
  }

  protected closeForm(): void {
    this.formMode.set('closed');
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { name, risk, benchmark } = this.form.getRawValue();
    const details = { name: name.trim(), risk, benchmark: benchmark.trim() };

    if (this.formMode() === 'create') {
      this.portfoliosService.createPortfolio(details).subscribe({
        next: (created) => {
          this.closeForm();
          this.loadPortfolios();
          this.select(created.portfolioId);
        },
        error: () => undefined,
      });
      return;
    }

    const portfolio = this.selectedPortfolio();
    if (portfolio) {
      this.portfoliosService.updatePortfolio(portfolio.portfolioId, details).subscribe({
        next: () => {
          this.closeForm();
          this.loadPortfolios();
        },
        error: () => undefined,
      });
    }
  }

  protected askToDelete(): void {
    this.closeForm();
    this.confirmingDelete.set(true);
  }

  protected deleteSelected(): void {
    const portfolio = this.selectedPortfolio();
    if (!portfolio || portfolio.defaultPortfolio) {
      return;
    }
    this.portfoliosService.deletePortfolio(portfolio.portfolioId).subscribe({
      next: () => {
        this.confirmingDelete.set(false);
        this.loadPortfolios();
        this.selectedId.set(ALL_HOLDINGS);
        this.loadPositions();
      },
      error: () => this.confirmingDelete.set(false),
    });
  }

  protected riskLabel(risk: PortfolioRisk): string {
    return risk.charAt(0) + risk.slice(1).toLowerCase() + ' risk';
  }

  private loadPortfolios(): void {
    this.portfoliosService.getPortfolios().subscribe({
      next: (portfolios) => this.portfolios.set(portfolios),
      error: () => undefined,
    });
  }

  private loadPositions(): void {
    const portfolioId = this.showingAll() ? undefined : this.selectedId();
    this.positionsService.getPositions(portfolioId).subscribe({
      next: (positions) => {
        this.positions.set(positions);
        this.watchLivePrices(positions.filter((p) => p.quantity > 0).map((p) => p.symbol));
      },
      error: () => this.positions.set([]),
    });
  }

  private loadPrices(): void {
    this.instruments.getLatestPrices().subscribe({
      next: (prices) => {
        this.prices.set(prices);
        this.pricesFailed.set(false);
      },
      error: () => this.pricesFailed.set(true),
    });
  }

  // Live prices for the holdings shown, as on the dashboard. Only restarts when the symbols change
  private watchLivePrices(symbols: string[]): void {
    const key = [...new Set(symbols)].sort().join(',');
    if (key === this.watchedSymbols) {
      return;
    }
    this.watchedSymbols = key;
    this.liveWatch?.unsubscribe();
    this.liveWatch = this.liveFeed
      .watch(key ? key.split(',') : [])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ prices }) => this.livePrices.set(prices));
  }

  private loadCash(): void {
    this.accounts.getAccount().subscribe({
      next: (account) => this.cash.set(account.cashBalance),
      error: () => this.cash.set(null),
    });
  }
}

function sum(values: (number | null)[]): number {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0);
}

function compare(a: unknown, b: unknown, factor: number): number {
  const aMissing = a === null || a === '';
  const bMissing = b === null || b === '';
  if (aMissing || bMissing) return Number(aMissing) - Number(bMissing);
  if (typeof a === 'number' && typeof b === 'number') return (a - b) * factor;
  return String(a).localeCompare(String(b)) * factor;
}
