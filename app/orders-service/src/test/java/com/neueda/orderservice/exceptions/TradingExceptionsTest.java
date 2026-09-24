package com.neueda.orderservice.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TradingExceptionsTest {

    private final Throwable cause = new RuntimeException("root cause");

    @Test
    @DisplayName("Every order exception is a TradingException")
    void allExtendTradingException() {
        assertInstanceOf(TradingException.class, new AccountNotActiveException("x"));
        assertInstanceOf(TradingException.class, new DuplicateOrderException("x"));
        assertInstanceOf(TradingException.class, new InstrumentNotFoundException("x"));
        assertInstanceOf(TradingException.class, new InsufficientFundsException("x"));
        assertInstanceOf(TradingException.class, new InsufficientHoldingsException("x"));
        assertInstanceOf(TradingException.class, new InvalidOrderException("x"));
        assertInstanceOf(TradingException.class, new OrderExecutionException("x"));
    }

    @Test
    @DisplayName("Message-and-cause constructors keep both")
    void messageAndCauseConstructors() {
        assertMessageAndCause(new TradingException("trading", cause), "trading");
        assertMessageAndCause(new AccountNotActiveException("inactive", cause), "inactive");
        assertMessageAndCause(new DuplicateOrderException("duplicate", cause), "duplicate");
        assertMessageAndCause(new InstrumentNotFoundException("missing", cause), "missing");
        assertMessageAndCause(new InsufficientFundsException("funds", cause), "funds");
        assertMessageAndCause(new InsufficientHoldingsException("holdings", cause), "holdings");
        assertMessageAndCause(new OrderExecutionException("execution", cause), "execution");
    }

    @Test
    @DisplayName("Message-only constructors keep the message")
    void messageOnlyConstructors() {
        assertEquals("trading", new TradingException("trading").getMessage());
        assertEquals("holdings", new InsufficientHoldingsException("holdings").getMessage());
        assertEquals("execution", new OrderExecutionException("execution").getMessage());
    }

    @Test
    @DisplayName("DuplicateOrderException builds its message from the order id or key")
    void duplicateOrderMessages() {
        assertEquals("Order already exists: ORD-1", new DuplicateOrderException("ORD-1").getMessage());
        assertEquals("Duplicate idempotencyKey: key-1",
            new DuplicateOrderException("key-1", "idempotencyKey").getMessage());
    }

    @Test
    @DisplayName("InvalidOrderException has a default message")
    void invalidOrderDefaultMessage() {
        assertEquals("Invalid order", new InvalidOrderException().getMessage());
    }

    private void assertMessageAndCause(Exception ex, String message) {
        assertEquals(message, ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
