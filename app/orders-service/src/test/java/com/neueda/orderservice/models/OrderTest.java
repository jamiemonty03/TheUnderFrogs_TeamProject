package com.neueda.orderservice.models;

import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {

    private Order newOrder(BigDecimal price, int quantity) {
        return new Order("ORD001", "ACC001", "AAPL", OrderSide.BUY, quantity, price, "idem-key-1");
    }

    @Test
    @DisplayName("constructor: Valid arguments create a NEW order with zero version and timestamps set")
    public void testConstructorValidArguments() {
        Order order = new Order("ORD001", "ACC001", "AAPL", OrderSide.BUY, 10, new BigDecimal("150.00"), "idem-key-1");

        assertEquals("ORD001", order.getOrderId());
        assertEquals("ACC001", order.getAccountId());
        assertEquals("AAPL", order.getSymbol());
        assertEquals(OrderSide.BUY, order.getSide());
        assertEquals(10, order.getQuantity());
        assertEquals(new BigDecimal("150.00"), order.getPrice());
        assertEquals("idem-key-1", order.getIdempotencyKey());
        assertEquals(OrderStatus.NEW, order.getOrderStatus());
        assertEquals(0, order.getVersion());
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getLastUpdated());
    }

    @Test
    @DisplayName("constructor: Throws exception when accountId is null")
    public void testConstructorNullAccountId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ORD001", null, "AAPL", OrderSide.BUY, 10, new BigDecimal("150.00"), "idem-key-1"));
    }

    @Test
    @DisplayName("constructor: Throws exception when symbol is null")
    public void testConstructorNullSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ORD001", "ACC001", null, OrderSide.BUY, 10, new BigDecimal("150.00"), "idem-key-1"));
    }

    @Test
    @DisplayName("constructor: Throws exception when symbol is blank")
    public void testConstructorBlankSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ORD001", "ACC001", "   ", OrderSide.BUY, 10, new BigDecimal("150.00"), "idem-key-1"));
    }

    @Test
    @DisplayName("constructor: Throws exception when order side is null")
    public void testConstructorNullSide() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ORD001", "ACC001", "AAPL", null, 10, new BigDecimal("150.00"), "idem-key-1"));
    }

    @Test
    @DisplayName("constructor: Throws exception when quantity is zero")
    public void testConstructorZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () -> newOrder(new BigDecimal("150.00"), 0));
    }

    @Test
    @DisplayName("constructor: Throws exception when quantity is negative")
    public void testConstructorNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> newOrder(new BigDecimal("150.00"), -5));
    }

    @Test
    @DisplayName("constructor: Throws exception when price is null")
    public void testConstructorNullPrice() {
        assertThrows(IllegalArgumentException.class, () -> newOrder(null, 10));
    }

    @Test
    @DisplayName("constructor: Throws exception when price is below the 0.01 minimum")
    public void testConstructorPriceBelowMinimum() {
        assertThrows(IllegalArgumentException.class, () -> newOrder(new BigDecimal("0.00"), 10));
    }

    @Test
    @DisplayName("constructor: Accepts price exactly at the 0.01 minimum")
    public void testConstructorPriceAtMinimumBoundary() {
        Order order = newOrder(new BigDecimal("0.01"), 10);

        assertEquals(new BigDecimal("0.01"), order.getPrice());
    }

    @Test
    @DisplayName("constructor: Throws exception when idempotencyKey is null")
    public void testConstructorNullIdempotencyKey() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ORD001", "ACC001", "AAPL", OrderSide.BUY, 10, new BigDecimal("150.00"), null));
    }

    @Test
    @DisplayName("constructor: Throws exception when idempotencyKey is blank")
    public void testConstructorBlankIdempotencyKey() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("ORD001", "ACC001", "AAPL", OrderSide.BUY, 10, new BigDecimal("150.00"), "   "));
    }

    @Test
    @DisplayName("constructor: Throws exception when orderId is null")
    public void testConstructorNullOrderId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order(null, "ACC001", "AAPL", OrderSide.BUY, 10, new BigDecimal("150.00"), "idem-key-1"));
    }

    @Test
    @DisplayName("constructor: Throws exception when orderId is blank")
    public void testConstructorBlankOrderId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Order("   ", "ACC001", "AAPL", OrderSide.BUY, 10, new BigDecimal("150.00"), "idem-key-1"));
    }

    @Test
    @DisplayName("setOrderStatus: Transitions status away from the default NEW state")
    public void testSetOrderStatusTransition() {
        Order order = newOrder(new BigDecimal("150.00"), 10);

        order.setOrderStatus(OrderStatus.FILLED);

        assertEquals(OrderStatus.FILLED, order.getOrderStatus());
    }

    @Test
    @DisplayName("no-arg constructor: Leaves fields unset and does not run validation")
    public void testNoArgConstructorLeavesFieldsNull() {
        Order order = new Order();

        assertNull(order.getOrderId());
        assertNull(order.getAccountId());
        assertNull(order.getSymbol());
        assertNull(order.getOrderStatus());
        assertEquals(0, order.getVersion());
        assertEquals(0, order.getQuantity());
    }
}
