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
import com.neueda.instrumentservice.mappers.InstrumentMapper;
import com.neueda.instrumentservice.mappers.TrackedTickerMapper;

@ExtendWith(MockitoExtension.class)
public class InstrumentServiceTest {

    @Mock
    private InstrumentMapper instrumentMapper;

    @Mock
    private TrackedTickerMapper trackedTickerMapper;

    private InstrumentService instrumentService;

    @BeforeEach
    public void setUp() {
        instrumentService = new InstrumentService(instrumentMapper, trackedTickerMapper);
    }

    @Test
    @DisplayName("getAllInstruments: Returns all instruments mapped to InstrumentResponse")
    public void testGetAllInstruments() {
        Instrument apple = new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);
        Instrument tesla = new Instrument("TSLA", "Tesla Inc.", "EQUITY", "USD", "NASDAQ", false);
        when(instrumentMapper.findAll()).thenReturn(List.of(apple, tesla));

        List<InstrumentResponse> result = instrumentService.getAllInstruments();

        assertEquals(2, result.size());
        assertEquals(new InstrumentResponse("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true), result.get(0));
        assertEquals(new InstrumentResponse("TSLA", "Tesla Inc.", "EQUITY", "USD", "NASDAQ", false), result.get(1));
    }

    @Test
    @DisplayName("getAllInstruments: Returns empty list when repository has no instruments")
    public void testGetAllInstrumentsEmpty() {
        when(instrumentMapper.findAll()).thenReturn(List.of());

        assertTrue(instrumentService.getAllInstruments().isEmpty());
    }

    @Test
    @DisplayName("getInstrumentBySymbol: Returns matching instrument as InstrumentResponse")
    public void testGetInstrumentBySymbolFound() throws InstrumentNotFoundException {
        Instrument apple = new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);
        when(instrumentMapper.findBySymbol("AAPL")).thenReturn(Optional.of(apple));

        InstrumentResponse result = instrumentService.getInstrumentBySymbol("AAPL");

        assertEquals(new InstrumentResponse("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true), result);
    }

    @Test
    @DisplayName("getInstrumentBySymbol: Throws InstrumentNotFoundException when symbol is missing")
    public void testGetInstrumentBySymbolNotFound() {
        when(instrumentMapper.findBySymbol("ZZZZ")).thenReturn(Optional.empty());

        assertThrows(InstrumentNotFoundException.class,
                () -> instrumentService.getInstrumentBySymbol("ZZZZ"));
    }

    @Test
    @DisplayName("deleteInstrument: Deletes the instrument and deactivates its tracked_tickers row")
    public void testDeleteInstrumentFound() throws InstrumentNotFoundException {
        when(instrumentMapper.exists("AAPL")).thenReturn(true);
        when(trackedTickerMapper.deactivate("AAPL")).thenReturn(1);

        instrumentService.deleteInstrument("AAPL");

        verify(instrumentMapper, times(1)).delete("AAPL");
        verify(trackedTickerMapper, times(1)).deactivate("AAPL");
    }

    @Test
    @DisplayName("deleteInstrument: Still deletes without error when the symbol isn't in tracked_tickers")
    public void testDeleteInstrumentNotTracked() {
        when(instrumentMapper.exists("CUSTOM")).thenReturn(true);
        when(trackedTickerMapper.deactivate("CUSTOM")).thenReturn(0);

        assertDoesNotThrow(() -> instrumentService.deleteInstrument("CUSTOM"));

        verify(instrumentMapper, times(1)).delete("CUSTOM");
        verify(trackedTickerMapper, times(1)).deactivate("CUSTOM");
    }

    @Test
    @DisplayName("deleteInstrument: Throws InstrumentNotFoundException and does not delete when missing")
    public void testDeleteInstrumentNotFound() {
        when(instrumentMapper.exists("ZZZZ")).thenReturn(false);

        assertThrows(InstrumentNotFoundException.class,
                () -> instrumentService.deleteInstrument("ZZZZ"));
        verify(instrumentMapper, never()).delete(any());
        verify(trackedTickerMapper, never()).deactivate(anyString());
    }

    @Test
    @DisplayName("resolveTicker: Cleans spaces, dollar signs and case before matching a symbol exactly")
    public void testResolveTickerCleansTheSymbol() {
        Instrument microsoft = new Instrument("MSFT", "Microsoft Corporation", "EQUITY", "USD", "NASDAQ", true);
        when(instrumentMapper.findBySymbol("MSFT")).thenReturn(Optional.of(microsoft));

        assertEquals(List.of("MSFT"), symbols(instrumentService.resolveTicker("MSF T")));
        assertEquals(List.of("MSFT"), symbols(instrumentService.resolveTicker(" $msft ")));
        verify(instrumentMapper, never()).findAll();
    }

    @Test
    @DisplayName("resolveTicker: Falls back to a case-insensitive name search, sorted and capped")
    public void testResolveTickerSearchesNames() {
        when(instrumentMapper.findAll()).thenReturn(List.of(
                new Instrument("GOOGL", "Alphabet Inc. Class A", "EQUITY", "USD", "NASDAQ", true),
                new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true),
                new Instrument("GOOG", "Alphabet Inc. Class C", "EQUITY", "USD", "NASDAQ", true)));

        assertEquals(List.of("GOOG", "GOOGL"), symbols(instrumentService.resolveTicker("alphabet")));
    }

    @Test
    @DisplayName("resolveTicker: Returns at most five name matches")
    public void testResolveTickerCapsMatches() {
        when(instrumentMapper.findAll()).thenReturn(List.of("A", "B", "C", "D", "E", "F").stream()
                .map(s -> new Instrument(s, "Index Fund " + s, "ETF", "USD", "NYSE", true))
                .toList());

        assertEquals(InstrumentService.MAX_TICKER_MATCHES, instrumentService.resolveTicker("index fund").size());
    }

    @Test
    @DisplayName("resolveTicker: Returns nothing for blank, too-short or unknown queries")
    public void testResolveTickerNoMatch() {
        when(instrumentMapper.findAll()).thenReturn(List.of(
                new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true)));

        assertTrue(instrumentService.resolveTicker(null).isEmpty());
        assertTrue(instrumentService.resolveTicker("  ").isEmpty());
        assertTrue(instrumentService.resolveTicker("zz").isEmpty());
        assertTrue(instrumentService.resolveTicker("Nonexistent Corp").isEmpty());
    }

    private static List<String> symbols(List<InstrumentResponse> instruments) {
        return instruments.stream().map(InstrumentResponse::symbol).toList();
    }
}
