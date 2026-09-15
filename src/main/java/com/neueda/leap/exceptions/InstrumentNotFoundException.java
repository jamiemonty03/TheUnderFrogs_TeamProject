package com.neueda.leap.exceptions;

/**
 * Thrown when an order references an instrument (stock, bond, ETF) that does not exist.
 * 
 * Indicates the symbol in the order is invalid or has not been added to the system.
 * All instruments must be defined before orders can be placed on them.
 */
public class InstrumentNotFoundException extends TradingException {
    
    /**
     * Constructs an InstrumentNotFoundException with symbol details.
     * 
     * @param symbol the trading symbol that was not found
     */
    public InstrumentNotFoundException(String symbol) {
        super("Instrument not found: " + symbol);
    }

    /**
     * Constructs an InstrumentNotFoundException with a custom message.
     * 
     * @param message detailed error message
     */
    public InstrumentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
