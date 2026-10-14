package com.neueda.instrumentservice.listeners;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;
import com.neueda.instrumentservice.services.LivePriceCache;

/**
 * Keeps LivePriceCache up to date from the PRICE_UPDATED events trade-executor's MarketDataPoller publishes.
 * Reads the topic from the beginning on every start, because the poller only publishes when a quote changes.
 * Bad messages are logged and skipped: this is a cache, so there is nothing to retry or dead-letter.
 */
@Component
public class MarketDataListener extends AbstractConsumerSeekAware {

    static final String TOPIC = "market-data";
    static final String PRICE_UPDATED = "PRICE_UPDATED";

    private static final Logger log = LoggerFactory.getLogger(MarketDataListener.class);

    private final LivePriceCache cache;
    private final ObjectMapper mapper;

    public MarketDataListener(LivePriceCache cache, ObjectMapper mapper) {
        this.cache = cache;
        this.mapper = mapper;
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        super.onPartitionsAssigned(assignments, callback);
        callback.seekToBeginning(assignments.keySet());
    }

    @KafkaListener(topics = TOPIC)
    public void onMessage(String message) {
        try {
            Event event = mapper.readValue(message, Event.class);
            if (!PRICE_UPDATED.equals(event.eventType())) {
                return;
            }
            Payload p = event.payload();
            if (p == null || p.symbol() == null || p.price() == null || p.quoteTime() == null) {
                log.warn("Skipping PRICE_UPDATED event {} with missing fields", event.eventId());
                return;
            }
            cache.update(new LivePriceResponse(p.symbol(), p.price(), p.bid(), p.ask(), p.quoteTime()));
        } catch (Exception e) {
            log.warn("Skipping unreadable market-data message: {}", e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Event(String eventId, String eventType, Payload payload) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Payload(String symbol, BigDecimal price, BigDecimal bid, BigDecimal ask, Instant quoteTime) {}
}
