package com.neueda.instrumentservice.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class BondTest {

    private Bond newBond() {
        return new Bond("BND01", "US Treasury 10Y", new BigDecimal("98.50"), LocalDateTime.now(),
                "Government", "Vanguard", "Open-End Fund", new BigDecimal("0.05"),
                new BigDecimal("98.50"), new BigDecimal("50000000000"), new BigDecimal("49500000000"),
                new BigDecimal("4.25"), new BigDecimal("3.75"), new BigDecimal("6.5"),
                new BigDecimal("2.1"), new BigDecimal("1.8"), new BigDecimal("2.0"),
                new BigDecimal("4.9"), new BigDecimal("3.6"));
    }

    @Test
    @DisplayName("constructor: Populates both inherited Asset fields and Bond-specific fields")
    public void testConstructorAssignsAllFields() {
        Bond bond = newBond();

        assertEquals("BND01", bond.getSymbol());
        assertEquals("US Treasury 10Y", bond.getName());
        assertEquals(new BigDecimal("98.50"), bond.getPrice());
        assertEquals("Government", bond.getCategory());
        assertEquals("Vanguard", bond.getFundFamily());
        assertEquals(new BigDecimal("4.25"), bond.getYieldToMaturity());
        assertEquals(new BigDecimal("3.75"), bond.getCouponRate());
        assertEquals(new BigDecimal("6.5"), bond.getDuration());
    }

    @Test
    @DisplayName("constructor: Inherits Asset's validation and rejects a null tradeDate")
    public void testConstructorRejectsNullTradeDate() {
        assertThrows(IllegalArgumentException.class, () ->
                new Bond("BND01", "US Treasury 10Y", new BigDecimal("98.50"), null,
                        "Government", "Vanguard", "Open-End Fund", null,
                        null, null, null, null, null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("constructor: Inherits Asset's validation and rejects a blank name")
    public void testConstructorRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Bond("BND01", "   ", new BigDecimal("98.50"), LocalDateTime.now(),
                        "Government", "Vanguard", "Open-End Fund", null,
                        null, null, null, null, null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("no-arg constructor: Leaves fields unset until populated via setters")
    public void testNoArgConstructorThenSetters() {
        Bond bond = new Bond();
        assertNull(bond.getSymbol());
        assertNull(bond.getCategory());

        bond.setSymbol("BND02");
        bond.setCategory("Corporate");
        bond.setCouponRate(new BigDecimal("5.0"));

        assertEquals("BND02", bond.getSymbol());
        assertEquals("Corporate", bond.getCategory());
        assertEquals(new BigDecimal("5.0"), bond.getCouponRate());
    }

    @Test
    @DisplayName("toString: Identifies itself as a Bond and includes key fields")
    public void testToStringIncludesBondFields() {
        Bond bond = newBond();

        String result = bond.toString();

        assertTrue(result.startsWith("Bond{"));
        assertTrue(result.contains("category='Government'"));
        assertTrue(result.contains("symbol='BND01'"));
    }
}
