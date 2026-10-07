package com.neueda.tradeexecutor.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import com.neueda.tradeexecutor.dtos.OrderOutcomePayload;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.enums.OrderStatus;
import com.neueda.tradeexecutor.exceptions.TradeEventPublishException;

@ExtendWith(MockitoExtension.class)
class TradeEventPublisherTest {

    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private TradeEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TradeEventPublisher(kafkaTemplate);
    }

    private static OrderOutcomePayload outcome(OrderStatus status) {
        return new OrderOutcomePayload("ORD-1", "ACC-1", "AAPL", OrderSide.BUY, 10,
                status == OrderStatus.FILLED ? new BigDecimal("150.00") : null, status,
                status == OrderStatus.REJECTED ? "Insufficient funds" : null);
    }

    @SuppressWarnings("unchecked")
    private EventEnvelope<OrderOutcomePayload> sentEnvelope() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(Topics.TRADE_EVENTS), eq("ACC-1"), captor.capture());
        return (EventEnvelope<OrderOutcomePayload>) captor.getValue();
    }

    @Test
    void filledOutcomeIsPublishedAsOrderFilledKeyedByAccount() {
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, Object>) null));

        publisher.publishOutcome(outcome(OrderStatus.FILLED));

        EventEnvelope<OrderOutcomePayload> event = sentEnvelope();
        assertEquals(EventTypes.ORDER_FILLED, event.eventType());
        assertEquals("ACC-1", event.key());
        assertEquals(outcome(OrderStatus.FILLED), event.payload());
    }

    @Test
    void rejectedOutcomeIsPublishedAsOrderRejected() {
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, Object>) null));

        publisher.publishOutcome(outcome(OrderStatus.REJECTED));

        assertEquals(EventTypes.ORDER_REJECTED, sentEnvelope().eventType());
    }

    @Test
    void failedSendThrowsSoTheOffsetIsNotAcknowledged() {
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        assertThrows(TradeEventPublishException.class, () -> publisher.publishOutcome(outcome(OrderStatus.FILLED)));
    }
}
