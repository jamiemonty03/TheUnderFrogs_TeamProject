package com.neueda.orderservice.exceptions;

public class AccountNotActiveException extends TradingException {
    public AccountNotActiveException(String message) {
        super(message);
    }

    public AccountNotActiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
