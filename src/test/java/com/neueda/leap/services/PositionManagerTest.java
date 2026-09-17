package com.neueda.leap.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.neueda.leap.exceptions.InsufficientHoldingsException;
import com.neueda.leap.models.Position;
import com.neueda.leap.repositories.InMemoryPositionRepository;

class PositionManagerTest {

    private InMemoryPositionRepository repository;
    private PositionManager positionManager;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPositionRepository();
        positionManager = new PositionManager(repository, new PositionService());
    }

    @Test
    void buyCreatesPositionAndCalculatesAverageCost() {
        positionManager.updatePositionAfterBuy("ACC-1", "AAPL", 10, new BigDecimal("100.00"));
        positionManager.updatePositionAfterBuy("ACC-1", "AAPL", 10, new BigDecimal("120.00"));

        assertEquals(20, positionManager.getTotalQuantity("ACC-1", "AAPL"));
        assertEquals(new BigDecimal("110.0000"), positionManager.getAverageCost("ACC-1", "AAPL"));
        assertTrue(positionManager.hasPosition("ACC-1", "AAPL"));
    }

    @Test
    void sellingEntirePositionDeletesIt() throws InsufficientHoldingsException {
        repository.save(new Position("ACC-1", "AAPL", new BigDecimal("10"), new BigDecimal("100")));

        positionManager.updatePositionAfterSell("ACC-1", "AAPL", 10);

        assertTrue(positionManager.getPosition("ACC-1", "AAPL").isEmpty());
        assertFalse(positionManager.hasPosition("ACC-1", "AAPL"));
    }

    @Test
    void sellingMoreThanHeldLeavesPositionUnchanged() {
        Position position = new Position("ACC-1", "AAPL", new BigDecimal("10"), new BigDecimal("100"));
        repository.save(position);

        assertThrows(InsufficientHoldingsException.class,
            () -> positionManager.updatePositionAfterSell("ACC-1", "AAPL", 11));

        assertEquals(new BigDecimal("10"), positionManager.getPosition("ACC-1", "AAPL").orElseThrow().getQuantity());
    }
}