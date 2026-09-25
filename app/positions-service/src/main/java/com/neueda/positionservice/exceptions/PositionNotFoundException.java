package com.neueda.positionservice.exceptions;

public class PositionNotFoundException extends RuntimeException {
    
    public PositionNotFoundException(String accountId, String symbol) {
        super("Position not found for account " + accountId + " and symbol " + symbol);
    }
    
    public PositionNotFoundException(String message) {
        super(message);
    }
}
