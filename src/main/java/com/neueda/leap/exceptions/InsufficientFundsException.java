package com.neueda.leap.exceptions;

import java.math.BigDecimal;

/**
 * Thrown when a BUY order cannot be executed due to insufficient cash balance.
 * 
 * The account does not have enough available cash to purchase the requested quantity
 * of the instrument at the specified price.
 */
public class InsufficientFundsException extends TradingException {
    
    /**
     * Constructs an InsufficientFundsException with detailed balance information.
     * 
     * @param required the total cost required for the order (quantity × price)
     * @param available the available cash balance in the account
     * @param accountId the account attempting the purchase
     */
    public InsufficientFundsException(BigDecimal required, BigDecimal available, String accountId) {
        super("Insufficient funds in account " + accountId + 
              ": required " + required + ", available " + available);
    }

    /**
     * Constructs an InsufficientFundsException with a custom message.
     * 
     * @param message detailed error message
     */
    public InsufficientFundsException(String message) {
        super(message);
    }

    /**
     * Constructs an InsufficientFundsException with a default message.
     */
    public InsufficientFundsException() {
        super("Insufficient funds");
    }
}
