package com.neueda.tradeexecutor.clients;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.neueda.tradeexecutor.dtos.AlpacaQuote;

@Component
public class AlpacaMarketDataClient {

    private static final Logger log = LoggerFactory.getLogger(AlpacaMarketDataClient.class);
    static final String DATA_URL = "https://data.alpaca.markets/v2";

    record LatestQuotes(Map<String, AlpacaQuote> quotes) {}

    private final RestTemplate restTemplate;
    private final boolean configured;

    public AlpacaMarketDataClient(RestTemplateBuilder builder,
            @Value("${alpaca.key-id}") String keyId,
            @Value("${alpaca.secret-key}") String secretKey) {
        this.restTemplate = builder
                .defaultHeader("APCA-API-KEY-ID", keyId)
                .defaultHeader("APCA-API-SECRET-KEY", secretKey)
                .build();
        this.configured = !keyId.isBlank() && !secretKey.isBlank();
        if (!configured) {
            log.warn("ALPACA_KEY_ID / ALPACA_SECRET_KEY not set: using daily closes and not polling market data");
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    public Map<String, AlpacaQuote> latestQuotes(List<String> symbols) {
        LatestQuotes response = restTemplate.getForObject(
                DATA_URL + "/stocks/quotes/latest?symbols={symbols}&feed=iex",
                LatestQuotes.class, String.join(",", symbols));
        return response == null || response.quotes() == null ? Map.of() : response.quotes();
    }
}
