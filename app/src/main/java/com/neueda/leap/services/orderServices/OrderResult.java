package com.neueda.leap.services.orderServices;

/**
 * Encapsulates the outcome of an order execution attempt.
 * 
 * Tracks:
 * - Success/failure status
 * - Outcome message (error details if failed)
 * - Rollback state (captured state before transaction for manual undo)
 * 
 * Example on success:
 *   new OrderResult(true, "Order ABC123 FILLED: 100 shares @ $50", null)
 * 
 * Example on failure:
 *   new OrderResult(false, "Insufficient holdings: need 50, have 30", capturedState)
 */
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
