package com.neueda.instrumentservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;
import com.neueda.instrumentservice.dtos.responses.PricePointResponse;
import com.neueda.instrumentservice.dtos.responses.PriceSnapshot;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.mappers.InstrumentMapper;
import com.neueda.instrumentservice.mappers.PriceHistoryMapper;

@ExtendWith(MockitoExtension.class)
class PriceSnapshotServiceTest {

    @Mock
    private PriceHistoryMapper priceHistoryMapper;

    @Mock
    private InstrumentMapper instrumentMapper;

    private LatestQuoteCache quotes;
    private PriceSnapshotService service;

    @BeforeEach
    void setUp() {
        quotes = new LatestQuoteCache();
        service = new PriceSnapshotService(priceHistoryMapper, instrumentMapper, quotes);
    }

    private static PricePointResponse close(String date, String close) {
        return new PricePointResponse(LocalDate.parse(date), null, null, null,
                close == null ? null : new BigDecimal(close), null, null, null);
    }

    private static LivePriceResponse liveQuote(String price, String time) {
        return new LivePriceResponse("MSFT", new BigDecimal(price), null, null, Instant.parse(time));
    }

    private void givenCloses(PricePointResponse... closes) {
        when(instrumentMapper.exists("MSFT")).thenReturn(true);
        when(priceHistoryMapper.findRecent("MSFT", 2)).thenReturn(List.of(closes));
    }

    @Test
    void usesTheLatestDailyCloseWhenThereIsNoLiveQuote() throws Exception {
        givenCloses(close("2026-10-15", "421.05"), close("2026-10-16", "412.37"));

        PriceSnapshot snapshot = service.getSnapshot("MSFT").orElseThrow();

        assertThat(snapshot.price()).isEqualByComparingTo("412.37");
        assertThat(snapshot.previousClose()).isEqualByComparingTo("421.05");
        assertThat(snapshot.changePercent()).isEqualByComparingTo("-2.06");
        assertThat(snapshot.priceSource()).isEqualTo(PriceSnapshot.Source.DAILY_CLOSE);
        assertThat(snapshot.asOf()).isEqualTo(Instant.parse("2026-10-16T20:00:00Z"));
    }

    @Test
    void comparesALiveQuoteFromALaterDayWithTheLatestDailyClose() throws Exception {
        givenCloses(close("2026-10-15", "421.05"), close("2026-10-16", "412.37"));
        quotes.record(liveQuote("420.00", "2026-10-19T14:32:00Z"));

        PriceSnapshot snapshot = service.getSnapshot("MSFT").orElseThrow();

        assertThat(snapshot.price()).isEqualByComparingTo("420.00");
        assertThat(snapshot.previousClose()).isEqualByComparingTo("412.37");
        assertThat(snapshot.changePercent()).isEqualByComparingTo("1.85");
        assertThat(snapshot.priceSource()).isEqualTo(PriceSnapshot.Source.LIVE);
        assertThat(snapshot.asOf()).isEqualTo(Instant.parse("2026-10-19T14:32:00Z"));
    }

    @Test
    void comparesALiveQuoteFromTheSameDayAsTheLatestCloseWithTheCloseBefore() throws Exception {
        givenCloses(close("2026-10-15", "421.05"), close("2026-10-16", "412.37"));
        quotes.record(liveQuote("413.00", "2026-10-16T21:00:00Z"));

        PriceSnapshot snapshot = service.getSnapshot("MSFT").orElseThrow();

        assertThat(snapshot.previousClose()).isEqualByComparingTo("421.05");
        assertThat(snapshot.priceSource()).isEqualTo(PriceSnapshot.Source.LIVE);
    }

    @Test
    void ignoresALiveQuoteOlderThanTheLatestDailyClose() throws Exception {
        givenCloses(close("2026-10-15", "421.05"), close("2026-10-16", "412.37"));
        quotes.record(liveQuote("400.00", "2026-10-14T14:00:00Z"));

        PriceSnapshot snapshot = service.getSnapshot("MSFT").orElseThrow();

        assertThat(snapshot.price()).isEqualByComparingTo("412.37");
        assertThat(snapshot.priceSource()).isEqualTo(PriceSnapshot.Source.DAILY_CLOSE);
    }

    @Test
    void leavesTheChangeEmptyWhenThereIsNoPreviousClose() throws Exception {
        givenCloses(close("2026-10-16", "412.37"));

        PriceSnapshot snapshot = service.getSnapshot("MSFT").orElseThrow();

        assertThat(snapshot.previousClose()).isNull();
        assertThat(snapshot.changePercent()).isNull();
    }

    @Test
    void skipsDaysWithNoClosePrice() throws Exception {
        givenCloses(close("2026-10-15", "421.05"), close("2026-10-16", null));

        PriceSnapshot snapshot = service.getSnapshot("MSFT").orElseThrow();

        assertThat(snapshot.price()).isEqualByComparingTo("421.05");
        assertThat(snapshot.previousClose()).isNull();
    }

    @Test
    void returnsNothingWhenTheInstrumentHasNoPrices() throws Exception {
        givenCloses();

        assertThat(service.getSnapshot("MSFT")).isEmpty();
    }

    @Test
    void rejectsAnUnknownSymbol() {
        when(instrumentMapper.exists("NOPE")).thenReturn(false);

        assertThatThrownBy(() -> service.getSnapshot("NOPE")).isInstanceOf(InstrumentNotFoundException.class);
        verifyNoInteractions(priceHistoryMapper);
    }

    @Test
    void doesNotDivideByAZeroPreviousClose() {
        assertThat(PriceSnapshotService.changePercent(new BigDecimal("5"), BigDecimal.ZERO)).isNull();
    }
}
