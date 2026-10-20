import { TOPICS, TopicId, symbols } from './topics';

function question(id: TopicId, input: string): string {
  return TOPICS.find((topic) => topic.id === id)!.question(input);
}

describe('topics', () => {
  it('turns what the user typed into a clear question', () => {
    expect(question('news', ' msft ')).toBe('Latest news on MSFT');
    expect(question('move', '$nvda')).toBe('Why is NVDA moving today?');
    expect(question('trends', 'aapl')).toBe('What is the recent trend for AAPL?');
    expect(question('fundamentals', 'spy')).toBe('Show the fundamentals for SPY');
    expect(question('learn', ' P/E ratio ')).toBe('Explain P/E ratio in plain language');
  });

  it('compares at most three symbols', () => {
    expect(question('compare', 'aapl msft')).toBe('Compare AAPL and MSFT');
    expect(question('compare', 'aapl, msft, nvda, tsla')).toBe('Compare AAPL, MSFT and NVDA');
  });

  it('reads symbols separated by commas, spaces or "and"', () => {
    expect(symbols('aapl,msft  and $nvda')).toEqual(['AAPL', 'MSFT', 'NVDA']);
  });
});
