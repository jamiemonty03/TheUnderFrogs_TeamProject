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
    @DisplayName("getters: Every Stock-specific getter returns the value passed to the constructor")
    public void testAllGettersReturnConstructorValues() {
        Stock stock = newStock();

        assertEquals(new BigDecimal("25.0"), stock.getForwardPe());
        assertEquals(new BigDecimal("5.5"), stock.getTrailingEps());
        assertEquals(new BigDecimal("0.92"), stock.getDividendRate());
        assertEquals(new BigDecimal("0.15"), stock.getPayoutRatio());
        assertEquals(new BigDecimal("35.2"), stock.getPriceToBook());
        assertEquals(new BigDecimal("1.45"), stock.getReturnOnEquity());
        assertEquals(new BigDecimal("2500000000000"), stock.getMarketCap());
        assertEquals(new BigDecimal("15500000000"), stock.getSharesOutstanding());
        assertEquals(new BigDecimal("383000000000"), stock.getTotalRevenue());
    }

    @Test
    @DisplayName("setters: Every Stock-specific setter updates the value returned by its getter")
    public void testAllSettersMutateFields() {
        Stock stock = new Stock();

        stock.setIndustry("Software");
        stock.setCountry("USA");
        stock.setFullTimeEmployees(200000);
        stock.setBeta(new BigDecimal("1.10"));
        stock.setTrailingPe(new BigDecimal("30.0"));
        stock.setForwardPe(new BigDecimal("27.0"));
        stock.setTrailingEps(new BigDecimal("6.0"));
        stock.setDividendRate(new BigDecimal("1.00"));
        stock.setPayoutRatio(new BigDecimal("0.20"));
        stock.setPriceToBook(new BigDecimal("40.0"));
        stock.setReturnOnEquity(new BigDecimal("1.50"));
        stock.setSharesOutstanding(new BigDecimal("16000000000"));
        stock.setTotalRevenue(new BigDecimal("400000000000"));
        stock.setWebsite("https://example.com");

        assertEquals("Software", stock.getIndustry());
        assertEquals("USA", stock.getCountry());
        assertEquals(200000, stock.getFullTimeEmployees());
        assertEquals(new BigDecimal("1.10"), stock.getBeta());
        assertEquals(new BigDecimal("30.0"), stock.getTrailingPe());
        assertEquals(new BigDecimal("27.0"), stock.getForwardPe());
        assertEquals(new BigDecimal("6.0"), stock.getTrailingEps());
        assertEquals(new BigDecimal("1.00"), stock.getDividendRate());
        assertEquals(new BigDecimal("0.20"), stock.getPayoutRatio());
        assertEquals(new BigDecimal("40.0"), stock.getPriceToBook());
        assertEquals(new BigDecimal("1.50"), stock.getReturnOnEquity());
        assertEquals(new BigDecimal("16000000000"), stock.getSharesOutstanding());
        assertEquals(new BigDecimal("400000000000"), stock.getTotalRevenue());
        assertEquals("https://example.com", stock.getWebsite());
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
