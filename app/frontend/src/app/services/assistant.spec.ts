import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Assistant, AssistantChatResponse } from './assistant';

const answer: AssistantChatResponse = {
  reply: 'MSFT is down 2.06%.',
  aiSummary: true,
  checksPassed: true,
  model: 'openai/gpt-oss-120b',
  sources: [],
  disclaimer: 'Information only, not financial advice.',
};

describe('Assistant', () => {
  let assistant: Assistant;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    assistant = TestBed.inject(Assistant);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts the question to the assistant endpoint', async () => {
    const result = firstValueFrom(assistant.ask('Why is Microsoft down today?'));
    const request = http.expectOne('/api/assistant/chat');
    request.flush(answer);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ message: 'Why is Microsoft down today?' });
    expect(await result).toEqual(answer);
  });
});
