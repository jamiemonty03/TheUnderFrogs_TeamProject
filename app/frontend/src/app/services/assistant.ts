import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import type { AssistantChatResponse } from './interfaces/assistant.interfaces';

export type {
  AssistantChatResponse,
  AssistantSource,
  AssistantSourceType,
} from './interfaces/assistant.interfaces';

export const MAX_QUESTION_LENGTH = 500;

@Injectable({ providedIn: 'root' })
export class Assistant {
  private readonly http = inject(HttpClient);

  ask(message: string): Observable<AssistantChatResponse> {
    return this.http.post<AssistantChatResponse>('/api/assistant/chat', { message });
  }
}
