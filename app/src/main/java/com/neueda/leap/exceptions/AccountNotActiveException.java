package com.neueda.leap.exceptions;


public class AccountNotActiveException extends TradingException {
    
    public AccountNotActiveException(String accountId, String currentStatus) {
        super("Account cannot trade: " + accountId + " status is " + currentStatus + " (must be ACTIVE)");
    }

    public AccountNotActiveException(String message) {
        super(message);
    }

    public AccountNotActiveException() {
        super("Account is not active");
    }
}
