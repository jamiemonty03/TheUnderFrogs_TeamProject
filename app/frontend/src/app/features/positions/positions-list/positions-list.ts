import { CurrencyPipe, DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { interval } from 'rxjs';
import { Accounts } from '../../../services/accounts';
import { Auth } from '../../../services/auth/auth';
import { Instruments, LatestPrice } from '../../../services/instruments';
import { Portfolio, PortfolioRisk } from '../../../services/interfaces/portfolios.interfaces';
import { Portfolios } from '../../../services/portfolios';
import { Position } from '../../../services/interfaces/positions.interfaces';
import { Positions } from '../../../services/positions';
import { OrderDialog, OrderDialogData } from '../../orders/order-dialog/order-dialog';

export const ALL_HOLDINGS = 'ALL';
export const REFRESH_MS = 30_000;

type FormMode = 'closed' | 'create' | 'edit';

export interface HoldingRow {
  symbol: string;
  name: string;
  assetClass: string;
  portfolioId: string | null;
  portfolioName: string;
  quantity: number;
  averageCost: number;
  totalCost: number;
  price: number | null;
  marketValue: number | null;
  pnl: number | null;
  pnlPercent: number | null;
  dayChange: number | null;
  dayPercent: number | null;
  weight: number | null;
}

type SortKey = keyof HoldingRow;

@Component({
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    PercentPipe,
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
  private readonly accounts = inject(Accounts);
  private readonly auth = inject(Auth);
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  protected readonly risks: PortfolioRisk[] = ['LOW', 'MEDIUM', 'HIGH'];

  protected readonly portfolios = signal<Portfolio[]>([]);
  protected readonly selectedId = signal<string>(ALL_HOLDINGS);
  protected readonly positions = signal<Position[]>([]);
  protected readonly prices = signal<Map<string, LatestPrice>>(new Map());
  protected readonly pricesFailed = signal(false);
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

  private readonly unsortedRows = computed<HoldingRow[]>(() => {
    const names = new Map(this.portfolios().map((p) => [p.portfolioId, p.name]));
    const prices = this.prices();
    const rows = this.positions().map((p) => {
      const quote = prices.get(p.symbol);
      const price = quote?.price ?? null;
      const totalCost = p.quantity * p.averageCost;
      const marketValue = price === null ? null : p.quantity * price;
      const pnl = marketValue === null ? null : marketValue - totalCost;
      const previousClose = quote?.previousClose ?? null;
      const hasDay = price !== null && previousClose !== null && previousClose !== 0;
      return {
        symbol: p.symbol,
        name: quote?.name ?? '',
        assetClass: quote?.assetClass ?? '',
        portfolioId: p.portfolioId,
        portfolioName: (p.portfolioId && names.get(p.portfolioId)) || 'Main',
        quantity: p.quantity,
        averageCost: p.averageCost,
        totalCost,
        price,
        marketValue,
        pnl,
        pnlPercent: pnl === null || totalCost === 0 ? null : pnl / totalCost,
        dayChange: hasDay ? (price - previousClose) * p.quantity : null,
        dayPercent: hasDay ? (price - previousClose) / previousClose : null,
        weight: null as number | null,
      };
    });
    const totalValue = sum(rows.map((r) => r.marketValue));
    return rows.map((r) => ({
      ...r,
      weight: r.marketValue === null || totalValue === 0 ? null : r.marketValue / totalValue,
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

  protected readonly marketValue = computed(() => sum(this.rows().map((r) => r.marketValue)));

  protected readonly pnl = computed(() => sum(this.rows().map((r) => r.pnl)));

  protected readonly pnlPercent = computed(() => {
    const pricedCost = sum(
      this.rows()
        .filter((r) => r.pnl !== null)
        .map((r) => r.totalCost),
    );
    return pricedCost === 0 ? null : this.pnl() / pricedCost;
  });

  protected readonly dayChange = computed(() => sum(this.rows().map((r) => r.dayChange)));

  protected readonly dayPercent = computed(() => {
    const withDay = this.rows().filter((r) => r.dayChange !== null);
    const previousValue =
      sum(withDay.map((r) => r.marketValue)) - sum(withDay.map((r) => r.dayChange));
    return previousValue === 0 ? null : this.dayChange() / previousValue;
  });

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

  protected trade(row: HoldingRow, side: 'BUY' | 'SELL'): void {
    const data: OrderDialogData = {
      side,
      symbol: row.symbol,
      portfolioId: row.portfolioId ?? undefined,
      portfolioName: row.portfolioName,
    };
    this.dialog
      .open(OrderDialog, {
        width: '920px',
        maxWidth: '95vw',
        panelClass: 'order-dialog-panel',
        data,
      })
      .afterClosed()
      .subscribe(() => this.refresh());
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
      next: (positions) => this.positions.set(positions),
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

  private loadCash(): void {
    const accountId = this.auth.currentUser()?.accountId;
    if (!accountId) {
      this.cash.set(null);
      return;
    }
    this.accounts.getAccount(accountId).subscribe({
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
