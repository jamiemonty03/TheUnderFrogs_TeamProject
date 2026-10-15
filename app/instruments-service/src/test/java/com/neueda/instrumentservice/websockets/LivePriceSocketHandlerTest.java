package com.neueda.instrumentservice.websockets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;

public class LivePriceSocketHandlerTest {

    private final LivePriceSocketHandler handler = new LivePriceSocketHandler(
            new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

    private static LivePriceResponse quote(String symbol, String price) {
        BigDecimal p = new BigDecimal(price);
        return new LivePriceResponse(symbol, p, p, p, Instant.parse("2026-10-13T14:30:00Z"));
    }

    private static WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private static TextMessage json(String body) {
        return new TextMessage(body);
    }

    @Test
    void pushesEachNewQuoteToEveryConnectedBrowser() throws Exception {
        WebSocketSession first = session("s1");
        WebSocketSession second = session("s2");
        handler.afterConnectionEstablished(first);
        handler.afterConnectionEstablished(second);

        handler.broadcast(quote("TSLA", "238.75"));

        TextMessage tsla = json("""
                [{"symbol":"TSLA","price":238.75,"bid":238.75,"ask":238.75,"quoteTime":"2026-10-13T14:30:00Z"}]""");
        verify(first).sendMessage(tsla);
        verify(second).sendMessage(tsla);
    }

    @Test
    void stopsSendingToAClosedBrowser() throws Exception {
        WebSocketSession browser = session("s1");
        handler.afterConnectionEstablished(browser);
        handler.afterConnectionClosed(browser, CloseStatus.NORMAL);

        handler.broadcast(quote("TSLA", "238.75"));

        verify(browser, never()).sendMessage(any());
    }

    @Test
    void dropsABrowserItCannotSendTo() throws Exception {
        WebSocketSession broken = session("s1");
        doThrow(new IOException("gone")).when(broken).sendMessage(any());
        handler.afterConnectionEstablished(broken);

        handler.broadcast(quote("TSLA", "238.75"));

        handler.broadcast(quote("AAPL", "189.85"));

        // Once for the first quote, which failed, and never again.
        verify(broken, times(1)).sendMessage(any());
    }
}
