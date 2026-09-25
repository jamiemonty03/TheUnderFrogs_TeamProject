package com.neueda.accountservice.exceptions;

public class AccountNotFoundException extends TradingException {
    

    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }

    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
