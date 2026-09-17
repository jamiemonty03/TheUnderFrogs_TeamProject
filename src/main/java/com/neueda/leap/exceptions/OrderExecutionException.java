package com.neueda.leap.exceptions;

public class OrderExecutionException extends TradingException {
    

    public OrderExecutionException(String message) {
        super(message);
    }


    public OrderExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
