package com.neueda.leap.exceptions;

/**
 * Base exception for all trading operation failures.
 * 
 * Provides a common parent for all trading-related exceptions,
 * allowing higher-level code to catch and handle trading errors uniformly.
 *
 */
public class TradingException extends Exception {
    
    public TradingException(String message) {
        super(message);
    }

    public TradingException(String message, Throwable cause) {
        super(message, cause);
    }
}
