package com.neueda.tradeexecutor.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.Test;
import com.neueda.tradeexecutor.enums.OrderSide;

class OrderPlacedEventDeserializerTest {
    private final OrderPlacedEventDeserializer deserializer = new OrderPlacedEventDeserializer();

    @Test
    void deserializesOrderPlacedJson() {
        String json = "{\"eventId\":\"evt-1\",\"eventType\":\"ORDER_PLACED\",\"occurredAt\":\"2026-09-28T10:00:00Z\",\"key\":\"ACC-1\",\"payload\":{\"orderId\":\"ORD-1\",\"accountId\":\"ACC-1\",\"symbol\":\"AAPL\",\"side\":\"BUY\",\"quantity\":2,\"price\":10.00,\"idempotencyKey\":\"idem-1\"}}";
        var event = deserializer.deserialize("orders", json.getBytes(StandardCharsets.UTF_8));
        assertEquals("ORD-1", event.payload().orderId());
        assertEquals(OrderSide.BUY, event.payload().side());
    }

    @Test
    void malformedEnvelopeFailsDeserialization() {
        assertThrows(SerializationException.class,
                () -> deserializer.deserialize("orders", "not json".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void missingOrderIdFailsDeserialization() {
        String json = "{\"eventId\":\"evt-1\",\"eventType\":\"ORDER_PLACED\",\"occurredAt\":\"2026-09-28T10:00:00Z\",\"key\":\"ACC-1\",\"payload\":{\"accountId\":\"ACC-1\"}}";
        assertThrows(SerializationException.class,
                () -> deserializer.deserialize("orders", json.getBytes(StandardCharsets.UTF_8)));
    }
}
