import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, input, signal } from '@angular/core';
import type { Holding } from '../portfolio.interfaces';

const PALETTE = ['#3987e5', '#d95926', '#199e70', '#c98500', '#d55181', '#008300', '#9085e9', '#e66767'];
const OTHER_COLOUR = 'var(--ts-neutral)';
const MAX_SEGMENTS = PALETTE.length;

export interface AllocationSegment {
  key: string;
  label: string;
  name: string;
  value: number;
  weight: number;
  colour: string;
}

export function toSegments(holdings: Holding[]): AllocationSegment[] {
  const priced = holdings.filter((holding) => holding.marketValue !== null && holding.marketValue > 0);
  const total = priced.reduce((sum, holding) => sum + holding.marketValue!, 0);
  if (total === 0) {
    return [];
  }

  const bySize = [...priced].sort((first, second) => second.marketValue! - first.marketValue!);
  const shown = bySize.length > MAX_SEGMENTS ? bySize.slice(0, MAX_SEGMENTS - 1) : bySize;
  const rest = bySize.slice(shown.length);

  const colourOrder = shown.map((holding) => holding.symbol).sort();
  const segments: AllocationSegment[] = shown.map((holding) => ({
    key: holding.symbol,
    label: holding.symbol,
    name: holding.name,
    value: holding.marketValue!,
    weight: (holding.marketValue! / total) * 100,
    colour: PALETTE[colourOrder.indexOf(holding.symbol)],
  }));

  if (rest.length > 0) {
    const value = rest.reduce((sum, holding) => sum + holding.marketValue!, 0);
    segments.push({
      key: '__other',
      label: 'Other',
      name: `${rest.length} more holdings`,
      value,
      weight: (value / total) * 100,
      colour: OTHER_COLOUR,
    });
  }
  return segments;
}

@Component({
  imports: [CurrencyPipe, DecimalPipe],
  selector: 'app-allocation',
  styleUrl: './allocation.scss',
  templateUrl: './allocation.html',
})
export class Allocation {
  readonly holdings = input.required<Holding[]>();

  protected readonly segments = computed(() => toSegments(this.holdings()));
  protected readonly unpriced = computed(
    () => this.holdings().filter((holding) => holding.marketValue === null).length,
  );
  protected readonly active = signal<string | null>(null);

  protected readonly summary = computed(() =>
    this.segments()
      .map((segment) => `${segment.label} ${segment.weight.toFixed(1)}%`)
      .join(', '),
  );
}
