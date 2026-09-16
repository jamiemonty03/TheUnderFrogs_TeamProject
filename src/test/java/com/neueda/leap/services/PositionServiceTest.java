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
        // Start with 100 shares at $50 average cost
        position = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
    }
    
    @Test
    public void testApplyBuy() {
        // Buy 50 more shares at $60
        positionService.applyBuy(position, new BigDecimal("50"), new BigDecimal("60"));
        
        // Should have 150 shares
        assertEquals(new BigDecimal("150"), position.getQuantity());
        
        // Average cost: (100*50 + 50*60) / 150 = 8000 / 150 = 53.33
        assertTrue(position.getAverageCost().compareTo(new BigDecimal("53.3")) > 0);
        assertTrue(position.getAverageCost().compareTo(new BigDecimal("53.4")) < 0);
    }
    
    @Test
    public void testApplySell() {
        // Sell 30 shares
        positionService.applySell(position, new BigDecimal("30"));
        
        // Should have 70 shares left
        assertEquals(new BigDecimal("70"), position.getQuantity());
        
        // Average cost unchanged
        assertEquals(new BigDecimal("50"), position.getAverageCost());
    }
    
    @Test
    public void testSellMoreThanHolding() {
        // Try to sell 150 shares (only have 100)
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
}