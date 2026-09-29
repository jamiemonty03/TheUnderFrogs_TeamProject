package com.neueda.orderservice.events;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventPublisher {

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
    }
}
