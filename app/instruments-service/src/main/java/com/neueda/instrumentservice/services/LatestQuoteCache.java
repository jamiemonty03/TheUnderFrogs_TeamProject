package com.neueda.instrumentservice.services;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;

@Component
public class LatestQuoteCache {

    private final Map<String, LivePriceResponse> latest = new ConcurrentHashMap<>();

    public void record(LivePriceResponse quote) {
        latest.merge(quote.symbol(), quote,
                (current, incoming) -> incoming.quoteTime().isAfter(current.quoteTime()) ? incoming : current);
    }

    public Optional<LivePriceResponse> find(String symbol) {
        return Optional.ofNullable(latest.get(symbol));
    }
}
