package com.neueda.orderservice.exceptions;

public class AccountAuthorizationException extends TradingException {

    public AccountAuthorizationException(String message) {
        super(message);
    }

    public AccountAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
