import { Component, signal, viewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PricePoint } from '../../../services/interfaces/instruments.interfaces';
import { PriceChart } from './price-chart';

const POINTS: PricePoint[] = Array.from({ length: 300 }, (_, i) => ({
  date: new Date(Date.UTC(2025, 0, 1) + i * 86_400_000).toISOString().slice(0, 10),
  close: 100 + i,
  movingAvg20: i >= 19 ? 90 + i : null,
  movingAvg50: i >= 49 ? 75 + i : null,
}));

@Component({
  imports: [PriceChart],
  template: `<app-price-chart [points]="points()" />`,
})
class Host {
  readonly points = signal(POINTS);
  readonly chart = viewChild.required(PriceChart);
}

describe('PriceChart', () => {
  let fixture: ComponentFixture<Host>;
  const chart = () => fixture.componentInstance.chart();
  const el = (testId: string) =>
    fixture.nativeElement.querySelector(`[data-testid="${testId}"]`) as HTMLElement | null;
  const text = (testId: string) => el(testId)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  const tableRows = () =>
    fixture.nativeElement.querySelectorAll('[data-testid="chart-table"] tbody tr').length;
  const press = (key: string) => {
    el('price-chart')!.dispatchEvent(new KeyboardEvent('keydown', { key }));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Host] }).compileComponents();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
  });

  it('draws the close and both averages as Chart.js line series', () => {
    const { labels, datasets } = chart().data();

    expect(datasets.map((d) => d.label)).toEqual(['Close', '20-day average', '50-day average']);
    expect(datasets.map((d) => d.borderColor)).toEqual(['#18a957', '#3a8ad6', '#c48320']);
    expect(labels).toHaveLength(252);
    expect(datasets[0].data.at(-1)).toBe(399);
    expect(el('price-chart')!.querySelector('canvas')).not.toBeNull();
  });

  it('shows the last year by default, with the change over it', () => {
    expect(tableRows()).toBe(252);
    // 251 days of +$1 from $148
    expect(text('chart-change')).toBe('+$251.00 (+169.59%) over 1Y');
    expect(el('range-1Y')!.getAttribute('aria-pressed')).toBe('true');
  });

  it('switches range', () => {
    el('range-1M')!.click();
    fixture.detectChanges();

    expect(chart().data().labels).toHaveLength(21);
    expect(tableRows()).toBe(21);
    expect(text('chart-change')).toBe('+$20.00 (+5.28%) over 1M');
    expect(el('range-1M')!.getAttribute('aria-pressed')).toBe('true');
  });

  it('leaves a gap where an average has no value yet, rather than drawing to zero', () => {
    fixture.componentInstance.points.set(POINTS.slice(0, 60));
    fixture.detectChanges();

    const ma50 = chart().data().datasets[2];
    expect(ma50.data.slice(0, 49).every((v) => v === null)).toBe(true);
    expect(ma50.data[49]).toBe(124);
    expect(ma50.spanGaps).toBe(false);
  });

  it('labels the axes and tooltip as money and dates', () => {
    const options = chart().options()!;
    const yTick = options.scales!['y']!.ticks!.callback as (v: number) => string;
    const tooltip = options.plugins!.tooltip!.callbacks!;

    expect(yTick.call(null as never, 250)).toBe('$250');
    expect(yTick.call(null as never, 2.5)).toBe('$2.50');
    expect(
      (tooltip.title as (items: { label: string }[]) => string)([{ label: '2026-10-14' }]),
    ).toBe('Wed 14 Oct 2026');
    expect(
      (tooltip.label as (item: { raw: number | null; dataset: { label: string } }) => string)({
        raw: null,
        dataset: { label: '50-day average' },
      }),
    ).toBe(' –  50-day average');
  });

  it('a legend names the three series', () => {
    const items = Array.from(el('chart-legend')!.querySelectorAll('li')).map((li) =>
      li.textContent?.trim(),
    );
    expect(items).toEqual(['Close', '20-day average', '50-day average']);
  });

  it('the arrow keys, Home and End step through the days', () => {
    press('End');
    expect(chart().keyboardIndex).toBe(251);

    press('ArrowLeft');
    expect(chart().keyboardIndex).toBe(250);

    press('Home');
    expect(chart().keyboardIndex).toBe(0);

    press('ArrowLeft');
    expect(chart().keyboardIndex).toBe(0);
  });
});
