import { ComponentFixture, TestBed } from '@angular/core/testing';
import type { Holding } from '../portfolio.interfaces';
import { Allocation, toSegments } from './allocation';

const holding = (symbol: string, marketValue: number | null): Holding => ({
  symbol,
  name: `${symbol} Inc.`,
  quantity: 1,
  averageCost: 1,
  price: marketValue,
  priceSource: marketValue === null ? null : 'close',
  costBasis: 1,
  marketValue,
  unrealisedPnl: marketValue === null ? null : marketValue - 1,
  pnlPercent: null,
  dailyPnl: null,
  dailyPnlPercent: null,
  dailyCostBasis: null,
  weight: null,
});

describe('toSegments', () => {
  it('orders segments largest first and weights them by market value', () => {
    const segments = toSegments([holding('AAPL', 250), holding('TSLA', 750)]);

    expect(segments.map((segment) => segment.label)).toEqual(['TSLA', 'AAPL']);
    expect(segments.map((segment) => segment.weight)).toEqual([75, 25]);
  });

  it('leaves out unpriced holdings', () => {
    const segments = toSegments([holding('AAPL', 100), holding('AGG', null)]);

    expect(segments).toHaveLength(1);
    expect(segments[0]).toMatchObject({ label: 'AAPL', weight: 100 });
  });

  it('keeps a colour on its symbol when prices reorder the bar', () => {
    const before = toSegments([holding('AAPL', 300), holding('TSLA', 100)]);
    const after = toSegments([holding('AAPL', 100), holding('TSLA', 300)]);
    const colourOf = (segments: ReturnType<typeof toSegments>, symbol: string) =>
      segments.find((segment) => segment.label === symbol)!.colour;

    expect(colourOf(after, 'AAPL')).toBe(colourOf(before, 'AAPL'));
    expect(colourOf(after, 'TSLA')).toBe(colourOf(before, 'TSLA'));
  });

  it('folds everything past the seventh holding into Other', () => {
    const holdings = Array.from({ length: 10 }, (_, index) => holding(`S${index}`, 100 - index));
    const segments = toSegments(holdings);

    expect(segments).toHaveLength(8);
    expect(segments.at(-1)).toMatchObject({ label: 'Other', name: '3 more holdings', value: 93 + 92 + 91 });
  });

  it('returns nothing when no holding has a value', () => {
    expect(toSegments([holding('AGG', null)])).toEqual([]);
  });
});

describe('Allocation', () => {
  let fixture: ComponentFixture<Allocation>;
  let page: HTMLElement;

  async function render(holdings: Holding[]) {
    await TestBed.configureTestingModule({ imports: [Allocation] }).compileComponents();
    fixture = TestBed.createComponent(Allocation);
    fixture.componentRef.setInput('holdings', holdings);
    fixture.detectChanges();
    page = fixture.nativeElement;
  }

  it('draws a segment and a legend entry per holding', async () => {
    await render([holding('AAPL', 250), holding('TSLA', 750)]);

    expect(page.querySelectorAll('.allocation__segment')).toHaveLength(2);
    const legend = [...page.querySelectorAll('.allocation__item')].map((item) => item.textContent);
    expect(legend.map((text) => text!.trim())).toEqual(['TSLA', 'AAPL']);
    expect(page.querySelector('.allocation__bar')!.getAttribute('aria-label')).toBe(
      'Portfolio weighting: TSLA 75.0%, AAPL 25.0%',
    );
  });

  it('shows a tooltip for the hovered segment', async () => {
    await render([holding('AAPL', 250), holding('TSLA', 750)]);

    page.querySelector('.allocation__segment')!.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.detectChanges();

    expect(page.querySelector('[role="tooltip"]')!.textContent).toContain('TSLA');
    expect(page.querySelectorAll('.allocation__segment--dim')).toHaveLength(1);
  });

  it('notes how many holdings are unpriced', async () => {
    await render([holding('AAPL', 250), holding('AGG', null)]);

    expect(page.textContent).toContain('Excludes 1 unpriced');
  });
});
