package com.neueda.instrumentservice.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;
import com.neueda.instrumentservice.services.LivePriceCache;

public class MarketDataListenerTest {

    private final LivePriceCache cache = new LivePriceCache();
    private final MarketDataListener listener =
            new MarketDataListener(cache, new ObjectMapper().findAndRegisterModules());

    // The envelope trade-executor's MarketDataPoller publishes.
    private static String event(String eventType, String payload) {
        return """
                {"eventId":"e-1","eventType":"%s","occurredAt":"2026-10-13T14:30:01Z","key":"AAPL","payload":%s}
                """.formatted(eventType, payload);
    }

    private static final String AAPL_PAYLOAD = """
            {"symbol":"AAPL","price":189.85,"bid":189.80,"ask":189.90,"currency":"USD","quoteTime":"2026-10-13T14:30:00Z"}
            """;

    @Test
    void cachesAPriceUpdatedEvent() {
        listener.onMessage(event("PRICE_UPDATED", AAPL_PAYLOAD));

        List<LivePriceResponse> quotes = cache.get(List.of("AAPL"));
        assertEquals(1, quotes.size());
        assertEquals(new BigDecimal("189.85"), quotes.get(0).price());
        assertEquals(new BigDecimal("189.80"), quotes.get(0).bid());
        assertEquals(new BigDecimal("189.90"), quotes.get(0).ask());
        assertEquals(Instant.parse("2026-10-13T14:30:00Z"), quotes.get(0).quoteTime());
    }

    @Test
    void ignoresOtherEventTypes() {
        listener.onMessage(event("ORDER_PLACED", AAPL_PAYLOAD));

        assertTrue(cache.get(List.of("AAPL")).isEmpty());
    }

    @Test
    void skipsEventsMissingAPriceOrQuoteTime() {
        listener.onMessage(event("PRICE_UPDATED", "{\"symbol\":\"AAPL\",\"bid\":189.80}"));

        assertTrue(cache.get(List.of("AAPL")).isEmpty());
    }

    @Test
    void skipsUnreadableMessagesWithoutThrowing() {
        listener.onMessage("not json");
        listener.onMessage(event("PRICE_UPDATED", AAPL_PAYLOAD));

        assertEquals(1, cache.get(List.of("AAPL")).size());
    }
}
