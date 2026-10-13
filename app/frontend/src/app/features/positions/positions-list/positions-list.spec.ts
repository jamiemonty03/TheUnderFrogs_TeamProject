import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { Portfolio, Portfolios } from '../../../services/portfolios';
import { Position, Positions } from '../../../services/positions';
import { PositionsList } from './positions-list';

const MAIN: Portfolio = {
  portfolioId: 'PF0011',
  accountId: 'ACC0011',
  name: 'Main',
  risk: 'MEDIUM',
  benchmark: null,
  defaultPortfolio: true,
};
const GROWTH: Portfolio = {
  portfolioId: 'PF0012',
  accountId: 'ACC0011',
  name: 'Growth',
  risk: 'HIGH',
  benchmark: 'Nasdaq-100',
  defaultPortfolio: false,
};
const position = (portfolioId: string, symbol: string, quantity: number, averageCost: number): Position => ({
  accountId: 'ACC0011',
  portfolioId,
  symbol,
  quantity,
  averageCost,
});
const ALL = [position('PF0011', 'AAPL', 10, 100), position('PF0012', 'NVDA', 2, 500), position('PF0012', 'AAPL', 4, 150)];
const IN_GROWTH = ALL.filter((p) => p.portfolioId === 'PF0012');

describe('PositionsList (Portfolios page)', () => {
  let fixture: ComponentFixture<PositionsList>;
  let portfolios: {
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    getPortfolios: ReturnType<typeof vi.fn>;
    createPortfolio: ReturnType<typeof vi.fn>;
    updatePortfolio: ReturnType<typeof vi.fn>;
    deletePortfolio: ReturnType<typeof vi.fn>;
  };
  let positions: {
    loading: ReturnType<typeof signal<boolean>>;
    error: ReturnType<typeof signal<string | null>>;
    getPositions: ReturnType<typeof vi.fn>;
  };

  const el = (testId: string): HTMLElement | null =>
    fixture.nativeElement.querySelector(`[data-testid="${testId}"]`);
  const text = (testId: string): string => el(testId)?.textContent?.trim() ?? '';
  const click = async (testId: string) => {
    el(testId)!.click();
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const rowCount = () => fixture.nativeElement.querySelectorAll('tr.mat-mdc-row').length;
  const headers = () =>
    Array.from(fixture.nativeElement.querySelectorAll('th') as NodeListOf<HTMLElement>).map((th) => th.textContent?.trim());

  beforeEach(async () => {
    portfolios = {
      loading: signal(false),
      error: signal<string | null>(null),
      getPortfolios: vi.fn(() => of([MAIN, GROWTH])),
      createPortfolio: vi.fn(() => of({ ...GROWTH, portfolioId: 'PF0013', name: 'Income', risk: 'LOW' })),
      updatePortfolio: vi.fn(() => of({ ...GROWTH, name: 'Long-term growth' })),
      deletePortfolio: vi.fn(() => of(undefined)),
    };
    positions = {
      loading: signal(false),
      error: signal<string | null>(null),
      getPositions: vi.fn((portfolioId?: string) => of(portfolioId === 'PF0012' ? IN_GROWTH : ALL)),
    };

    await TestBed.configureTestingModule({
      imports: [PositionsList],
      providers: [
        provideRouter([]),
        { provide: Portfolios, useValue: portfolios },
        { provide: Positions, useValue: positions },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PositionsList);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('opens on all holdings, across every portfolio, with a portfolio column', () => {
    expect(positions.getPositions).toHaveBeenCalledWith(undefined);
    expect(text('portfolios-title')).toBe('All holdings');
    expect(rowCount()).toBe(3);
    expect(headers()).toContain('Portfolio');
    expect(text('holding-count')).toBe('3');
  });

  it('adds up the total cost of the holdings shown', () => {
    // 10 x 100 + 2 x 500 + 4 x 150
    expect(text('total-cost')).toBe('$2,600.00');
  });

  it('lists every portfolio in the sidebar, marking Main', () => {
    expect(text('portfolio-PF0011')).toContain('Main');
    expect(text('portfolio-PF0012')).toContain('Growth');
    expect(text('portfolio-PF0012')).toContain('High risk');
  });

  it("selecting a portfolio shows only that portfolio's holdings", async () => {
    await click('portfolio-PF0012');

    expect(positions.getPositions).toHaveBeenLastCalledWith('PF0012');
    expect(text('portfolios-title')).toBe('Growth');
    expect(rowCount()).toBe(2);
    expect(headers()).not.toContain('Portfolio');
    expect(el('portfolio-PF0012')!.getAttribute('aria-pressed')).toBe('true');
  });

  it('shows an empty state when a portfolio has no holdings', async () => {
    positions.getPositions.mockReturnValue(of([]));

    await click('portfolio-PF0011');

    expect(el('holdings-table')).toBeNull();
    expect(text('holdings-empty')).toContain('No holdings in Main yet.');
  });

  it('creates a portfolio with a trimmed name and then shows it', async () => {
    await click('new-portfolio');
    const name = el('portfolio-name') as HTMLInputElement;
    name.value = '  Income  ';
    name.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    await click('save-portfolio');

    expect(portfolios.createPortfolio).toHaveBeenCalledWith({ name: 'Income', risk: 'MEDIUM', benchmark: '' });
    expect(positions.getPositions).toHaveBeenLastCalledWith('PF0013');
    expect(el('portfolio-form')).toBeNull();
  });

  it('does not create a portfolio without a name', async () => {
    await click('new-portfolio');

    await click('save-portfolio');

    expect(portfolios.createPortfolio).not.toHaveBeenCalled();
    expect(el('portfolio-form')).not.toBeNull();
  });

  it('edits the selected portfolio, starting from its current details', async () => {
    await click('portfolio-PF0012');
    await click('edit-portfolio');
    const name = el('portfolio-name') as HTMLInputElement;
    expect(name.value).toBe('Growth');

    name.value = 'Long-term growth';
    name.dispatchEvent(new Event('input'));
    await click('save-portfolio');

    expect(portfolios.updatePortfolio).toHaveBeenCalledWith('PF0012', {
      name: 'Long-term growth',
      risk: 'HIGH',
      benchmark: 'Nasdaq-100',
    });
  });

  it('offers no delete for Main', async () => {
    await click('portfolio-PF0011');

    expect(el('edit-portfolio')).not.toBeNull();
    expect(el('delete-portfolio')).toBeNull();
  });

  it('asks before deleting, says the holdings move to Main, then goes back to all holdings', async () => {
    await click('portfolio-PF0012');
    await click('delete-portfolio');

    expect(fixture.nativeElement.textContent).toContain('Its 2 holdings move into Main.');
    expect(portfolios.deletePortfolio).not.toHaveBeenCalled();

    await click('confirm-delete');

    expect(portfolios.deletePortfolio).toHaveBeenCalledWith('PF0012');
    expect(text('portfolios-title')).toBe('All holdings');
    expect(positions.getPositions).toHaveBeenLastCalledWith(undefined);
  });

  it("shows a service's error message", async () => {
    portfolios.error.set("There's already a portfolio called Growth");
    fixture.detectChanges();

    expect(text('portfolios-error')).toBe("There's already a portfolio called Growth");
  });
});
