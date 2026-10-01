package com.neueda.tradeexecutor.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.neueda.tradeexecutor.dtos.AlpacaQuote;

@Component
public class QuoteValidator {

    static final BigDecimal MAX_SPREAD_PERCENT = BigDecimal.valueOf(2);
    static final Duration MAX_QUOTE_AGE = Duration.ofSeconds(60);

    private final Clock clock;

    @Autowired
    public QuoteValidator() {
        this(Clock.systemUTC());
    }

    public QuoteValidator(Clock clock) {
        this.clock = clock;
    }

    public String problemWith(AlpacaQuote quote) {
        if (quote == null || quote.bp() == null || quote.ap() == null || quote.t() == null) {
            return "no quote";
        }
        if (quote.bp().signum() <= 0 || quote.ap().signum() <= 0) {
            return "missing bid or ask";
        }
        if (quote.bp().compareTo(quote.ap()) > 0) {
            return "bid " + quote.bp() + " is above ask " + quote.ap();
        }
        if (Duration.between(quote.t(), clock.instant()).compareTo(MAX_QUOTE_AGE) > 0) {
            return "quote from " + quote.t() + " is older than " + MAX_QUOTE_AGE;
        }
        BigDecimal spreadPercent = quote.ap().subtract(quote.bp()).multiply(BigDecimal.valueOf(100))
                .divide(mid(quote), 2, RoundingMode.HALF_UP);
        if (spreadPercent.compareTo(MAX_SPREAD_PERCENT) > 0) {
            return "spread " + spreadPercent + "% is above " + MAX_SPREAD_PERCENT + "%";
        }
        return null;
    }

    public static BigDecimal mid(AlpacaQuote quote) {
        return quote.bp().add(quote.ap()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }
}
