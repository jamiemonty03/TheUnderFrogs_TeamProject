import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { FLASH_MS, PriceFlash } from './price-flash';

@Component({
  imports: [PriceFlash],
  template: `<span class="price-flash" [appPriceFlash]="price()">{{ price() }}</span>`,
})
class Host {
  readonly price = signal<number | null>(100);
}

describe('PriceFlash', () => {
  let fixture: ComponentFixture<Host>;
  let span: HTMLElement;

  const set = (price: number | null) => {
    fixture.componentInstance.price.set(price);
    fixture.detectChanges();
  };

  beforeEach(() => {
    vi.useFakeTimers();
    fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    span = fixture.nativeElement.querySelector('span');
  });

  afterEach(() => vi.useRealTimers());

  it('does not flash on the first price', () => {
    expect(span.className).toBe('price-flash');
  });

  it('flashes up when the price rises and down when it falls', () => {
    set(101);
    expect(span.classList).toContain('price-flash--up');

    set(99);
    expect(span.classList).toContain('price-flash--down');
    expect(span.classList).not.toContain('price-flash--up');
  });

  it('clears the flash after FLASH_MS', () => {
    set(101);

    vi.advanceTimersByTime(FLASH_MS - 1);
    expect(span.classList).toContain('price-flash--up');

    vi.advanceTimersByTime(1);
    expect(span.classList).not.toContain('price-flash--up');
  });

  it('keeps flashing for the full time when the price moves again mid-flash', () => {
    set(101);
    vi.advanceTimersByTime(FLASH_MS - 500);
    set(102);

    vi.advanceTimersByTime(FLASH_MS - 1);
    expect(span.classList).toContain('price-flash--up');
  });

  it('ignores an unchanged price and nulls, comparing against the last real price', () => {
    set(100);
    expect(span.classList).not.toContain('price-flash--up');

    set(null);
    expect(span.className).toBe('price-flash');

    set(98);
    expect(span.classList).toContain('price-flash--down');
  });

  it('cancels a pending clear when destroyed', () => {
    set(101);
    fixture.destroy();

    expect(() => vi.advanceTimersByTime(FLASH_MS)).not.toThrow();
  });
});
