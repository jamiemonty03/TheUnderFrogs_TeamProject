package com.neueda.tradeexecutor.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import com.neueda.tradeexecutor.dtos.AlpacaQuote;

class QuoteValidatorTest {

    private static final Instant NOW = Instant.parse("2026-09-30T15:00:00Z");

    private final QuoteValidator validator = new QuoteValidator(Clock.fixed(NOW, ZoneOffset.UTC));

    private static AlpacaQuote quote(String bid, String ask, Instant time) {
        return new AlpacaQuote(new BigDecimal(bid), new BigDecimal(ask), time);
    }

    @Test
    void freshTightQuoteIsUsable() {
        assertNull(validator.problemWith(quote("150.00", "150.10", NOW.minusSeconds(1))));
    }

    @Test
    void missingQuoteIsRejected() {
        assertEquals("no quote", validator.problemWith(null));
        assertEquals("no quote", validator.problemWith(new AlpacaQuote(null, new BigDecimal("150.10"), NOW)));
    }

    @Test
    void zeroBidIsRejected() {
        assertEquals("missing bid or ask", validator.problemWith(quote("0", "150.10", NOW)));
    }

    @Test
    void bidAboveAskIsRejected() {
        assertTrue(validator.problemWith(quote("150.20", "150.10", NOW)).startsWith("bid 150.20 is above ask"));
    }

    @Test
    void quoteOlderThanMaxAgeIsRejected() {
        assertTrue(validator.problemWith(quote("150.00", "150.10", NOW.minusSeconds(61))).contains("is older than"));
    }

    @Test
    void afterHoursWideSpreadIsRejected() {
        // Real IEX quote after the close: AAPL 313.32 / 347.03, about 10% of mid
        assertEquals("spread 10.21% is above 2%", validator.problemWith(quote("313.32", "347.03", NOW)));
    }

    @Test
    void midIsHalfwayBetweenBidAndAsk() {
        assertEquals(0, new BigDecimal("150.05").compareTo(QuoteValidator.mid(quote("150.00", "150.10", NOW))));
    }
}
