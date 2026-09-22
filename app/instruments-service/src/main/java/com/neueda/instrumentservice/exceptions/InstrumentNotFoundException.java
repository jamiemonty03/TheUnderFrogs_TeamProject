package com.neueda.instrumentservice.exceptions;

public class InstrumentNotFoundException extends TradingException {
    
    public InstrumentNotFoundException(String symbol) {
        super("Instrument not found");
    }

    public InstrumentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
