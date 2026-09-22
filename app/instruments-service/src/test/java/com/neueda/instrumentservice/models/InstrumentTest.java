package com.neueda.instrumentservice.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InstrumentTest {

    @Test
    @DisplayName("constructor: Valid arguments populate fields and default version/timestamps")
    public void testConstructorValidArguments() {
        Instrument instrument = new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);

        assertEquals("AAPL", instrument.getSymbol());
        assertEquals("Apple Inc.", instrument.getName());
        assertEquals("EQUITY", instrument.getAssetClass());
        assertEquals("USD", instrument.getCurrency());
        assertEquals("NASDAQ", instrument.getExchange());
        assertTrue(instrument.isTradable());
        assertEquals(0, instrument.getVersion());
        assertNotNull(instrument.getCreatedAt());
        assertNotNull(instrument.getLastUpdated());
    }

    @Test
    @DisplayName("constructor: Correctly stores tradable=false rather than defaulting to true")
    public void testConstructorNonTradableInstrument() {
        Instrument instrument = new Instrument("HALT", "Halted Corp.", "EQUITY", "USD", "NASDAQ", false);

        assertFalse(instrument.isTradable());
    }

    @Test
    @DisplayName("constructor: Throws exception when symbol is null")
    public void testConstructorNullSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instrument(null, "Apple Inc.", "EQUITY", "USD", "NASDAQ", true));
    }

    @Test
    @DisplayName("constructor: Throws exception when symbol is blank")
    public void testConstructorBlankSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instrument("   ", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true));
    }

    @Test
    @DisplayName("constructor: Throws exception when name is null")
    public void testConstructorNullName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instrument("AAPL", null, "EQUITY", "USD", "NASDAQ", true));
    }

    @Test
    @DisplayName("constructor: Throws exception when name is blank")
    public void testConstructorBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instrument("AAPL", "   ", "EQUITY", "USD", "NASDAQ", true));
    }

    @Test
    @DisplayName("constructor: Throws exception when assetClass is null")
    public void testConstructorNullAssetClass() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instrument("AAPL", "Apple Inc.", null, "USD", "NASDAQ", true));
    }

    @Test
    @DisplayName("constructor: Throws exception when assetClass is blank")
    public void testConstructorBlankAssetClass() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instrument("AAPL", "Apple Inc.", "   ", "USD", "NASDAQ", true));
    }

    @Test
    @DisplayName("constructor: Throws exception when currency is null")
    public void testConstructorNullCurrency() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instrument("AAPL", "Apple Inc.", "EQUITY", null, "NASDAQ", true));
    }

    @Test
    @DisplayName("constructor: Throws exception when currency is blank")
    public void testConstructorBlankCurrency() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instrument("AAPL", "Apple Inc.", "EQUITY", "   ", "NASDAQ", true));
    }

    @Test
    @DisplayName("constructor: Throws exception when exchange is null")
    public void testConstructorNullExchange() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", null, true));
    }

    @Test
    @DisplayName("constructor: Throws exception when exchange is blank")
    public void testConstructorBlankExchange() {
        assertThrows(IllegalArgumentException.class, () ->
                new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "   ", true));
    }

    @Test
    @DisplayName("no-arg constructor: Leaves fields unset, unlike the validating constructor")
    public void testNoArgConstructorLeavesFieldsNull() {
        Instrument instrument = new Instrument();

        assertNull(instrument.getSymbol());
        assertNull(instrument.getAssetClass());
        assertFalse(instrument.isTradable());
        assertEquals(0, instrument.getVersion());
    }

    @Test
    @DisplayName("setters: Mutating fields after construction updates getters accordingly")
    public void testSettersUpdateState() {
        Instrument instrument = new Instrument("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);

        instrument.setTradable(false);
        instrument.setVersion(4);
        instrument.setUpdatedBy("system");

        assertFalse(instrument.isTradable());
        assertEquals(4, instrument.getVersion());
        assertEquals("system", instrument.getUpdatedBy());
    }
}
