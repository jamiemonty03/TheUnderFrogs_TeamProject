package com.neueda.positionservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.positionservice.dtos.requests.UpdatePositionRequest;
import com.neueda.positionservice.exceptions.InsufficientHoldingsException;
import com.neueda.positionservice.exceptions.PositionNotFoundException;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.models.PositionId;
import com.neueda.positionservice.repositories.PositionRepository;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository repository;

    @InjectMocks
    private PositionService positionService;

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

    // Tests (patchPosition / updatePositionAfterSell)

    private Position stubExisting() {
        Position existing = new Position("ACC-1", "AAPL", new BigDecimal("100"), new BigDecimal("150.50"));
        when(repository.findById(new PositionId("ACC-1", "AAPL"))).thenReturn(Optional.of(existing));
        when(repository.save(any(Position.class))).thenAnswer(inv -> inv.getArgument(0));
        return existing;
    }

    @Test
    @DisplayName("patchPosition: Updating averageCost only keeps the existing quantity")
    void testPatchAverageCostOnlyKeepsQuantity() {
        stubExisting();

        Position result = positionService.patchPosition("ACC-1", "AAPL",
            new UpdatePositionRequest(null, new BigDecimal("160.00"), null));

        assertEquals(new BigDecimal("100"), result.getQuantity());
        assertEquals(new BigDecimal("160.00"), result.getAverageCost());
    }

    @Test
    @DisplayName("patchPosition: Updating quantity only keeps the existing averageCost")
    void testPatchQuantityOnlyKeepsAverageCost() {
        stubExisting();

        Position result = positionService.patchPosition("ACC-1", "AAPL",
            new UpdatePositionRequest(new BigDecimal("120"), null, null));

        assertEquals(new BigDecimal("120"), result.getQuantity());
        assertEquals(new BigDecimal("150.50"), result.getAverageCost());
    }

    @Test
    @DisplayName("patchPosition: Throws PositionNotFoundException when the position does not exist")
    void testPatchPositionNotFound() {
        when(repository.findById(new PositionId("ACC-1", "MSFT"))).thenReturn(Optional.empty());

        assertThrows(PositionNotFoundException.class, () -> positionService.patchPosition("ACC-1", "MSFT",
            new UpdatePositionRequest(new BigDecimal("10"), null, null)));
    }

    @Test
    @DisplayName("updatePositionAfterSell: Selling all shares deletes the position and returns quantity 0")
    void testSellAllSharesDeletesAndReturnsPosition() throws InsufficientHoldingsException {
        Position existing = new Position("ACC-1", "AAPL", new BigDecimal("100"), new BigDecimal("150.50"));
        when(repository.findById(new PositionId("ACC-1", "AAPL"))).thenReturn(Optional.of(existing));

        Position result = positionService.updatePositionAfterSell("ACC-1", "AAPL", 100);

        assertEquals(0, result.getQuantity().compareTo(BigDecimal.ZERO));
        verify(repository).deleteById(new PositionId("ACC-1", "AAPL"));
        verify(repository, never()).save(any(Position.class));
    }
}
