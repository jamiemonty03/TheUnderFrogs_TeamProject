package com.neueda.orderservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.neueda.orderservice.exceptions.InstrumentServiceUnavailableException;
import com.neueda.orderservice.models.Instrument;

@DisplayName("InstrumentDirectory Unit Tests")
class InstrumentDirectoryTest {

    private static final String URL = "http://instruments/api/instruments";

    private RestTemplate restTemplate;
    private InstrumentDirectory directory;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        directory = new InstrumentDirectory(restTemplate);
        ReflectionTestUtils.setField(directory, "instrumentsServiceUrl", URL);
    }

    @Test
    @DisplayName("Returns only the symbols in the asset class, matching case-insensitively")
    void symbolsInAssetClass() throws Exception {
        when(restTemplate.getForObject(URL, Instrument[].class)).thenReturn(new Instrument[] {
            instrument("AAPL", "Equity"), instrument("SPY", "ETF"), instrument("TLT", "ETF"), instrument("UST10", "Bond")
        });

        assertThat(directory.symbolsInAssetClass("etf")).containsExactlyInAnyOrder("SPY", "TLT");
    }

    @Test
    @DisplayName("Unknown asset class is rejected before calling instruments-service")
    void unknownAssetClass() {
        assertThatThrownBy(() -> directory.symbolsInAssetClass("Crypto"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Equity, Bond, ETF");
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Instruments-service failure is reported as unavailable")
    void instrumentsServiceDown() {
        when(restTemplate.getForObject(URL, Instrument[].class)).thenThrow(new ResourceAccessException("down"));

        assertThatThrownBy(() -> directory.symbolsInAssetClass("Equity"))
            .isInstanceOf(InstrumentServiceUnavailableException.class);
    }

    private static Instrument instrument(String symbol, String assetClass) {
        Instrument instrument = new Instrument(symbol, symbol, new BigDecimal("100"), true);
        instrument.setAssetClass(assetClass);
        return instrument;
    }
}
