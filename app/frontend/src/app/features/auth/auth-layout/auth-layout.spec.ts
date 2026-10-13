import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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

describe('AuthLayout', () => {
  let fixture: ComponentFixture<Host>;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(Host);
    page = fixture.nativeElement;
    fixture.detectChanges();
  });

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

  it('shows ticker moves with two decimals and marks falls', () => {
    const quotes = Array.from(page.querySelectorAll('.auth__quote')).map((quote) =>
      quote.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(quotes).toContain('TSLA ▼ 2.10%');
    expect(quotes).toContain('AAPL ▲ 1.24%');
    expect(page.querySelectorAll('.auth__quote-change--down')).toHaveLength(2);
  });
});
