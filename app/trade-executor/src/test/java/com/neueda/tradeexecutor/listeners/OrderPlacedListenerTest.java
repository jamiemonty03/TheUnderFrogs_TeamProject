package com.neueda.tradeexecutor.listeners;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.client.ResourceAccessException;
import java.time.Instant;
import com.neueda.tradeexecutor.dtos.OrderPlacedPayload;
import com.neueda.tradeexecutor.events.EventEnvelope;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.services.OrderExecutionService;

@ExtendWith(MockitoExtension.class)
class OrderPlacedListenerTest {

    @Mock private OrderExecutionService executionService;
    @Mock private Acknowledgment ack;

    private OrderPlacedListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderPlacedListener(executionService);
    }

    private static EventEnvelope<OrderPlacedPayload> event(String eventType) {
        return new EventEnvelope<>("evt-1", eventType, Instant.parse("2026-09-28T10:00:00Z"), "ACC-1",
                new OrderPlacedPayload("ORD-1", "ACC-1", "AAPL", OrderSide.BUY, 10,
                        new java.math.BigDecimal("150.00"), "idem-1"));
    }

    @Test
    void orderPlacedIsExecutedThenAcknowledged() throws Exception {
        listener.onMessage(event("ORDER_PLACED"), ack);

        InOrder order = inOrder(executionService, ack);
        order.verify(executionService).execute("ORD-1");
        order.verify(ack).acknowledge();
    }

    @Test
    void otherEventTypesAreAcknowledgedWithoutExecuting() throws Exception {
        listener.onMessage(event("ORDER_CANCELLED"), ack);

        verifyNoInteractions(executionService);
        verify(ack).acknowledge();
    }

    @Test
    void notAcknowledgedWhenExecutionFails() {
        doThrow(new ResourceAccessException("orders-service unreachable"))
                .when(executionService).execute(anyString());

        assertThrows(ResourceAccessException.class,
                () -> listener.onMessage(event("ORDER_PLACED"), ack));

        verify(ack, never()).acknowledge();
    }

}
