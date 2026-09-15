package com.neueda.leap.exceptions;

import java.math.BigDecimal;

/**
 * Thrown when a SELL order cannot be executed due to insufficient holdings.
 * 
 * The account does not own enough shares/units of the instrument to fulfill
 * the requested sell order.
 */
public class InsufficientHoldingsException extends TradingException {
    
    /**
     * Constructs an InsufficientHoldingsException with detailed position information.
     * 
     * @param symbol the trading symbol being sold
     * @param requestedQuantity the number of units requested to sell
     * @param availableQuantity the number of units currently held
     * @param accountId the account attempting the sale
     */
    public InsufficientHoldingsException(String symbol, BigDecimal requestedQuantity, 
                                        BigDecimal availableQuantity, String accountId) {
        super("Insufficient holdings in account " + accountId + 
              " for " + symbol + ": requested " + requestedQuantity + 
              ", available " + availableQuantity);
    }

    /**
     * Constructs an InsufficientHoldingsException with a custom message.
     * 
     * @param message detailed error message
     */
    public InsufficientHoldingsException(String message) {
        super(message);
    }

    /**
     * Constructs an InsufficientHoldingsException with a default message.
     * 
     */
    public InsufficientHoldingsException() {
        super("Insufficient holdings to execute the sell order.");
    }
}
