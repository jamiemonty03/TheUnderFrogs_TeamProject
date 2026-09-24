package com.neueda.orderservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.exceptions.DuplicateOrderException;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.repositories.OrderRepository;

class OrderServiceEdgeCaseTest {

    private OrderRepository orderRepository;
    private OrderService orderService;
    private Account account;
    private Instrument instrument;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderService = new OrderService(orderRepository);
        account = new Account("ACC001", "John Doe", new BigDecimal("20000.00"), AccountStatus.ACTIVE);
        instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);
    }

    @Test
    @DisplayName("Constructor rejects a null repository")
    void rejectsNullRepository() {
        assertThrows(IllegalArgumentException.class, () -> new OrderService(null));
    }

    @Test
    @DisplayName("Re-using an idempotency key throws DuplicateOrderException and saves only once")
    void rejectsDuplicateIdempotencyKey() throws Exception {
        orderService.placeOrder(account, instrument, OrderSide.BUY, BigDecimal.ONE, BigDecimal.TEN, "dup-key");

        DuplicateOrderException ex = assertThrows(DuplicateOrderException.class, () ->
            orderService.placeOrder(account, instrument, OrderSide.BUY, BigDecimal.ONE, BigDecimal.TEN, "dup-key"));

        assertEquals("Duplicate idempotencyKey: dup-key", ex.getMessage());
        verify(orderRepository, times(1)).save(org.mockito.ArgumentMatchers.any(Order.class));
    }

    @Test
    @DisplayName("getOrderByIdempotencyKey finds placed orders and is empty for unknown keys")
    void lookupByIdempotencyKey() throws Exception {
        Order placed = orderService.placeOrder(account, instrument, OrderSide.SELL, BigDecimal.ONE, BigDecimal.TEN, "key-1");

        assertSame(placed, orderService.getOrderByIdempotencyKey("key-1").orElseThrow());
        assertTrue(orderService.getOrderByIdempotencyKey("unknown").isEmpty());
    }

    @Test
    @DisplayName("saveOrder persists the order and rejects null")
    void saveOrder() {
        Order order = new Order("ORD-1", "ACC001", "AAPL", OrderSide.BUY, 1, BigDecimal.TEN, "key-2");

        assertSame(order, orderService.saveOrder(order));
        verify(orderRepository).save(order);
        assertThrows(IllegalArgumentException.class, () -> orderService.saveOrder(null));
    }
}
