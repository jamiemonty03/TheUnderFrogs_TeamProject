import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { Instruments, LatestPrice, LivePriceFeed } from '../../../services/instruments';
import { LivePrices } from '../../../services/live-prices';

import { AuthLayout, AuthTab } from './auth-layout';

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

// Each moves by a round percentage from a previous close of 100
const quote = (symbol: string, price: number | null): [string, LatestPrice] => [
  symbol,
  {
    symbol,
    name: symbol,
    assetClass: 'Equity',
    price,
    previousClose: 100,
    tradeDate: '2026-10-14',
  },
];
const PRICES = new Map([
  quote('AAPL', 101.24),
  quote('MSFT', 100.88),
  quote('TSLA', 97.9),
  quote('SPY', 100.41),
  quote('NVDA', 103.02),
  quote('BND', 99.88),
]);

describe('AuthLayout', () => {
  let fixture: ComponentFixture<Host>;
  let page: HTMLElement;
  let prices: Observable<Map<string, LatestPrice>>;

  let liveFeed: Subject<LivePriceFeed>;

  const create = async () => {
    liveFeed = new Subject<LivePriceFeed>();
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [
        provideRouter([]),
        { provide: Instruments, useValue: { getLatestPrices: () => prices } },
        { provide: LivePrices, useValue: { watch: () => liveFeed.asObservable() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    page = fixture.nativeElement;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    prices = of(PRICES);
    await create();
  });

  const tickerSymbols = () =>
    Array.from(page.querySelectorAll('.auth__quote')).map(
      (q) => q.textContent?.trim().split(/\s/)[0],
    );

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

  it('shows each move since the previous close with two decimals, marking falls', () => {
    expect(quotes()).toEqual([
      'AAPL ▲ 1.24%',
      'MSFT ▲ 0.88%',
      'TSLA ▼ 2.10%',
      'SPY ▲ 0.41%',
      'NVDA ▲ 3.02%',
      'BND ▼ 0.12%',
    ]);
    expect(page.querySelectorAll('.auth__quote-change--down')).toHaveLength(2);
  });

  it('moves with live prices while the market is open', () => {
    liveFeed.next({
      prices: new Map([['NVDA', { symbol: 'NVDA', price: 95, bid: null, ask: null, quoteTime: '2026-10-15T14:30:00Z' }]]),
      stale: false,
    });
    fixture.detectChanges();

    expect(quotes()).toContain('NVDA ▼ 5.00%');
  });

  it('flashes a ticker change green when its live price rises and red when it falls', () => {
    const tick = (price: number) => {
      liveFeed.next({
        prices: new Map([['NVDA', { symbol: 'NVDA', price, bid: null, ask: null, quoteTime: '2026-10-15T14:30:00Z' }]]),
        stale: false,
      });
      fixture.detectChanges();
    };
    const nvdaChange = () =>
      Array.from(page.querySelectorAll('.auth__quote'))
        .find((li) => li.textContent?.includes('NVDA'))!
        .querySelector('.auth__quote-change')!;

    tick(104); // up from the 103.02 close
    expect(nvdaChange().classList).toContain('price-flash--up');

    tick(99);
    expect(nvdaChange().classList).toContain('price-flash--down');
    expect(nvdaChange().classList).not.toContain('price-flash--up');
    expect(nvdaChange().classList).toContain('auth__quote-change--down'); // the up/down colour survives the flash
  });

  it('leaves out a symbol that has no price', async () => {
    TestBed.resetTestingModule();
    prices = of(new Map([...PRICES, quote('NVDA', null)]));
    await create();

    expect(tickerSymbols()).toEqual(['AAPL', 'MSFT', 'TSLA', 'SPY', 'BND']);
  });

  it('hides the ticker if prices cannot be loaded', async () => {
    TestBed.resetTestingModule();
    prices = throwError(() => new Error('down'));
    await create();

    expect(page.querySelector('.auth__ticker')).toBeNull();
  });
});
