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

import com.neueda.instrumentservice.dtos.responses.StockResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.models.Stock;
import com.neueda.instrumentservice.repositories.StockRepository;

@ExtendWith(MockitoExtension.class)
public class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    private StockService stockService;

    private Stock newStock() {
        return new Stock("AAPL", "Apple Inc.", new BigDecimal("150.25"), LocalDateTime.now(),
                "Technology", "Consumer Electronics", "USA", 150000,
                new BigDecimal("1.25"), new BigDecimal("28.5"), new BigDecimal("25.0"),
                new BigDecimal("5.5"), new BigDecimal("0.92"), new BigDecimal("0.15"),
                new BigDecimal("35.2"), new BigDecimal("1.45"), new BigDecimal("2500000000000"),
                new BigDecimal("15500000000"), new BigDecimal("383000000000"), "https://apple.com");
    }

    @BeforeEach
    public void setUp() {
        stockService = new StockService(stockRepository);
    }

    @Test
    @DisplayName("getAllStocks: Returns all stocks mapped to StockResponse")
    public void testGetAllStocks() {
        when(stockRepository.findAll()).thenReturn(List.of(newStock()));

        List<StockResponse> result = stockService.getAllStocks();

        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).symbol());
        assertEquals("Technology", result.get(0).sector());
    }

    @Test
    @DisplayName("getAllStocks: Returns empty list when repository has no stocks")
    public void testGetAllStocksEmpty() {
        when(stockRepository.findAll()).thenReturn(List.of());

        assertTrue(stockService.getAllStocks().isEmpty());
    }

    @Test
    @DisplayName("getStockBySymbol: Returns matching stock as StockResponse")
    public void testGetStockBySymbolFound() throws InstrumentNotFoundException {
        when(stockRepository.findBySymbol("AAPL")).thenReturn(Optional.of(newStock()));

        StockResponse result = stockService.getStockBySymbol("AAPL");

        assertEquals("AAPL", result.symbol());
        assertEquals("https://apple.com", result.website());
    }

    @Test
    @DisplayName("getStockBySymbol: Throws InstrumentNotFoundException when symbol is missing")
    public void testGetStockBySymbolNotFound() {
        when(stockRepository.findBySymbol("ZZZZ")).thenReturn(Optional.empty());

        assertThrows(InstrumentNotFoundException.class,
                () -> stockService.getStockBySymbol("ZZZZ"));
    }
}
