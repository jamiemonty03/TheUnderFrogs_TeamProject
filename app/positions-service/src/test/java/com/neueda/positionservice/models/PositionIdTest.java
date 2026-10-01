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

    @Test
    @DisplayName("PositionId: Reflexive equality (id equals itself)")
    public void testPositionIdReflexiveEquality() {
        PositionId id = new PositionId("ACC001", "AAPL");
        assertEquals(id, id);
    }

    @Test
    @DisplayName("PositionId: Symmetric equality (id1.equals(id2) implies id2.equals(id1))")
    public void testPositionIdSymmetricEquality() {
        PositionId id1 = new PositionId("ACC001", "AAPL");
        PositionId id2 = new PositionId("ACC001", "AAPL");
        
        assertTrue(id1.equals(id2) && id2.equals(id1));
    }

    @Test
    @DisplayName("PositionId: Transitive equality (id1.equals(id2) && id2.equals(id3) implies id1.equals(id3))")
    public void testPositionIdTransitiveEquality() {
        PositionId id1 = new PositionId("ACC001", "AAPL");
        PositionId id2 = new PositionId("ACC001", "AAPL");
        PositionId id3 = new PositionId("ACC001", "AAPL");
        
        assertTrue(id1.equals(id2) && id2.equals(id3) && id1.equals(id3));
    }

    @Test
    @DisplayName("PositionId: Hash code consistency across multiple calls")
    public void testPositionIdHashCodeConsistency() {
        PositionId id = new PositionId("ACC001", "AAPL");
        int hash1 = id.hashCode();
        int hash2 = id.hashCode();
        int hash3 = id.hashCode();
        
        assertEquals(hash1, hash2);
        assertEquals(hash2, hash3);
    }

    @Test
    @DisplayName("PositionId: Both null values comparison")
    public void testPositionIdBothNullValues() {
        PositionId id1 = new PositionId(null, null);
        PositionId id2 = new PositionId(null, null);
        
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("PositionId: Null accountId and different symbols")
    public void testPositionIdNullAccountDifferentSymbols() {
        PositionId id1 = new PositionId(null, "AAPL");
        PositionId id2 = new PositionId(null, "MSFT");
        
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("PositionId: Different accountIds and null symbol")
    public void testPositionIdDifferentAccountsNullSymbol() {
        PositionId id1 = new PositionId("ACC001", null);
        PositionId id2 = new PositionId("ACC002", null);
        
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("PositionId: Case sensitivity in accountId")
    public void testPositionIdCaseSensitiveAccountId() {
        PositionId id1 = new PositionId("acc001", "AAPL");
        PositionId id2 = new PositionId("ACC001", "AAPL");
        
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("PositionId: Case sensitivity in symbol")
    public void testPositionIdCaseSensitiveSymbol() {
        PositionId id1 = new PositionId("ACC001", "aapl");
        PositionId id2 = new PositionId("ACC001", "AAPL");
        
        assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("PositionId: No-arg constructor creates default id")
    public void testPositionIdNoArgConstructor() {
        PositionId id = new PositionId();
        
        assertNull(id.getAccountId());
        assertNull(id.getSymbol());
    }
}

