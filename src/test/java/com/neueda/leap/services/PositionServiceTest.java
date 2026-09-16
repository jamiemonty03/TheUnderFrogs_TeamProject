package com.neueda.leap.services;

import com.neueda.leap.models.Position;
import com.neueda.leap.exceptions.InsufficientHoldingsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

public class PositionServiceTest {
    
    private PositionService positionService;
    private Position position;
    
    @BeforeEach
    public void setUp() {
        positionService = new PositionService();
        position = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
    }
    
    @Test
    public void testApplyBuy() {
        positionService.applyBuy(position, new BigDecimal("50"), new BigDecimal("60"));
        
        assertEquals(new BigDecimal("150"), position.getQuantity());
        assertTrue(position.getAverageCost().compareTo(new BigDecimal("53.3")) > 0);
        assertTrue(position.getAverageCost().compareTo(new BigDecimal("53.4")) < 0);
    }
    
    @Test
    public void testApplySell() throws InsufficientHoldingsException {
        positionService.applySell(position, new BigDecimal("30"));
        
        assertEquals(new BigDecimal("70"), position.getQuantity());
        assertEquals(new BigDecimal("50"), position.getAverageCost());
    }
    
    @Test
    public void testSellMoreThanHolding() throws InsufficientHoldingsException {
        assertThrows(InsufficientHoldingsException.class, () -> {
            positionService.applySell(position, new BigDecimal("150"));
        });
    }
    
    @Test
    public void testBuyWithNullPosition() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(null, new BigDecimal("50"), new BigDecimal("60"));
        });
    }

    @Test
    public void testSellWithNullPosition() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applySell(null, new BigDecimal("50"));
        });
    }

    @Test
    public void testBuyWithZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(position, BigDecimal.ZERO, new BigDecimal("60"));
        });
    }

    @Test
    public void testBuyWithNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(position, new BigDecimal("-50"), new BigDecimal("60"));
        });
    }

    @Test
    public void testBuyWithZeroPrice() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(position, new BigDecimal("50"), BigDecimal.ZERO);
        });
    }

    @Test
    public void testBuyWithNegativePrice() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(position, new BigDecimal("50"), new BigDecimal("-60"));
        });
    }

    @Test
    public void testBuyWithNullQuantity() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(position, null, new BigDecimal("60"));
        });
    }

    @Test
    public void testBuyWithNullPrice() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(position, new BigDecimal("50"), null);
        });
    }

    @Test
    public void testSellWithZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applySell(position, BigDecimal.ZERO);
        });
    }

    @Test
    public void testSellWithNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applySell(position, new BigDecimal("-30"));
        });
    }

    @Test
    public void testSellWithNullQuantity() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applySell(position, null);
        });
    }

    @Test
    public void testSellExactQuantity() throws InsufficientHoldingsException {
        positionService.applySell(position, new BigDecimal("100"));
        assertEquals(BigDecimal.ZERO, position.getQuantity());
    }

    @Test
    public void testEqualsWithSameAccountAndSymbol() {
        Position pos1 = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        Position pos2 = new Position("ACC001", "AAPL", new BigDecimal("200"), new BigDecimal("60"));
        
        assertTrue(pos1.equals(pos2));
    }

    @Test
    public void testEqualsWithDifferentSymbol() {
        Position pos1 = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        Position pos2 = new Position("ACC001", "MSFT", new BigDecimal("100"), new BigDecimal("50"));
        
        assertFalse(pos1.equals(pos2));
    }

    @Test
    public void testEqualsWithDifferentAccount() {
        Position pos1 = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        Position pos2 = new Position("ACC002", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        
        assertFalse(pos1.equals(pos2));
    }

    @Test
    public void testEqualsWithNull() {
        Position pos1 = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        
        assertFalse(pos1.equals(null));
    }

    @Test
    public void testEqualsSameObject() {
        Position pos1 = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        
        assertTrue(pos1.equals(pos1));
    }

    @Test
    public void testHashCodeConsistency() {
        Position pos1 = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        Position pos2 = new Position("ACC001", "AAPL", new BigDecimal("200"), new BigDecimal("60"));
        
        assertTrue(pos1.equals(pos2));
        assertEquals(pos1.hashCode(), pos2.hashCode());
    }

    @Test
    public void testHashCodeDifference() {
        Position pos1 = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        Position pos2 = new Position("ACC001", "MSFT", new BigDecimal("100"), new BigDecimal("50"));
        
        assertNotEquals(pos1.hashCode(), pos2.hashCode());
    }
}