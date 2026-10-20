import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, effect, inject, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { Assistant, AssistantChatResponse, MAX_QUESTION_LENGTH } from '../../services/assistant';
import { Auth } from '../../services/auth';

export interface ChatEntry {
  id: number;
  role: 'user' | 'assistant';
  text: string;
  response?: AssistantChatResponse;
  error?: boolean;
}

export const HISTORY_KEY = 'assistant.history';
export const MAX_ENTRIES = 40;

@Injectable({ providedIn: 'root' })
export class AssistantChat {
  private readonly assistant = inject(Assistant);
  private readonly auth = inject(Auth);

  readonly open = signal(false);
  readonly pending = signal(false);
  readonly entries = signal<ChatEntry[]>(readHistory());

  constructor() {
    effect(() => {
      if (this.auth.currentUser() === null) {
        this.clear();
      }
    });
    effect(() => writeHistory(this.entries()));
  }

  toggle(): void {
    this.open.update((open) => !open);
  }

  close(): void {
    this.open.set(false);
  }

  ask(question: string): void {
    const message = question.trim().slice(0, MAX_QUESTION_LENGTH);
    if (!message || this.pending()) {
      return;
    }
    this.add({ role: 'user', text: message });
    this.pending.set(true);
    this.assistant
      .ask(message)
      .pipe(finalize(() => this.pending.set(false)))
      .subscribe({
        next: (response) => this.add({ role: 'assistant', text: response.reply, response }),
        error: (error: HttpErrorResponse) =>
          this.add({ role: 'assistant', text: friendlyError(error), error: true }),
      });
  }

  clear(): void {
    this.entries.set([]);
  }

  private add(entry: Omit<ChatEntry, 'id'>): void {
    this.entries.update((entries) => {
      const id = (entries.at(-1)?.id ?? 0) + 1;
      return [...entries, { ...entry, id }].slice(-MAX_ENTRIES);
    });
  }
}

export function friendlyError(error: HttpErrorResponse): string {
  if (error.status === 429) {
    return (
      error.error?.message ?? 'You are asking a little fast. Please wait a moment and try again.'
    );
  }
  if (error.status === 422) {
    return error.error?.message ?? 'Please ask a shorter question.';
  }
  if (error.status === 0) {
    return "I can't reach the assistant right now. Check your connection and try again.";
  }
  return 'The assistant is unavailable right now. Please try again in a minute.';
}

function readHistory(): ChatEntry[] {
  try {
    const saved = JSON.parse(sessionStorage.getItem(HISTORY_KEY) ?? '[]');
    return Array.isArray(saved) ? saved.slice(-MAX_ENTRIES) : [];
  } catch {
    return [];
  }
}

function writeHistory(entries: ChatEntry[]): void {
  try {
    if (entries.length === 0) {
      sessionStorage.removeItem(HISTORY_KEY);
    } else {
      sessionStorage.setItem(HISTORY_KEY, JSON.stringify(entries));
    }
  } catch {
    return;
  }
}
