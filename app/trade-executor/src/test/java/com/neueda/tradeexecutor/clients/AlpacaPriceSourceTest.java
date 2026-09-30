package com.neueda.tradeexecutor.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import com.neueda.tradeexecutor.dtos.AlpacaQuote;
import com.neueda.tradeexecutor.dtos.InstrumentDto;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.services.QuoteValidator;

@ExtendWith(MockitoExtension.class)
class AlpacaPriceSourceTest {

    private static final Instant NOW = Instant.parse("2026-09-30T15:00:00Z");
    private static final InstrumentDto AAPL = new InstrumentDto("AAPL", "Equity", true);
    private static final BigDecimal DAILY_CLOSE = new BigDecimal("318.00");

    @Mock private AlpacaMarketDataClient alpaca;
    @Mock private InstrumentsPriceSource fallback;

    private AlpacaPriceSource priceSource;

    @BeforeEach
    void setUp() {
        QuoteValidator validator = new QuoteValidator(Clock.fixed(NOW, ZoneOffset.UTC));
        priceSource = new AlpacaPriceSource(alpaca, validator, fallback);
    }

    private void alpacaQuotes(String bid, String ask) {
        when(alpaca.isConfigured()).thenReturn(true);
        when(alpaca.latestQuotes(List.of("AAPL"))).thenReturn(
                Map.of("AAPL", new AlpacaQuote(new BigDecimal(bid), new BigDecimal(ask), NOW.minusSeconds(1))));
    }

    @Test
    void buyIsPricedAtTheAsk() {
        alpacaQuotes("150.00", "150.10");

        assertEquals(new BigDecimal("150.10"), priceSource.getPrice(AAPL, OrderSide.BUY).orElseThrow());
        verifyNoInteractions(fallback);
    }

    @Test
    void sellIsPricedAtTheBid() {
        alpacaQuotes("150.00", "150.10");

        assertEquals(new BigDecimal("150.00"), priceSource.getPrice(AAPL, OrderSide.SELL).orElseThrow());
        verifyNoInteractions(fallback);
    }

    @Test
    void unusableQuoteFallsBackToDailyClose() {
        alpacaQuotes("313.32", "347.03");   // real after-hours quote, ~10% spread
        when(fallback.getPrice(AAPL, OrderSide.BUY)).thenReturn(Optional.of(DAILY_CLOSE));

        assertEquals(DAILY_CLOSE, priceSource.getPrice(AAPL, OrderSide.BUY).orElseThrow());
    }

    @Test
    void alpacaErrorFallsBackToDailyClose() {
        when(alpaca.isConfigured()).thenReturn(true);
        when(alpaca.latestQuotes(List.of("AAPL"))).thenThrow(new ResourceAccessException("timed out"));
        when(fallback.getPrice(AAPL, OrderSide.BUY)).thenReturn(Optional.of(DAILY_CLOSE));

        assertEquals(DAILY_CLOSE, priceSource.getPrice(AAPL, OrderSide.BUY).orElseThrow());
    }

    @Test
    void withoutKeysAlpacaIsNotCalled() {
        when(alpaca.isConfigured()).thenReturn(false);
        when(fallback.getPrice(AAPL, OrderSide.BUY)).thenReturn(Optional.of(DAILY_CLOSE));

        assertEquals(DAILY_CLOSE, priceSource.getPrice(AAPL, OrderSide.BUY).orElseThrow());
        verify(alpaca, never()).latestQuotes(anyList());
    }
}
