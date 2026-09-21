package com.neueda.orderservice.services.orderServices;

public class OrderResult {

    private final boolean success;
    private final String message;
    private final Object rollbackState;

    public OrderResult(boolean success, String message, Object rollbackState) {
        this.success = success;
        this.message = message != null ? message : "";
        this.rollbackState = rollbackState;
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

    @Override
    public String toString() {
        return String.format(
            "OrderResult{success=%s, message=%s}",
            success, message
        );
    }
}
