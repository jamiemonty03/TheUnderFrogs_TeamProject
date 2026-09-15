package com.neueda.leap.services;

import com.neueda.leap.models.Order;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.exceptions.DuplicateOrderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/** Tests for OrderService idempotency support. */
public class OrderServiceTest {
    
    private OrderService orderService;
    
    @BeforeEach
    void setUp() {
        orderService = new OrderService();
    }
    
    @Test
    void testCreateOrderStoresIdempotencyKey() throws DuplicateOrderException {
        Order order = new Order("ORDER-001", "ACC-001", "AAPL", OrderSide.BUY, 100,
                new BigDecimal("150.00"), "key-123");
        
        Order created = orderService.createOrder(order);
        
        assertEquals("key-123", created.getIdempotencyKey());
        assertTrue(orderService.getOrderByIdempotencyKey("key-123").isPresent());
    }
    
    @Test
    void testDuplicateIdempotencyKeyRejected() throws DuplicateOrderException {
        Order order1 = new Order("ORDER-001", "ACC-001", "AAPL", OrderSide.BUY, 100,
                new BigDecimal("150.00"), "key-123");
        Order order2 = new Order("ORDER-002", "ACC-001", "MSFT", OrderSide.BUY, 50,
                new BigDecimal("300.00"), "key-123");
        
        orderService.createOrder(order1);
        
        DuplicateOrderException ex = assertThrows(DuplicateOrderException.class, () -> {
            orderService.createOrder(order2);
        });
        
        assertTrue(ex.getMessage().contains("key-123"));
    }
    
    @Test
    void testDifferentIdempotencyKeysAccepted() throws DuplicateOrderException {
        Order order1 = new Order("ORDER-001", "ACC-001", "AAPL", OrderSide.BUY, 100,
                new BigDecimal("150.00"), "key-001");
        Order order2 = new Order("ORDER-002", "ACC-001", "MSFT", OrderSide.BUY, 50,
                new BigDecimal("300.00"), "key-002");
        
        orderService.createOrder(order1);
        orderService.createOrder(order2);
        
        assertTrue(orderService.getOrderByIdempotencyKey("key-001").isPresent());
        assertTrue(orderService.getOrderByIdempotencyKey("key-002").isPresent());
    }
    
    @Test
    void testNullIdempotencyKeyRejected() {
        Order order = new Order("ORDER-001", "ACC-001", "AAPL", OrderSide.BUY, 100,
                new BigDecimal("150.00"), null);
        
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(order));
    }
}
