package com.neueda.instrumentservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.neueda.instrumentservice.repositories.TrackedTickerRepository;

@ExtendWith(MockitoExtension.class)
public class InstrumentServiceTest {

    @Mock
    private InstrumentRepository instrumentRepository;

    @Mock
    private TrackedTickerRepository trackedTickerRepository;

    private InstrumentService instrumentService;

    @BeforeEach
    public void setUp() {
        instrumentService = new InstrumentService(instrumentRepository, trackedTickerRepository);
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

    @Test
    @DisplayName("deleteInstrument: Deletes the instrument and deactivates its tracked_tickers row")
    public void testDeleteInstrumentFound() throws InstrumentNotFoundException {
        when(instrumentRepository.exists("AAPL")).thenReturn(true);
        when(trackedTickerRepository.deactivate("AAPL")).thenReturn(1);

        instrumentService.deleteInstrument("AAPL");

        verify(instrumentRepository, times(1)).delete("AAPL");
        verify(trackedTickerRepository, times(1)).deactivate("AAPL");
    }

    @Test
    @DisplayName("deleteInstrument: Still deletes without error when the symbol isn't in tracked_tickers")
    public void testDeleteInstrumentNotTracked() {
        when(instrumentRepository.exists("CUSTOM")).thenReturn(true);
        when(trackedTickerRepository.deactivate("CUSTOM")).thenReturn(0);

        assertDoesNotThrow(() -> instrumentService.deleteInstrument("CUSTOM"));

        verify(instrumentRepository, times(1)).delete("CUSTOM");
        verify(trackedTickerRepository, times(1)).deactivate("CUSTOM");
    }

    @Test
    @DisplayName("deleteInstrument: Throws InstrumentNotFoundException and does not delete when missing")
    public void testDeleteInstrumentNotFound() {
        when(instrumentRepository.exists("ZZZZ")).thenReturn(false);

        assertThrows(InstrumentNotFoundException.class,
                () -> instrumentService.deleteInstrument("ZZZZ"));
        verify(instrumentRepository, never()).delete(any());
        verify(trackedTickerRepository, never()).deactivate(anyString());
    }
}
