package com.neueda.tradeexecutor.services;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.neueda.tradeexecutor.clients.AlpacaMarketDataClient;
import com.neueda.tradeexecutor.dtos.AlpacaClock;

/** Whether orders may fill now: only while the US market is open, by Alpaca's market clock. */
@Service
public class MarketHours {

    private static final Logger log = LoggerFactory.getLogger(MarketHours.class);
    static final Duration CACHE_FOR = Duration.ofSeconds(60);

    private final AlpacaMarketDataClient alpaca;
    private final Clock clock;
    private boolean lastOpen;
    private Instant checkedAt;

    @Autowired
    public MarketHours(AlpacaMarketDataClient alpaca) {
        this(alpaca, Clock.systemUTC());
    }

    public MarketHours(AlpacaMarketDataClient alpaca, Clock clock) {
        this.alpaca = alpaca;
        this.clock = clock;
    }

    public synchronized boolean isOpen() {
        // Without Alpaca keys there's no clock or live price: behave as always open and fill at the daily close
        if (!alpaca.isConfigured()) {
            return true;
        }
        Instant now = clock.instant();
        if (checkedAt != null && Duration.between(checkedAt, now).compareTo(CACHE_FOR) < 0) {
            return lastOpen;
        }
        try {
            AlpacaClock marketClock = alpaca.marketClock();
            lastOpen = marketClock != null && marketClock.isOpen();
            checkedAt = now;
            return lastOpen;
        } catch (Exception e) {
            // Don't cache a failure: treat as closed so orders wait, and ask again next time
            log.warn("Alpaca market clock unavailable, treating the market as closed: {}", e.getMessage());
            return false;
        }
    }
}
