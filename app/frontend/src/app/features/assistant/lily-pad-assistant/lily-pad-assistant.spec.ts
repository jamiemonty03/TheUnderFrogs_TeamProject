import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Observable, Subject, of } from 'rxjs';
import { Assistant, AssistantChatResponse } from '../../../services/assistant';
import { Auth } from '../../../services/auth';
import { AssistantChat } from '../assistant-chat';
import { TOPICS } from '../topics';
import { LilyPadAssistant } from './lily-pad-assistant';

@Component({ template: '' })
class Blank {}

const checked: AssistantChatResponse = {
  reply: [
    '### MSFT today',
    'MSFT is **down 2.06%** (daily close, 16 Oct).',
    '',
    '- Related headline: Azure growth slows',
    '- SPY was flat',
    '',
    '| Symbol | Change |',
    '|---|---|',
    '| MSFT | -2.06% |',
  ].join('\n'),
  aiSummary: true,
  checksPassed: true,
  model: 'openai/gpt-oss-120b',
  sources: [
    {
      type: 'NEWS',
      symbol: 'MSFT',
      label: 'Azure growth slows',
      url: 'https://news.example.com/azure',
      asOf: '2026-10-19T13:05:00Z',
    },
    {
      type: 'PRICE',
      symbol: 'MSFT',
      label: 'Yahoo Finance daily close',
      url: null,
      asOf: '2026-10-16T20:00:00Z',
    },
  ],
  disclaimer: 'Information only, not financial advice.',
};

describe('LilyPadAssistant', () => {
  let fixture: ComponentFixture<LilyPadAssistant>;
  let ask: ReturnType<typeof vi.fn<(message: string) => Observable<AssistantChatResponse>>>;
  let page: HTMLElement;

  async function render(url = '/dashboard') {
    TestBed.configureTestingModule({
      imports: [LilyPadAssistant],
      providers: [
        provideRouter([
          { path: 'dashboard', component: Blank },
          { path: 'instruments/:id', component: Blank },
        ]),
        { provide: Assistant, useValue: { ask } },
        { provide: Auth, useValue: { currentUser: signal({ username: 'demo' }) } },
      ],
    });
    await TestBed.inject(Router).navigateByUrl(url);
    fixture = TestBed.createComponent(LilyPadAssistant);
    fixture.detectChanges();
    await fixture.whenStable();
    page = fixture.nativeElement as HTMLElement;
  }

  function byTestId<T extends HTMLElement = HTMLElement>(id: string): T | null {
    return page.querySelector<T>(`[data-testid="${id}"]`);
  }

  function allByTestId<T extends HTMLElement = HTMLElement>(id: string): T[] {
    return [...page.querySelectorAll<T>(`[data-testid="${id}"]`)];
  }

  async function openPanel() {
    byTestId('assistant-lily-pad')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  function type(text: string) {
    const input = byTestId<HTMLTextAreaElement>('assistant-input')!;
    input.value = text;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    return input;
  }

  function topic(title: string) {
    return allByTestId<HTMLButtonElement>('assistant-topic').find((t) =>
      t.textContent?.includes(title),
    )!;
  }

  async function askAboutMsft() {
    await render();
    await openPanel();
    type('Why is Microsoft down today?');
    byTestId('assistant-send')!.click();
    fixture.detectChanges();
  }

  beforeEach(() => {
    sessionStorage.clear();
    ask = vi.fn(() => of(checked));
  });

  afterEach(() => TestBed.resetTestingModule());

  it('shows only the lily pad until it is pressed, then swaps it for the chat', async () => {
    await render();
    const pad = byTestId('assistant-lily-pad')!;

    expect(byTestId('assistant-panel')).toBeNull();
    expect(pad.getAttribute('aria-expanded')).toBe('false');
    expect(pad.getAttribute('aria-label')).toBe('Open AI assistant');

    await openPanel();

    expect(byTestId('assistant-panel')).not.toBeNull();
    expect(TestBed.inject(AssistantChat).open()).toBe(true);
  });

  it('brings the lily pad back when the chat closes', async () => {
    await render();
    await openPanel();

    byTestId('assistant-close')!.click();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(byTestId('assistant-lily-pad')).not.toBeNull();
  });

  it('closes with the close button and with Escape', async () => {
    await render();
    const chat = TestBed.inject(AssistantChat);

    await openPanel();
    byTestId('assistant-close')!.click();
    expect(chat.open()).toBe(false);
    fixture.detectChanges();
    await fixture.whenStable();

    await openPanel();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(chat.open()).toBe(false);
  });

  it('welcomes the user with the six research topics', async () => {
    await render();
    await openPanel();

    expect(byTestId('assistant-welcome')!.textContent).toContain(
      'What’s moving beneath the surface?',
    );
    expect(
      allByTestId('assistant-topic').map((t) =>
        t.querySelector('.topic__title')!.textContent!.trim(),
      ),
    ).toEqual(TOPICS.map((t) => t.title));
    expect(byTestId<HTMLTextAreaElement>('assistant-input')!.placeholder).toBe(
      'Choose a topic to get started',
    );
    expect(page.textContent).toContain(
      'No portfolio analysis, recommendations or trading actions.',
    );
  });

  it('shapes the question from the chosen topic', async () => {
    await render();
    await openPanel();

    topic('News').click();
    fixture.detectChanges();
    expect(byTestId('assistant-selected-topic')!.textContent).toContain('News');
    expect(byTestId<HTMLTextAreaElement>('assistant-input')!.placeholder).toBe(
      'Which stock or fund? e.g. MSFT',
    );

    type('msft');
    byTestId('assistant-send')!.click();
    fixture.detectChanges();

    expect(ask).toHaveBeenCalledWith('Latest news on MSFT');
    expect(byTestId('assistant-selected-topic')).toBeNull();
  });

  it('turns up to three symbols into a comparison', async () => {
    await render();
    await openPanel();

    topic('Compare').click();
    type('aapl, msft and nvda tsla');
    byTestId('assistant-send')!.click();

    expect(ask).toHaveBeenCalledWith('Compare AAPL, MSFT and NVDA');
  });

  it('fills in the symbol of the instrument you are looking at', async () => {
    await render('/instruments/msft');
    await openPanel();

    topic('Explain a move').click();
    fixture.detectChanges();

    expect(byTestId<HTMLTextAreaElement>('assistant-input')!.value).toBe('MSFT');
    byTestId('assistant-send')!.click();
    expect(ask).toHaveBeenCalledWith('Why is MSFT moving today?');
  });

  it('still accepts a free question without a topic', async () => {
    await askAboutMsft();

    expect(ask).toHaveBeenCalledWith('Why is Microsoft down today?');
  });

  it('formats the answer into headings, bold text, lists and tables', async () => {
    await askAboutMsft();

    const reply = byTestId('assistant-reply')!;
    expect(reply.querySelector('h3')!.textContent).toContain('MSFT today');
    expect(reply.querySelector('strong')!.textContent).toBe('down 2.06%');
    expect([...reply.querySelectorAll('ul li')].map((li) => li.textContent!.trim())).toEqual([
      'Related headline: Azure growth slows',
      'SPY was flat',
    ]);
    expect([...reply.querySelectorAll('th')].map((th) => th.textContent!.trim())).toEqual([
      'Symbol',
      'Change',
    ]);
    expect(reply.querySelector('td')!.textContent!.trim()).toBe('MSFT');
    expect(reply.textContent).not.toContain('**');
    expect(reply.textContent).not.toContain('|');
  });

  it('shows the checked badge, sources, model and disclaimer with the answer', async () => {
    await askAboutMsft();

    const answer = allByTestId('assistant-message')[1];
    expect(byTestId('assistant-badge')!.textContent).toContain('Checked against our data');
    expect(answer.textContent).toContain('Information only, not financial advice.');
    expect(answer.textContent).toContain('Answered by openai/gpt-oss-120b');

    const sources = allByTestId('assistant-source');
    expect(sources).toHaveLength(2);
    const link = sources[0].querySelector<HTMLAnchorElement>('a')!;
    expect(link.href).toBe('https://news.example.com/azure');
    expect(link.target).toBe('_blank');
    expect(link.rel).toContain('noopener');
    expect(link.rel).toContain('noreferrer');
    expect(sources[1].querySelector('a')).toBeNull();
    expect(sources[1].querySelector('time')!.getAttribute('datetime')).toBe('2026-10-16T20:00:00Z');
  });

  it('says when it is showing raw data instead of an AI summary', async () => {
    ask = vi.fn(() => of({ ...checked, aiSummary: false, checksPassed: false, model: null }));
    await askAboutMsft();

    const badge = byTestId('assistant-badge')!;
    expect(badge.textContent).toContain('AI summary unavailable');
    expect(badge.classList).toContain('message__badge--raw');
    expect(page.textContent).not.toContain('Answered by');
  });

  it('shows the reply as text, never as HTML', async () => {
    ask = vi.fn(() => of({ ...checked, reply: '<img src=x onerror="alert(1)"> **<b>bold</b>**' }));
    await askAboutMsft();

    const reply = byTestId('assistant-reply')!;
    expect(reply.querySelector('img')).toBeNull();
    expect(reply.querySelector('b')).toBeNull();
    expect(reply.textContent).toContain('<img src=x');
    expect(reply.querySelector('strong')!.textContent).toBe('<b>bold</b>');
  });

  it('sends with Enter, keeps Shift+Enter for a new line and clears the box', async () => {
    await render();
    await openPanel();

    const input = type('Compare AAPL and MSFT');
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true }));
    expect(ask).not.toHaveBeenCalled();

    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    fixture.detectChanges();

    expect(ask).toHaveBeenCalledWith('Compare AAPL and MSFT');
    expect(input.value).toBe('');
  });

  it('limits questions to 500 characters and disables Ask for a blank question', async () => {
    await render();
    await openPanel();

    expect(byTestId<HTMLTextAreaElement>('assistant-input')!.maxLength).toBe(500);
    expect(byTestId<HTMLButtonElement>('assistant-send')!.disabled).toBe(true);
  });

  it('shows the frogs hopping while it waits, spins the pad if closed, then shows the conversation', async () => {
    const reply = new Subject<AssistantChatResponse>();
    ask = vi.fn(() => reply.asObservable());
    await askAboutMsft();

    expect(byTestId('assistant-thinking')).not.toBeNull();
    expect(byTestId('assistant-welcome')).toBeNull();
    expect(allByTestId<HTMLButtonElement>('assistant-topic').every((t) => t.disabled)).toBe(true);

    byTestId('assistant-close')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(page.querySelector('.pad__pending')).not.toBeNull();
    await openPanel();

    reply.next(checked);
    reply.complete();
    fixture.detectChanges();

    expect(byTestId('assistant-thinking')).toBeNull();
    expect(allByTestId('assistant-topic')).toHaveLength(TOPICS.length);
  });
});
