package com.neueda.instrumentservice.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class StockTest {

    private Stock newStock() {
        return new Stock("AAPL", "Apple Inc.", new BigDecimal("150.25"), LocalDateTime.now(),
                "Technology", "Consumer Electronics", "USA", 150000,
                new BigDecimal("1.25"), new BigDecimal("28.5"), new BigDecimal("25.0"),
                new BigDecimal("5.5"), new BigDecimal("0.92"), new BigDecimal("0.15"),
                new BigDecimal("35.2"), new BigDecimal("1.45"), new BigDecimal("2500000000000"),
                new BigDecimal("15500000000"), new BigDecimal("383000000000"), "https://apple.com");
    }

    @Test
    @DisplayName("constructor: Populates both inherited Asset fields and Stock-specific fields")
    public void testConstructorAssignsAllFields() {
        Stock stock = newStock();

        assertEquals("AAPL", stock.getSymbol());
        assertEquals("Apple Inc.", stock.getName());
        assertEquals(new BigDecimal("150.25"), stock.getPrice());
        assertEquals("Technology", stock.getSector());
        assertEquals("Consumer Electronics", stock.getIndustry());
        assertEquals("USA", stock.getCountry());
        assertEquals(150000, stock.getFullTimeEmployees());
        assertEquals(new BigDecimal("1.25"), stock.getBeta());
        assertEquals(new BigDecimal("28.5"), stock.getTrailingPe());
        assertEquals("https://apple.com", stock.getWebsite());
    }

    @Test
    @DisplayName("constructor: Inherits Asset's validation and rejects a blank symbol")
    public void testConstructorRejectsBlankSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Stock("   ", "Apple Inc.", new BigDecimal("150.25"), LocalDateTime.now(),
                        "Technology", "Consumer Electronics", "USA", 150000,
                        null, null, null, null, null, null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("constructor: Inherits Asset's validation and rejects a negative price")
    public void testConstructorRejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class, () ->
                new Stock("AAPL", "Apple Inc.", new BigDecimal("-1.00"), LocalDateTime.now(),
                        "Technology", "Consumer Electronics", "USA", 150000,
                        null, null, null, null, null, null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("no-arg constructor: Leaves fields unset until populated via setters")
    public void testNoArgConstructorThenSetters() {
        Stock stock = new Stock();
        assertNull(stock.getSymbol());
        assertNull(stock.getSector());

        stock.setSymbol("MSFT");
        stock.setSector("Technology");
        stock.setMarketCap(new BigDecimal("3000000000000"));

        assertEquals("MSFT", stock.getSymbol());
        assertEquals("Technology", stock.getSector());
        assertEquals(new BigDecimal("3000000000000"), stock.getMarketCap());
    }

    @Test
    @DisplayName("toString: Identifies itself as a Stock and includes key fields")
    public void testToStringIncludesStockFields() {
        Stock stock = newStock();

        String result = stock.toString();

        assertTrue(result.startsWith("Stock{"));
        assertTrue(result.contains("sector='Technology'"));
        assertTrue(result.contains("symbol='AAPL'"));
    }
}
