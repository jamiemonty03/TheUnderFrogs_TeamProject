package com.neueda.orderservice.exceptions;

public class InsufficientHoldingsException extends TradingException {
    public InsufficientHoldingsException(String message) {
        super(message);
    }

    public InsufficientHoldingsException(String message, Throwable cause) {
        super(message, cause);
    }
}
