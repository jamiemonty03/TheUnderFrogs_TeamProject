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

import com.neueda.instrumentservice.dtos.responses.BondResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.models.Bond;
import com.neueda.instrumentservice.repositories.BondRepository;

@ExtendWith(MockitoExtension.class)
public class BondServiceTest {

    @Mock
    private BondRepository bondRepository;

    private BondService bondService;

    private Bond newBond() {
        return new Bond("BND01", "US Treasury 10Y", new BigDecimal("98.50"), LocalDateTime.now(),
                "Government", "Vanguard", "Open-End Fund", new BigDecimal("0.05"),
                new BigDecimal("98.50"), new BigDecimal("50000000000"), new BigDecimal("49500000000"),
                new BigDecimal("4.25"), new BigDecimal("3.75"), new BigDecimal("6.5"),
                new BigDecimal("2.1"), new BigDecimal("1.8"), new BigDecimal("2.0"),
                new BigDecimal("4.9"), new BigDecimal("3.6"));
    }

    @BeforeEach
    public void setUp() {
        bondService = new BondService(bondRepository);
    }

    @Test
    @DisplayName("getAllBonds: Returns all bonds mapped to BondResponse")
    public void testGetAllBonds() {
        when(bondRepository.findAll()).thenReturn(List.of(newBond()));

        List<BondResponse> result = bondService.getAllBonds();

        assertEquals(1, result.size());
        assertEquals("BND01", result.get(0).symbol());
        assertEquals("Government", result.get(0).category());
    }

    @Test
    @DisplayName("getAllBonds: Returns empty list when repository has no bonds")
    public void testGetAllBondsEmpty() {
        when(bondRepository.findAll()).thenReturn(List.of());

        assertTrue(bondService.getAllBonds().isEmpty());
    }

    @Test
    @DisplayName("getBondBySymbol: Returns matching bond as BondResponse")
    public void testGetBondBySymbolFound() throws InstrumentNotFoundException {
        when(bondRepository.findBySymbol("BND01")).thenReturn(Optional.of(newBond()));

        BondResponse result = bondService.getBondBySymbol("BND01");

        assertEquals("BND01", result.symbol());
        assertEquals(new BigDecimal("3.75"), result.couponRate());
    }

    @Test
    @DisplayName("getBondBySymbol: Throws InstrumentNotFoundException when symbol is missing")
    public void testGetBondBySymbolNotFound() {
        when(bondRepository.findBySymbol("ZZZZ")).thenReturn(Optional.empty());

        assertThrows(InstrumentNotFoundException.class,
                () -> bondService.getBondBySymbol("ZZZZ"));
    }
}
