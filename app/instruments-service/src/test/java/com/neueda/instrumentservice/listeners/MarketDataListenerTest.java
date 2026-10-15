package com.neueda.instrumentservice.listeners;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;
import com.neueda.instrumentservice.websockets.LivePriceSocketHandler;

public class MarketDataListenerTest {

    private final LivePriceSocketHandler socket = mock(LivePriceSocketHandler.class);
    private final MarketDataListener listener =
            new MarketDataListener(socket, new ObjectMapper().findAndRegisterModules());

    private static String event(String eventType, String payload) {
        return """
                {"eventId":"e-1","eventType":"%s","occurredAt":"2026-10-13T14:30:01Z","key":"AAPL","payload":%s}
                """.formatted(eventType, payload);
    }

    private static final String AAPL_PAYLOAD = """
            {"symbol":"AAPL","price":189.85,"bid":189.80,"ask":189.90,"currency":"USD","quoteTime":"2026-10-13T14:30:00Z"}
            """;

    private static final LivePriceResponse AAPL_QUOTE = new LivePriceResponse("AAPL", new BigDecimal("189.85"),
            new BigDecimal("189.80"), new BigDecimal("189.90"), Instant.parse("2026-10-13T14:30:00Z"));

    @Test
    void pushesAPriceUpdatedEventToConnectedBrowsers() {
        listener.onMessage(event("PRICE_UPDATED", AAPL_PAYLOAD));

        verify(socket).broadcast(AAPL_QUOTE);
    }

    @Test
    void ignoresOtherEventTypes() {
        listener.onMessage(event("ORDER_PLACED", AAPL_PAYLOAD));

        verifyNoInteractions(socket);
    }

    @Test
    void skipsEventsMissingAPriceOrQuoteTime() {
        listener.onMessage(event("PRICE_UPDATED", "{\"symbol\":\"AAPL\",\"bid\":189.80}"));

        verifyNoInteractions(socket);
    }

    @Test
    void skipsUnreadableMessagesWithoutThrowing() {
        listener.onMessage("not json");
        listener.onMessage(event("PRICE_UPDATED", AAPL_PAYLOAD));

        verify(socket).broadcast(AAPL_QUOTE);
    }
}
