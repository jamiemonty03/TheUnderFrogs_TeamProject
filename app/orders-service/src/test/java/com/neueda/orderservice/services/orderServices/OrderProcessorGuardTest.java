package com.neueda.orderservice.services.orderServices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.exceptions.InvalidOrderException;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.services.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class OrderProcessorGuardTest {

    private final OrderService orderService = mock(OrderService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    @Test
    @DisplayName("Constructor rejects null dependencies")
    void constructorRejectsNulls() {
        assertEquals("OrderService cannot be null", assertThrows(IllegalArgumentException.class,
            () -> new OrderProcessor(null, eventPublisher)).getMessage());
        assertEquals("ApplicationEventPublisher cannot be null", assertThrows(IllegalArgumentException.class,
            () -> new OrderProcessor(orderService, null)).getMessage());
    }

    @Test
    @DisplayName("A null side is rejected before creating an order or event")
    void nullSideRejectedBeforeOrderCreation() throws Exception {
        OrderProcessor processor = new OrderProcessor(orderService, eventPublisher);
        Account account = new Account("ACC-1", "Test", BigDecimal.TEN, AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);

        assertThrows(InvalidOrderException.class, () ->
            processor.processOrder(account, instrument, null, BigDecimal.ONE, BigDecimal.TEN, "key-1"));

        verify(orderService, never()).placeOrder(any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
