package com.neueda.leap.exceptions;

public class InvalidOrderException extends TradingException {

    public InvalidOrderException(String message) {
        super(message);
    }

    public InvalidOrderException() {
        super("Invalid order");
    }
}
