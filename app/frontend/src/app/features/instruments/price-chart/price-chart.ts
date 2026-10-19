import { CurrencyPipe, DatePipe, DecimalPipe, formatDate } from '@angular/common';
import {
  Component,
  ElementRef,
  computed,
  input,
  linkedSignal,
  signal,
  viewChild,
} from '@angular/core';
import {
  BarController,
  BarElement,
  CategoryScale,
  Chart,
  ChartConfiguration,
  ChartData,
  ChartType,
  Filler,
  LineController,
  LineElement,
  LinearScale,
  Plugin,
  PointElement,
  ScriptableContext,
  Tooltip,
} from 'chart.js';
import { BaseChartDirective, provideCharts } from 'ng2-charts';
import { LivePrice, PricePoint } from '../../../services/interfaces/instruments.interfaces';

export type RangeKey = '1M' | '3M' | '6M' | '1Y';
export type ChartMode = 'line' | 'candles';

export const RANGES: { key: RangeKey; days: number }[] = [
  { key: '1M', days: 21 },
  { key: '3M', days: 63 },
  { key: '6M', days: 126 },
  { key: '1Y', days: 252 },
];

export const MODES: { key: ChartMode; label: string }[] = [
  { key: 'line', label: 'Line' },
  { key: 'candles', label: 'Candles' },
];

export const SERIES = [
  { key: 'close', label: 'Close', colour: '#18a957' },
  { key: 'movingAvg20', label: '20-day average', colour: '#3a8ad6' },
  { key: 'movingAvg50', label: '50-day average', colour: '#c48320' },
] as const;

export const AVERAGES = SERIES.slice(1);

export type AverageKey = (typeof AVERAGES)[number]['key'];

export const UP = '#22c55e';
export const DOWN = '#ef4444';

export const MIN_DAYS = 10;

const ZOOM_STEP = 1.25;

type Candle = PricePoint & { open: number; high: number; low: number };

const withNulls = (p: PricePoint): Required<PricePoint> => ({
  ...p,
  open: p.open ?? null,
  high: p.high ?? null,
  low: p.low ?? null,
  volume: p.volume ?? null,
});

const isCandle = (p: PricePoint): p is Candle => p.open != null && p.high != null && p.low != null;

const isUp = (p: PricePoint) => (p.open ?? p.close) <= p.close;

const NEW_YORK_DAY = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'America/New_York',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

export function tradingDay(time: string | null | undefined): string | null {
  const date = new Date(time ?? '');
  return Number.isNaN(date.getTime()) ? null : NEW_YORK_DAY.format(date);
}

export interface SessionRange {
  date: string;
  high: number;
  low: number;
}

export function withLive(
  points: Required<PricePoint>[],
  quote: LivePrice | null,
  session: SessionRange | null,
): Required<PricePoint>[] {
  const last = points.at(-1);
  if (!quote || !last) {
    return points;
  }
  const day = tradingDay(quote.quoteTime);
  if (day === null || day < last.date) {
    return points;
  }
  const seen = session?.date === day ? session : { high: quote.price, low: quote.price };
  if (day === last.date) {
    return [
      ...points.slice(0, -1),
      {
        ...last,
        close: quote.price,
        high: Math.max(last.high ?? last.close, seen.high, quote.price),
        low: Math.min(last.low ?? last.close, seen.low, quote.price),
      },
    ];
  }
  const open = last.close;
  return [
    ...points,
    {
      date: day,
      open,
      high: Math.max(open, seen.high, quote.price),
      low: Math.min(open, seen.low, quote.price),
      close: quote.price,
      volume: null,
      movingAvg20: null,
      movingAvg50: null,
    },
  ];
}

const money = (value: number) =>
  value.toLocaleString('en-US', { style: 'currency', currency: 'USD' });

const compact = (value: number) =>
  value.toLocaleString('en-US', { notation: 'compact', maximumFractionDigits: 2 });

export function token(name: string, fallback: string): string {
  if (typeof document === 'undefined') {
    return fallback;
  }
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

const faded = (hex: string) => `${hex}59`;

const areaFill =
  (hex: string) =>
  ({ chart }: ScriptableContext<'line'>) => {
    const { chartArea, ctx } = chart;
    if (!chartArea) {
      return 'transparent';
    }
    const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
    gradient.addColorStop(0, `${hex}40`);
    gradient.addColorStop(1, `${hex}00`);
    return gradient;
  };

function tag(
  ctx: CanvasRenderingContext2D,
  label: string,
  right: number,
  y: number,
  fill: string,
  colour: string,
  fontFamily: string,
): void {
  ctx.font = `600 11px ${fontFamily}`;
  const width = ctx.measureText(label).width + 10;
  ctx.fillStyle = fill;
  ctx.fillRect(right - width, y - 9, width, 18);
  ctx.fillStyle = colour;
  ctx.textBaseline = 'middle';
  ctx.fillText(label, right - width + 5, y);
}

const pointerY = new WeakMap<Chart, number | null>();

export interface LastPrice {
  price: number;
  colour: string;
}

/** The crosshair, plus a line at the latest price when `last` gives one. */
export const crosshairWith = (last: () => LastPrice | null): Plugin => ({
  id: 'crosshair',
  afterEvent(chart, args) {
    const { event, inChartArea } = args;
    const y = event.type === 'mouseout' || !inChartArea ? null : event.y;
    if (pointerY.get(chart) !== y) {
      pointerY.set(chart, y);
      args.changed = true;
    }
  },
  afterDatasetsDraw(chart) {
    const { ctx, chartArea } = chart;
    const fontFamily = getComputedStyle(chart.canvas).fontFamily;
    const scale = chart.scales['y'];

    // The latest price as a dashed line with a tag on the price axis
    const latest = last();
    if (latest && scale) {
      const { price, colour } = latest;
      const y = scale.getPixelForValue(price);
      if (y >= chartArea.top && y <= chartArea.bottom) {
        ctx.save();
        ctx.strokeStyle = colour;
        ctx.lineWidth = 1;
        ctx.setLineDash([2, 3]);
        ctx.beginPath();
        ctx.moveTo(chartArea.left, y);
        ctx.lineTo(chartArea.right, y);
        ctx.stroke();
        tag(ctx, money(price), chartArea.right, y, colour, '#ffffff', fontFamily);
        ctx.restore();
      }
    }

    const active = chart.tooltip?.getActiveElements();
    if (!active?.length) {
      return;
    }
    const text = token('--ts-text', '#e8f5ec');
    const bg = token('--ts-bg', '#07110b');
    const x = active[0].element.x;
    const y = pointerY.get(chart);
    ctx.save();
    ctx.strokeStyle = token('--ts-text-muted', '#9db5a7');
    ctx.lineWidth = 1;
    ctx.setLineDash([4, 4]);
    ctx.beginPath();
    ctx.moveTo(x, chartArea.top);
    ctx.lineTo(x, chartArea.bottom);
    if (y != null && scale) {
      ctx.moveTo(chartArea.left, y);
      ctx.lineTo(chartArea.right, y);
    }
    ctx.stroke();
    ctx.setLineDash([]);

    if (y != null && scale) {
      tag(ctx, money(scale.getValueForPixel(y) ?? 0), chartArea.right, y, text, bg, fontFamily);
    }

    const label = chart.data.labels?.[active[0].index];
    if (typeof label === 'string') {
      ctx.font = `600 11px ${fontFamily}`;
      const date = formatDate(label, 'EEE d MMM yy', 'en-US');
      const width = ctx.measureText(date).width + 10;
      const left = Math.min(Math.max(x - width / 2, chartArea.left), chartArea.right - width);
      ctx.fillStyle = text;
      ctx.fillRect(left, chartArea.bottom - 18, width, 18);
      ctx.fillStyle = bg;
      ctx.textBaseline = 'middle';
      ctx.fillText(date, left + 5, chartArea.bottom - 9);
    }
    ctx.restore();
  },
});

/** The plain crosshair, for charts with no latest-price line. */
export const crosshair = crosshairWith(() => null);

@Component({
  imports: [BaseChartDirective, CurrencyPipe, DatePipe, DecimalPipe],
  providers: [
    provideCharts({
      registerables: [
        BarController,
        BarElement,
        LineController,
        LineElement,
        PointElement,
        LinearScale,
        CategoryScale,
        Tooltip,
        Filler,
      ],
    }),
  ],
  selector: 'app-price-chart',
  styleUrl: './price-chart.scss',
  templateUrl: './price-chart.html',
})
export class PriceChart {
  readonly points = input.required<PricePoint[]>();
  readonly live = input<LivePrice | null>(null);

  private readonly session = linkedSignal<LivePrice | null, SessionRange | null>({
    source: this.live,
    computation: (quote, previous) => {
      if (!quote) {
        return null;
      }
      const date = tradingDay(quote.quoteTime);
      if (date === null) {
        return null;
      }
      const seen = previous?.value;
      return seen?.date === date
        ? {
            date,
            high: Math.max(seen.high, quote.price),
            low: Math.min(seen.low, quote.price),
          }
        : { date, high: quote.price, low: quote.price };
    },
  });

  protected readonly ranges = RANGES;
  protected readonly modes = MODES;
  protected readonly series = SERIES;
  protected readonly averages = AVERAGES;
  protected readonly plugins = [crosshairWith(() => this.lastPrice())];

  readonly mode = signal<ChartMode>('line');
  /** The moving averages drawn over the price; none until you switch them on. */
  readonly shownAverages = signal<ReadonlySet<AverageKey>>(new Set());
  protected readonly range = signal<RangeKey>('1Y');
  private readonly activeIndex = signal<number | null>(null);
  private readonly hoverIndex = signal<number | null>(null);
  private readonly chart = viewChild(BaseChartDirective);
  private readonly plot = viewChild.required<ElementRef<HTMLElement>>('plot');

  private readonly all = computed(() =>
    withLive(this.points().map(withNulls), this.live(), this.session()),
  );

  readonly isLive = computed(() => {
    const quote = this.live();
    const last = this.points().at(-1);
    const day = quote ? tradingDay(quote.quoteTime) : null;
    return day !== null && !!last && day >= last.date;
  });

  private readonly zoom = signal<{ start: number; end: number } | null>(null);
  private drag: { x: number; start: number; end: number } | null = null;
  protected readonly dragging = signal(false);

  readonly window = computed(() => {
    const zoom = this.zoom();
    if (zoom) {
      return zoom;
    }
    const total = this.all().length;
    const days = RANGES.find((r) => r.key === this.range())!.days;
    return { start: Math.max(0, total - days), end: total };
  });

  readonly zoomed = computed(() => this.zoom() !== null);

  protected readonly visible = computed(() => {
    const { start, end } = this.window();
    return this.all().slice(start, end);
  });

  protected readonly period = computed(() =>
    this.zoomed() ? `${this.visible().length} days` : this.range(),
  );

  protected readonly hasCandles = computed(() => this.visible().some(isCandle));

  protected readonly chartType = computed<ChartType>(() =>
    this.mode() === 'candles' && this.hasCandles() ? 'bar' : 'line',
  );

  protected readonly change = computed(() => {
    const shown = this.visible();
    if (shown.length < 2) {
      return null;
    }
    const first = shown[0].close;
    const amount = shown[shown.length - 1].close - first;
    return { amount, percent: first === 0 ? 0 : (amount / first) * 100 };
  });

  readonly ohlc = computed(() => {
    const shown = this.visible();
    const index = this.hoverIndex() ?? this.activeIndex() ?? shown.length - 1;
    const day = shown[index];
    if (!day) {
      return null;
    }
    const all = this.all();
    const previous = all[this.window().start + index - 1]?.close ?? day.open ?? null;
    const amount = previous === null ? null : day.close - previous;
    return {
      ...day,
      amount,
      percent: amount === null || !previous ? null : (amount / previous) * 100,
    };
  });

  readonly lastPrice = computed<LastPrice | null>(() => {
    const latest = this.all().at(-1);
    return this.chartType() === 'bar' && latest
      ? { price: latest.close, colour: isUp(latest) ? UP : DOWN }
      : null;
  });

  readonly data = computed<ChartData<ChartType, unknown[], string>>(() =>
    this.chartType() === 'bar' ? this.candleData() : this.lineData(),
  );

  private lineData(): ChartData<'line', (number | null)[], string> {
    const shown = this.visible();
    const surface = token('--ts-bg-raised', '#091510');
    return {
      labels: shown.map((p) => p.date),
      datasets: SERIES.filter((s) => s.key === 'close' || this.shownAverages().has(s.key)).map(
        (s) => ({
          label: s.label,
          data: shown.map((p) => p[s.key]),
          borderColor: s.colour,
          backgroundColor: s.key === 'close' ? areaFill(s.colour) : s.colour,
          fill: s.key === 'close' ? 'start' : false,
          borderWidth: 2,
          borderJoinStyle: 'round' as const,
          borderCapStyle: 'round' as const,
          pointRadius: 0,
          pointHoverRadius: 4,
          pointHoverBorderWidth: 2,
          pointHoverBorderColor: surface,
          spanGaps: false,
        }),
      ),
    };
  }

  private candleData(): ChartData<ChartType, unknown[], string> {
    const shown = this.visible();
    const colours = shown.map((p) => (isUp(p) ? UP : DOWN));
    return {
      labels: shown.map((p) => p.date),
      datasets: [
        ...AVERAGES.filter((s) => this.shownAverages().has(s.key)).map((s) => ({
          type: 'line' as const,
          label: s.label,
          data: shown.map((p) => p[s.key]),
          borderColor: s.colour,
          backgroundColor: s.colour,
          borderWidth: 1.5,
          pointRadius: 0,
          pointHoverRadius: 0,
          spanGaps: false,
          order: 0,
        })),
        {
          type: 'bar' as const,
          label: 'Body',
          data: shown.map((p) => (isCandle(p) ? [p.open, p.close] : null)),
          backgroundColor: colours,
          borderColor: colours,
          borderWidth: 0,
          minBarLength: 1,
          grouped: false,
          categoryPercentage: 0.8,
          barPercentage: 0.85,
          order: 1,
        },
        {
          type: 'bar' as const,
          label: 'Wick',
          data: shown.map((p) => (isCandle(p) ? [p.low, p.high] : null)),
          backgroundColor: colours,
          borderWidth: 0,
          grouped: false,
          barThickness: 1,
          order: 2,
        },
        {
          type: 'bar' as const,
          label: 'Volume',
          data: shown.map((p) => p.volume ?? null),
          backgroundColor: colours.map(faded),
          borderWidth: 0,
          grouped: false,
          categoryPercentage: 0.8,
          barPercentage: 0.85,
          yAxisID: 'volume',
          order: 3,
        },
      ],
    };
  }

  readonly options = computed<ChartConfiguration['options']>(() => {
    const muted = token('--ts-text-muted', '#9db5a7');
    const grid = token('--ts-divider', '#1a2c21');
    const font = {
      family:
        typeof document === 'undefined' ? undefined : getComputedStyle(document.body).fontFamily,
    };
    const shown = this.visible();
    const dateFormat = shown.length > 130 ? 'MMM yy' : 'MMM d';
    const labels = shown.map((p) => p.date);
    const candles = this.chartType() === 'bar';
    const maxVolume = Math.max(0, ...shown.map((p) => p.volume ?? 0));
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      interaction: { mode: 'index', intersect: false },
      onHover: (_, elements) => this.hoverIndex.set(elements[0]?.index ?? null),
      scales: {
        x: {
          grid: { display: candles, color: grid },
          border: { color: grid },
          ticks: {
            color: muted,
            font,
            maxRotation: 0,
            autoSkip: true,
            maxTicksLimit: 6,
            callback: (_, index) => formatDate(labels[index], dateFormat, 'en-US'),
          },
        },
        y: {
          position: candles ? 'right' : 'left',
          beginAtZero: false,
          grid: { color: grid },
          border: { display: false },
          afterDataLimits: candles
            ? (scale) => {
                scale.min -= (scale.max - scale.min) * 0.25;
              }
            : undefined,
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
        ...(candles && {
          volume: {
            display: false,
            min: 0,
            max: maxVolume * 5 || 1,
          },
        }),
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          enabled: !candles,
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
              ` ${item.raw === null ? '-' : money(item.raw as number)}  ${item.dataset.label}`,
          },
        },
      },
    };
  });

  protected readonly tableRows = computed(() => [...this.visible()].reverse());

  protected readonly compact = compact;

  protected setRange(key: RangeKey): void {
    this.range.set(key);
    this.zoom.set(null);
    this.activeIndex.set(null);
  }

  protected setMode(mode: ChartMode): void {
    this.mode.set(mode);
    this.activeIndex.set(null);
    this.hoverIndex.set(null);
  }

  protected toggleAverage(key: AverageKey): void {
    const shown = new Set(this.shownAverages());
    if (!shown.delete(key)) {
      shown.add(key);
    }
    this.shownAverages.set(shown);
  }

  protected readonly canPan = computed(() => this.visible().length < this.all().length);

  protected readonly canZoomIn = computed(() => this.visible().length > MIN_DAYS);

  protected readonly canZoomOut = computed(() => this.visible().length < this.all().length);

  protected zoomButton(factor: number): void {
    const anchor = this.window().end === this.all().length ? 1 : 0.5;
    this.zoomBy(factor, anchor);
  }

  protected readonly zoomStep = ZOOM_STEP;

  resetZoom(): void {
    this.zoom.set(null);
    this.activeIndex.set(null);
  }

  zoomBy(factor: number, anchor = 1): boolean {
    const total = this.all().length;
    const { start, end } = this.window();
    const days = end - start;
    const target = Math.round(Math.min(total, Math.max(MIN_DAYS, days / factor)));
    if (target === days || total === 0) {
      return false;
    }
    const pivot = start + days * anchor;
    let newStart = Math.round(pivot - target * anchor);
    newStart = Math.min(Math.max(0, newStart), total - target);
    this.setWindow(newStart, newStart + target);
    return true;
  }

  panBy(days: number): void {
    const total = this.all().length;
    const { start, end } = this.window();
    const width = end - start;
    const newStart = Math.min(Math.max(0, start + days), total - width);
    if (newStart !== start) {
      this.setWindow(newStart, newStart + width);
    }
  }

  private setWindow(start: number, end: number): void {
    const total = this.all().length;
    const days = RANGES.find((r) => r.key === this.range())!.days;
    const isRange = end === total && end - start === Math.min(days, total);
    this.zoom.set(isRange ? null : { start, end });
    this.activeIndex.set(null);
    this.hoverIndex.set(null);
  }

  private plotArea(): { left: number; width: number } {
    const rect = this.plot().nativeElement.getBoundingClientRect();
    const area = this.chart()?.chart?.chartArea;
    return area
      ? { left: rect.left + area.left, width: area.right - area.left }
      : { left: rect.left, width: rect.width };
  }

  protected onWheel(event: WheelEvent): void {
    const { left, width } = this.plotArea();
    const anchor = width > 0 ? Math.min(1, Math.max(0, (event.clientX - left) / width)) : 1;
    if (this.zoomBy(event.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP, anchor)) {
      event.preventDefault();
    }
  }

  protected onPointerDown(event: PointerEvent): void {
    if (event.button !== 0) {
      return;
    }
    const { start, end } = this.window();
    this.drag = { x: event.clientX, start, end };
    this.dragging.set(true);
    (event.target as Element).setPointerCapture?.(event.pointerId);
  }

  protected onPointerMove(event: PointerEvent): void {
    if (!this.drag) {
      return;
    }
    const { width } = this.plotArea();
    const days = this.drag.end - this.drag.start;
    if (width <= 0) {
      return;
    }
    const shift = Math.round(((this.drag.x - event.clientX) / width) * days);
    const total = this.all().length;
    const start = Math.min(Math.max(0, this.drag.start + shift), total - days);
    if (start !== this.window().start) {
      this.setWindow(start, start + days);
    }
  }

  protected onPointerUp(): void {
    this.drag = null;
    this.dragging.set(false);
  }

  get keyboardIndex(): number | null {
    return this.activeIndex();
  }

  protected onKeydown(event: KeyboardEvent): void {
    const zoomKeys: Record<string, () => void> = {
      '+': () => this.zoomBy(ZOOM_STEP),
      '=': () => this.zoomBy(ZOOM_STEP),
      '-': () => this.zoomBy(1 / ZOOM_STEP),
      '0': () => this.resetZoom(),
    };
    if (event.key in zoomKeys) {
      event.preventDefault();
      zoomKeys[event.key]();
      return;
    }
    if (event.shiftKey && (event.key === 'ArrowLeft' || event.key === 'ArrowRight')) {
      event.preventDefault();
      const step = Math.max(1, Math.round(this.visible().length / 10));
      this.panBy(event.key === 'ArrowLeft' ? -step : step);
      return;
    }

    const last = this.visible().length - 1;
    const current = this.activeIndex() ?? last;
    const steps: Record<string, number> = {
      ArrowLeft: current - 1,
      ArrowRight: current + 1,
      Home: 0,
      End: last,
    };
    if (!(event.key in steps) || last < 0) {
      return;
    }
    event.preventDefault();
    const index = Math.min(last, Math.max(0, steps[event.key]));
    this.activeIndex.set(index);
    this.showTooltip(index);
  }

  protected clearActive(): void {
    this.activeIndex.set(null);
    this.showTooltip(null);
  }

  private showTooltip(index: number | null): void {
    const chart = this.chart()?.chart;
    if (!chart) {
      return;
    }
    const active =
      index === null
        ? []
        : chart.data.datasets
            .map((_, datasetIndex) => ({ datasetIndex, index }))
            .filter(({ datasetIndex }) => chart.data.datasets[datasetIndex].data[index] != null);
    chart.setActiveElements(active);
    chart.tooltip?.setActiveElements(active, { x: 0, y: 0 });
    chart.update();
  }
}
