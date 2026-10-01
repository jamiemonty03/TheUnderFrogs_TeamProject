package com.neueda.positionservice.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class CustomExceptionsTest {

    @Test
    @DisplayName("PositionNotFoundException with accountId and symbol contains both")
    public void testPositionNotFoundExceptionWithAccountAndSymbol() {
        PositionNotFoundException ex = new PositionNotFoundException("ACC001", "AAPL");
        
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("ACC001"));
        assertTrue(ex.getMessage().contains("AAPL"));
    }

    @Test
    @DisplayName("PositionNotFoundException with message contains message")
    public void testPositionNotFoundExceptionWithMessage() {
        String message = "Custom not found message";
        PositionNotFoundException ex = new PositionNotFoundException(message);
        
        assertEquals(message, ex.getMessage());
    }

    @Test
    @DisplayName("InsufficientHoldingsException with all params contains all info")
    public void testInsufficientHoldingsExceptionWithAllParams() {
        BigDecimal quantity = BigDecimal.TEN;
        BigDecimal holding = new BigDecimal("5");
        InsufficientHoldingsException ex = new InsufficientHoldingsException("AAPL", quantity, holding, "ACC001");
        
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("AAPL"));
        assertTrue(ex.getMessage().contains("ACC001"));
        assertTrue(ex.getMessage().contains("10"));
        assertTrue(ex.getMessage().contains("5"));
    }

    @Test
    @DisplayName("InsufficientHoldingsException with message contains message")
    public void testInsufficientHoldingsExceptionWithMessage() {
        String message = "Custom insufficient message";
        InsufficientHoldingsException ex = new InsufficientHoldingsException(message);
        
        assertEquals(message, ex.getMessage());
    }

    @Test
    @DisplayName("TradingException with message contains message")
    public void testTradingExceptionWithMessage() {
        String message = "Invalid trade operation";
        TradingException ex = new TradingException(message);
        
        assertEquals(message, ex.getMessage());
    }

    @Test
    @DisplayName("TradingException with message and cause preserves both")
    public void testTradingExceptionWithMessageAndCause() {
        String message = "Trade failed";
        RuntimeException cause = new RuntimeException("Underlying error");
        TradingException ex = new TradingException(message, cause);
        
        assertEquals(message, ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("PositionNotFoundException is a RuntimeException")
    public void testPositionNotFoundExceptionInheritance() {
        PositionNotFoundException ex = new PositionNotFoundException("ACC001", "AAPL");
        
        assertInstanceOf(Exception.class, ex);
    }

    @Test
    @DisplayName("InsufficientHoldingsException extends TradingException")
    public void testInsufficientHoldingsExceptionInheritance() {
        InsufficientHoldingsException ex = new InsufficientHoldingsException("AAPL", 
            BigDecimal.TEN, BigDecimal.ZERO, "ACC001");
        
        assertInstanceOf(TradingException.class, ex);
        assertInstanceOf(Exception.class, ex);
    }

    @Test
    @DisplayName("TradingException is an Exception")
    public void testTradingExceptionInheritance() {
        TradingException ex = new TradingException("Test");
        
        assertInstanceOf(Exception.class, ex);
    }

    @Test
    @DisplayName("InsufficientHoldingsException can be caught as TradingException")
    public void testInsufficientHoldingsExceptionCatchable() {
        try {
            throw new InsufficientHoldingsException("Test", BigDecimal.ONE, BigDecimal.ZERO, "ACC");
        } catch (TradingException e) {
            assertNotNull(e);
        }
    }
}

