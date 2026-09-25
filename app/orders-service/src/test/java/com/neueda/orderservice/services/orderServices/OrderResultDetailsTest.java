package com.neueda.orderservice.services.orderServices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.models.Order;

class OrderResultDetailsTest {

    @Test
    @DisplayName("A null message becomes an empty string")
    void nullMessageBecomesEmpty() {
        assertEquals("", new OrderResult(false, null, null).getMessage());
    }

    @Test
    @DisplayName("The 4-arg constructor keeps the order and rollback state")
    void keepsOrderAndRollbackState() {
        Order order = new Order("ORD-1", "ACC-1", "AAPL", OrderSide.BUY, 1, BigDecimal.TEN, "key-1");
        Object rollbackState = new Object();

        OrderResult result = new OrderResult(true, "filled", rollbackState, order);

        assertSame(order, result.getOrder());
        assertSame(rollbackState, result.getRollbackState());
    }

    @Test
    @DisplayName("The 3-arg constructor has no order")
    void threeArgConstructorHasNoOrder() {
        assertNull(new OrderResult(true, "filled", null).getOrder());
    }

    @Test
    @DisplayName("toString shows success and message")
    void toStringShowsSuccessAndMessage() {
        assertEquals("OrderResult{success=true, message=filled}", new OrderResult(true, "filled", null).toString());
    }
}
