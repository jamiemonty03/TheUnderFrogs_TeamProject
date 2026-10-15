import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { Instruments, LatestPrice, LivePriceFeed } from '../../../services/instruments';
import { LivePrices } from '../../../services/live-prices';
import { AuthLayout, AuthTab } from './auth-layout';

const close = (symbol: string, price: number, previousClose: number): LatestPrice => ({
  symbol,
  name: symbol,
  assetClass: 'Equity',
  price,
  previousClose,
  tradeDate: '2026-10-14T00:00:00',
});
const CLOSES = new Map(
  [close('SPY', 571.2, 568.86), close('AAPL', 189.84, 187.51), close('NVDA', 131.2, 133.97), close('KO', 90, 89)].map(
    (p) => [p.symbol, p],
  ),
);

@Component({
  imports: [AuthLayout],
  template: `
    <app-auth-layout
      heading="Join the pond"
      subheading="Start trading in under a minute."
      [activeTab]="tab()"
    >
      <form class="projected-form"></form>
    </app-auth-layout>
  `,
})
class Host {
  readonly tab = signal<AuthTab>('register');
}

describe('AuthLayout', () => {
  let fixture: ComponentFixture<Host>;
  let page: HTMLElement;

  let liveFeed: Subject<LivePriceFeed>;

  async function setUp(closes: Observable<Map<string, LatestPrice>> = of(CLOSES)) {
    TestBed.resetTestingModule();
    liveFeed = new Subject<LivePriceFeed>();
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [
        provideRouter([]),
        { provide: Instruments, useValue: { getLatestPrices: () => closes } },
        { provide: LivePrices, useValue: { watch: () => liveFeed.asObservable() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    page = fixture.nativeElement;
    fixture.detectChanges();
  }

  beforeEach(() => setUp());

  const tabs = () => Array.from(page.querySelectorAll<HTMLAnchorElement>('.auth__tab'));

  it('shows the brand, headline and tagline', () => {
    expect(page.querySelector('.auth__brand-name')?.textContent?.trim()).toBe('Toadstock');
    expect(page.querySelector('.auth__brand-badge')?.textContent?.trim()).toBe('NYSE: TOAD');
    const headline = page.querySelector('.auth__headline')!;
    expect(headline.textContent).toContain('Leap ahead');
    expect(headline.querySelector('br')).not.toBeNull();
    expect(headline.querySelector('.auth__headline-accent')?.textContent).toBe('market.');
  });

  it('shows the heading and subheading it is given, with the page form in the panel', () => {
    expect(page.querySelector('h1')?.textContent?.trim()).toBe('Join the pond');
    expect(page.querySelector('.auth__subtitle')?.textContent?.trim()).toBe(
      'Start trading in under a minute.',
    );
    expect(page.querySelector('.auth__content .projected-form')).not.toBeNull();
  });

  it('links the tabs to /login and /register', () => {
    expect(tabs().map((tab) => [tab.textContent?.trim(), tab.getAttribute('href')])).toEqual([
      ['Log in', '/login'],
      ['Create account', '/register'],
    ]);
  });

  it.each([
    ['register', 'Create account'],
    ['login', 'Log in'],
  ] as const)('highlights the %s tab and marks it as the current page', (tab, label) => {
    fixture.componentInstance.tab.set(tab);
    fixture.detectChanges();

    const active = tabs().filter((link) => link.classList.contains('auth__tab--active'));
    expect(active.map((link) => link.textContent?.trim())).toEqual([label]);
    expect(active[0].getAttribute('aria-current')).toBe('page');
    expect(tabs().filter((link) => link.hasAttribute('aria-current'))).toHaveLength(1);
  });

  it('hides the decorative ticker from screen readers', () => {
    expect(page.querySelector('.auth__ticker')?.getAttribute('aria-hidden')).toBe('true');
  });

  const quotes = () =>
    Array.from(page.querySelectorAll('.auth__quote')).map((quote) =>
      quote.textContent?.replace(/\s+/g, ' ').trim(),
    );

  it("shows the ticker symbols' real moves from the previous close, in ticker order, marking falls", () => {
    // KO isn't a ticker symbol; QQQ, AMZN and GOOGL have no prices here.
    expect(quotes()).toEqual(['SPY ▲ 0.41%', 'AAPL ▲ 1.24%', 'NVDA ▼ 2.07%']);
    expect(page.querySelectorAll('.auth__quote-change--down')).toHaveLength(1);
  });

  it('moves with live prices while the market is open', () => {
    liveFeed.next({
      prices: new Map([['NVDA', { symbol: 'NVDA', price: 137.99, bid: null, ask: null, quoteTime: '2026-10-15T14:30:00Z' }]]),
      stale: false,
    });
    fixture.detectChanges();

    expect(quotes()).toContain('NVDA ▲ 3.00%');
  });

  it('hides the ticker when prices cannot be loaded', async () => {
    await setUp(throwError(() => new Error('down')));

    expect(page.querySelector('.auth__ticker')).toBeNull();
  });
});
