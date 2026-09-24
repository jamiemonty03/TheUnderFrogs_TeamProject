package com.neueda.orderservice.services.orderServices;

import com.neueda.orderservice.models.Order;

public class OrderResult {

    private final boolean success;
    private final String message;
    private final Object rollbackState;
    private final Order order;

    public OrderResult(boolean success, String message, Object rollbackState) {
        this(success, message, rollbackState, null);
    }

    public OrderResult(boolean success, String message, Object rollbackState, Order order) {
        this.success = success;
        this.message = message != null ? message : "";
        this.rollbackState = rollbackState;
        this.order = order;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getRollbackState() {
        return rollbackState;
    }

    public Order getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return String.format(
            "OrderResult{success=%s, message=%s}",
            success, message
        );
    }
}
