import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EMPTY, Observable, catchError, forkJoin, map, of, switchMap, tap } from 'rxjs';
import { Instruments, LivePrice, dayChange } from '../../../services/instruments';
import {
  BaseInstrumentResponse,
  BondResponse,
  EtfResponse,
  InstrumentSummary,
  StockResponse,
} from '../../../services/interfaces/instruments.interfaces';
import { Portfolio } from '../../../services/interfaces/portfolios.interfaces';
import { Position } from '../../../services/interfaces/positions.interfaces';
import { Portfolios } from '../../../services/portfolios';
import { Positions } from '../../../services/positions';

type Status = 'loading' | 'ready' | 'not-found' | 'error';
type Details = StockResponse | EtfResponse | BondResponse;

export interface Fact {
  label: string;
  value: string;
}

export const MISSING = '–';

@Component({
  imports: [CurrencyPipe, DecimalPipe, MatButtonModule, RouterLink],
  selector: 'app-instruments-detail',
  styleUrl: './instruments-detail.scss',
  templateUrl: './instruments-detail.html',
})
export class InstrumentsDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly instruments = inject(Instruments);
  private readonly positionsService = inject(Positions);
  private readonly portfoliosService = inject(Portfolios);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly symbol = signal('');
  protected readonly status = signal<Status>('loading');
  protected readonly instrument = signal<InstrumentSummary | null>(null);
  protected readonly details = signal<Details | null>(null);
  protected readonly detailsStatus = signal<'loading' | 'ready' | 'error'>('loading');
  private readonly live = signal<LivePrice | null>(null);
  private readonly positions = signal<Position[]>([]);
  private readonly portfolios = signal<Portfolio[]>([]);

  protected readonly price = computed(() => this.live()?.price ?? this.details()?.price ?? null);
  protected readonly priceSource = computed(() =>
    this.live() ? 'live' : this.details()?.price != null ? 'close' : null,
  );
  protected readonly change = computed(() =>
    dayChange({ price: this.price(), previousClose: this.details()?.previousClose ?? null }),
  );

  protected readonly facts = computed<Fact[]>(() => {
    const instrument = this.instrument();
    const details = this.details();
    return instrument && details ? factsFor(instrument.assetClass, details) : [];
  });

  /** What you hold of this instrument, in each portfolio. */
  protected readonly holdings = computed(() => {
    const names = new Map(this.portfolios().map((p) => [p.portfolioId, p.name]));
    return this.positions()
      .filter((p) => p.symbol === this.symbol() && p.quantity > 0)
      .map((p) => ({
        portfolioId: p.portfolioId,
        portfolioName: names.get(p.portfolioId ?? '') ?? 'Main',
        quantity: p.quantity,
        averageCost: p.averageCost,
        marketValue: this.price() === null ? null : p.quantity * this.price()!,
      }));
  });

  constructor() {
    // The same page is reused when you search for another symbol, so follow the URL
    this.route.paramMap
      .pipe(
        map((params) => (params.get('id') ?? '').trim().toUpperCase()),
        tap((symbol) => this.reset(symbol)),
        switchMap((symbol) => this.load(symbol)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe();

    this.route.paramMap
      .pipe(
        map((params) => (params.get('id') ?? '').trim().toUpperCase()),
        switchMap((symbol) => this.instruments.watchLivePrices([symbol])),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(({ prices }) => this.live.set(prices.get(this.symbol()) ?? null));

    forkJoin({
      positions: this.positionsService.getPositions(),
      portfolios: this.portfoliosService.getPortfolios(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ positions, portfolios }) => {
          this.positions.set(positions);
          this.portfolios.set(portfolios);
        },
        error: () => undefined,
      });
  }

  protected retry(): void {
    const symbol = this.symbol();
    this.reset(symbol);
    this.load(symbol).pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
  }

  private reset(symbol: string): void {
    this.symbol.set(symbol);
    this.status.set('loading');
    this.instrument.set(null);
    this.details.set(null);
    this.detailsStatus.set('loading');
    this.live.set(null);
  }

  // Find out what the instrument is, then load the key facts for its type
  private load(symbol: string): Observable<unknown> {
    return this.instruments.getInstrument(symbol).pipe(
      catchError((err: unknown) => {
        this.status.set(
          err instanceof HttpErrorResponse && err.status === 404 ? 'not-found' : 'error',
        );
        return EMPTY;
      }),
      tap((instrument) => {
        this.instrument.set(instrument);
        this.status.set('ready');
      }),
      switchMap((instrument) =>
        this.detailsFor(instrument).pipe(
          tap((details) => {
            this.details.set(details);
            this.detailsStatus.set('ready');
          }),
          catchError(() => {
            this.detailsStatus.set('error');
            return of(null);
          }),
        ),
      ),
    );
  }

  private detailsFor(instrument: InstrumentSummary): Observable<Details> {
    switch (instrument.assetClass) {
      case 'Equity':
        return this.instruments.getStock(instrument.symbol);
      case 'ETF':
        return this.instruments.getEtf(instrument.symbol);
      case 'Bond':
        return this.instruments.getBond(instrument.symbol);
    }
  }
}

/** The key facts shown for each type. The data stores some percentages as 0.16 (= 0.16%) and some as 0.0419 (= 4.19%). */
export function factsFor(
  assetClass: InstrumentSummary['assetClass'],
  details: BaseInstrumentResponse,
): Fact[] {
  if (assetClass === 'Equity') {
    const stock = details as StockResponse;
    return [
      { label: 'Sector', value: text(stock.sector) },
      { label: 'Industry', value: text(stock.industry) },
      { label: 'P/E (trailing)', value: decimal(stock.trailingPe) },
      { label: 'EPS (trailing)', value: money(stock.trailingEps) },
      { label: 'Market cap', value: compactMoney(stock.marketCap) },
    ];
  }
  if (assetClass === 'ETF') {
    const etf = details as EtfResponse;
    return [
      { label: 'Category', value: text(etf.category) },
      { label: 'Fund family', value: text(etf.fundFamily) },
      { label: 'Expense ratio', value: percent(etf.netExpenseRatio) },
      { label: 'YTD return', value: percent(etf.ytdReturn) },
    ];
  }
  const bond = details as BondResponse;
  return [
    { label: 'Category', value: text(bond.category) },
    { label: 'Distribution yield', value: percent(bond.distributionYield, 100) },
    { label: 'Expense ratio', value: percent(bond.netExpenseRatio) },
    { label: 'YTD return', value: percent(bond.ytdReturn) },
  ];
}

const present = (value: number | null | undefined): value is number =>
  value !== null && value !== undefined && !Number.isNaN(Number(value));

const text = (value: string | null | undefined): string => value?.trim() || MISSING;

const decimal = (value: number | null | undefined): string =>
  present(value) ? Number(value).toFixed(2) : MISSING;

const money = (value: number | null | undefined): string =>
  present(value)
    ? Number(value).toLocaleString('en-US', { style: 'currency', currency: 'USD' })
    : MISSING;

const compactMoney = (value: number | null | undefined): string =>
  present(value)
    ? Number(value).toLocaleString('en-US', {
        style: 'currency',
        currency: 'USD',
        notation: 'compact',
        maximumFractionDigits: 2,
      })
    : MISSING;

const percent = (value: number | null | undefined, scale = 1): string =>
  present(value) ? `${(Number(value) * scale).toFixed(2)}%` : MISSING;
