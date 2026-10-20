import { Component, inject } from '@angular/core';
import { AssistantChat } from '../assistant-chat';
import { AssistantPanel } from '../assistant-panel/assistant-panel';

@Component({
  selector: 'app-lily-pad-assistant',
  imports: [AssistantPanel],
  templateUrl: './lily-pad-assistant.html',
  styleUrl: './lily-pad-assistant.scss',
  host: { '(document:keydown.escape)': 'chat.close()' },
})
export class LilyPadAssistant {
  protected readonly chat = inject(AssistantChat);
}
