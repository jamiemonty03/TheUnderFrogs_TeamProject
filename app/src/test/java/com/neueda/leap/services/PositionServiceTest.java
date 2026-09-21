package com.neueda.leap.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.neueda.leap.exceptions.InsufficientHoldingsException;
import com.neueda.leap.models.Position;
import com.neueda.leap.repositories.InMemoryPositionRepository;

class PositionServiceTest {

    private InMemoryPositionRepository repository;
    private PositionService positionService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPositionRepository();
        positionService = new PositionService(repository);
    }

    // Tests (applyBuy / applySell) 

    @Test
    @DisplayName("applyBuy: Calculates FIFO average cost correctly")
    void testApplyBuyCalculatesFIFOAverageCost() {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        
        positionService.applyBuy(position, new BigDecimal("50"), new BigDecimal("60"));
        
        assertEquals(new BigDecimal("150"), position.getQuantity());
        assertEquals(new BigDecimal("53.3333"), position.getAverageCost());
        assertEquals(1, position.getVersion());
    }

    @Test
    @DisplayName("applyBuy: Multiple buy operations accumulate quantity and recalculate average cost")
    void testApplyBuyMultipleTimes() {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("10"), new BigDecimal("100"));
        
        positionService.applyBuy(position, new BigDecimal("10"), new BigDecimal("120"));
        positionService.applyBuy(position, new BigDecimal("5"), new BigDecimal("110"));
        
        assertEquals(new BigDecimal("25"), position.getQuantity());
        assertEquals(new BigDecimal("110.0000"), position.getAverageCost());
    }

    @Test
    @DisplayName("applySell: Reduces quantity while keeping average cost unchanged")
    void testApplySellReducesQuantityKeepsAverageCost() throws InsufficientHoldingsException {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        BigDecimal originalCost = position.getAverageCost();
        
        positionService.applySell(position, new BigDecimal("30"));
        
        assertEquals(new BigDecimal("70"), position.getQuantity());
        assertEquals(originalCost, position.getAverageCost());
        assertEquals(1, position.getVersion());
    }

    @Test
    @DisplayName("applySell: Can sell exact quantity held")
    void testApplySellExactQuantity() throws InsufficientHoldingsException {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        
        positionService.applySell(position, new BigDecimal("100"));
        
        assertEquals(BigDecimal.ZERO, position.getQuantity());
        assertEquals(new BigDecimal("50"), position.getAverageCost());
    }

    @Test
    @DisplayName("applySell: Throws exception when selling more than held")
    void testApplySellMoreThanHeldThrowsException() {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("50"), new BigDecimal("50"));
        
        assertThrows(InsufficientHoldingsException.class, () -> {
            positionService.applySell(position, new BigDecimal("100"));
        });
        
        assertEquals(new BigDecimal("50"), position.getQuantity());
    }

    @Test
    @DisplayName("applyBuy: Throws exception when position is null")
    void testApplyBuyNullPositionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(null, new BigDecimal("10"), new BigDecimal("50"));
        });
    }

    @Test
    @DisplayName("applyBuy: Throws exception when quantity is zero")
    void testApplyBuyZeroQuantityThrowsException() {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("10"), new BigDecimal("50"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(position, BigDecimal.ZERO, new BigDecimal("50"));
        });
    }

    @Test
    @DisplayName("applyBuy: Throws exception when quantity is negative")
    void testApplyBuyNegativeQuantityThrowsException() {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("10"), new BigDecimal("50"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(position, new BigDecimal("-5"), new BigDecimal("50"));
        });
    }

    @Test
    @DisplayName("applyBuy: Throws exception when price is zero")
    void testApplyBuyZeroPriceThrowsException() {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("10"), new BigDecimal("50"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applyBuy(position, new BigDecimal("10"), BigDecimal.ZERO);
        });
    }

    @Test
    @DisplayName("applySell: Throws exception when position is null")
    void testApplySellNullPositionThrowsException() throws InsufficientHoldingsException {
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applySell(null, new BigDecimal("10"));
        });
    }

    @Test
    @DisplayName("applySell: Throws exception when quantity is zero")
    void testApplySellZeroQuantityThrowsException() throws InsufficientHoldingsException {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("10"), new BigDecimal("50"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applySell(position, BigDecimal.ZERO);
        });
    }

    @Test
    @DisplayName("applySell: Throws exception when quantity is negative")
    void testApplySellNegativeQuantityThrowsException() throws InsufficientHoldingsException {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("10"), new BigDecimal("50"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            positionService.applySell(position, new BigDecimal("-5"));
        });
    }

    // ============ Integration Tests (updatePositionAfterBuy / updatePositionAfterSell with Repository) ============

    @Test
    @DisplayName("updatePositionAfterBuy: Creates position and calculates FIFO average cost")
    void testUpdatePositionAfterBuyCreatesPositionAndCalculatesAverageCost() {
        positionService.updatePositionAfterBuy("ACC-1", "AAPL", 10, new BigDecimal("100.00"));
        positionService.updatePositionAfterBuy("ACC-1", "AAPL", 10, new BigDecimal("120.00"));

        assertEquals(20, positionService.getTotalQuantity("ACC-1", "AAPL"));
        assertEquals(new BigDecimal("110.0000"), positionService.getAverageCost("ACC-1", "AAPL"));
        assertTrue(positionService.hasPosition("ACC-1", "AAPL"));
    }

    @Test
    @DisplayName("updatePositionAfterSell: Deletes position when quantity reaches zero")
    void testUpdatePositionAfterSellDeletesPositionWhenQuantityReachesZero() throws InsufficientHoldingsException {
        repository.save(new Position("ACC-1", "AAPL", new BigDecimal("10"), new BigDecimal("100")));

        positionService.updatePositionAfterSell("ACC-1", "AAPL", 10);

        assertTrue(positionService.getPosition("ACC-1", "AAPL").isEmpty());
        assertFalse(positionService.hasPosition("ACC-1", "AAPL"));
    }

    @Test
    @DisplayName("updatePositionAfterSell: Reduces quantity on partial sell")
    void testUpdatePositionAfterSellReducesQuantityWhenPartialSell() throws InsufficientHoldingsException {
        repository.save(new Position("ACC-1", "AAPL", new BigDecimal("20"), new BigDecimal("100")));

        positionService.updatePositionAfterSell("ACC-1", "AAPL", 10);

        assertEquals(10, positionService.getTotalQuantity("ACC-1", "AAPL"));
        assertTrue(positionService.hasPosition("ACC-1", "AAPL"));
    }

    @Test
    @DisplayName("updatePositionAfterSell: Throws exception when trying to sell more than held")
    void testUpdatePositionAfterSellMoreThanHeldLeavesPositionUnchanged() {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("10"), new BigDecimal("100"));
        repository.save(position);

        assertThrows(InsufficientHoldingsException.class,
            () -> positionService.updatePositionAfterSell("ACC-1", "AAPL", 11));

        assertEquals(new BigDecimal("10"), positionService.getPosition("ACC-1", "AAPL").orElseThrow().getQuantity());
    }

    @Test
    @DisplayName("updatePositionAfterSell: Throws exception for non-existent position")
    void testUpdatePositionAfterSellNonExistentPositionThrowsException() {
        assertThrows(InsufficientHoldingsException.class,
            () -> positionService.updatePositionAfterSell("ACC-1", "NONEXISTENT", 10));
    }

    @Test
    @DisplayName("getOrCreatePosition: Creates new position if it doesn't exist")
    void testGetOrCreatePositionCreatesNewIfNotExists() {
        Position position = positionService.getOrCreatePosition("ACC-1", "AAPL");
        
        assertTrue(repository.findByAccountAndSymbol("ACC-1", "AAPL").isPresent());
        assertEquals("ACC-1", position.getAccountId());
        assertEquals("AAPL", position.getSymbol());
        assertEquals(BigDecimal.ZERO, position.getQuantity());
    }

    @Test
    @DisplayName("getOrCreatePosition: Returns existing position if it already exists")
    void testGetOrCreatePositionReturnsExistingIfExists() {
        Position original = new Position("ACC-1", "AAPL", new BigDecimal("100"), new BigDecimal("50"));
        repository.save(original);
        
        Position retrieved = positionService.getOrCreatePosition("ACC-1", "AAPL");
        
        assertEquals(new BigDecimal("100"), retrieved.getQuantity());
        assertEquals(new BigDecimal("50"), retrieved.getAverageCost());
    }
}
