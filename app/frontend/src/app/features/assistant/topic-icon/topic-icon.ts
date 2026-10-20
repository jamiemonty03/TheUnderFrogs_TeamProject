import { Component, input } from '@angular/core';
import { TopicId } from '../topics';

@Component({
  selector: 'app-topic-icon',
  template: `
    <svg viewBox="0 0 24 24" aria-hidden="true">
      @switch (topic()) {
        @case ('news') {
          <path d="M4 5h12v14H6a2 2 0 0 1-2-2V5Z" />
          <path d="M16 9h4v8a2 2 0 0 1-2 2h-2" />
          <path d="M7 9h6M7 12h6M7 15h4" />
        }
        @case ('move') {
          <path d="M3 12h4l3-7 4 14 3-7h4" />
        }
        @case ('trends') {
          <path d="m3 17 6-6 4 4 8-8" />
          <path d="M15 7h6v6" />
        }
        @case ('fundamentals') {
          <path d="M6 20V11M12 20V4M18 20v-6" />
        }
        @case ('compare') {
          <rect x="4" y="4" width="16" height="16" rx="2" />
          <path d="M9.5 4v16M14.5 4v16" />
        }
        @default {
          <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H12v17H6.5A2.5 2.5 0 0 0 4 22.5V5.5Z" />
          <path d="M20 5.5A2.5 2.5 0 0 0 17.5 3H12v17h5.5a2.5 2.5 0 0 1 2.5 2.5V5.5Z" />
        }
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex: none;
      width: var(--icon-size, 1.1rem);
      height: var(--icon-size, 1.1rem);
    }

    svg {
      width: 100%;
      height: 100%;
      fill: none;
      stroke: var(--ts-frog);
      stroke-width: 1.8;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
  `,
})
export class TopicIcon {
  readonly topic = input.required<TopicId>();
}
