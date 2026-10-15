package com.neueda.instrumentservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;

public class LivePriceCacheTest {

    private final LivePriceCache cache = new LivePriceCache();

    private static LivePriceResponse quote(String symbol, String price, String time) {
        BigDecimal p = new BigDecimal(price);
        return new LivePriceResponse(symbol, p, p, p, Instant.parse(time));
    }

    @Test
    void returnsTheLatestQuoteForEachRequestedSymbol() {
        cache.update(quote("AAPL", "189.85", "2026-10-13T14:30:00Z"));
        cache.update(quote("TSLA", "238.75", "2026-10-13T14:30:00Z"));

        List<LivePriceResponse> quotes = cache.get(List.of("AAPL", "TSLA"));

        assertEquals(List.of("AAPL", "TSLA"), quotes.stream().map(LivePriceResponse::symbol).toList());
    }

    @Test
    void aNewerQuoteReplacesTheHeldOne() {
        cache.update(quote("AAPL", "189.85", "2026-10-13T14:30:00Z"));
        cache.update(quote("AAPL", "190.10", "2026-10-13T14:30:05Z"));

        assertEquals(new BigDecimal("190.10"), cache.get(List.of("AAPL")).get(0).price());
    }

    @Test
    void anOlderOrSameTimeQuoteIsIgnored() {
        cache.update(quote("AAPL", "190.10", "2026-10-13T14:30:05Z"));
        cache.update(quote("AAPL", "189.85", "2026-10-13T14:30:00Z"));
        cache.update(quote("AAPL", "150.00", "2026-10-13T14:30:05Z"));

        assertEquals(new BigDecimal("190.10"), cache.get(List.of("AAPL")).get(0).price());
    }

    @Test
    void getAllReturnsEveryQuoteSortedBySymbol() {
        cache.update(quote("TSLA", "238.75", "2026-10-13T14:30:00Z"));
        cache.update(quote("AAPL", "189.85", "2026-10-13T14:30:00Z"));

        assertEquals(List.of("AAPL", "TSLA"), cache.getAll().stream().map(LivePriceResponse::symbol).toList());
    }

    @Test
    void symbolsWithoutAQuoteAreLeftOut() {
        cache.update(quote("AAPL", "189.85", "2026-10-13T14:30:00Z"));

        assertEquals(1, cache.get(List.of("AAPL", "ZZZZ", "AAPL")).size());
        assertTrue(cache.get(List.of("ZZZZ")).isEmpty());
    }
}
