import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { App } from './app';

@Component({ template: '' })
class Blank {}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
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

  it('shows the navigation bar and page padding on normal pages', async () => {
    const page = await open('/dashboard');

    expect(page.querySelector('app-navigation-bar')).not.toBeNull();
    expect(page.querySelector('main')?.classList).toContain('page-shell');
    expect(page.querySelector('main router-outlet')).not.toBeNull();
  });

  it.each(['/login', '/login?returnUrl=%2Forders', '/register'])(
    'shows %s full-screen, without the navigation bar',
    async (url) => {
      const page = await open(url);

      expect(page.querySelector('app-navigation-bar')).toBeNull();
      expect(page.querySelector('main')?.classList).not.toContain('page-shell');
    },
  );
});
