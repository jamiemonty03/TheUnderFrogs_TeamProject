package com.neueda.instrumentservice.services;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;

/**
 * Latest live quote per symbol, filled from the market-data topic.
 * In memory only: it is rebuilt from the topic on startup.
 */
@Service
public class LivePriceCache {

    private final Map<String, LivePriceResponse> quotes = new ConcurrentHashMap<>();

    public void update(LivePriceResponse quote) {
        quotes.merge(quote.symbol(), quote,
                (held, incoming) -> incoming.quoteTime().isAfter(held.quoteTime()) ? incoming : held);
    }


    public List<LivePriceResponse> getAll() {
        return quotes.values().stream().sorted(Comparator.comparing(LivePriceResponse::symbol)).toList();
    }

    public List<LivePriceResponse> get(Collection<String> symbols) {
        return symbols.stream().distinct().map(quotes::get).filter(Objects::nonNull).toList();
    }
}
