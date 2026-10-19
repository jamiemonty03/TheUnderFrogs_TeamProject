import { CurrencyPipe, DatePipe, DecimalPipe, formatDate } from '@angular/common';
import { Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import {
  CategoryScale,
  ChartConfiguration,
  ChartData,
  LineController,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import { BaseChartDirective, provideCharts } from 'ng2-charts';
import { Observable, catchError, combineLatest, distinctUntilChanged, filter, forkJoin, map, of, switchMap, tap, throwError } from 'rxjs';
import { Auth } from '../../../services/auth/auth';
import { Instruments } from '../../../services/instruments';
import { PricePoint } from '../../../services/interfaces/instruments.interfaces';
import { ClientTradeResponse } from '../../../services/interfaces/orders.interfaces';
import { Orders } from '../../../services/orders';
import { Position } from '../../../services/positions';
import { RANGES, SERIES, crosshair, token } from '../../instruments/price-chart/price-chart';

const HISTORY_FAILED = "Couldn't load price history. Try again in a moment.";
const TRADES_FAILED = "Couldn't load your trade history. Try again in a moment.";
const NO_ACCOUNT = 'No trading account is linked to this user.';
const MAX_HISTORY_DAYS = 1260;

export type PerformanceView = 'holdings' | 'trades';
export type PerformanceRange = (typeof RANGES)[number]['key'] | 'All';

export const VIEWS: { key: PerformanceView; label: string }[] = [
  { key: 'holdings', label: 'Current holdings' },
  { key: 'trades', label: 'Trade history' },
];

export const PERFORMANCE_RANGES: { key: PerformanceRange; days: number }[] = [
  ...RANGES,
  { key: 'All', days: Number.POSITIVE_INFINITY },
];

const DEFAULT_RANGE: Record<PerformanceView, PerformanceRange> = { holdings: '1Y', trades: 'All' };

export interface ValuePoint {
  date: string;
  value: number;
}

export interface ReturnPoint {
  date: string;
  value: number;
  gain: number;
  bought: number;
}

interface TradeData {
  trades: ClientTradeResponse[];
  histories: ReadonlyMap<string, PricePoint[]>;
}

export function holdingsHistory(
  positions: Pick<Position, 'symbol' | 'quantity'>[],
  histories: ReadonlyMap<string, PricePoint[]>,
): ValuePoint[] {
  const quantities = new Map<string, number>();
  for (const position of positions) {
    if (position.quantity > 0 && histories.has(position.symbol)) {
      quantities.set(position.symbol, (quantities.get(position.symbol) ?? 0) + position.quantity);
    }
  }
  if (quantities.size === 0) {
    return [];
  }

  const closes = new Map<string, Map<string, number>>();
  const dates = new Set<string>();
  for (const symbol of quantities.keys()) {
    const byDate = new Map<string, number>();
    for (const point of histories.get(symbol)!) {
      byDate.set(point.date, point.close);
      dates.add(point.date);
    }
    closes.set(symbol, byDate);
  }

  const last = new Map<string, number>();
  const points: ValuePoint[] = [];
  for (const date of [...dates].sort()) {
    for (const [symbol, byDate] of closes) {
      const close = byDate.get(date);
      if (close !== undefined) {
        last.set(symbol, close);
      }
    }
    if (last.size === quantities.size) {
      let value = 0;
      for (const [symbol, quantity] of quantities) {
        value += quantity * last.get(symbol)!;
      }
      points.push({ date, value });
    }
  }
  return points;
}

export function returnHistory(
  trades: ClientTradeResponse[],
  histories: ReadonlyMap<string, PricePoint[]>,
): ReturnPoint[] {
  if (trades.length === 0) {
    return [];
  }
  const sorted = [...trades].sort((first, second) => first.tradeDate.localeCompare(second.tradeDate));
  const start = sorted[0].tradeDate;

  const closes = new Map<string, Map<string, number>>();
  const dates = new Set(sorted.map((trade) => trade.tradeDate));
  for (const [symbol, points] of histories) {
    const byDate = new Map<string, number>();
    for (const point of points) {
      if (point.date >= start) {
        byDate.set(point.date, point.close);
        dates.add(point.date);
      }
    }
    closes.set(symbol, byDate);
  }

  const quantities = new Map<string, number>();
  const prices = new Map<string, number>();
  let netInvested = 0;
  let bought = 0;
  let next = 0;
  const points: ReturnPoint[] = [];
  for (const date of [...dates].sort()) {
    for (; next < sorted.length && sorted[next].tradeDate <= date; next++) {
      const trade = sorted[next];
      const quantity = Number(trade.quantity);
      const cost = quantity * Number(trade.price);
      const signed = trade.tradeType === 'BUY' ? quantity : -quantity;
      quantities.set(trade.symbol, (quantities.get(trade.symbol) ?? 0) + signed);
      netInvested += trade.tradeType === 'BUY' ? cost : -cost;
      bought += trade.tradeType === 'BUY' ? cost : 0;
      if (!prices.has(trade.symbol)) {
        prices.set(trade.symbol, Number(trade.price));
      }
    }
    for (const [symbol, byDate] of closes) {
      const close = byDate.get(date);
      if (close !== undefined) {
        prices.set(symbol, close);
      }
    }
    let value = 0;
    for (const [symbol, quantity] of quantities) {
      value += quantity * (prices.get(symbol) ?? 0);
    }
    points.push({ date, value, gain: value - netInvested, bought });
  }
  return points;
}

export interface Change {
  amount: number;
  percent: number | null;
}

export function valueChange(points: ValuePoint[]): Change | null {
  if (points.length < 2) {
    return null;
  }
  const first = points[0].value;
  const amount = points[points.length - 1].value - first;
  return { amount, percent: first === 0 ? null : (amount / first) * 100 };
}

export function rangeChange(points: ReturnPoint[], fromStart: boolean): Change | null {
  if (points.length < (fromStart ? 1 : 2)) {
    return null;
  }
  const first = points[0];
  const last = points[points.length - 1];
  const amount = fromStart ? last.gain : last.gain - first.gain;
  const base = fromStart ? last.bought : first.value + (last.bought - first.bought);
  return { amount, percent: base === 0 ? null : (amount / base) * 100 };
}

export function historyDaysSince(date: string, today = new Date()): number {
  const calendarDays = Math.ceil((today.getTime() - new Date(date).getTime()) / 86_400_000);
  return Math.min(MAX_HISTORY_DAYS, Math.max(21, Math.ceil((calendarDays * 5) / 7) + 10));
}

function sliceRange<T>(points: T[], range: PerformanceRange): T[] {
  const days = PERFORMANCE_RANGES.find((r) => r.key === range)!.days;
  return Number.isFinite(days) ? points.slice(-days) : points;
}

const money = (value: number) => value.toLocaleString('en-US', { style: 'currency', currency: 'USD' });
const signedMoney = (value: number) => `${value > 0 ? '+' : ''}${money(value)}`;

@Component({
  imports: [BaseChartDirective, CurrencyPipe, DatePipe, DecimalPipe],
  providers: [
    provideCharts({
      registerables: [LineController, LineElement, PointElement, LinearScale, CategoryScale, Tooltip],
    }),
  ],
  selector: 'app-performance',
  styleUrls: ['../../instruments/price-chart/price-chart.scss', './performance.scss'],
  templateUrl: './performance.html',
})
export class Performance {
  private readonly auth = inject(Auth);
  private readonly orders = inject(Orders);
  private readonly instruments = inject(Instruments);
  private readonly destroyRef = inject(DestroyRef);

  readonly positions = input<Position[]>([]);
  readonly positionsLoading = input(false);

  protected readonly views = VIEWS;
  protected readonly plugins = [crosshair];
  protected readonly view = signal<PerformanceView>('holdings');
  protected readonly range = signal<PerformanceRange>(DEFAULT_RANGE.holdings);
  protected readonly ranges = computed(() => (this.view() === 'trades' ? PERFORMANCE_RANGES : RANGES));

  private readonly histories = signal<ReadonlyMap<string, PricePoint[]>>(new Map());
  private readonly historiesLoading = signal(false);
  private readonly historiesError = signal<string | null>(null);
  private readonly historiesReload = signal(0);

  private readonly tradeData = signal<TradeData | null>(null);
  private readonly tradesLoading = signal(false);
  private readonly tradesError = signal<string | null>(null);
  private readonly tradesReload = signal(0);

  private readonly symbols = computed(() =>
    [...new Set(this.positions().filter((p) => p.quantity > 0).map((p) => p.symbol))].sort().join(','),
  );

  protected readonly loading = computed(() =>
    this.view() === 'trades' ? this.tradesLoading() : this.positionsLoading() || this.historiesLoading(),
  );
  protected readonly error = computed(() => (this.view() === 'trades' ? this.tradesError() : this.historiesError()));
  protected readonly empty = computed(() =>
    this.view() === 'trades' ? (this.tradeData()?.trades.length ?? 0) === 0 : this.symbols() === '',
  );

  private readonly holdingsPoints = computed(() => holdingsHistory(this.positions(), this.histories()));
  private readonly tradePoints = computed(() => {
    const data = this.tradeData();
    return data ? returnHistory(data.trades, data.histories) : [];
  });
  protected readonly firstTradeDate = computed(() => this.tradePoints()[0]?.date ?? null);

  private readonly visibleHoldings = computed(() => sliceRange(this.holdingsPoints(), this.range()));
  private readonly visibleTrades = computed(() => sliceRange(this.tradePoints(), this.range()));

  protected readonly visible = computed(() =>
    this.view() === 'trades'
      ? this.visibleTrades().map((p) => ({ date: p.date, y: p.gain, value: p.value }))
      : this.visibleHoldings().map((p) => ({ date: p.date, y: p.value, value: p.value })),
  );

  protected readonly change = computed(() =>
    this.view() === 'trades'
      ? rangeChange(this.visibleTrades(), this.range() === 'All')
      : valueChange(this.visibleHoldings()),
  );

  protected readonly tableRows = computed(() => [...this.visible()].reverse());

  readonly data = computed<ChartData<'line', number[], string>>(() => {
    const shown = this.visible();
    const colour = SERIES[0].colour;
    return {
      labels: shown.map((p) => p.date),
      datasets: [
        {
          label: this.view() === 'trades' ? 'Total return' : 'Holdings value',
          data: shown.map((p) => p.y),
          borderColor: colour,
          backgroundColor: colour,
          borderWidth: 2,
          borderJoinStyle: 'round',
          borderCapStyle: 'round',
          pointRadius: 0,
          pointHoverRadius: 4,
          pointHoverBorderWidth: 2,
          pointHoverBorderColor: token('--ts-bg-raised', '#091510'),
        },
      ],
    };
  });

  readonly options = computed<ChartConfiguration<'line'>['options']>(() => {
    const muted = token('--ts-text-muted', '#9db5a7');
    const grid = token('--ts-divider', '#1a2c21');
    const font = {
      family: typeof document === 'undefined' ? undefined : getComputedStyle(document.body).fontFamily,
    };
    const shown = this.visible();
    const trades = this.view() === 'trades';
    const spansYears = shown.length > 0 && shown[0].date.slice(0, 4) !== shown[shown.length - 1].date.slice(0, 4);
    const dateFormat = this.range() === '1Y' || spansYears ? 'MMM yy' : 'MMM d';
    const labels = shown.map((p) => p.date);
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      interaction: { mode: 'index', intersect: false },
      scales: {
        x: {
          grid: { display: false },
          border: { color: grid },
          ticks: {
            color: muted,
            font,
            maxRotation: 0,
            autoSkip: true,
            maxTicksLimit: 5,
            callback: (_, index) => formatDate(labels[index], dateFormat, 'en-US'),
          },
        },
        y: {
          grid: { color: grid },
          border: { display: false },
          ticks: {
            color: muted,
            font,
            maxTicksLimit: 6,
            callback: (value) =>
              Number(value).toLocaleString('en-US', {
                style: 'currency',
                currency: 'USD',
                maximumFractionDigits: Number.isInteger(Number(value)) ? 0 : 2,
              }),
          },
        },
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: token('--ts-bg', '#07110b'),
          borderColor: grid,
          borderWidth: 1,
          titleColor: muted,
          bodyColor: token('--ts-text', '#e8f5ec'),
          titleFont: font,
          bodyFont: font,
          padding: 10,
          boxWidth: 14,
          boxHeight: 2,
          callbacks: {
            title: (items) => formatDate(items[0].label, 'EEE d MMM y', 'en-US'),
            label: (item) =>
              trades
                ? ` ${signedMoney(item.raw as number)}  Total return`
                : ` ${money(item.raw as number)}  Holdings value`,
            afterLabel: (item) => (trades ? `   ${money(shown[item.dataIndex].value)}  Holdings value` : ''),
          },
        },
      },
    };
  });

  constructor() {
    this.watchHoldingsHistory();
    this.watchTradeHistory();
  }

  protected setView(view: PerformanceView): void {
    this.view.set(view);
    this.range.set(DEFAULT_RANGE[view]);
  }

  protected retry(): void {
    if (this.view() === 'trades') {
      this.tradesReload.update((n) => n + 1);
    } else {
      this.historiesReload.update((n) => n + 1);
    }
  }

  private watchHoldingsHistory(): void {
    combineLatest([toObservable(this.symbols).pipe(distinctUntilChanged()), toObservable(this.historiesReload)])
      .pipe(
        switchMap(([symbols]) => {
          const list = symbols ? symbols.split(',') : [];
          if (list.length === 0) {
            return of(new Map<string, PricePoint[]>());
          }
          this.historiesLoading.set(true);
          this.historiesError.set(null);
          return forkJoin(
            list.map((symbol) => this.instruments.getHistory(symbol).pipe(map((points) => [symbol, points] as const))),
          ).pipe(
            map((entries) => new Map(entries)),
            catchError(() => {
              this.historiesError.set(HISTORY_FAILED);
              return of(new Map<string, PricePoint[]>());
            }),
          );
        }),
        tap(() => this.historiesLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((histories) => this.histories.set(histories));
  }

  private watchTradeHistory(): void {
    let fetched: number | null = null;
    toObservable(computed(() => (this.view() === 'trades' ? this.tradesReload() : null)))
      .pipe(
        filter((attempt): attempt is number => attempt !== null && attempt !== fetched),
        tap((attempt) => {
          fetched = attempt;
          this.tradesLoading.set(true);
          this.tradesError.set(null);
        }),
        switchMap(() =>
          this.loadTrades().pipe(
            catchError((err: Error) => {
              this.tradesError.set(err.message === NO_ACCOUNT ? NO_ACCOUNT : TRADES_FAILED);
              return of(null);
            }),
          ),
        ),
        tap(() => this.tradesLoading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((data) => this.tradeData.set(data));
  }

  private loadTrades(): Observable<TradeData> {
    const accountId = this.auth.currentUser()?.accountId;
    if (!accountId) {
      return throwError(() => new Error(NO_ACCOUNT));
    }
    return this.orders.getTradesByAccount(accountId).pipe(
      switchMap((trades) => {
        if (trades.length === 0) {
          return of<TradeData>({ trades, histories: new Map() });
        }
        const first = trades.reduce((earliest, trade) => (trade.tradeDate < earliest ? trade.tradeDate : earliest), trades[0].tradeDate);
        const days = historyDaysSince(first);
        const symbols = [...new Set(trades.map((trade) => trade.symbol))];
        return forkJoin(
          symbols.map((symbol) => this.instruments.getHistory(symbol, days).pipe(map((points) => [symbol, points] as const))),
        ).pipe(map((entries) => ({ trades, histories: new Map(entries) })));
      }),
    );
  }
}
