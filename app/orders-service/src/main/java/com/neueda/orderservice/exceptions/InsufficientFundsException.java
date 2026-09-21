package com.neueda.orderservice.exceptions;

public class InsufficientFundsException extends TradingException {
    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(message, cause);
    }
}
