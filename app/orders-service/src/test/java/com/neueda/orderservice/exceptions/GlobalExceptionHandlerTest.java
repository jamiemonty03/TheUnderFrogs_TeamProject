package com.neueda.orderservice.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.neueda.orderservice.dtos.responses.ErrorResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("InvalidOrderException returns 422 VAL-422 with the exception message")
    void handleInvalidOrder() {
        var response = handler.handleInvalidOrder(
            new InvalidOrderException("Quantity must be a whole number of shares"));

        assertEquals(422, response.getStatusCode().value());
        ErrorResponse body = response.getBody();
        assertEquals("VAL-422", body.errorCode());
        assertEquals("Quantity must be a whole number of shares", body.message());
    }
}
