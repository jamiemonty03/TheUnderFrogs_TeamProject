package com.neueda.orderservice.exceptions;

import com.neueda.orderservice.enums.OrderStatus;

public class OrderStatusConflictException extends TradingException {

    private final OrderStatus currentStatus;

    public OrderStatusConflictException(String orderId, OrderStatus expectedStatus, OrderStatus currentStatus) {
        super("Order " + orderId + " is " + currentStatus + ", expected " + expectedStatus);
        this.currentStatus = currentStatus;
    }

    public OrderStatusConflictException(String orderId, OrderStatus expectedStatus, OrderStatus currentStatus, Throwable cause) {
        this(orderId, expectedStatus, currentStatus);
        initCause(cause);
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}
