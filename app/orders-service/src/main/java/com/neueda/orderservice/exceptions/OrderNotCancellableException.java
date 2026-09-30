package com.neueda.orderservice.exceptions;

import com.neueda.orderservice.enums.OrderStatus;

public class OrderNotCancellableException extends TradingException {

    private final OrderStatus currentStatus;

    public OrderNotCancellableException(String orderId, OrderStatus currentStatus) {
        super("Order " + orderId + " cannot be cancelled: status is " + currentStatus);
        this.currentStatus = currentStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}
