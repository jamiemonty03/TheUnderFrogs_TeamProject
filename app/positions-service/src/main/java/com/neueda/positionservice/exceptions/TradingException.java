package com.neueda.positionservice.exceptions;

public class TradingException extends Exception {
    

    public TradingException(String message) {
        super(message);
    }

    public TradingException(String message, Throwable cause) {
        super(message, cause);
    }
}
