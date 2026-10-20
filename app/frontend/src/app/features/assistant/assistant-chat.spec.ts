import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Observable, Subject, of, throwError } from 'rxjs';
import { Assistant, AssistantChatResponse } from '../../services/assistant';
import { Auth } from '../../services/auth';
import { AssistantChat, HISTORY_KEY, MAX_ENTRIES, friendlyError } from './assistant-chat';

const answer: AssistantChatResponse = {
  reply: 'MSFT is down 2.06%.',
  aiSummary: true,
  checksPassed: true,
  model: 'openai/gpt-oss-120b',
  sources: [],
  disclaimer: 'Information only, not financial advice.',
};

describe('AssistantChat', () => {
  let ask: ReturnType<typeof vi.fn<(message: string) => Observable<AssistantChatResponse>>>;
  const currentUser = signal<{ username: string } | null>({ username: 'demo' });

  function createChat(): AssistantChat {
    TestBed.configureTestingModule({
      providers: [
        { provide: Assistant, useValue: { ask } },
        { provide: Auth, useValue: { currentUser } },
      ],
    });
    return TestBed.inject(AssistantChat);
  }

  beforeEach(() => {
    sessionStorage.clear();
    currentUser.set({ username: 'demo' });
    ask = vi.fn(() => of(answer));
  });

  afterEach(() => TestBed.resetTestingModule());

  it('adds the question and the answer to the conversation', () => {
    const chat = createChat();

    chat.ask('  Why is Microsoft down today?  ');

    expect(ask).toHaveBeenCalledWith('Why is Microsoft down today?');
    expect(chat.entries().map((e) => [e.role, e.text])).toEqual([
      ['user', 'Why is Microsoft down today?'],
      ['assistant', 'MSFT is down 2.06%.'],
    ]);
    expect(chat.entries()[1].response).toEqual(answer);
    expect(chat.pending()).toBe(false);
  });

  it('shows it is thinking until the answer arrives and ignores questions meanwhile', () => {
    const reply = new Subject<AssistantChatResponse>();
    ask = vi.fn(() => reply.asObservable());
    const chat = createChat();

    chat.ask('First question');
    chat.ask('Second question');

    expect(chat.pending()).toBe(true);
    expect(ask).toHaveBeenCalledTimes(1);
    reply.next(answer);
    reply.complete();
    expect(chat.pending()).toBe(false);
  });

  it('ignores a blank question', () => {
    const chat = createChat();

    chat.ask('   ');

    expect(ask).not.toHaveBeenCalled();
    expect(chat.entries()).toEqual([]);
  });

  it('shows a friendly message when the request fails', () => {
    ask = vi.fn(() =>
      throwError(
        () =>
          new HttpErrorResponse({
            status: 429,
            error: { errorCode: 'AI-429', message: 'You can ask up to 5 questions a minute.' },
          }),
      ),
    );
    const chat = createChat();

    chat.ask('Why is NVDA down today?');

    const last = chat.entries().at(-1)!;
    expect(last.error).toBe(true);
    expect(last.text).toBe('You can ask up to 5 questions a minute.');
    expect(chat.pending()).toBe(false);
  });

  it('keeps the conversation for this browser session', () => {
    const chat = createChat();
    chat.ask('Why is Microsoft down today?');
    TestBed.tick();

    const saved = JSON.parse(sessionStorage.getItem(HISTORY_KEY)!);
    expect(saved).toHaveLength(2);

    TestBed.resetTestingModule();
    expect(createChat().entries()).toHaveLength(2);
  });

  it('forgets the conversation when the user signs out', () => {
    const chat = createChat();
    chat.ask('Why is Microsoft down today?');
    TestBed.tick();

    currentUser.set(null);
    TestBed.tick();

    expect(chat.entries()).toEqual([]);
    expect(sessionStorage.getItem(HISTORY_KEY)).toBeNull();
  });

  it('keeps only the most recent messages', () => {
    const chat = createChat();

    for (let i = 0; i < MAX_ENTRIES; i++) {
      chat.ask(`Question ${i}`);
    }

    expect(chat.entries()).toHaveLength(MAX_ENTRIES);
    expect(chat.entries().at(-2)!.text).toBe(`Question ${MAX_ENTRIES - 1}`);
  });

  it('opens and closes', () => {
    const chat = createChat();

    chat.toggle();
    expect(chat.open()).toBe(true);
    chat.close();
    expect(chat.open()).toBe(false);
  });
});

describe('friendlyError', () => {
  it('explains a lost connection, a too-long question and an unavailable assistant', () => {
    expect(friendlyError(new HttpErrorResponse({ status: 0 }))).toContain(
      "can't reach the assistant",
    );
    expect(
      friendlyError(new HttpErrorResponse({ status: 422, error: { message: 'too long' } })),
    ).toBe('too long');
    expect(friendlyError(new HttpErrorResponse({ status: 503 }))).toContain(
      'unavailable right now',
    );
    expect(friendlyError(new HttpErrorResponse({ status: 429 }))).toContain('wait a moment');
  });
});
