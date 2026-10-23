import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { App } from './app';
import { AssistantChat } from './features/assistant/assistant-chat';
import { Instruments } from './services/instruments';

@Component({ template: '' })
class Blank {}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        { provide: Instruments, useValue: { getNews: () => of([]) } },
        provideRouter([
          { path: 'dashboard', component: Blank },
          { path: 'login', component: Blank },
          { path: 'register', component: Blank },
        ]),
      ],
    }).compileComponents();
  });

  async function open(url: string) {
    const fixture = TestBed.createComponent(App);
    await TestBed.inject(Router).navigateByUrl(url);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('should create the app', () => {
    expect(TestBed.createComponent(App).componentInstance).toBeTruthy();
  });

  it('shows no shell, and so loads no news, until the first page has been decided', () => {
    const getNews = vi.spyOn(TestBed.inject(Instruments), 'getNews');
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('app-navigation-bar')).toBeNull();
    expect(page.querySelector('app-news-footer')).toBeNull();
    expect(getNews).not.toHaveBeenCalled();
  });

  it('shows the navigation bar and page padding on normal pages', async () => {
    const page = await open('/dashboard');

    expect(page.querySelector('app-navigation-bar')).not.toBeNull();
    expect(page.querySelector('app-news-footer')).not.toBeNull();
    expect(page.querySelector('main')?.classList).toContain('page-shell');
    expect(page.querySelector('main router-outlet')).not.toBeNull();
  });

  it.each(['/login', '/login?returnUrl=%2Forders', '/register'])(
    'shows %s full-screen, without the navigation bar',
    async (url) => {
      const page = await open(url);

      expect(page.querySelector('app-navigation-bar')).toBeNull();
      expect(page.querySelector('app-news-footer')).toBeNull();
      expect(page.querySelector('main')?.classList).not.toContain('page-shell');
      expect(page.querySelector('app-lily-pad-assistant')).toBeNull();
    },
  );

  it('shows the lily pad assistant on normal pages', async () => {
    const page = await open('/dashboard');

    expect(page.querySelector('app-lily-pad-assistant')).not.toBeNull();
  });

  it('makes room for the assistant instead of covering the page', async () => {
    const fixture = TestBed.createComponent(App);
    await TestBed.inject(Router).navigateByUrl('/dashboard');
    fixture.detectChanges();
    const host = fixture.nativeElement as HTMLElement;
    expect(host.classList).not.toContain('assistant-open');

    TestBed.inject(AssistantChat).toggle();
    fixture.detectChanges();

    expect(host.classList).toContain('assistant-open');
  });
});
