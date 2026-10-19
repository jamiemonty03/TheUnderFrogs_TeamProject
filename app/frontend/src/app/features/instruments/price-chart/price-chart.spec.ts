import { Component, signal, viewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PricePoint } from '../../../services/interfaces/instruments.interfaces';
import { DOWN, MIN_DAYS, PriceChart, UP } from './price-chart';

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

  const showAverages = () => {
    el('toggle-movingAvg20')!.click();
    el('toggle-movingAvg50')!.click();
    fixture.detectChanges();
  };

  it('draws only the close until the averages are switched on', () => {
    const { datasets } = chart().data();

    expect(datasets.map((d) => d.label)).toEqual(['Close']);
    expect(el('toggle-movingAvg20')!.getAttribute('aria-pressed')).toBe('false');
  });

  it('switches each average on and off', () => {
    el('toggle-movingAvg50')!.click();
    fixture.detectChanges();
    expect(
      chart()
        .data()
        .datasets.map((d) => d.label),
    ).toEqual(['Close', '50-day average']);
    expect(el('toggle-movingAvg50')!.getAttribute('aria-pressed')).toBe('true');

    el('toggle-movingAvg50')!.click();
    fixture.detectChanges();
    expect(
      chart()
        .data()
        .datasets.map((d) => d.label),
    ).toEqual(['Close']);
  });

  it('draws the close and both averages as Chart.js line series', () => {
    showAverages();
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
    showAverages();

    const ma50 = chart().data().datasets[2];
    expect(ma50.data.slice(0, 49).every((v) => v === null)).toBe(true);
    expect(ma50.data[49]).toBe(124);
    expect((ma50 as { spanGaps?: boolean }).spanGaps).toBe(false);
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
    ).toBe(' -  50-day average');
  });

  it('a legend names the three series', () => {
    const items = Array.from(el('chart-legend')!.querySelectorAll('li')).map((li) =>
      li.textContent?.trim(),
    );
    expect(items).toEqual(['Close', '20-day average', '50-day average']);
  });

  describe('candles', () => {
    const OHLC: PricePoint[] = POINTS.map((p, i) => ({
      ...p,
      open: i % 2 ? p.close + 2 : p.close - 2,
      high: p.close + 5,
      low: p.close - 5,
      volume: 1_000_000 + i,
    }));

    const showCandles = () => {
      fixture.componentInstance.points.set(OHLC);
      fixture.detectChanges();
      el('mode-candles')!.click();
      fixture.detectChanges();
    };

    it('starts on the line chart', () => {
      expect(el('mode-line')!.getAttribute('aria-pressed')).toBe('true');
      expect(el('chart-ohlc')).toBeNull();
    });

    it("can't switch to candles without open, high and low prices", () => {
      expect((el('mode-candles') as HTMLButtonElement).disabled).toBe(true);
    });

    it('draws each day as a body from open to close and a wick from low to high', () => {
      showCandles();
      const { datasets } = chart().data();
      const byLabel = (label: string) => datasets.find((d) => d.label === label)!;

      expect(chart().mode()).toBe('candles');
      expect(datasets.map((d) => d.label)).toEqual(['Body', 'Wick', 'Volume']);
      expect(byLabel('Body').data.at(-1)).toEqual([401, 399]);
      expect(byLabel('Wick').data.at(-1)).toEqual([394, 404]);
      expect(byLabel('Volume').data.at(-1)).toBe(1_000_299);
    });

    it('draws the averages over the candles once switched on', () => {
      showCandles();
      showAverages();

      expect(
        chart()
          .data()
          .datasets.map((d) => d.label),
      ).toEqual(['20-day average', '50-day average', 'Body', 'Wick', 'Volume']);
    });

    it('colours rising days green and falling days red', () => {
      showCandles();
      const body = chart()
        .data()
        .datasets.find((d) => d.label === 'Body')!;
      const colours = body.backgroundColor as string[];

      expect(colours.at(-2)).toBe(UP);
      expect(colours.at(-1)).toBe(DOWN);
      expect([UP, DOWN]).toEqual(['#22c55e', '#ef4444']);
    });

    it('marks the latest price with a line in its candle colour', () => {
      showCandles();
      expect(chart().lastPrice()).toEqual({ price: 399, colour: DOWN });
    });

    it('reads out the latest open, high, low, close and volume', () => {
      showCandles();

      const readout = text('chart-ohlc');
      expect(readout).toContain('Mon 27 Oct 2025');
      expect(readout).toContain('O401.00H404.00L394.00C399.00');
      expect(readout).toContain('+1.00 (+0.25%)');
      expect(readout).toContain('Vol1M');
    });

    it('the readout follows the keyboard', () => {
      showCandles();
      press('Home');

      expect(text('chart-ohlc')).toContain('Tue 18 Feb 2025O146.00H153.00L143.00C148.00');
    });

    it('adds the open, high, low and volume to the table', () => {
      showCandles();
      const headers = Array.from(
        fixture.nativeElement.querySelectorAll('[data-testid="chart-table"] th'),
      ).map((th) => (th as HTMLElement).textContent?.trim());

      expect(headers).toEqual([
        'Date',
        'Open',
        'High',
        'Low',
        'Close',
        '20-day average',
        '50-day average',
        'Volume',
      ]);
    });

    it('switches back to the line', () => {
      showCandles();
      el('mode-line')!.click();
      fixture.detectChanges();

      expect(chart().data().datasets).toHaveLength(1);
      expect(el('chart-ohlc')).toBeNull();
    });
  });

  describe('zoom and pan', () => {
    const labels = () => chart().data().labels as string[];
    const wheel = (deltaY: number) => {
      el('price-chart')!.dispatchEvent(new WheelEvent('wheel', { deltaY, cancelable: true }));
      fixture.detectChanges();
    };
    const keyDown = (key: string, shiftKey = false) => {
      el('price-chart')!.dispatchEvent(new KeyboardEvent('keydown', { key, shiftKey }));
      fixture.detectChanges();
    };

    it('scrolling up zooms in towards today', () => {
      wheel(-100);

      expect(labels()).toHaveLength(202);
      expect(labels().at(-1)).toBe(POINTS.at(-1)!.date);
      expect(chart().zoomed()).toBe(true);
      expect(text('chart-change')).toContain('over 202 days');
      expect(el('range-1Y')!.getAttribute('aria-pressed')).toBe('false');
    });

    it('lets the page scroll when the chart can zoom out no further', () => {
      const scroll = (deltaY: number) => {
        const event = new WheelEvent('wheel', { deltaY, cancelable: true });
        el('price-chart')!.dispatchEvent(event);
        fixture.detectChanges();
        return event.defaultPrevented;
      };

      el('range-1M')!.click();
      fixture.detectChanges();
      expect(scroll(100)).toBe(true);

      while (scroll(100));
      expect(labels()).toHaveLength(300);
      expect(scroll(100)).toBe(false);
      expect(scroll(-100)).toBe(true);
    });

    it('stops zooming in at ten days, and out at all the history', () => {
      for (let i = 0; i < 30; i++) wheel(-100);
      expect(labels()).toHaveLength(MIN_DAYS);

      for (let i = 0; i < 30; i++) wheel(100);
      expect(labels()).toHaveLength(300);
    });

    it('zooms around the point under the mouse', () => {
      chart().zoomBy(2, 0.5);
      fixture.detectChanges();

      // 252 days from day 48 halve to 126, centred on day 174
      expect(chart().window()).toEqual({ start: 111, end: 237 });
    });

    it('Shift with the arrows pans, and stops at the ends', () => {
      keyDown('+');
      keyDown('ArrowLeft', true);
      const { start, end } = chart().window();
      expect(end).toBeLessThan(300);

      for (let i = 0; i < 50; i++) keyDown('ArrowLeft', true);
      expect(chart().window()).toEqual({ start: 0, end: end - start });
    });

    it('dragging right brings older days into view', () => {
      el('price-chart')!.getBoundingClientRect = () => ({ left: 0, width: 504 }) as DOMRect;
      const pointer = (type: string, clientX: number) =>
        el('price-chart')!.dispatchEvent(new MouseEvent(type, { clientX, button: 0 }));

      pointer('pointerdown', 300);
      pointer('pointermove', 320);
      pointer('pointerup', 320);
      fixture.detectChanges();

      expect(chart().window()).toEqual({ start: 38, end: 290 });

      pointer('pointerdown', 0);
      pointer('pointermove', 500);
      pointer('pointerup', 500);
      fixture.detectChanges();

      expect(chart().window()).toEqual({ start: 0, end: 252 });
    });

    it('the + and - buttons zoom, and stop at the limits', () => {
      expect((el('zoom-out') as HTMLButtonElement).disabled).toBe(false);
      el('zoom-in')!.click();
      fixture.detectChanges();
      expect(labels()).toHaveLength(202);
      expect(labels().at(-1)).toBe(POINTS.at(-1)!.date);

      for (let i = 0; i < 10; i++) {
        el('zoom-out')!.click();
        fixture.detectChanges();
      }
      expect(labels()).toHaveLength(300);
      expect((el('zoom-out') as HTMLButtonElement).disabled).toBe(true);
    });

    it('the hint says to zoom in first when everything is in view', () => {
      for (let i = 0; i < 5; i++) wheel(100);
      expect(text('chart-hint')).toContain('zoom in, then drag');

      wheel(-100);
      expect(text('chart-hint')).toContain('Drag to move through time');
    });

    it('Reset zoom, double-click, 0 and the range buttons go back to the range', () => {
      wheel(-100);
      el('zoom-reset')!.click();
      fixture.detectChanges();
      expect(chart().zoomed()).toBe(false);
      expect(el('zoom-reset')).toBeNull();

      wheel(-100);
      el('price-chart')!.dispatchEvent(new MouseEvent('dblclick'));
      fixture.detectChanges();
      expect(chart().zoomed()).toBe(false);

      wheel(-100);
      keyDown('0');
      expect(chart().zoomed()).toBe(false);

      wheel(-100);
      el('range-3M')!.click();
      fixture.detectChanges();
      expect(chart().zoomed()).toBe(false);
      expect(labels()).toHaveLength(63);
    });
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
