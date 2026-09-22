package com.neueda.instrumentservice.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AssetTest {

    /**
     * Minimal concrete subclass used only to exercise the validation and
     * behaviour that lives on the abstract Asset superclass itself.
     */
    private static class TestAsset extends Asset {
        TestAsset() {
            super();
        }

        TestAsset(String symbol, String name, BigDecimal price, LocalDateTime tradeDate) {
            super(symbol, name, price, tradeDate);
        }
    }

    @Test
    @DisplayName("constructor: Valid arguments populate fields and default version/timestamps")
    public void testConstructorValidArguments() {
        LocalDateTime tradeDate = LocalDateTime.of(2024, 1, 15, 9, 30);
        Asset asset = new TestAsset("AAPL", "Apple Inc.", new BigDecimal("150.25"), tradeDate);

        assertEquals("AAPL", asset.getSymbol());
        assertEquals("Apple Inc.", asset.getName());
        assertEquals(new BigDecimal("150.25"), asset.getPrice());
        assertEquals(tradeDate, asset.getTradeDate());
        assertEquals(0, asset.getVersion());
        assertNotNull(asset.getCreatedAt());
        assertNotNull(asset.getLastUpdated());
    }

    @Test
    @DisplayName("constructor: Throws exception when symbol is null")
    public void testConstructorNullSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new TestAsset(null, "Apple Inc.", new BigDecimal("150.25"), LocalDateTime.now()));
    }

    @Test
    @DisplayName("constructor: Throws exception when symbol is blank")
    public void testConstructorBlankSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new TestAsset("   ", "Apple Inc.", new BigDecimal("150.25"), LocalDateTime.now()));
    }

    @Test
    @DisplayName("constructor: Throws exception when name is null")
    public void testConstructorNullName() {
        assertThrows(IllegalArgumentException.class, () ->
                new TestAsset("AAPL", null, new BigDecimal("150.25"), LocalDateTime.now()));
    }

    @Test
    @DisplayName("constructor: Throws exception when name is blank")
    public void testConstructorBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
                new TestAsset("AAPL", "   ", new BigDecimal("150.25"), LocalDateTime.now()));
    }

    @Test
    @DisplayName("constructor: Throws exception when price is null")
    public void testConstructorNullPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                new TestAsset("AAPL", "Apple Inc.", null, LocalDateTime.now()));
    }

    @Test
    @DisplayName("constructor: Throws exception when price is negative")
    public void testConstructorNegativePrice() {
        assertThrows(IllegalArgumentException.class, () ->
                new TestAsset("AAPL", "Apple Inc.", new BigDecimal("-0.01"), LocalDateTime.now()));
    }

    @Test
    @DisplayName("constructor: Accepts a price of exactly zero")
    public void testConstructorZeroPriceAllowed() {
        Asset asset = new TestAsset("HALT", "Halted Corp.", BigDecimal.ZERO, LocalDateTime.now());

        assertEquals(BigDecimal.ZERO, asset.getPrice());
    }

    @Test
    @DisplayName("constructor: Throws exception when tradeDate is null")
    public void testConstructorNullTradeDate() {
        assertThrows(IllegalArgumentException.class, () ->
                new TestAsset("AAPL", "Apple Inc.", new BigDecimal("150.25"), null));
    }

    @Test
    @DisplayName("no-arg constructor: Leaves fields unset, unlike the validating constructor")
    public void testNoArgConstructorLeavesFieldsNull() {
        Asset asset = new TestAsset();

        assertNull(asset.getSymbol());
        assertNull(asset.getPrice());
        assertNull(asset.getTradeDate());
        assertEquals(0, asset.getVersion());
    }

    @Test
    @DisplayName("toString: Uses the concrete subclass name rather than the literal 'Asset'")
    public void testToStringUsesConcreteClassName() {
        Asset asset = new TestAsset("AAPL", "Apple Inc.", new BigDecimal("150.25"), LocalDateTime.now());

        assertTrue(asset.toString().startsWith("TestAsset{"));
        assertTrue(asset.toString().contains("symbol='AAPL'"));
    }

    @Test
    @DisplayName("setters: Mutating fields after construction updates getters accordingly")
    public void testSettersUpdateState() {
        Asset asset = new TestAsset("AAPL", "Apple Inc.", new BigDecimal("150.25"), LocalDateTime.now());

        asset.setPrice(new BigDecimal("175.00"));
        asset.setVersion(2);
        asset.setUpdatedBy("system");

        assertEquals(new BigDecimal("175.00"), asset.getPrice());
        assertEquals(2, asset.getVersion());
        assertEquals("system", asset.getUpdatedBy());
    }
}
