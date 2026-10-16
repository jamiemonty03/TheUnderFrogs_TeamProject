package com.neueda.instrumentservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.neueda.instrumentservice.websockets.LivePriceSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    public static final String LIVE_PRICES_PATH = "/instruments/live-prices/stream";

    private final LivePriceSocketHandler livePriceSocketHandler;

    public WebSocketConfig(LivePriceSocketHandler livePriceSocketHandler) {
        this.livePriceSocketHandler = livePriceSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(livePriceSocketHandler, LIVE_PRICES_PATH).setAllowedOrigins("*");
    }
}
