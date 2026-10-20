export type TopicId = 'news' | 'move' | 'trends' | 'fundamentals' | 'compare' | 'learn';

export interface Topic {
  id: TopicId;
  title: string;
  description: string;
  placeholder: string;
  usesSymbol: boolean;
  question: (input: string) => string;
}

export const MAX_COMPARE = 3;

export const TOPICS: readonly Topic[] = [
  {
    id: 'news',
    title: 'News',
    description: 'Source-linked stock and fund news.',
    placeholder: 'Which stock or fund? e.g. MSFT',
    usesSymbol: true,
    question: (input) => `Latest news on ${symbols(input)[0]}`,
  },
  {
    id: 'move',
    title: 'Explain a move',
    description: 'Price moves through retrieved news.',
    placeholder: 'Which stock or fund? e.g. NVDA',
    usesSymbol: true,
    question: (input) => `Why is ${symbols(input)[0]} moving today?`,
  },
  {
    id: 'trends',
    title: 'Recent trends',
    description: 'Recent stock and fund trends.',
    placeholder: 'Which stock or fund? e.g. AAPL',
    usesSymbol: true,
    question: (input) => `What is the recent trend for ${symbols(input)[0]}?`,
  },
  {
    id: 'fundamentals',
    title: 'Fundamentals',
    description: 'P/E and expense ratios, when available.',
    placeholder: 'Which stock or fund? e.g. SPY',
    usesSymbol: true,
    question: (input) => `Show the fundamentals for ${symbols(input)[0]}`,
  },
  {
    id: 'compare',
    title: 'Compare',
    description: `Name up to ${MAX_COMPARE} symbols to compare.`,
    placeholder: 'Up to 3 symbols, e.g. AAPL, MSFT, NVDA',
    usesSymbol: true,
    question: (input) => `Compare ${joinNames(symbols(input).slice(0, MAX_COMPARE))}`,
  },
  {
    id: 'learn',
    title: 'Learn a term',
    description: 'Plain-language investing definitions.',
    placeholder: 'Which term? e.g. P/E ratio',
    usesSymbol: false,
    question: (input) => `Explain ${input.trim()} in plain language`,
  },
];

export function symbols(input: string): string[] {
  return input
    .split(/[\s,]+/)
    .map((part) => part.replace(/^\$/, '').toUpperCase())
    .filter((part) => part && part !== 'AND');
}

function joinNames(names: string[]): string {
  return names.length < 2 ? names.join('') : `${names.slice(0, -1).join(', ')} and ${names.at(-1)}`;
}
