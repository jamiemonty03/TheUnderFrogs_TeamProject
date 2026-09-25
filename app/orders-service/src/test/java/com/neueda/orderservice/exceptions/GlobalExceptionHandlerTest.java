package com.neueda.orderservice.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.neueda.orderservice.dtos.responses.ErrorResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static void assertError(ResponseEntity<ErrorResponse> response, int status, String code, String message) {
        assertEquals(status, response.getStatusCode().value());
        assertEquals(code, response.getBody().errorCode());
        assertEquals(message, response.getBody().message());
    }

    @Test
    @DisplayName("AccountNotActiveException returns 403 ACC-403")
    void handleAccountNotActive() {
        assertError(handler.handleAccountNotActive(new AccountNotActiveException("Account ACC1 is CLOSED")),
            403, "ACC-403", "Account ACC1 is CLOSED");
    }

    @Test
    @DisplayName("InstrumentNotFoundException returns 404 INS-404")
    void handleInstrumentNotFound() {
        assertError(handler.handleInstrumentNotFound(new InstrumentNotFoundException("Instrument not found: XYZ")),
            404, "INS-404", "Instrument not found: XYZ");
    }

    @Test
    @DisplayName("InsufficientFundsException returns 400 ORD-400")
    void handleInsufficientFunds() {
        assertError(handler.handleInsufficientFunds(new InsufficientFundsException("Insufficient funds")),
            400, "ORD-400", "Insufficient funds");
    }

    @Test
    @DisplayName("InsufficientHoldingsException returns 409 ORD-409")
    void handleInsufficientHoldings() {
        assertError(handler.handleInsufficientHoldings(new InsufficientHoldingsException("Not enough AAPL")),
            409, "ORD-409", "Not enough AAPL");
    }

    @Test
    @DisplayName("DuplicateOrderException returns 409 ORD-409")
    void handleDuplicateOrder() {
        assertError(handler.handleDuplicateOrder(new DuplicateOrderException("key-1", "idempotencyKey")),
            409, "ORD-409", "Duplicate idempotencyKey: key-1");
    }

    @Test
    @DisplayName("InvalidOrderException returns 422 VAL-422 with the exception message")
    void handleInvalidOrder() {
        assertError(handler.handleInvalidOrder(new InvalidOrderException("Quantity must be a whole number of shares")),
            422, "VAL-422", "Quantity must be a whole number of shares");
    }

    @Test
    @DisplayName("IllegalArgumentException returns 422 VAL-422")
    void handleIllegalArgument() {
        assertError(handler.handleIllegalArgument(new IllegalArgumentException("Order cannot be null")),
            422, "VAL-422", "Order cannot be null");
    }

    @Test
    @DisplayName("Bean validation failure returns 422 VAL-422 with the first field message")
    void handleValidationUsesFirstFieldError() {
        MethodArgumentNotValidException ex = validationException(List.of(
            new FieldError("placeOrderRequest", "quantity", "Quantity is required"),
            new FieldError("placeOrderRequest", "price", "Price is required")));

        assertError(handler.handleValidation(ex), 422, "VAL-422", "Quantity is required");
    }

    @Test
    @DisplayName("Bean validation failure with no field errors falls back to a generic message")
    void handleValidationWithoutFieldErrors() {
        assertError(handler.handleValidation(validationException(List.of())), 422, "VAL-422", "Validation failed");
    }

    private static MethodArgumentNotValidException validationException(List<FieldError> fieldErrors) {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        return ex;
    }
}
