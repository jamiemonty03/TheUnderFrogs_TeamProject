export interface TopUpStart {
  checkoutUrl: string;
}

export interface TopUpConfirmation {
  amount: number;
  cashBalance: number;
}
