import { Component, input, output } from '@angular/core';
import { TOPICS, Topic, TopicId } from '../topics';
import { TopicIcon } from '../topic-icon/topic-icon';

@Component({
  selector: 'app-assistant-welcome',
  imports: [TopicIcon],
  templateUrl: './assistant-welcome.html',
  styleUrl: './assistant-welcome.scss',
  host: { 'data-testid': 'assistant-welcome' },
})
export class AssistantWelcome {
  readonly selected = input<TopicId | null>(null);
  readonly choose = output<Topic>();

  protected readonly topics = TOPICS;
}
