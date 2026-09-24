package com.neueda.positionservice.exceptions;

import java.math.BigDecimal;

public class InsufficientHoldingsException extends TradingException {
    
    public InsufficientHoldingsException(String symbol, BigDecimal requestedQuantity, 
                                        BigDecimal availableQuantity, String accountId) {
        super("Insufficient holdings in account " + accountId + 
              " for " + symbol + ": requested " + requestedQuantity + 
              ", available " + availableQuantity);
    }

    public InsufficientHoldingsException(String message) {
        super(message);
    }
}
