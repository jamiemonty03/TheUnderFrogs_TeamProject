package com.neueda.tradeexecutor.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.tradeexecutor.dtos.OrderPlacedPayload;
import com.neueda.tradeexecutor.events.EventEnvelope;
import com.neueda.tradeexecutor.events.EventTypes;
import com.neueda.tradeexecutor.events.Topics;
import com.neueda.tradeexecutor.services.OrderExecutionService;

@Component
public class OrderPlacedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedListener.class);

    private static final TypeReference<EventEnvelope<OrderPlacedPayload>> ORDER_PLACED_EVENT =
            new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final OrderExecutionService executionService;

    public OrderPlacedListener(ObjectMapper objectMapper, OrderExecutionService executionService) {
        this.objectMapper = objectMapper;
        this.executionService = executionService;
    }

    @KafkaListener(topics = Topics.ORDERS)
    public void onMessage(String message, Acknowledgment ack) throws JsonProcessingException {
        EventEnvelope<OrderPlacedPayload> event = objectMapper.readValue(message, ORDER_PLACED_EVENT);

        if (!EventTypes.ORDER_PLACED.equals(event.eventType())) {
            log.info("Ignoring {} event {}", event.eventType(), event.eventId());
            ack.acknowledge();
            return;
        }

        executionService.execute(event.payload().orderId());
        ack.acknowledge();
    }
}
