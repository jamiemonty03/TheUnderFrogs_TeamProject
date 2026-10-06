package com.neueda.tradeexecutor.events;

import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.neueda.tradeexecutor.dtos.OrderPlacedPayload;

/** JSON deserializer kept behind Spring Kafka's ErrorHandlingDeserializer. */
public class OrderPlacedEventDeserializer implements Deserializer<EventEnvelope<OrderPlacedPayload>> {
    private static final TypeReference<EventEnvelope<OrderPlacedPayload>> EVENT_TYPE = new TypeReference<>() {};
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public EventEnvelope<OrderPlacedPayload> deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            EventEnvelope<OrderPlacedPayload> event = mapper.readValue(data, EVENT_TYPE);
            OrderPlacedPayload payload = event.payload();
            if (payload.orderId() == null || payload.orderId().isBlank()) throw new IllegalArgumentException("payload.orderId is required");
            if (payload.accountId() == null || payload.accountId().isBlank()) throw new IllegalArgumentException("payload.accountId is required");
            if (payload.symbol() == null || payload.symbol().isBlank()) throw new IllegalArgumentException("payload.symbol is required");
            if (payload.side() == null) throw new IllegalArgumentException("payload.side is required");
            if (payload.quantity() <= 0) throw new IllegalArgumentException("payload.quantity must be positive");
            if (payload.priceLimit() == null || payload.priceLimit().signum() <= 0) throw new IllegalArgumentException("payload.priceLimit must be positive");
            if (payload.idempotencyKey() == null || payload.idempotencyKey().isBlank()) throw new IllegalArgumentException("payload.idempotencyKey is required");
            return event;
        } catch (Exception e) {
            throw new SerializationException("Invalid orders event JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}
}
