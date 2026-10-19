package com.neueda.instrumentservice.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;

class LatestQuoteCacheTest {

    private final LatestQuoteCache cache = new LatestQuoteCache();

    private static LivePriceResponse quote(String price, String time) {
        return new LivePriceResponse("AAPL", new BigDecimal(price), null, null, Instant.parse(time));
    }

    @Test
    void returnsNothingForASymbolWithNoQuote() {
        assertThat(cache.find("AAPL")).isEmpty();
    }

    @Test
    void keepsTheNewerQuote() {
        cache.record(quote("189.00", "2026-10-19T14:30:00Z"));
        cache.record(quote("190.00", "2026-10-19T14:31:00Z"));

        assertThat(cache.find("AAPL")).contains(quote("190.00", "2026-10-19T14:31:00Z"));
    }

    @Test
    void ignoresAQuoteThatArrivesOutOfOrder() {
        cache.record(quote("190.00", "2026-10-19T14:31:00Z"));
        cache.record(quote("189.00", "2026-10-19T14:30:00Z"));

        assertThat(cache.find("AAPL")).contains(quote("190.00", "2026-10-19T14:31:00Z"));
    }
}
