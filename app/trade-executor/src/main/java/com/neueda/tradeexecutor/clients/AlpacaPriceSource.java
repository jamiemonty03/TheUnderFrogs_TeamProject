package com.neueda.tradeexecutor.clients;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import com.neueda.tradeexecutor.dtos.AlpacaQuote;
import com.neueda.tradeexecutor.dtos.InstrumentDto;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.services.PriceSource;
import com.neueda.tradeexecutor.services.QuoteValidator;

@Component
@Primary
public class AlpacaPriceSource implements PriceSource {

    private static final Logger log = LoggerFactory.getLogger(AlpacaPriceSource.class);

    private final AlpacaMarketDataClient alpaca;
    private final QuoteValidator quoteValidator;
    private final InstrumentsPriceSource fallback;

    public AlpacaPriceSource(AlpacaMarketDataClient alpaca, QuoteValidator quoteValidator,
            InstrumentsPriceSource fallback) {
        this.alpaca = alpaca;
        this.quoteValidator = quoteValidator;
        this.fallback = fallback;
    }

    @Override
    public Optional<BigDecimal> getPrice(InstrumentDto instrument, OrderSide side) {
        Optional<BigDecimal> live = livePrice(instrument.symbol(), side);
        if (live.isPresent()) {
            log.info("Priced {} {} at {} from Alpaca", side, instrument.symbol(), live.get());
            return live;
        }
        Optional<BigDecimal> close = fallback.getPrice(instrument, side);
        log.info("Priced {} {} at {} from the daily close", side, instrument.symbol(), close.orElse(null));
        return close;
    }

    private Optional<BigDecimal> livePrice(String symbol, OrderSide side) {
        if (!alpaca.isConfigured()) {
            return Optional.empty();
        }
        AlpacaQuote quote;
        try {
            quote = alpaca.latestQuotes(List.of(symbol)).get(symbol);
        } catch (RestClientException e) {
            log.warn("Alpaca quote for {} unavailable: {}", symbol, e.getMessage());
            return Optional.empty();
        }
        String problem = quoteValidator.problemWith(quote);
        if (problem != null) {
            log.info("Ignoring Alpaca quote for {}: {}", symbol, problem);
            return Optional.empty();
        }
        return Optional.of(side == OrderSide.BUY ? quote.ap() : quote.bp());
    }
}
