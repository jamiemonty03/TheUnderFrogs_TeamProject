package com.neueda.leap.exceptions;

/**
 * Thrown when an order references an account that does not exist.
 * 
 * Indicates the account ID in the order is invalid or has been deleted.
 * This is a critical error that should prevent order processing.
 */
public class AccountNotFoundException extends TradingException {
    
    /**
     * Constructs an AccountNotFoundException with account details.
     * 
     * @param accountId the account ID that was not found
     */
    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }

    /**
     * Constructs an AccountNotFoundException with a custom message.
     * 
     * @param message detailed error message
     */
    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
