package com.neueda.leap.exceptions;

/**
 * Thrown when an order attempts to trade on an account that is not ACTIVE.
 * 
 * Accounts may be in states like INACTIVE, SUSPENDED, or CLOSED.
 * Only ACTIVE accounts can execute trades.
 */
public class AccountNotActiveException extends TradingException {
    
    /**
     * Constructs an AccountNotActiveException with account and status details.
     * 
     * @param accountId the account attempting the trade
     * @param currentStatus the current status of the account
     */
    public AccountNotActiveException(String accountId, String currentStatus) {
        super("Account cannot trade: " + accountId + " status is " + currentStatus + " (must be ACTIVE)");
    }

    /**
     * Constructs an AccountNotActiveException with a custom message.
     * 
     * @param message detailed error message
     */
    public AccountNotActiveException(String message) {
        super(message);
    }

    /**
     * Constructs an AccountNotActiveException with a default message.
     */
    public AccountNotActiveException() {
        super("Account is not active");
    }
}
