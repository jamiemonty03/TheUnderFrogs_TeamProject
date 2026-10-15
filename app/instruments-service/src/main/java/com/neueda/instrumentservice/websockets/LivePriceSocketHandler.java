package com.neueda.instrumentservice.websockets;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;

/**
 * Pushes each live quote to every connected browser as it arrives.
 * Nothing is sent on connect: until a symbol ticks, the browser shows its last close.
 * Every message is a JSON array of quotes.
 */
@Component
public class LivePriceSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LivePriceSocketHandler.class);

    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_LIMIT_BYTES = 512 * 1024;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public LivePriceSocketHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(),
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_LIMIT_BYTES));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    public void broadcast(LivePriceResponse quote) {
        if (sessions.isEmpty()) {
            return;
        }
        try {
            TextMessage message = toMessage(List.of(quote));
            sessions.values().forEach(session -> send(session, message));
        } catch (JsonProcessingException e) {
            log.warn("Couldn't serialise the quote for {}: {}", quote.symbol(), e.getMessage());
        }
    }

    private TextMessage toMessage(List<LivePriceResponse> quotes) throws JsonProcessingException {
        return new TextMessage(mapper.writeValueAsString(quotes));
    }

    private void send(WebSocketSession session, TextMessage message) {
        try {
            session.sendMessage(message);
        } catch (IOException | RuntimeException e) {
            log.debug("Dropping live-price session {}: {}", session.getId(), e.getMessage());
            sessions.remove(session.getId());
        }
    }
}
