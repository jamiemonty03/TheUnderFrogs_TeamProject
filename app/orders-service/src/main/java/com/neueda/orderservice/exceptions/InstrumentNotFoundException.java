package com.neueda.orderservice.exceptions;

public class InstrumentNotFoundException extends TradingException {
    public InstrumentNotFoundException(String message) {
        super(message);
    }

    public InstrumentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
