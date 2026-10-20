package com.neueda.tradeexecutor.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import com.neueda.tradeexecutor.clients.AlpacaMarketDataClient;
import com.neueda.tradeexecutor.dtos.AlpacaClock;

@ExtendWith(MockitoExtension.class)
class MarketHoursTest {

    private static final Instant NOW = Instant.parse("2026-10-19T15:00:00Z");
    private static final AlpacaClock OPEN = new AlpacaClock(true, NOW.plusSeconds(86_400), NOW.plusSeconds(3_600));
    private static final AlpacaClock CLOSED = new AlpacaClock(false, NOW.plusSeconds(3_600), NOW.plusSeconds(27_000));

    @Mock private AlpacaMarketDataClient alpaca;

    private Instant now;
    private MarketHours marketHours;

    @BeforeEach
    void setUp() {
        now = NOW;
        Clock clock = new Clock() {
            @Override public Instant instant() { return now; }
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        };
        marketHours = new MarketHours(alpaca, clock);
    }

    @Test
    void isOpenWhenTheClockSaysSo() {
        when(alpaca.isConfigured()).thenReturn(true);
        when(alpaca.marketClock()).thenReturn(OPEN);

        assertTrue(marketHours.isOpen());
    }

    @Test
    void isClosedWhenTheClockSaysSo() {
        when(alpaca.isConfigured()).thenReturn(true);
        when(alpaca.marketClock()).thenReturn(CLOSED);

        assertFalse(marketHours.isOpen());
    }

    @Test
    void asksTheClockAtMostOnceAMinute() {
        when(alpaca.isConfigured()).thenReturn(true);
        when(alpaca.marketClock()).thenReturn(CLOSED, OPEN);

        assertFalse(marketHours.isOpen());
        now = NOW.plus(MarketHours.CACHE_FOR).minusSeconds(1);
        assertFalse(marketHours.isOpen());
        verify(alpaca, times(1)).marketClock();

        now = NOW.plus(MarketHours.CACHE_FOR);
        assertTrue(marketHours.isOpen());
        verify(alpaca, times(2)).marketClock();
    }

    @Test
    void withoutKeysIsAlwaysOpenAndNeverAsks() {
        when(alpaca.isConfigured()).thenReturn(false);

        assertTrue(marketHours.isOpen());
        verify(alpaca, never()).marketClock();
    }

    @Test
    void treatsAnUnreachableClockAsClosedAndAsksAgainNextTime() {
        when(alpaca.isConfigured()).thenReturn(true);
        when(alpaca.marketClock()).thenThrow(new ResourceAccessException("timed out")).thenReturn(OPEN);

        assertFalse(marketHours.isOpen());
        now = NOW.plus(Duration.ofSeconds(1));
        assertTrue(marketHours.isOpen());
    }
}
