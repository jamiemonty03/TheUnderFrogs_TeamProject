export type AssistantSourceType = 'PRICE' | 'NEWS' | 'TREND' | 'FUNDAMENTALS';

export interface AssistantSource {
  type: AssistantSourceType;
  symbol: string;
  label: string;
  url: string | null;
  asOf: string | null;
}

export interface AssistantChatResponse {
  reply: string;
  aiSummary: boolean;
  checksPassed: boolean;
  model: string | null;
  sources: AssistantSource[];
  disclaimer: string;
}
