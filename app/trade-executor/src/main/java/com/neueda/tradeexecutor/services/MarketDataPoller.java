package com.neueda.tradeexecutor.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.neueda.tradeexecutor.clients.AlpacaMarketDataClient;
import com.neueda.tradeexecutor.clients.InstrumentsClient;
import com.neueda.tradeexecutor.dtos.AlpacaQuote;
import com.neueda.tradeexecutor.dtos.PriceUpdatedPayload;
import com.neueda.tradeexecutor.events.EventEnvelope;
import com.neueda.tradeexecutor.events.EventTypes;
import com.neueda.tradeexecutor.events.Topics;

@Service
public class MarketDataPoller {

    private static final Logger log = LoggerFactory.getLogger(MarketDataPoller.class);
    static final int BATCH_SIZE = 100;

    private final InstrumentsClient instrumentsClient;
    private final AlpacaMarketDataClient alpaca;
    private final QuoteValidator quoteValidator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, AlpacaQuote> lastPublished = new ConcurrentHashMap<>();

    public MarketDataPoller(InstrumentsClient instrumentsClient, AlpacaMarketDataClient alpaca,
            QuoteValidator quoteValidator, KafkaTemplate<String, Object> kafkaTemplate) {
        this.instrumentsClient = instrumentsClient;
        this.alpaca = alpaca;
        this.quoteValidator = quoteValidator;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${market-data.poll-interval-ms}", initialDelay = 10_000)
    public void poll() {
        if (!alpaca.isConfigured()) {
            return;
        }
        try {
            for (List<String> batch : batches(instrumentsClient.getTradableSymbols(), BATCH_SIZE)) {
                alpaca.latestQuotes(batch).forEach(this::publishIfChanged);
            }
        } catch (Exception e) {
            log.warn("Market-data poll failed, trying again next poll: {}", e.getMessage());
        }
    }

    private void publishIfChanged(String symbol, AlpacaQuote quote) {
        AlpacaQuote previous = lastPublished.get(symbol);
        boolean unchanged = previous != null && previous.bp().equals(quote.bp()) && previous.ap().equals(quote.ap());
        if (unchanged || quoteValidator.problemWith(quote) != null) {
            return;
        }
        PriceUpdatedPayload payload = new PriceUpdatedPayload(
                symbol, QuoteValidator.mid(quote), quote.bp(), quote.ap(), "USD", quote.t());
        kafkaTemplate.send(Topics.MARKET_DATA, symbol, EventEnvelope.of(EventTypes.PRICE_UPDATED, symbol, payload));
        lastPublished.put(symbol, quote);
    }

    static List<List<String>> batches(List<String> symbols, int size) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < symbols.size(); i += size) {
            batches.add(symbols.subList(i, Math.min(i + size, symbols.size())));
        }
        return batches;
    }
}
