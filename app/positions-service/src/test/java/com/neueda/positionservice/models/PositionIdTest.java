package com.neueda.positionservice.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PositionIdTest {

    @Test
    @DisplayName("PositionId: Valid accountId and symbol create a valid id")
    public void testPositionIdValidConstructor() {
        PositionId id = new PositionId("ACC001", "AAPL");
        
        assertEquals("ACC001", id.getAccountId());
        assertEquals("AAPL", id.getSymbol());
    }

    @Test
    @DisplayName("PositionId: Allows null accountId")
    public void testPositionIdNullAccountId() {
        PositionId id = new PositionId(null, "AAPL");
        assertNull(id.getAccountId());
    }

    @Test
    @DisplayName("PositionId: Allows null symbol")
    public void testPositionIdNullSymbol() {
        PositionId id = new PositionId("ACC001", null);
        assertNull(id.getSymbol());
    }

    @Test
    @DisplayName("PositionId: Two ids with same values are equal")
    public void testPositionIdEquality() {
        PositionId id1 = new PositionId("ACC001", "AAPL");
        PositionId id2 = new PositionId("ACC001", "AAPL");
        
        assertEquals(id1, id2);
    }

    @Test
    @DisplayName("PositionId: Two ids with different accountIds are not equal")
    public void testPositionIdInequalityDifferentAccount() {
        PositionId id1 = new PositionId("ACC001", "AAPL");
        PositionId id2 = new PositionId("ACC002", "AAPL");
        
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("PositionId: Two ids with different symbols are not equal")
    public void testPositionIdInequalityDifferentSymbol() {
        PositionId id1 = new PositionId("ACC001", "AAPL");
        PositionId id2 = new PositionId("ACC001", "MSFT");
        
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("PositionId: Hash codes are equal for equal ids")
    public void testPositionIdHashCode() {
        PositionId id1 = new PositionId("ACC001", "AAPL");
        PositionId id2 = new PositionId("ACC001", "AAPL");
        
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("PositionId: Can be used in collections")
    public void testPositionIdInCollection() {
        PositionId id1 = new PositionId("ACC001", "AAPL");
        PositionId id2 = new PositionId("ACC001", "AAPL");
        PositionId id3 = new PositionId("ACC001", "MSFT");
        
        var set = new java.util.HashSet<>();
        set.add(id1);
        set.add(id2);  // Should not add duplicate
        set.add(id3);
        
        assertEquals(2, set.size());
    }

    @Test
    @DisplayName("PositionId: Null comparison returns false")
    public void testPositionIdEqualsNull() {
        PositionId id = new PositionId("ACC001", "AAPL");
        assertNotEquals(id, null);
    }

    @Test
    @DisplayName("PositionId: Different type comparison returns false")
    public void testPositionIdEqualsDifferentType() {
        PositionId id = new PositionId("ACC001", "AAPL");
        assertNotEquals(id, "string");
    }
}

