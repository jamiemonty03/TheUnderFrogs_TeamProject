package com.neueda.positionservice.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class PositionTest {

    @Test
    @DisplayName("constructor: Valid arguments create a position with no version until it is saved")
    public void testConstructorValidArguments() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));

        assertEquals("ACC001", position.getAccountId());
        assertEquals("AAPL", position.getSymbol());
        assertEquals(new BigDecimal("10"), position.getQuantity());
        assertEquals(new BigDecimal("150.00"), position.getAverageCost());
        assertNull(position.getVersion());
    }

    @Test
    @DisplayName("constructor: Throws exception when accountId is null")
    public void testConstructorNullAccountId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Position(null, "AAPL", new BigDecimal("10"), new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("constructor: Throws exception when accountId is blank")
    public void testConstructorBlankAccountId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Position("   ", "AAPL", new BigDecimal("10"), new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("constructor: Throws exception when symbol is null")
    public void testConstructorNullSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Position("ACC001", null, new BigDecimal("10"), new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("constructor: Throws exception when symbol is blank")
    public void testConstructorBlankSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
                new Position("ACC001", "   ", new BigDecimal("10"), new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("constructor: Throws exception when quantity is null")
    public void testConstructorNullQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
                new Position("ACC001", "AAPL", null, new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("constructor: Throws exception when averageCost is null")
    public void testConstructorNullAverageCost() {
        assertThrows(IllegalArgumentException.class, () ->
                new Position("ACC001", "AAPL", new BigDecimal("10"), null));
    }

    @Test
    @DisplayName("getTotalCostBasis: Computes quantity multiplied by average cost")
    public void testGetTotalCostBasis() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));

        assertEquals(new BigDecimal("1500.00"), position.getTotalCostBasis());
    }

    @Test
    @DisplayName("getTotalCostBasis: Returns zero when quantity is zero, regardless of average cost")
    public void testGetTotalCostBasisWithZeroQuantity() {
        Position position = new Position("ACC001", "AAPL", BigDecimal.ZERO, new BigDecimal("150.00"));

        assertEquals(0, new BigDecimal("0.00").compareTo(position.getTotalCostBasis()));
    }

    @Test
    @DisplayName("getMarketValue: Computes current price multiplied by quantity, independent of average cost")
    public void testGetMarketValue() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));

        BigDecimal marketValue = position.getMarketValue(new BigDecimal("200.00"));

        assertEquals(new BigDecimal("2000.00"), marketValue);
    }

    @Test
    @DisplayName("getMarketValue: Reflects a loss when current price is below average cost")
    public void testGetMarketValueBelowCost() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));

        BigDecimal marketValue = position.getMarketValue(new BigDecimal("100.00"));

        assertEquals(new BigDecimal("1000.00"), marketValue);
        assertTrue(marketValue.compareTo(position.getTotalCostBasis()) < 0);
    }

    @Test
    @DisplayName("equals: Two positions with the same accountId and symbol are equal, even with different quantities")
    public void testEqualsSameAccountAndSymbol() {
        Position position1 = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        Position position2 = new Position("ACC001", "AAPL", new BigDecimal("999"), new BigDecimal("1.00"));

        assertEquals(position1, position2);
        assertEquals(position1.hashCode(), position2.hashCode());
    }

    @Test
    @DisplayName("equals: Positions differing by symbol are not equal")
    public void testEqualsDifferentSymbol() {
        Position position1 = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        Position position2 = new Position("ACC001", "MSFT", new BigDecimal("10"), new BigDecimal("150.00"));

        assertNotEquals(position1, position2);
    }

    @Test
    @DisplayName("equals: Positions differing by accountId are not equal")
    public void testEqualsDifferentAccountId() {
        Position position1 = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        Position position2 = new Position("ACC002", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));

        assertNotEquals(position1, position2);
    }

    @Test
    @DisplayName("equals: A position is not equal to null or an object of a different type")
    public void testEqualsNullAndDifferentType() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));

        assertNotEquals(null, position);
        assertNotEquals("not a position", position);
    }

    @Test
    @DisplayName("equals: A position is equal to itself (reflexive)")
    public void testEqualsReflexive() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));

        assertEquals(position, position);
    }

    @Test
    @DisplayName("no-arg constructor: Leaves fields unset, unlike the validating constructor")
    public void testNoArgConstructorLeavesFieldsNull() {
        Position position = new Position();

        assertNull(position.getAccountId());
        assertNull(position.getSymbol());
        assertNull(position.getQuantity());
        assertNull(position.getAverageCost());
        assertNull(position.getVersion());
    }

    @Test
    @DisplayName("onCreate: New position gets createdAt and lastUpdated set to now")
    public void testOnCreateSetsTimestamps() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));

        position.onCreate();

        assertNotNull(position.getCreatedAt());
        assertEquals(position.getCreatedAt(), position.getLastUpdated());
    }

    @Test
    @DisplayName("onUpdate: Refreshes lastUpdated and keeps createdAt")
    public void testOnUpdateKeepsCreatedAt() throws InterruptedException {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        position.onCreate();
        var createdAt = position.getCreatedAt();
        Thread.sleep(5);

        position.onUpdate();

        assertEquals(createdAt, position.getCreatedAt());
        assertTrue(position.getLastUpdated().isAfter(createdAt));
    }
}
