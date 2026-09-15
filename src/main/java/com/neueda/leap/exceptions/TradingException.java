package com.neueda.leap.exceptions;

/**
 * Base exception for all trading operation failures.
 * 
 * Provides a common parent for all trading-related exceptions,
 * allowing higher-level code to catch and handle trading errors uniformly.
 * 
 * All trading exceptions inherit from this class to enable:
 * - Unified error handling: catch (TradingException e) { ... }
 * - Consistent error messaging across the application
 * - Clear error context and diagnostics
 */
public class TradingException extends Exception {
    
    /**
     * Constructs a TradingException with a descriptive error message.
     * 
     * @param message clear description of what went wrong
     */
    public TradingException(String message) {
        super(message);
    }

    /**
     * Constructs a TradingException with a message and underlying cause.
     * 
     * @param message clear description of what went wrong
     * @param cause the underlying exception that triggered this error
     */
    public TradingException(String message, Throwable cause) {
        super(message, cause);
    }
}
