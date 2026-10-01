package com.neueda.positionservice.exceptions;

import com.neueda.positionservice.dtos.responses.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Handle PositionNotFoundException returns 404")
    public void testHandlePositionNotFoundException() {
        PositionNotFoundException ex = new PositionNotFoundException("ACC001", "AAPL");
        
        ResponseEntity<ErrorResponse> response = handler.handlePositionNotFound(ex);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("POS-404", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Handle InsufficientHoldingsException returns 409")
    public void testHandleInsufficientHoldingsException() {
        InsufficientHoldingsException ex = new InsufficientHoldingsException("AAPL", 
            java.math.BigDecimal.TEN, java.math.BigDecimal.ZERO, "ACC001");
        
        ResponseEntity<ErrorResponse> response = handler.handleInsufficientHoldings(ex);
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("POS-409", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Handle OptimisticLockingFailureException returns 409")
    public void testHandleOptimisticLockingFailure() {
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");
        
        ResponseEntity<ErrorResponse> response = handler.handleConcurrentUpdate(ex);
        
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("POS-409", response.getBody().errorCode());
        assertTrue(response.getBody().message().contains("changed by another request"));
    }

    @Test
    @DisplayName("Handle TradingException returns 400")
    public void testHandleTradingException() {
        TradingException ex = new TradingException("Invalid trade");
        
        ResponseEntity<ErrorResponse> response = handler.handleTradingException(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("TRD-400", response.getBody().errorCode());
    }

    @Test
    @DisplayName("Handle HttpMessageNotReadableException returns 400")
    public void testHandleUnreadableBody() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Unreadable");
        
        ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(ex);
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("REQ-400", response.getBody().errorCode());
        assertTrue(response.getBody().message().contains("Malformed"));
    }

    @Test
    @DisplayName("Handle MethodArgumentNotValidException with field errors returns 422")
    public void testHandleValidationWithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        FieldError fieldError = new FieldError("object", "field", "Default message");
        List<FieldError> errors = new ArrayList<>();
        errors.add(fieldError);
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(errors);
        
        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VAL-400", response.getBody().errorCode());
        assertTrue(response.getBody().message().contains("Default message"));
    }

    @Test
    @DisplayName("Handle MethodArgumentNotValidException without field errors returns 422")
    public void testHandleValidationWithoutFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(new ArrayList<>());
        
        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);
        
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VAL-400", response.getBody().errorCode());
    }
}
