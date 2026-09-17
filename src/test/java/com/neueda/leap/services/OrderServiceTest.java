package com.neueda.leap.services;

import com.neueda.leap.models.Order;
import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.repositories.InMemoryPositionRepository;
import com.neueda.leap.repositories.PositionRepository;
import com.neueda.leap.exceptions.InvalidOrderException;
import com.neueda.leap.enums.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrderService Integration Tests")
public class OrderServiceTest {
    
    private OrderService orderService;
    private PositionRepository positionRepository;

    @BeforeEach
    void setUp() {
        positionRepository = new InMemoryPositionRepository();
        orderService = new OrderService(positionRepository);
    }

    @Test
    @DisplayName("Valid order placement creates order successfully")
    void testPlaceOrderSuccess() throws Exception {
        Account account = new Account("ACC001", "John Doe", new BigDecimal("20000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);

        Order order = orderService.placeOrder(
            account,
            instrument,
            OrderSide.BUY,
            new BigDecimal("100"),
            new BigDecimal("150.25"),
            "key-001"
        );

        assertNotNull(order);
        assertEquals("AAPL", order.getSymbol());
        assertEquals(100, order.getQuantity());
        assertEquals(new BigDecimal("150.25"), order.getPrice());
        assertEquals("key-001", order.getIdempotencyKey());
        assertNotNull(order.getOrderId());
        assertEquals(OrderStatus.NEW, order.getOrderStatus());
        assertNotNull(order.getCreatedAt());
    }

    @Test
    @DisplayName("Multiple orders can be placed with different idempotency keys")
    void testPlaceOrderIdempotency() throws Exception {
        Account account = new Account("ACC001", "John Doe", new BigDecimal("20000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);

        Order order1 = orderService.placeOrder(
            account,
            instrument,
            OrderSide.BUY,
            new BigDecimal("50"),
            new BigDecimal("150.25"),
            "key-idempotent-1"
        );

        Order order2 = orderService.placeOrder(
            account,
            instrument,
            OrderSide.BUY,
            new BigDecimal("50"),
            new BigDecimal("150.25"),
            "key-idempotent-2"
        );

        assertNotNull(order1, "First order should be created successfully");
        assertNotNull(order2, "Second order should be created successfully");
        assertEquals("AAPL", order1.getSymbol());
        assertEquals("AAPL", order2.getSymbol());
    }

    @Test
    @DisplayName("Fractional share quantities are rejected")
    void rejectsFractionalQuantities() {
        Account account = new Account("ACC001", "John Doe", new BigDecimal("20000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);

        assertThrows(InvalidOrderException.class, () -> orderService.placeOrder(
            account, instrument, OrderSide.BUY, new BigDecimal("1.5"),
            new BigDecimal("150.25"), "fractional-key"
        ));
    }

    @Test
    @DisplayName("Sell orders require a positive price")
    void rejectsZeroPricedSellOrders() {
        Account account = new Account("ACC001", "John Doe", new BigDecimal("20000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);

        assertThrows(InvalidOrderException.class, () -> orderService.placeOrder(
            account, instrument, OrderSide.SELL, new BigDecimal("1"),
            BigDecimal.ZERO, "zero-price-key"
        ));
    }
}
