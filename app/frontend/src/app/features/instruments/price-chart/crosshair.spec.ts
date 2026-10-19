import { Chart } from 'chart.js';
import { DOWN, LastPrice, UP, crosshair, crosshairWith } from './price-chart';

interface Drawn {
  text: string;
  x: number;
  y: number;
  fillStyle: string;
}

function fakeContext() {
  const ctx = {
    fillStyle: '',
    strokeStyle: '',
    font: '',
    lineWidth: 0,
    textBaseline: '',
    texts: [] as Drawn[],
    lines: [] as { from: [number, number]; to: [number, number]; strokeStyle: string }[],
    dashes: [] as number[][],
    from: [0, 0] as [number, number],
    save: vi.fn(),
    restore: vi.fn(),
    beginPath: vi.fn(),
    stroke: vi.fn(),
    fillRect: vi.fn(),
    setLineDash(dash: number[]) {
      ctx.dashes.push(dash);
    },
    moveTo(x: number, y: number) {
      ctx.from = [x, y];
    },
    lineTo(x: number, y: number) {
      ctx.lines.push({ from: ctx.from, to: [x, y], strokeStyle: ctx.strokeStyle });
    },
    measureText: (text: string) => ({ width: text.length * 6 }),
    fillText(text: string, x: number, y: number) {
      ctx.texts.push({ text, x, y, fillStyle: ctx.fillStyle });
    },
  };
  return ctx;
}

// A 200 x 100 plot where the price is 100 at the top and 0 at the bottom
function fakeChart(active: { index: number; x: number }[] = []) {
  const ctx = fakeContext();
  const chart = {
    ctx,
    chartArea: { top: 0, bottom: 100, left: 0, right: 200 },
    canvas: document.createElement('canvas'),
    scales: {
      y: {
        getPixelForValue: (value: number) => 100 - value,
        getValueForPixel: (pixel: number) => 100 - pixel,
      },
    },
    tooltip: {
      getActiveElements: () => active.map(({ index, x }) => ({ index, element: { x } })),
    },
    data: { labels: ['2026-10-13', '2026-10-14'] },
  };
  return { chart: chart as unknown as Chart, ctx };
}

const draw = (plugin: ReturnType<typeof crosshairWith>, chart: Chart) =>
  plugin.afterDatasetsDraw!(chart, {} as never, {}, false as never);

const point = (plugin: ReturnType<typeof crosshairWith>, chart: Chart, y: number | null) => {
  const args = {
    event: { type: y === null ? 'mouseout' : 'mousemove', y: y ?? 0 },
    inChartArea: y !== null,
    changed: false,
  };
  plugin.afterEvent!(chart, args as never, {});
  return args.changed;
};

describe('crosshair plugin', () => {
  describe('the latest-price line', () => {
    const plugin = crosshairWith((): LastPrice => ({ price: 40, colour: UP }));

    it('draws a dashed line across the plot at the latest price, in its candle colour', () => {
      const { chart, ctx } = fakeChart();
      draw(plugin, chart);

      expect(ctx.lines).toContainEqual({ from: [0, 60], to: [200, 60], strokeStyle: UP });
      expect(ctx.dashes[0]).toEqual([2, 3]);
    });

    it('tags the price on the right edge, in dark text that reads on green and red', () => {
      const { chart, ctx } = fakeChart();
      draw(plugin, chart);

      const tag = ctx.texts.find((t) => t.text === '$40.00')!;
      expect(tag.y).toBe(60);
      expect(tag.x).toBeLessThan(200);
      expect(tag.fillStyle).toBe('#07110b');
    });

    it('leaves it out when the price is off the visible scale', () => {
      const { chart, ctx } = fakeChart();
      draw(
        crosshairWith(() => ({ price: 150, colour: DOWN })),
        chart,
      );

      expect(ctx.lines).toHaveLength(0);
      expect(ctx.texts).toHaveLength(0);
    });

    it('is not drawn by the plain crosshair', () => {
      const { chart, ctx } = fakeChart();
      draw(crosshair, chart);

      expect(ctx.lines).toHaveLength(0);
    });
  });

  describe('following the pointer', () => {
    it('redraws only when the pointer moves to a new height or leaves', () => {
      const { chart } = fakeChart();
      const plugin = crosshairWith(() => null);

      expect(point(plugin, chart, 30)).toBe(true);
      expect(point(plugin, chart, 30)).toBe(false);
      expect(point(plugin, chart, 45)).toBe(true);
      expect(point(plugin, chart, null)).toBe(true);
      expect(point(plugin, chart, null)).toBe(false);
    });

    it('draws nothing more until a day is under the pointer', () => {
      const { chart, ctx } = fakeChart([]);
      draw(crosshair, chart);

      expect(ctx.save).not.toHaveBeenCalled();
      expect(ctx.texts).toHaveLength(0);
    });

    it('draws a vertical line at the day and a level line with the price under the pointer', () => {
      const { chart, ctx } = fakeChart([{ index: 1, x: 120 }]);
      point(crosshair, chart, 25);
      draw(crosshair, chart);

      expect(ctx.lines).toContainEqual(expect.objectContaining({ from: [120, 0], to: [120, 100] }));
      expect(ctx.lines).toContainEqual(expect.objectContaining({ from: [0, 25], to: [200, 25] }));
      // 25px down a 0-100 scale is $75
      expect(ctx.texts.map((t) => t.text)).toContain('$75.00');
    });

    it('labels the day along the bottom, kept inside the plot at the edges', () => {
      const { chart, ctx } = fakeChart([{ index: 1, x: 199 }]);
      draw(crosshair, chart);

      const date = ctx.texts.find((t) => t.text === 'Wed 14 Oct 26')!;
      expect(date.y).toBe(91);
      const width = 'Wed 14 Oct 26'.length * 6 + 10;
      expect(date.x).toBe(200 - width + 5);
    });

    it('shows no price tag once the pointer has left, only the day', () => {
      const { chart, ctx } = fakeChart([{ index: 0, x: 50 }]);
      point(crosshair, chart, 25);
      point(crosshair, chart, null);
      draw(crosshair, chart);

      expect(ctx.texts.map((t) => t.text)).toEqual(['Tue 13 Oct 26']);
      expect(ctx.lines).toHaveLength(1);
    });
  });
});
