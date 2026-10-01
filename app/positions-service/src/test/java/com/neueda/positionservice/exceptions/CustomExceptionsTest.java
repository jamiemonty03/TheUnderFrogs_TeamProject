package com.neueda.positionservice.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class CustomExceptionsTest {

    @Test
    @DisplayName("PositionNotFoundException contains correct message")
    public void testPositionNotFoundExceptionMessage() {
        PositionNotFoundException ex = new PositionNotFoundException("ACC001", "AAPL");
        
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("ACC001"));
        assertTrue(ex.getMessage().contains("AAPL"));
    }

    @Test
    @DisplayName("InsufficientHoldingsException contains correct message")
    public void testInsufficientHoldingsExceptionMessage() {
        BigDecimal quantity = BigDecimal.TEN;
        BigDecimal holding = new BigDecimal("5");
        InsufficientHoldingsException ex = new InsufficientHoldingsException("AAPL", quantity, holding, "ACC001");
        
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("AAPL"));
        assertTrue(ex.getMessage().contains("ACC001"));
    }

    @Test
    @DisplayName("TradingException contains correct message")
    public void testTradingExceptionMessage() {
        String message = "Invalid trade operation";
        TradingException ex = new TradingException(message);
        
        assertEquals(message, ex.getMessage());
    }

    @Test
    @DisplayName("PositionNotFoundException is an Exception")
    public void testPositionNotFoundExceptionInheritance() {
        PositionNotFoundException ex = new PositionNotFoundException("ACC001", "AAPL");
        
        assertInstanceOf(Exception.class, ex);
    }

    @Test
    @DisplayName("InsufficientHoldingsException is an Exception")
    public void testInsufficientHoldingsExceptionInheritance() {
        InsufficientHoldingsException ex = new InsufficientHoldingsException("AAPL", 
            BigDecimal.TEN, BigDecimal.ZERO, "ACC001");
        
        assertInstanceOf(Exception.class, ex);
    }

    @Test
    @DisplayName("TradingException is an Exception")
    public void testTradingExceptionInheritance() {
        TradingException ex = new TradingException("Test");
        
        assertInstanceOf(Exception.class, ex);
    }
}
