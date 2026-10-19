import { CurrencyPipe, DatePipe, DecimalPipe, formatDate } from '@angular/common';
import { Component, computed, input, signal, viewChild } from '@angular/core';
import {
  CategoryScale,
  ChartConfiguration,
  ChartData,
  LineController,
  LineElement,
  LinearScale,
  Plugin,
  PointElement,
  Tooltip,
} from 'chart.js';
import { BaseChartDirective, provideCharts } from 'ng2-charts';
import { PricePoint } from '../../../services/interfaces/instruments.interfaces';

export type RangeKey = '1M' | '3M' | '6M' | '1Y';

export const RANGES: { key: RangeKey; days: number }[] = [
  { key: '1M', days: 21 },
  { key: '3M', days: 63 },
  { key: '6M', days: 126 },
  { key: '1Y', days: 252 },
];

export const SERIES = [
  { key: 'close', label: 'Close', colour: '#18a957' },
  { key: 'movingAvg20', label: '20-day average', colour: '#3a8ad6' },
  { key: 'movingAvg50', label: '50-day average', colour: '#c48320' },
] as const;

const money = (value: number) =>
  value.toLocaleString('en-US', { style: 'currency', currency: 'USD' });

export function token(name: string, fallback: string): string {
  if (typeof document === 'undefined') {
    return fallback;
  }
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

export const crosshair: Plugin<'line'> = {
  id: 'crosshair',
  afterDatasetsDraw(chart) {
    const active = chart.tooltip?.getActiveElements();
    if (!active?.length) {
      return;
    }
    const { ctx, chartArea } = chart;
    const x = active[0].element.x;
    ctx.save();
    ctx.strokeStyle = token('--ts-text-muted', '#9db5a7');
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(x, chartArea.top);
    ctx.lineTo(x, chartArea.bottom);
    ctx.stroke();
    ctx.restore();
  },
};

@Component({
  imports: [BaseChartDirective, CurrencyPipe, DatePipe, DecimalPipe],
  providers: [
    provideCharts({
      registerables: [
        LineController,
        LineElement,
        PointElement,
        LinearScale,
        CategoryScale,
        Tooltip,
      ],
    }),
  ],
  selector: 'app-price-chart',
  styleUrl: './price-chart.scss',
  templateUrl: './price-chart.html',
})
export class PriceChart {
  readonly points = input.required<PricePoint[]>();

  protected readonly ranges = RANGES;
  protected readonly series = SERIES;
  protected readonly plugins = [crosshair];

  protected readonly range = signal<RangeKey>('1Y');
  private readonly activeIndex = signal<number | null>(null);
  private readonly chart = viewChild(BaseChartDirective);

  protected readonly visible = computed(() => {
    const days = RANGES.find((r) => r.key === this.range())!.days;
    return this.points().slice(-days);
  });

  protected readonly change = computed(() => {
    const shown = this.visible();
    if (shown.length < 2) {
      return null;
    }
    const first = shown[0].close;
    const amount = shown[shown.length - 1].close - first;
    return { amount, percent: first === 0 ? 0 : (amount / first) * 100 };
  });

  readonly data = computed<ChartData<'line', (number | null)[], string>>(() => {
    const shown = this.visible();
    const surface = token('--ts-bg-raised', '#091510');
    return {
      labels: shown.map((p) => p.date),
      datasets: SERIES.map((s) => ({
        label: s.label,
        data: shown.map((p) => p[s.key]),
        borderColor: s.colour,
        backgroundColor: s.colour,
        borderWidth: 2,
        borderJoinStyle: 'round' as const,
        borderCapStyle: 'round' as const,
        pointRadius: 0,
        pointHoverRadius: 4,
        pointHoverBorderWidth: 2,
        pointHoverBorderColor: surface,
        spanGaps: false,
      })),
    };
  });

  readonly options = computed<ChartConfiguration<'line'>['options']>(() => {
    const muted = token('--ts-text-muted', '#9db5a7');
    const grid = token('--ts-divider', '#1a2c21');
    const font = {
      family:
        typeof document === 'undefined' ? undefined : getComputedStyle(document.body).fontFamily,
    };
    const dateFormat = this.range() === '1Y' ? 'MMM yy' : 'MMM d';
    const labels = this.visible().map((p) => p.date);
    return {
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      // The tooltip finds the nearest day and lists every series; the pointer needn't touch a line
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
              ` ${item.raw === null ? '–' : money(item.raw as number)}  ${item.dataset.label}`,
          },
        },
      },
    };
  });

  protected readonly tableRows = computed(() => [...this.visible()].reverse());

  protected setRange(key: RangeKey): void {
    this.range.set(key);
    this.activeIndex.set(null);
  }

  /** The day the keyboard has stepped to, or null. */
  get keyboardIndex(): number | null {
    return this.activeIndex();
  }

  // The arrow keys, Home and End step the tooltip through the days, for keyboard users
  protected onKeydown(event: KeyboardEvent): void {
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
        : SERIES.map((_, datasetIndex) => ({ datasetIndex, index })).filter(
            ({ datasetIndex }) => chart.data.datasets[datasetIndex].data[index] !== null,
          );
    chart.setActiveElements(active);
    chart.tooltip?.setActiveElements(active, { x: 0, y: 0 });
    chart.update();
  }
}
