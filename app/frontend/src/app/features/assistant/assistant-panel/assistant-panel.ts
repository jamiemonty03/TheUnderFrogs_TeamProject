import {
  Component,
  ElementRef,
  afterRenderEffect,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';
import { MAX_QUESTION_LENGTH } from '../../../services/assistant';
import { AssistantChat } from '../assistant-chat';
import { AssistantMessage } from '../assistant-message/assistant-message';
import { AssistantWelcome } from '../assistant-welcome/assistant-welcome';
import { TopicIcon } from '../topic-icon/topic-icon';
import { TOPICS, Topic } from '../topics';

const INSTRUMENT_PAGE = /^\/instruments\/([^/?#]+)/;
const FREE_PLACEHOLDER = 'Choose a topic to get started';

@Component({
  selector: 'app-assistant-panel',
  imports: [AssistantMessage, AssistantWelcome, TopicIcon],
  templateUrl: './assistant-panel.html',
  styleUrl: './assistant-panel.scss',
  host: {
    role: 'complementary',
    'aria-label': 'Lily Pad AI research assistant',
    'data-testid': 'assistant-panel',
  },
})
export class AssistantPanel {
  protected readonly chat = inject(AssistantChat);
  private readonly router = inject(Router);

  protected readonly topics = TOPICS;
  protected readonly maxLength = MAX_QUESTION_LENGTH;
  protected readonly draft = signal('');
  protected readonly topic = signal<Topic | null>(null);

  private readonly input = viewChild<ElementRef<HTMLTextAreaElement>>('questionInput');
  private readonly log = viewChild<ElementRef<HTMLElement>>('chatLog');

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  protected readonly symbol = computed(() => {
    const match = INSTRUMENT_PAGE.exec(this.url() ?? '');
    return match ? decodeURIComponent(match[1]).toUpperCase() : null;
  });

  protected readonly placeholder = computed(() => this.topic()?.placeholder ?? FREE_PLACEHOLDER);

  protected readonly canSend = computed(() => !!this.draft().trim() && !this.chat.pending());

  constructor() {
    afterRenderEffect(() => {
      this.topic();
      this.input()?.nativeElement.focus();
    });
    afterRenderEffect(() => {
      this.chat.entries();
      this.chat.pending();
      const log = this.log()?.nativeElement;
      if (log) {
        log.scrollTop = log.scrollHeight;
      }
    });
  }

  protected chooseTopic(topic: Topic): void {
    this.topic.set(topic);
    const symbol = this.symbol();
    if (topic.usesSymbol && symbol && !this.draft().trim()) {
      this.draft.set(symbol);
    }
  }

  protected clearTopic(): void {
    this.topic.set(null);
  }

  protected send(): void {
    if (!this.canSend()) {
      return;
    }
    const topic = this.topic();
    this.chat.ask(topic ? topic.question(this.draft()) : this.draft());
    this.draft.set('');
    this.topic.set(null);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  protected onInput(event: Event): void {
    this.draft.set((event.target as HTMLTextAreaElement).value);
  }
}
