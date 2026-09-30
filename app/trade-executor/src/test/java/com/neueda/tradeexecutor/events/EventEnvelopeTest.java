package com.neueda.tradeexecutor.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EventEnvelopeTest {

    private static final String EVENT_ID = "7f3c9a2e-1b4d-4c8e-9a6f-2d5e8b1c3a70";
    private static final Instant NOW = Instant.parse("2026-09-28T10:15:00Z");
    private static final String ACCOUNT_ID = "ACC0001";
    private static final Map<String, Object> PAYLOAD = Map.of("symbol", "ACME", "quantity", 100);

    @Test
    void validEnvelope_isCreated() {
        EventEnvelope<Map<String, Object>> envelope =
                new EventEnvelope<>(EVENT_ID, EventTypes.ORDER_PLACED, NOW, ACCOUNT_ID, PAYLOAD);

        assertEquals(EVENT_ID, envelope.eventId());
        assertEquals(EventTypes.ORDER_PLACED, envelope.eventType());
        assertEquals(NOW, envelope.occurredAt());
        assertEquals(ACCOUNT_ID, envelope.key());
        assertEquals(PAYLOAD, envelope.payload());
    }

    @Test
    void of_fillsInEventIdAndOccurredAt() {
        EventEnvelope<Map<String, Object>> envelope =
                EventEnvelope.of(EventTypes.ORDER_PLACED, ACCOUNT_ID, PAYLOAD);

        assertNotNull(envelope.eventId());
        assertNotNull(envelope.occurredAt());
    }

    @Test
    void missingEventId_isRejected() {
        assertRejected("eventId",
                () -> new EventEnvelope<>(null, EventTypes.ORDER_PLACED, NOW, ACCOUNT_ID, PAYLOAD));
    }

    @Test
    void missingEventType_isRejected() {
        assertRejected("eventType",
                () -> new EventEnvelope<>(EVENT_ID, null, NOW, ACCOUNT_ID, PAYLOAD));
    }

    @Test
    void missingOccurredAt_isRejected() {
        assertRejected("occurredAt",
                () -> new EventEnvelope<>(EVENT_ID, EventTypes.ORDER_PLACED, null, ACCOUNT_ID, PAYLOAD));
    }

    @Test
    void missingKey_isRejected() {
        assertRejected("key",
                () -> new EventEnvelope<>(EVENT_ID, EventTypes.ORDER_PLACED, NOW, null, PAYLOAD));
    }

    @Test
    void missingPayload_isRejected() {
        assertRejected("payload",
                () -> new EventEnvelope<>(EVENT_ID, EventTypes.ORDER_PLACED, NOW, ACCOUNT_ID, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankTextFields_areRejected(String blank) {
        assertRejected("eventId",
                () -> new EventEnvelope<>(blank, EventTypes.ORDER_PLACED, NOW, ACCOUNT_ID, PAYLOAD));
        assertRejected("eventType",
                () -> new EventEnvelope<>(EVENT_ID, blank, NOW, ACCOUNT_ID, PAYLOAD));
        assertRejected("key",
                () -> new EventEnvelope<>(EVENT_ID, EventTypes.ORDER_PLACED, NOW, blank, PAYLOAD));
    }

    private static void assertRejected(String field, Runnable createEnvelope) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, createEnvelope::run);
        assertEquals(field + " is required", ex.getMessage());
    }
}
