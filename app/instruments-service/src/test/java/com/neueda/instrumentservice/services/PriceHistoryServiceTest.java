package com.neueda.instrumentservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.mappers.InstrumentMapper;
import com.neueda.instrumentservice.mappers.PriceHistoryMapper;

@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock
    private PriceHistoryMapper priceHistoryMapper;

    @Mock
    private InstrumentMapper instrumentMapper;

    private PriceHistoryService service;

    @BeforeEach
    void setUp() {
        service = new PriceHistoryService(priceHistoryMapper, instrumentMapper);
    }

    @Test
    void returnsTheHistoryForAKnownSymbol() throws Exception {
        when(instrumentMapper.exists("AAPL")).thenReturn(true);
        when(priceHistoryMapper.findRecent("AAPL", 63)).thenReturn(List.of());

        assertThat(service.getHistory("AAPL", 63)).isEmpty();
        verify(priceHistoryMapper).findRecent("AAPL", 63);
    }

    @Test
    void keepsTheNumberOfDaysSensible() throws Exception {
        when(instrumentMapper.exists("AAPL")).thenReturn(true);

        service.getHistory("AAPL", 0);
        service.getHistory("AAPL", 100_000);

        verify(priceHistoryMapper).findRecent("AAPL", 1);
        verify(priceHistoryMapper).findRecent("AAPL", PriceHistoryService.MAX_DAYS);
    }

    @Test
    void unknownSymbolThrowsNotFound() {
        when(instrumentMapper.exists("NOPE")).thenReturn(false);

        assertThatThrownBy(() -> service.getHistory("NOPE", 252)).isInstanceOf(InstrumentNotFoundException.class);
        verifyNoInteractions(priceHistoryMapper);
    }
}
