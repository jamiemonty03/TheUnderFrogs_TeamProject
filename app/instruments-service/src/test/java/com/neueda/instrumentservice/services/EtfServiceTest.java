package com.neueda.instrumentservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.instrumentservice.dtos.responses.EtfResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.models.Etf;
import com.neueda.instrumentservice.mappers.EtfMapper;

@ExtendWith(MockitoExtension.class)
public class EtfServiceTest {

    @Mock
    private EtfMapper etfMapper;

    private EtfService etfService;

    private Etf newEtf() {
        return new Etf("SPY", "SPDR S&P 500 ETF", new BigDecimal("450.00"), LocalDateTime.now(),
                "Large Blend", "State Street", "ETF", new BigDecimal("0.09"),
                new BigDecimal("450.10"), new BigDecimal("400000000000"), new BigDecimal("399000000000"),
                new BigDecimal("18.5"), new BigDecimal("10.2"), new BigDecimal("11.8"),
                new BigDecimal("1.0"), new BigDecimal("1.3"));
    }

    @BeforeEach
    public void setUp() {
        etfService = new EtfService(etfMapper);
    }

    @Test
    @DisplayName("getAllEtfs: Returns all ETFs mapped to EtfResponse")
    public void testGetAllEtfs() {
        when(etfMapper.findAll()).thenReturn(List.of(newEtf()));

        List<EtfResponse> result = etfService.getAllEtfs();

        assertEquals(1, result.size());
        assertEquals("SPY", result.get(0).symbol());
        assertEquals("Large Blend", result.get(0).category());
    }

    @Test
    @DisplayName("getAllEtfs: Returns empty list when repository has no ETFs")
    public void testGetAllEtfsEmpty() {
        when(etfMapper.findAll()).thenReturn(List.of());

        assertTrue(etfService.getAllEtfs().isEmpty());
    }

    @Test
    @DisplayName("getEtfBySymbol: Returns matching ETF as EtfResponse")
    public void testGetEtfBySymbolFound() throws InstrumentNotFoundException {
        when(etfMapper.findBySymbol("SPY")).thenReturn(Optional.of(newEtf()));

        EtfResponse result = etfService.getEtfBySymbol("SPY");

        assertEquals("SPY", result.symbol());
        assertEquals(new BigDecimal("1.3"), result.distributionYield());
    }

    @Test
    @DisplayName("getEtfBySymbol: Throws InstrumentNotFoundException when symbol is missing")
    public void testGetEtfBySymbolNotFound() {
        when(etfMapper.findBySymbol("ZZZZ")).thenReturn(Optional.empty());

        assertThrows(InstrumentNotFoundException.class,
                () -> etfService.getEtfBySymbol("ZZZZ"));
    }
}
