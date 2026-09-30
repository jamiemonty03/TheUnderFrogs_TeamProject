package com.neueda.orderservice.events;

import java.time.Instant;
import java.util.UUID;

public record EventEnvelope<T>(
        String eventId,
        String eventType,
        Instant occurredAt,
        String key,
        T payload) {

    public EventEnvelope {
        requireText(eventId, "eventId");
        requireText(eventType, "eventType");
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        requireText(key, "key");
        if (payload == null) {
            throw new IllegalArgumentException("payload is required");
        }
    }

    public static <T> EventEnvelope<T> of(String eventType, String key, T payload) {
        return new EventEnvelope<>(UUID.randomUUID().toString(), eventType, Instant.now(), key, payload);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
