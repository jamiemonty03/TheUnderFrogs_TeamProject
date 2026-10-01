package com.neueda.orderservice.exceptions;

public class OrderNotFoundException extends TradingException {

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}
