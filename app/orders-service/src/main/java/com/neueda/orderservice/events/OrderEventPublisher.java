package com.neueda.orderservice.events;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sends placement events only after the order transaction has committed. */
@Component
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String ordersTopic;

    public OrderEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${orders.topic:orders}") String ordersTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.ordersTopic = ordersTopic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOrderPlaced(OrderPlacedApplicationEvent event) {
        OrderEventEnvelope<OrderPlacedPayload> envelope = new OrderEventEnvelope<>(
                UUID.randomUUID().toString(),
                "ORDER_PLACED",
                Instant.now(),
                event.getPayload());
        kafkaTemplate.send(ordersTopic, event.getPayload().accountId(), envelope);
    }
}
