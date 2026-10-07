package com.neueda.orderservice.events;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOrderPlaced(OrderPlacedApplicationEvent event) {
        OrderPlacedPayload payload = event.getPayload();
        EventEnvelope<OrderPlacedPayload> envelope =
                EventEnvelope.of(EventTypes.ORDER_PLACED, payload.accountId(), payload);
        kafkaTemplate.send(Topics.ORDERS, envelope.key(), envelope);
        log.info("Event published {} {} {} {} {}", kv("eventType", envelope.eventType()),
                kv("eventId", envelope.eventId()), kv("topic", Topics.ORDERS), kv("key", envelope.key()),
                kv("orderId", payload.orderId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOrderCancelled(OrderCancelledApplicationEvent event) {
        OrderCancelledPayload payload = event.getPayload();
        EventEnvelope<OrderCancelledPayload> envelope =
                EventEnvelope.of(EventTypes.ORDER_CANCELLED, payload.accountId(), payload);
        kafkaTemplate.send(Topics.TRADE_EVENTS, envelope.key(), envelope);
        log.info("Event published {} {} {} {} {}", kv("eventType", envelope.eventType()),
                kv("eventId", envelope.eventId()), kv("topic", Topics.TRADE_EVENTS), kv("key", envelope.key()),
                kv("orderId", payload.orderId()));
    }
}
