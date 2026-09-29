package com.neueda.orderservice.events;

import java.time.Instant;

public record OrderEventEnvelope<T>(
        String eventId,
        String eventType,
        Instant occurredAt,
        T payload) {
}
