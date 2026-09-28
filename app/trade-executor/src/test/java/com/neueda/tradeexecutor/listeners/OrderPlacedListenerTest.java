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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.client.ResourceAccessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.neueda.tradeexecutor.services.OrderExecutionService;

@ExtendWith(MockitoExtension.class)
class OrderPlacedListenerTest {

    @Mock private OrderExecutionService executionService;
    @Mock private Acknowledgment ack;

    private OrderPlacedListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderPlacedListener(Jackson2ObjectMapperBuilder.json().build(), executionService);
    }

    private static String event(String eventType, String payload) {
        return """
            {
              "eventId": "evt-1",
              "eventType": "%s",
              "occurredAt": "2026-09-28T10:00:00Z",
              "key": "ACC-1",
              "payload": %s
            }
            """.formatted(eventType, payload);
    }

    private static final String ORDER_PLACED_PAYLOAD = """
        {
          "orderId": "ORD-1",
          "accountId": "ACC-1",
          "symbol": "AAPL",
          "side": "BUY",
          "quantity": 10,
          "price": 150.00,
          "idempotencyKey": "idem-1"
        }
        """;

    @Test
    void orderPlacedIsExecutedThenAcknowledged() throws Exception {
        listener.onMessage(event("ORDER_PLACED", ORDER_PLACED_PAYLOAD), ack);

        InOrder order = inOrder(executionService, ack);
        order.verify(executionService).execute("ORD-1");
        order.verify(ack).acknowledge();
    }

    @Test
    void extraPayloadFieldsAreIgnored() throws Exception {
        String payloadWithExtraField = """
            { "orderId": "ORD-1", "accountId": "ACC-1", "symbol": "AAPL", "side": "BUY",
              "quantity": 10, "price": 150.00, "idempotencyKey": "idem-1", "addedLater": "x" }
            """;

        listener.onMessage(event("ORDER_PLACED", payloadWithExtraField), ack);

        verify(executionService).execute("ORD-1");
        verify(ack).acknowledge();
    }

    @Test
    void otherEventTypesAreAcknowledgedWithoutExecuting() throws Exception {
        listener.onMessage(event("ORDER_CANCELLED", ORDER_PLACED_PAYLOAD), ack);

        verifyNoInteractions(executionService);
        verify(ack).acknowledge();
    }

    @Test
    void notAcknowledgedWhenExecutionFails() {
        doThrow(new ResourceAccessException("orders-service unreachable"))
                .when(executionService).execute(anyString());

        assertThrows(ResourceAccessException.class,
                () -> listener.onMessage(event("ORDER_PLACED", ORDER_PLACED_PAYLOAD), ack));

        verify(ack, never()).acknowledge();
    }

    @Test
    void malformedJsonIsNotAcknowledged() {
        assertThrows(JsonProcessingException.class, () -> listener.onMessage("not json", ack));

        verifyNoInteractions(executionService, ack);
    }

    @Test
    void envelopeMissingARequiredFieldIsNotAcknowledged() {
        String missingEventId = """
            { "eventType": "ORDER_PLACED", "occurredAt": "2026-09-28T10:00:00Z",
              "key": "ACC-1", "payload": %s }
            """.formatted(ORDER_PLACED_PAYLOAD);

        assertThrows(JsonProcessingException.class, () -> listener.onMessage(missingEventId, ack));

        verifyNoInteractions(executionService, ack);
    }
}
