package com.neueda.instrumentservice.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;
import com.neueda.instrumentservice.dtos.responses.PricePointResponse;
import com.neueda.instrumentservice.dtos.responses.PriceSnapshot;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.mappers.InstrumentMapper;
import com.neueda.instrumentservice.mappers.PriceHistoryMapper;

@Service
public class PriceSnapshotService {

    static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
    static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    private final PriceHistoryMapper priceHistoryMapper;
    private final InstrumentMapper instrumentMapper;
    private final LatestQuoteCache quotes;

    public PriceSnapshotService(PriceHistoryMapper priceHistoryMapper, InstrumentMapper instrumentMapper,
            LatestQuoteCache quotes) {
        this.priceHistoryMapper = priceHistoryMapper;
        this.instrumentMapper = instrumentMapper;
        this.quotes = quotes;
    }

    public Optional<PriceSnapshot> getSnapshot(String symbol) throws InstrumentNotFoundException {
        if (!instrumentMapper.exists(symbol)) {
            throw new InstrumentNotFoundException(symbol);
        }
        List<PricePointResponse> closes = priceHistoryMapper.findRecent(symbol, 2).stream()
                .filter(point -> point.close() != null)
                .toList();
        if (closes.isEmpty()) {
            return Optional.empty();
        }
        PricePointResponse latest = closes.get(closes.size() - 1);
        BigDecimal priorClose = closes.size() > 1 ? closes.get(0).close() : null;

        Optional<LivePriceResponse> live = quotes.find(symbol);
        if (live.isPresent()) {
            LivePriceResponse quote = live.get();
            LocalDate quoteDay = quote.quoteTime().atZone(MARKET_ZONE).toLocalDate();
            if (quoteDay.isAfter(latest.date())) {
                return Optional.of(snapshot(symbol, quote.price(), latest.close(),
                        PriceSnapshot.Source.LIVE, quote.quoteTime()));
            }
            if (quoteDay.isEqual(latest.date())) {
                return Optional.of(snapshot(symbol, quote.price(), priorClose,
                        PriceSnapshot.Source.LIVE, quote.quoteTime()));
            }
        }
        return Optional.of(snapshot(symbol, latest.close(), priorClose,
                PriceSnapshot.Source.DAILY_CLOSE, closeOf(latest.date())));
    }

    private static PriceSnapshot snapshot(String symbol, BigDecimal price, BigDecimal previousClose,
            PriceSnapshot.Source source, Instant asOf) {
        return new PriceSnapshot(symbol, price, previousClose, changePercent(price, previousClose), source, asOf);
    }

    static BigDecimal changePercent(BigDecimal price, BigDecimal previousClose) {
        if (previousClose == null || previousClose.signum() == 0) {
            return null;
        }
        return price.subtract(previousClose)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousClose, 2, RoundingMode.HALF_UP);
    }

    public static Instant closeOf(LocalDate day) {
        return day.atTime(MARKET_CLOSE).atZone(MARKET_ZONE).toInstant();
    }
}
