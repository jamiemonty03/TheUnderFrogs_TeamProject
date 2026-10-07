package com.neueda.tradeexecutor.events;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.neueda.tradeexecutor.dtos.OrderOutcomePayload;
import com.neueda.tradeexecutor.enums.OrderStatus;
import com.neueda.tradeexecutor.exceptions.TradeEventPublishException;

@Component
public class TradeEventPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TradeEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOutcome(OrderOutcomePayload payload) {
        String eventType = payload.status() == OrderStatus.FILLED ? EventTypes.ORDER_FILLED : EventTypes.ORDER_REJECTED;
        EventEnvelope<OrderOutcomePayload> event = EventEnvelope.of(eventType, payload.accountId(), payload);
        try {
            kafkaTemplate.send(Topics.TRADE_EVENTS, payload.accountId(), event).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TradeEventPublishException(payload.orderId(), e);
        } catch (ExecutionException | TimeoutException e) {
            throw new TradeEventPublishException(payload.orderId(), e);
        }
    }
}
