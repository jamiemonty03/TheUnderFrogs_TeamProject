package com.neueda.orderservice.services.orderServices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.exceptions.InvalidOrderException;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.services.OrderService;

class OrderProcessorGuardTest {

    private final OrderService orderService = mock(OrderService.class);
    private final BuyOrderStrategy buyStrategy = mock(BuyOrderStrategy.class);
    private final SellOrderStrategy sellStrategy = mock(SellOrderStrategy.class);

    @Test
    @DisplayName("Constructor rejects null dependencies")
    void constructorRejectsNulls() {
        assertEquals("OrderService cannot be null", assertThrows(IllegalArgumentException.class,
            () -> new OrderProcessor(null, buyStrategy, sellStrategy)).getMessage());
        assertEquals("BuyOrderStrategy cannot be null", assertThrows(IllegalArgumentException.class,
            () -> new OrderProcessor(orderService, null, sellStrategy)).getMessage());
        assertEquals("SellOrderStrategy cannot be null", assertThrows(IllegalArgumentException.class,
            () -> new OrderProcessor(orderService, buyStrategy, null)).getMessage());
    }

    @Test
    @DisplayName("A null side is rejected before any order is created")
    void nullSideRejectedBeforeOrderCreation() throws Exception {
        OrderProcessor processor = new OrderProcessor(orderService, buyStrategy, sellStrategy);
        Account account = new Account("ACC-1", "Test", BigDecimal.TEN, AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);

        assertThrows(InvalidOrderException.class, () ->
            processor.processOrder(account, instrument, null, BigDecimal.ONE, BigDecimal.TEN, "key-1"));

        verify(orderService, never()).placeOrder(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(buyStrategy, sellStrategy);
    }
}
