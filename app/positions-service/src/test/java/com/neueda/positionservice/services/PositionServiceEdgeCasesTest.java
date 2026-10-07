package com.neueda.positionservice.services;

import com.neueda.positionservice.dtos.requests.CreatePositionRequest;
import com.neueda.positionservice.dtos.requests.ReplacePositionRequest;
import com.neueda.positionservice.dtos.requests.UpdatePositionRequest;
import com.neueda.positionservice.exceptions.InsufficientHoldingsException;
import com.neueda.positionservice.exceptions.PositionNotFoundException;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.models.PositionId;
import com.neueda.positionservice.repositories.PositionRepository;
import com.neueda.positionservice.repositories.PositionMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PositionServiceEdgeCasesTest {

    @Mock
    private PositionRepository repository;

    @Mock
    private PositionMovementRepository movementRepository;

    private PositionService service;

    @BeforeEach
    public void setUp() {
        service = new PositionService(repository, movementRepository);
    }

    @Test
    @DisplayName("createPosition: Creates new position when none exists")
    public void testCreatePositionNew() {
        CreatePositionRequest request = new CreatePositionRequest("ACC001", "AAPL", 
            new BigDecimal("10"), new BigDecimal("150.00"));
        Position expected = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(expected);
        
        Position result = service.createPosition(request);
        
        assertNotNull(result);
        assertEquals("ACC001", result.getAccountId());
        verify(repository).save(any());
    }

    @Test
    @DisplayName("createPosition: Updates existing position when already present")
    public void testCreatePositionExisting() {
        CreatePositionRequest request = new CreatePositionRequest("ACC001", "AAPL", 
            new BigDecimal("5"), new BigDecimal("160.00"));
        Position existing = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        
        when(repository.findById(any())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(existing);
        
        Position result = service.createPosition(request);
        
        assertNotNull(result);
        verify(repository).save(any());
    }

    @Test
    @DisplayName("deletePosition: Throws exception when position not found")
    public void testDeletePositionNotFound() {
        when(repository.existsById(any())).thenReturn(false);
        
        assertThrows(PositionNotFoundException.class, () ->
                service.deletePosition("ACC001", "AAPL"));
        
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("updatePosition: Updates quantity when not null")
    public void testUpdatePositionWithQuantity() {
        Position existing = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        ReplacePositionRequest request = new ReplacePositionRequest(new BigDecimal("20"), new BigDecimal("155.00"));
        
        when(repository.findById(any())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(existing);
        
        service.updatePosition("ACC001", "AAPL", request);
        
        verify(repository).save(any());
    }

    @Test
    @DisplayName("patchPosition: Updates only specified fields")
    public void testPatchPositionWithOnlyQuantity() {
        Position existing = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        UpdatePositionRequest request = new UpdatePositionRequest(new BigDecimal("15"), null);
        
        when(repository.findById(any())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(existing);
        
        service.patchPosition("ACC001", "AAPL", request);
        
        verify(repository).save(any());
    }

    @Test
    @DisplayName("applyBuy: Throws exception when position is null")
    public void testApplyBuyNullPosition() {
        assertThrows(IllegalArgumentException.class, () ->
                service.applyBuy(null, BigDecimal.TEN, new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("applyBuy: Throws exception when quantity is negative")
    public void testApplyBuyNegativeQuantity() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        
        assertThrows(IllegalArgumentException.class, () ->
                service.applyBuy(position, new BigDecimal("-5"), new BigDecimal("150.00")));
    }

    @Test
    @DisplayName("applyBuy: Throws exception when price is zero")
    public void testApplyBuyZeroPrice() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        
        assertThrows(IllegalArgumentException.class, () ->
                service.applyBuy(position, BigDecimal.TEN, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("applySell: Throws exception when quantity exceeds holding")
    public void testApplySellInsufficientHoldings() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("5"), new BigDecimal("150.00"));
        
        assertThrows(InsufficientHoldingsException.class, () ->
                service.applySell(position, new BigDecimal("10")));
    }

    @Test
    @DisplayName("updatePositionAfterSell: Deletes position when quantity becomes zero")
    public void testUpdatePositionAfterSellDeletesWhenZero() throws InsufficientHoldingsException {
        Position existing = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        
        when(repository.findById(any())).thenReturn(Optional.of(existing));
        doNothing().when(repository).deleteById(any());
        
        service.updatePositionAfterSell("ACC001", "AAPL", 10);
        
        verify(repository).deleteById(any());
    }

    @Test
    @DisplayName("updatePositionAfterSell: Throws exception when position not found")
    public void testUpdatePositionAfterSellNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        
        assertThrows(InsufficientHoldingsException.class, () ->
                service.updatePositionAfterSell("ACC001", "AAPL", 10));
    }

    @Test
    @DisplayName("applyUpdate: Throws exception when quantity is negative")
    public void testApplyUpdateNegativeQuantity() {
        Position existing = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        
        when(repository.findById(any())).thenReturn(Optional.of(existing));
        
        assertThrows(IllegalArgumentException.class, () ->
                service.patchPosition("ACC001", "AAPL", 
                    new UpdatePositionRequest(new BigDecimal("-5"), null)));
    }

    @Test
    @DisplayName("applyUpdate: Throws exception when average cost is negative")
    public void testApplyUpdateNegativeAverageCost() {
        Position existing = new Position("ACC001", "AAPL", new BigDecimal("10"), new BigDecimal("150.00"));
        ReplacePositionRequest request = new ReplacePositionRequest(
            new BigDecimal("10"), new BigDecimal("-5"));
        
        when(repository.findById(any())).thenReturn(Optional.of(existing));
        
        assertThrows(IllegalArgumentException.class, () ->
                service.updatePosition("ACC001", "AAPL", request));
    }
}
