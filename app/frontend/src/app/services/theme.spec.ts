import { TestBed } from '@angular/core/testing';
import { THEME_KEY, Theme, applyInitialTheme } from './theme';

const htmlTheme = () => document.documentElement.dataset['theme'];

describe('Theme', () => {
  const create = () => TestBed.inject(Theme);

  beforeEach(() => {
    localStorage.clear();
    delete document.documentElement.dataset['theme'];
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('starts dark', () => {
    expect(create().theme()).toBe('dark');
    expect(htmlTheme()).toBe('dark');
  });

  it('starts dark even when the device prefers light, until the user switches', () => {
    vi.stubGlobal(
      'matchMedia',
      vi.fn(() => ({ matches: true, addEventListener: vi.fn() })),
    );

    expect(create().theme()).toBe('dark');
  });

  it('toggles between light and dark, and remembers the choice on this browser', () => {
    const theme = create();

    theme.toggle();
    expect(theme.theme()).toBe('light');
    expect(htmlTheme()).toBe('light');
    expect(localStorage.getItem(THEME_KEY)).toBe('light');

    theme.toggle();
    expect(theme.theme()).toBe('dark');
    expect(localStorage.getItem(THEME_KEY)).toBe('dark');
  });

  it('starts in the theme the user chose last time', () => {
    localStorage.setItem(THEME_KEY, 'light');

    expect(create().theme()).toBe('light');
    expect(htmlTheme()).toBe('light');
  });

  it('ignores a remembered value it does not recognise', () => {
    localStorage.setItem(THEME_KEY, 'purple');

    expect(create().theme()).toBe('dark');
  });

  it('still switches for this visit when the browser blocks storage', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });
    const theme = create();

    theme.toggle();

    expect(theme.theme()).toBe('light');
    expect(htmlTheme()).toBe('light');
  });

  it('can set the theme before Angular starts, so the page never flashes the wrong one', () => {
    applyInitialTheme();
    expect(htmlTheme()).toBe('dark');

    localStorage.setItem(THEME_KEY, 'light');
    applyInitialTheme();
    expect(htmlTheme()).toBe('light');
  });
});
