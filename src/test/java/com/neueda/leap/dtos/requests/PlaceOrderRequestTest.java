package com.neueda.leap.dtos.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import com.neueda.leap.enums.OrderSide;
import java.math.BigDecimal;
import java.util.Set;
import com.neueda.leap.dto.requests.PlaceOrderRequest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PlaceOrderRequest Validation Tests")
class PlaceOrderRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid request with all required fields passes validation")
    void testValidRequest() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("100.50"),
            new BigDecimal("150.25"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    @DisplayName("Request with blank symbol fails validation")
    void testBlankSymbol() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "",
            OrderSide.BUY,
            new BigDecimal("100.00"),
            new BigDecimal("150.00"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank symbol should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Symbol")));
    }

    @Test
    @DisplayName("Request with null symbol fails validation")
    void testNullSymbol() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            null,
            OrderSide.BUY,
            new BigDecimal("100.00"),
            new BigDecimal("150.00"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null symbol should fail validation");
    }

    @Test
    @DisplayName("Request with null OrderSide fails validation")
    void testNullOrderSide() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            null,
            new BigDecimal("100.00"),
            new BigDecimal("150.00"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null OrderSide should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("side")));
    }

    @Test
    @DisplayName("Request with null quantity fails validation")
    void testNullQuantity() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.BUY,
            null,
            new BigDecimal("150.00"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null quantity should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Quantity")));
    }

    @Test
    @DisplayName("Request with quantity below minimum fails validation")
    void testQuantityBelowMinimum() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("0.00"),
            new BigDecimal("150.00"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Quantity below 0.01 should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Quantity")));
    }

    @Test
    @DisplayName("Request with negative quantity fails validation")
    void testNegativeQuantity() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("-10.00"),
            new BigDecimal("150.00"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Negative quantity should fail validation");
    }

    @Test
    @DisplayName("Request with minimum valid quantity passes validation")
    void testMinimumValidQuantity() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("0.01"),
            new BigDecimal("150.00"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Minimum valid quantity should pass validation");
    }

    @Test
    @DisplayName("Request with null price fails validation")
    void testNullPrice() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("100.00"),
            null,
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null price should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Price")));
    }

    @Test
    @DisplayName("Request with price below minimum fails validation")
    void testPriceBelowMinimum() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("100.00"),
            new BigDecimal("0.00"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Price below 0.01 should fail validation");
    }

    @Test
    @DisplayName("Request with minimum valid price passes validation")
    void testMinimumValidPrice() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("100.00"),
            new BigDecimal("0.01"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Minimum valid price should pass validation");
    }

    @Test
    @DisplayName("Request with blank idempotency key fails validation")
    void testBlankIdempotencyKey() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("100.00"),
            new BigDecimal("150.00"),
            ""
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank idempotency key should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Idempotency")));
    }

    @Test
    @DisplayName("Request with null idempotency key fails validation")
    void testNullIdempotencyKey() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.BUY,
            new BigDecimal("100.00"),
            new BigDecimal("150.00"),
            null
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null idempotency key should fail validation");
    }

    @Test
    @DisplayName("Request with SELL side passes validation")
    void testSellOrderSide() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "AAPL",
            OrderSide.SELL,
            new BigDecimal("100.00"),
            new BigDecimal("150.00"),
            "idempotency-key-123"
        );

        Set<ConstraintViolation<PlaceOrderRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "SELL order side should pass validation");
    }
}
