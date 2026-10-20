import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';
import { NewsFooter } from './news-footer';
import { Instruments, NewsArticle } from '../../services/instruments';

describe('NewsFooter', () => {
  let fixture: ComponentFixture<NewsFooter>;

  function createNewsFooter(response: Observable<NewsArticle[]>) {
    TestBed.configureTestingModule({
      imports: [NewsFooter],
      providers: [{ provide: Instruments, useValue: { getNews: () => response } }],
    });
    fixture = TestBed.createComponent(NewsFooter);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  afterEach(() => TestBed.resetTestingModule());

  it('renders article headlines as safe new-tab links', () => {
    const page = createNewsFooter(
      of([
        {
          headline: 'Markets rally',
          articleUrl: 'https://example.com/article',
          publishedAt: '2026-10-14T08:00:00Z',
          updatedAt: '2026-10-14T08:00:00Z',
          symbols: ['AAPL', 'SPY'],
        },
      ]),
    );

    const link = page.querySelector<HTMLAnchorElement>('.news-footer__link');
    expect(link?.textContent).toContain('Markets rally');
    expect(link?.textContent).toContain('AAPL · SPY');
    expect(link?.target).toBe('_blank');
    expect(link?.rel).toContain('noopener');
    expect(link?.rel).toContain('noreferrer');
  });

  it('lists up to four tickers, then how many more, so one story cannot fill its card', () => {
    const page = createNewsFooter(
      of([
        {
          headline: 'Stocks moving premarket',
          articleUrl: 'https://example.com/movers',
          publishedAt: '2026-10-20T08:00:00Z',
          updatedAt: '2026-10-20T08:00:00Z',
          symbols: ['AIXI', 'AMRO', 'AMZN', 'AURE', 'BAOS', 'CLNN', 'GCDT'],
        },
      ]),
    );

    expect(page.querySelector('.news-footer__symbols')?.textContent?.trim()).toBe(
      'AIXI · AMRO · AMZN · AURE +3',
    );
  });

  it('shows a quiet empty state when no articles are returned', () => {
    const page = createNewsFooter(of([]));

    expect(page.querySelector('.news-footer__status')?.textContent).toContain(
      'No recent headlines available',
    );
  });

  it('shows a quiet error state when news cannot be loaded', () => {
    const page = createNewsFooter(throwError(() => new Error('offline')));

    expect(page.querySelector('.news-footer__status')?.textContent).toContain(
      'Headlines are temporarily unavailable',
    );
  });
});
