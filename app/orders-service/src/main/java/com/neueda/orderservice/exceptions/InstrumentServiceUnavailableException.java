package com.neueda.orderservice.exceptions;

public class InstrumentServiceUnavailableException extends TradingException {
    public InstrumentServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
