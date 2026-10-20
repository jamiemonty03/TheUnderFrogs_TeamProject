import { DestroyRef, Directive, ElementRef, Renderer2, effect, inject, input } from '@angular/core';

/** How long a flash lasts; matches the price-flash animation in styles.scss. */
export const FLASH_MS = 3_000;

const UP = 'price-flash--up';
const DOWN = 'price-flash--down';

/**
 * Briefly tints its element green when the bound price rises and red when it falls.
 * The first value and nulls never flash. Classes are added directly so they don't fight any [class] binding.
 *
 *   <span class="price-flash" [appPriceFlash]="row.price">{{ row.price | currency }}</span>
 */
@Directive({ selector: '[appPriceFlash]' })
export class PriceFlash {
  readonly appPriceFlash = input<number | null>(null);

  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly renderer = inject(Renderer2);
  private previous: number | null = null;
  private timer: ReturnType<typeof setTimeout> | undefined;

  constructor() {
    effect(() => this.onPrice(this.appPriceFlash()));
    inject(DestroyRef).onDestroy(() => clearTimeout(this.timer));
  }

  private onPrice(price: number | null): void {
    const previous = this.previous;
    if (price !== null) {
      this.previous = price;
    }
    if (price === null || previous === null || price === previous) {
      return;
    }
    this.flash(price > previous ? UP : DOWN);
  }

  private flash(direction: string): void {
    const element = this.el.nativeElement;
    clearTimeout(this.timer);
    this.renderer.removeClass(element, UP);
    this.renderer.removeClass(element, DOWN);
    // Force a reflow so a repeat flash in the same direction restarts the animation
    void element.offsetWidth;
    this.renderer.addClass(element, direction);
    this.timer = setTimeout(() => this.renderer.removeClass(element, direction), FLASH_MS);
  }
}
