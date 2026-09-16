package com.neueda.leap.services;

import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.exceptions.AccountNotActiveException;
import com.neueda.leap.exceptions.DuplicateOrderException;
import com.neueda.leap.exceptions.InstrumentNotFoundException;
import com.neueda.leap.exceptions.InsufficientFundsException;
import com.neueda.leap.exceptions.InsufficientHoldingsException;
import com.neueda.leap.exceptions.TradingException;
import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.models.Order;
import com.neueda.leap.repositories.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Tests for OrderService idempotency support. */
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private PositionRepository positionRepository;

    private OrderService orderService;
    private Account account;
    private Instrument instrument;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(positionRepository);
        account = new Account("ACC-001", "Jane Doe", new BigDecimal("100000.00"), AccountStatus.ACTIVE);
        instrument = new Instrument("AAPL", "Apple Inc.", "Equity", "USD", "NASDAQ", true);
    }

    @Test
    void testPlaceOrderStoresIdempotencyKey() throws AccountNotActiveException,
            InstrumentNotFoundException, TradingException, InsufficientFundsException,
            InsufficientHoldingsException, DuplicateOrderException {
        Order created = orderService.placeOrder(account, instrument, OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("150.00"), "key-123");

        assertEquals("key-123", created.getIdempotencyKey());
        assertTrue(orderService.getOrderByIdempotencyKey("key-123").isPresent());
    }

    @Test
    void testDuplicateIdempotencyKeyRejected() throws AccountNotActiveException,
            InstrumentNotFoundException, TradingException, InsufficientFundsException,
            InsufficientHoldingsException, DuplicateOrderException {
        orderService.placeOrder(account, instrument, OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("150.00"), "key-123");

        DuplicateOrderException ex = assertThrows(DuplicateOrderException.class, () ->
                orderService.placeOrder(account, instrument, OrderSide.BUY,
                        new BigDecimal("50"), new BigDecimal("300.00"), "key-123"));

        assertTrue(ex.getMessage().contains("key-123"));
    }

    @Test
    void testDifferentIdempotencyKeysAccepted() throws AccountNotActiveException,
            InstrumentNotFoundException, TradingException, InsufficientFundsException,
            InsufficientHoldingsException, DuplicateOrderException {
        orderService.placeOrder(account, instrument, OrderSide.BUY,
                new BigDecimal("100"), new BigDecimal("150.00"), "key-001");
        orderService.placeOrder(account, instrument, OrderSide.BUY,
                new BigDecimal("50"), new BigDecimal("300.00"), "key-002");

        assertTrue(orderService.getOrderByIdempotencyKey("key-001").isPresent());
        assertTrue(orderService.getOrderByIdempotencyKey("key-002").isPresent());
    }

    @Test
    void testNullIdempotencyKeyRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                orderService.placeOrder(account, instrument, OrderSide.BUY,
                        new BigDecimal("100"), new BigDecimal("150.00"), null));
    }
}
