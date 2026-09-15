package com.neueda.leap.enums;

/**
 * Order direction enumeration.
 * 
 * BUY: Acquire position (debit account cash, create/increase position)
 * SELL: Liquidate position (credit account cash, decrease position)
 */
public enum OrderSide {
    BUY,
    SELL
}
