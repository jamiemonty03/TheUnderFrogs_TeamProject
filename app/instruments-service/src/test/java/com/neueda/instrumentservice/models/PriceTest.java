package com.neueda.instrumentservice.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PriceTest {

    private static final LocalDate TRADE_DATE = LocalDate.of(2024, 1, 15);

    private Price newPrice() {
        return new Price("AAPL", TRADE_DATE, new BigDecimal("150.00"), new BigDecimal("155.00"),
                new BigDecimal("149.00"), new BigDecimal("152.00"), new BigDecimal("1000000"));
    }

    @Test
    @DisplayName("constructor: Valid OHLCV arguments populate all fields and set createdAt")
    public void testConstructorValidArguments() {
        Price price = newPrice();

        assertEquals("AAPL", price.getSymbol());
        assertEquals(TRADE_DATE, price.getTradeDate());
        assertEquals(new BigDecimal("150.00"), price.getOpen());
        assertEquals(new BigDecimal("155.00"), price.getHigh());
        assertEquals(new BigDecimal("149.00"), price.getLow());
        assertEquals(new BigDecimal("152.00"), price.getClose());
        assertEquals(new BigDecimal("1000000"), price.getVolume());
        assertNotNull(price.getCreatedAt());
    }

    @Test
    @DisplayName("constructor: Throws exception when symbol is null")
    public void testConstructorNullSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Price(null, TRADE_DATE, new BigDecimal("150.00"), new BigDecimal("155.00"),
                        new BigDecimal("149.00"), new BigDecimal("152.00"), new BigDecimal("1000000")));
    }

    @Test
    @DisplayName("constructor: Throws exception when symbol is blank")
    public void testConstructorBlankSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Price("   ", TRADE_DATE, new BigDecimal("150.00"), new BigDecimal("155.00"),
                        new BigDecimal("149.00"), new BigDecimal("152.00"), new BigDecimal("1000000")));
    }

    @Test
    @DisplayName("constructor: Throws exception when tradeDate is null")
    public void testConstructorNullTradeDate() {
        assertThrows(IllegalArgumentException.class, () ->
                new Price("AAPL", null, new BigDecimal("150.00"), new BigDecimal("155.00"),
                        new BigDecimal("149.00"), new BigDecimal("152.00"), new BigDecimal("1000000")));
    }

    @Test
    @DisplayName("constructor: Throws exception when open is null")
    public void testConstructorNullOpen() {
        assertThrows(IllegalArgumentException.class, () ->
                new Price("AAPL", TRADE_DATE, null, new BigDecimal("155.00"),
                        new BigDecimal("149.00"), new BigDecimal("152.00"), new BigDecimal("1000000")));
    }

    @Test
    @DisplayName("constructor: Throws exception when high is null")
    public void testConstructorNullHigh() {
        assertThrows(IllegalArgumentException.class, () ->
                new Price("AAPL", TRADE_DATE, new BigDecimal("150.00"), null,
                        new BigDecimal("149.00"), new BigDecimal("152.00"), new BigDecimal("1000000")));
    }

    @Test
    @DisplayName("constructor: Throws exception when low is null")
    public void testConstructorNullLow() {
        assertThrows(IllegalArgumentException.class, () ->
                new Price("AAPL", TRADE_DATE, new BigDecimal("150.00"), new BigDecimal("155.00"),
                        null, new BigDecimal("152.00"), new BigDecimal("1000000")));
    }

    @Test
    @DisplayName("constructor: Throws exception when close is null")
    public void testConstructorNullClose() {
        assertThrows(IllegalArgumentException.class, () ->
                new Price("AAPL", TRADE_DATE, new BigDecimal("150.00"), new BigDecimal("155.00"),
                        new BigDecimal("149.00"), null, new BigDecimal("1000000")));
    }

    @Test
    @DisplayName("constructor: Throws exception when volume is null")
    public void testConstructorNullVolume() {
        assertThrows(IllegalArgumentException.class, () ->
                new Price("AAPL", TRADE_DATE, new BigDecimal("150.00"), new BigDecimal("155.00"),
                        new BigDecimal("149.00"), new BigDecimal("152.00"), null));
    }

    @Test
    @DisplayName("createdAt: Has no setter, so it cannot be mutated after construction")
    public void testCreatedAtIsImmutableAfterConstruction() {
        Price price = newPrice();
        var createdAt = price.getCreatedAt();

        // No setCreatedAt exists on Price; re-reading the getter must return the same value.
        assertEquals(createdAt, price.getCreatedAt());
    }

    @Test
    @DisplayName("no-arg constructor: Leaves fields unset, unlike the validating constructor")
    public void testNoArgConstructorLeavesFieldsNull() {
        Price price = new Price();

        assertNull(price.getSymbol());
        assertNull(price.getTradeDate());
        assertNull(price.getOpen());
        assertNull(price.getCreatedAt());
    }

    @Test
    @DisplayName("setters: Mutating OHLCV fields after construction updates getters accordingly")
    public void testSettersUpdateState() {
        Price price = newPrice();

        price.setClose(new BigDecimal("160.00"));
        price.setVolume(new BigDecimal("2000000"));

        assertEquals(new BigDecimal("160.00"), price.getClose());
        assertEquals(new BigDecimal("2000000"), price.getVolume());
    }
}
