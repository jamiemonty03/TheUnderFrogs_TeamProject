import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { AssistantSource } from '../../../services/assistant';
import { ChatEntry } from '../assistant-chat';
import { formatReply } from '../format-reply';

const SOURCE_LABELS: Record<AssistantSource['type'], string> = {
  PRICE: 'Price',
  NEWS: 'News',
  TREND: 'Trend',
  FUNDAMENTALS: 'Fundamentals',
};

@Component({
  selector: 'app-assistant-message',
  imports: [DatePipe, NgTemplateOutlet],
  templateUrl: './assistant-message.html',
  styleUrl: './assistant-message.scss',
  host: {
    'data-testid': 'assistant-message',
    '[class.message--user]': "entry().role === 'user'",
    '[class.message--answer]': "entry().role === 'assistant'",
    '[class.message--error]': 'entry().error',
  },
})
export class AssistantMessage {
  readonly entry = input.required<ChatEntry>();

  protected readonly sourceLabels = SOURCE_LABELS;
  protected readonly formatted = computed(() => {
    const entry = this.entry();
    return entry.role === 'assistant' && !entry.error ? formatReply(entry.text) : null;
  });
}
