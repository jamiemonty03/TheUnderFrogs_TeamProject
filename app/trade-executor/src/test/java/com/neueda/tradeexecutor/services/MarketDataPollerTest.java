package com.neueda.tradeexecutor.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.ResourceAccessException;
import com.neueda.tradeexecutor.clients.AlpacaMarketDataClient;
import com.neueda.tradeexecutor.clients.InstrumentsClient;
import com.neueda.tradeexecutor.dtos.AlpacaQuote;
import com.neueda.tradeexecutor.dtos.PriceUpdatedPayload;
import com.neueda.tradeexecutor.events.EventEnvelope;
import com.neueda.tradeexecutor.events.EventTypes;
import com.neueda.tradeexecutor.events.Topics;

@ExtendWith(MockitoExtension.class)
class MarketDataPollerTest {

    private static final Instant NOW = Instant.parse("2026-09-30T15:00:00Z");

    @Mock private InstrumentsClient instrumentsClient;
    @Mock private AlpacaMarketDataClient alpaca;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private MarketDataPoller poller;

    @BeforeEach
    void setUp() {
        poller = new MarketDataPoller(instrumentsClient, alpaca,
                new QuoteValidator(Clock.fixed(NOW, ZoneOffset.UTC)), kafkaTemplate);
    }

    private void tradable(String... symbols) {
        when(alpaca.isConfigured()).thenReturn(true);
        when(instrumentsClient.getTradableSymbols()).thenReturn(List.of(symbols));
    }

    private static AlpacaQuote quote(String bid, String ask) {
        return new AlpacaQuote(new BigDecimal(bid), new BigDecimal(ask), NOW.minusSeconds(1));
    }

    @Test
    void publishesPriceUpdatedKeyedBySymbol() {
        tradable("AAPL");
        when(alpaca.latestQuotes(List.of("AAPL"))).thenReturn(Map.of("AAPL", quote("150.00", "150.10")));

        poller.poll();

        ArgumentCaptor<Object> sent = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(Topics.MARKET_DATA), eq("AAPL"), sent.capture());
        EventEnvelope<?> envelope = (EventEnvelope<?>) sent.getValue();
        assertEquals(EventTypes.PRICE_UPDATED, envelope.eventType());
        PriceUpdatedPayload payload = (PriceUpdatedPayload) envelope.payload();
        assertEquals(0, new BigDecimal("150.05").compareTo(payload.price()));
        assertEquals(new BigDecimal("150.00"), payload.bid());
        assertEquals(new BigDecimal("150.10"), payload.ask());
        assertEquals("USD", payload.currency());
    }

    @Test
    void onlyPublishesWhenBidOrAskChanges() {
        tradable("AAPL");
        when(alpaca.latestQuotes(List.of("AAPL")))
                .thenReturn(Map.of("AAPL", quote("150.00", "150.10")))
                .thenReturn(Map.of("AAPL", quote("150.00", "150.10")))
                .thenReturn(Map.of("AAPL", quote("150.02", "150.10")));

        poller.poll();
        poller.poll();
        poller.poll();

        verify(kafkaTemplate, times(2)).send(eq(Topics.MARKET_DATA), eq("AAPL"), any());
    }

    @Test
    void unusableQuotesAreNotPublished() {
        tradable("AAPL");
        when(alpaca.latestQuotes(List.of("AAPL"))).thenReturn(Map.of("AAPL", quote("313.32", "347.03")));

        poller.poll();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void failedPollIsSkippedAndTheNextPollStillPublishes() {
        tradable("AAPL");
        when(alpaca.latestQuotes(List.of("AAPL")))
                .thenThrow(new ResourceAccessException("timed out"))
                .thenReturn(Map.of("AAPL", quote("150.00", "150.10")));

        poller.poll();
        poller.poll();

        verify(kafkaTemplate).send(eq(Topics.MARKET_DATA), eq("AAPL"), any());
    }

    @Test
    void withoutKeysNothingIsCalled() {
        when(alpaca.isConfigured()).thenReturn(false);

        poller.poll();

        verifyNoInteractions(instrumentsClient, kafkaTemplate);
    }

    @Test
    void symbolsAreSplitIntoBatchesOfAtMost100() {
        assertEquals(1, MarketDataPoller.batches(symbols(30), 100).size());
        assertEquals(1, MarketDataPoller.batches(symbols(100), 100).size());
        assertEquals(2, MarketDataPoller.batches(symbols(101), 100).size());
        assertEquals(5, MarketDataPoller.batches(symbols(450), 100).size());
    }

    private static List<String> symbols(int count) {
        return IntStream.range(0, count).mapToObj(i -> "S" + i).toList();
    }
}
