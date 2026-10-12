package com.neueda.orderservice.exceptions;

public class ClientTradeNotFoundException extends TradingException {
    public ClientTradeNotFoundException(String message) {
        super(message);
    }
}
