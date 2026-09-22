package com.neueda.instrumentservice.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class EtfTest {

    private Etf newEtf() {
        return new Etf("SPY", "SPDR S&P 500 ETF", new BigDecimal("450.00"), LocalDateTime.now(),
                "Large Blend", "State Street", "ETF", new BigDecimal("0.09"),
                new BigDecimal("450.10"), new BigDecimal("400000000000"), new BigDecimal("399000000000"),
                new BigDecimal("18.5"), new BigDecimal("10.2"), new BigDecimal("11.8"),
                new BigDecimal("1.0"), new BigDecimal("1.3"));
    }

    @Test
    @DisplayName("constructor: Populates both inherited Asset fields and Etf-specific fields")
    public void testConstructorAssignsAllFields() {
        Etf etf = newEtf();

        assertEquals("SPY", etf.getSymbol());
        assertEquals("SPDR S&P 500 ETF", etf.getName());
        assertEquals(new BigDecimal("450.00"), etf.getPrice());
        assertEquals("Large Blend", etf.getCategory());
        assertEquals("State Street", etf.getFundFamily());
        assertEquals(new BigDecimal("18.5"), etf.getYtdReturn());
        assertEquals(new BigDecimal("1.0"), etf.getBeta3Year());
        assertEquals(new BigDecimal("1.3"), etf.getDistributionYield());
    }

    @Test
    @DisplayName("constructor: Inherits Asset's validation and rejects a blank symbol")
    public void testConstructorRejectsBlankSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Etf("   ", "SPDR S&P 500 ETF", new BigDecimal("450.00"), LocalDateTime.now(),
                        "Large Blend", "State Street", "ETF", null,
                        null, null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("constructor: Inherits Asset's validation and rejects a null price")
    public void testConstructorRejectsNullPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                new Etf("SPY", "SPDR S&P 500 ETF", null, LocalDateTime.now(),
                        "Large Blend", "State Street", "ETF", null,
                        null, null, null, null, null, null, null, null));
    }

    @Test
    @DisplayName("no-arg constructor: Leaves fields unset until populated via setters")
    public void testNoArgConstructorThenSetters() {
        Etf etf = new Etf();
        assertNull(etf.getSymbol());
        assertNull(etf.getCategory());

        etf.setSymbol("QQQ");
        etf.setCategory("Large Growth");
        etf.setNetExpenseRatio(new BigDecimal("0.20"));

        assertEquals("QQQ", etf.getSymbol());
        assertEquals("Large Growth", etf.getCategory());
        assertEquals(new BigDecimal("0.20"), etf.getNetExpenseRatio());
    }

    @Test
    @DisplayName("toString: Identifies itself as an Etf and includes key fields")
    public void testToStringIncludesEtfFields() {
        Etf etf = newEtf();

        String result = etf.toString();

        assertTrue(result.startsWith("Etf{"));
        assertTrue(result.contains("category='Large Blend'"));
        assertTrue(result.contains("symbol='SPY'"));
    }
}
