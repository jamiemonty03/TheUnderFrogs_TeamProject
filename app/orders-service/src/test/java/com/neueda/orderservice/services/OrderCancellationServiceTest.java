package com.neueda.orderservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.events.OrderCancelledApplicationEvent;
import com.neueda.orderservice.events.OrderCancelledPayload;
import com.neueda.orderservice.exceptions.OrderNotCancellableException;
import com.neueda.orderservice.exceptions.OrderNotFoundException;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.repositories.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private OrderCancellationService cancellationService;

    @BeforeEach
    void setUp() {
        cancellationService = new OrderCancellationService(orderRepository, eventPublisher);
    }

    private static Order order(OrderStatus status) {
        Order order = new Order("ORD-1", "ACC-1", "AAPL", OrderSide.BUY, 10, new BigDecimal("150.00"), "idem-1");
        order.setOrderStatus(status);
        return order;
    }

    @Test
    void cancelsNewOrderAndPublishesOrderCancelled() throws Exception {
        when(orderRepository.updateStatusIfCurrent("ORD-1", "NEW", "CANCELLED")).thenReturn(1);
        when(orderRepository.findById("ORD-1")).thenReturn(Optional.of(order(OrderStatus.CANCELLED)));

        cancellationService.cancel("ORD-1");

        ArgumentCaptor<OrderCancelledApplicationEvent> event = ArgumentCaptor.forClass(OrderCancelledApplicationEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        OrderCancelledPayload payload = event.getValue().getPayload();
        assertEquals("ORD-1", payload.orderId());
        assertEquals("ACC-1", payload.accountId());
        assertEquals("AAPL", payload.symbol());
        assertEquals(OrderSide.BUY, payload.side());
        assertEquals(10, payload.quantity());
        assertNull(payload.fillPrice());
        assertEquals(OrderStatus.CANCELLED, payload.status());
        assertEquals("Cancelled by trader", payload.reason());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"FILLED", "REJECTED", "CANCELLED"})
    void orderNoLongerNewIsRejectedWithItsCurrentStatusAndNothingIsPublished(OrderStatus status) {
        when(orderRepository.updateStatusIfCurrent("ORD-1", "NEW", "CANCELLED")).thenReturn(0);
        when(orderRepository.findById("ORD-1")).thenReturn(Optional.of(order(status)));

        OrderNotCancellableException ex = assertThrows(OrderNotCancellableException.class,
                () -> cancellationService.cancel("ORD-1"));

        assertEquals(status, ex.getCurrentStatus());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void unknownOrderIsNotFoundAndNothingIsPublished() {
        when(orderRepository.updateStatusIfCurrent("MISSING", "NEW", "CANCELLED")).thenReturn(0);
        when(orderRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> cancellationService.cancel("MISSING"));

        verify(eventPublisher, never()).publishEvent(any());
    }
}
