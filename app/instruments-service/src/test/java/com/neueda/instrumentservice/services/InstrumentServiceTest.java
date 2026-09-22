package com.neueda.instrumentservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.instrumentservice.dtos.responses.InstrumentResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.models.Instrument;
import com.neueda.instrumentservice.repositories.InstrumentRepository;

@ExtendWith(MockitoExtension.class)
public class InstrumentServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    private InstrumentService instrumentService;

    @BeforeEach
    public void setUp() {
        instrumentService = new InstrumentService(instrumentRepository);
    }

    @Test
    @DisplayName("getAllInstruments: Returns all instruments mapped to InstrumentResponse")
    public void testGetAllInstruments() {
        Instrument apple = new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);
        Instrument tesla = new Instrument("TSLA", "Tesla Inc.", "EQUITY", "USD", "NASDAQ", false);
        when(instrumentRepository.findAll()).thenReturn(List.of(apple, tesla));

        List<InstrumentResponse> result = instrumentService.getAllInstruments();

        assertEquals(2, result.size());
        assertEquals(new InstrumentResponse("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true), result.get(0));
        assertEquals(new InstrumentResponse("TSLA", "Tesla Inc.", "EQUITY", "USD", "NASDAQ", false), result.get(1));
    }

    @Test
    @DisplayName("getAllInstruments: Returns empty list when repository has no instruments")
    public void testGetAllInstrumentsEmpty() {
        when(instrumentRepository.findAll()).thenReturn(List.of());

        assertTrue(instrumentService.getAllInstruments().isEmpty());
    }

    @Test
    @DisplayName("getInstrumentBySymbol: Returns matching instrument as InstrumentResponse")
    public void testGetInstrumentBySymbolFound() throws InstrumentNotFoundException {
        Instrument apple = new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);
        when(instrumentRepository.findBySymbol("AAPL")).thenReturn(Optional.of(apple));

        InstrumentResponse result = instrumentService.getInstrumentBySymbol("AAPL");

        assertEquals(new InstrumentResponse("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true), result);
    }

    @Test
    @DisplayName("getInstrumentBySymbol: Throws InstrumentNotFoundException when symbol is missing")
    public void testGetInstrumentBySymbolNotFound() {
        when(instrumentRepository.findBySymbol("ZZZZ")).thenReturn(Optional.empty());

        assertThrows(InstrumentNotFoundException.class,
                () -> instrumentService.getInstrumentBySymbol("ZZZZ"));
    }
}
