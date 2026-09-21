package com.neueda.leap.exceptions;

public class InstrumentNotFoundException extends TradingException {
    
    public InstrumentNotFoundException(String symbol) {
        super("Instrument not found: " + symbol);
    }

    public InstrumentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
