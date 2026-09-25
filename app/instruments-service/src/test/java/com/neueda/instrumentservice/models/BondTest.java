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
    @DisplayName("getters: Every Bond-specific getter returns the value passed to the constructor")
    public void testAllGettersReturnConstructorValues() {
        Bond bond = newBond();

        assertEquals("Government", bond.getCategory());
        assertEquals("Vanguard", bond.getFundFamily());
        assertEquals("Open-End Fund", bond.getLegalType());
        assertEquals(new BigDecimal("0.05"), bond.getNetExpenseRatio());
        assertEquals(new BigDecimal("98.50"), bond.getNavPrice());
        assertEquals(new BigDecimal("50000000000"), bond.getTotalAssets());
        assertEquals(new BigDecimal("49500000000"), bond.getNetAssets());
        assertEquals(new BigDecimal("4.25"), bond.getYieldToMaturity());
        assertEquals(new BigDecimal("3.75"), bond.getCouponRate());
        assertEquals(new BigDecimal("6.5"), bond.getDuration());
        assertEquals(new BigDecimal("2.1"), bond.getYtdReturn());
        assertEquals(new BigDecimal("1.8"), bond.getThreeYearAvgReturn());
        assertEquals(new BigDecimal("2.0"), bond.getFiveYearAvgReturn());
        assertEquals(new BigDecimal("4.9"), bond.getBeta3Year());
        assertEquals(new BigDecimal("3.6"), bond.getDistributionYield());
    }

    @Test
    @DisplayName("setters: Every Bond-specific setter updates the value returned by its getter")
    public void testAllSettersMutateFields() {
        Bond bond = new Bond();

        bond.setFundFamily("Fidelity");
        bond.setLegalType("Closed-End Fund");
        bond.setNetExpenseRatio(new BigDecimal("0.10"));
        bond.setNavPrice(new BigDecimal("99.00"));
        bond.setTotalAssets(new BigDecimal("1000"));
        bond.setNetAssets(new BigDecimal("900"));
        bond.setYieldToMaturity(new BigDecimal("5.0"));
        bond.setDuration(new BigDecimal("7.0"));
        bond.setYtdReturn(new BigDecimal("3.0"));
        bond.setThreeYearAvgReturn(new BigDecimal("2.5"));
        bond.setFiveYearAvgReturn(new BigDecimal("2.2"));
        bond.setBeta3Year(new BigDecimal("1.1"));
        bond.setDistributionYield(new BigDecimal("4.0"));

        assertEquals("Fidelity", bond.getFundFamily());
        assertEquals("Closed-End Fund", bond.getLegalType());
        assertEquals(new BigDecimal("0.10"), bond.getNetExpenseRatio());
        assertEquals(new BigDecimal("99.00"), bond.getNavPrice());
        assertEquals(new BigDecimal("1000"), bond.getTotalAssets());
        assertEquals(new BigDecimal("900"), bond.getNetAssets());
        assertEquals(new BigDecimal("5.0"), bond.getYieldToMaturity());
        assertEquals(new BigDecimal("7.0"), bond.getDuration());
        assertEquals(new BigDecimal("3.0"), bond.getYtdReturn());
        assertEquals(new BigDecimal("2.5"), bond.getThreeYearAvgReturn());
        assertEquals(new BigDecimal("2.2"), bond.getFiveYearAvgReturn());
        assertEquals(new BigDecimal("1.1"), bond.getBeta3Year());
        assertEquals(new BigDecimal("4.0"), bond.getDistributionYield());
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
