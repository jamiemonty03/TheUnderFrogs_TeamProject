import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { RouterLink } from '@angular/router';
import { Portfolio, PortfolioRisk, Portfolios } from '../../../services/portfolios';
import { Position, Positions } from '../../../services/positions';

export const ALL_HOLDINGS = 'ALL';

type FormMode = 'closed' | 'create' | 'edit';

interface HoldingRow {
  symbol: string;
  portfolioName: string;
  quantity: number;
  averageCost: number;
  totalCost: number;
}

@Component({
  imports: [
    CurrencyPipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
  ],
  selector: 'app-positions-list',
  styleUrl: './positions-list.scss',
  templateUrl: './positions-list.html',
})
export class PositionsList implements OnInit {
  protected readonly portfoliosService = inject(Portfolios);
  protected readonly positionsService = inject(Positions);
  private readonly fb = inject(FormBuilder);

  protected readonly risks: PortfolioRisk[] = ['LOW', 'MEDIUM', 'HIGH'];

  protected readonly portfolios = signal<Portfolio[]>([]);
  protected readonly selectedId = signal<string>(ALL_HOLDINGS);
  protected readonly positions = signal<Position[]>([]);
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

  protected readonly columns = computed(() =>
    this.showingAll()
      ? ['symbol', 'portfolio', 'quantity', 'averageCost', 'totalCost']
      : ['symbol', 'quantity', 'averageCost', 'totalCost'],
  );

  protected readonly rows = computed<HoldingRow[]>(() => {
    const names = new Map(this.portfolios().map((p) => [p.portfolioId, p.name]));
    return this.positions()
      .map((p) => ({
        symbol: p.symbol,
        portfolioName: (p.portfolioId && names.get(p.portfolioId)) || 'Main',
        quantity: p.quantity,
        averageCost: p.averageCost,
        totalCost: p.quantity * p.averageCost,
      }))
      .sort((a, b) => b.totalCost - a.totalCost);
  });

  protected readonly totalCost = computed(() => this.rows().reduce((sum, row) => sum + row.totalCost, 0));

  protected readonly symbolCount = computed(() => new Set(this.rows().map((row) => row.symbol)).size);

  protected readonly error = computed(() => this.portfoliosService.error() ?? this.positionsService.error());

  ngOnInit(): void {
    this.loadPortfolios();
    this.loadPositions();
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
    this.form.reset({ name: portfolio.name, risk: portfolio.risk, benchmark: portfolio.benchmark ?? '' });
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
}
