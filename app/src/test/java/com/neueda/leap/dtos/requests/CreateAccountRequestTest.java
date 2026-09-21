package com.neueda.leap.dtos.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;
import com.neueda.leap.dtos.requests.CreateAccountRequest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateAccountRequest Validation Tests")
class CreateAccountRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid request with all required fields passes validation")
    void testValidRequest() {
        CreateAccountRequest request = new CreateAccountRequest(
            "John Doe",
            new BigDecimal("10000.00")
        );

        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    @DisplayName("Valid request with zero initial balance passes validation")
    void testValidRequestWithZeroBalance() {
        CreateAccountRequest request = new CreateAccountRequest(
            "Jane Smith",
            new BigDecimal("0")
        );

        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Request with zero balance should pass validation");
    }

    @Test
    @DisplayName("Request with blank holder name fails validation")
    void testBlankHolderName() {
        CreateAccountRequest request = new CreateAccountRequest(
            "",
            new BigDecimal("5000.00")
        );

        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank holder name should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Holder")));
    }

    @Test
    @DisplayName("Request with null holder name fails validation")
    void testNullHolderName() {
        CreateAccountRequest request = new CreateAccountRequest(
            null,
            new BigDecimal("5000.00")
        );

        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null holder name should fail validation");
    }

    @Test
    @DisplayName("Request with null initial balance fails validation")
    void testNullInitialBalance() {
        CreateAccountRequest request = new CreateAccountRequest(
            "John Doe",
            null
        );

        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null initial balance should fail validation");
    }

    @Test
    @DisplayName("Request with negative initial balance fails validation")
    void testNegativeInitialBalance() {
        CreateAccountRequest request = new CreateAccountRequest(
            "John Doe",
            new BigDecimal("-100.00")
        );

        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Negative initial balance should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("balance")));
    }

    @Test
    @DisplayName("Request with large initial balance passes validation")
    void testLargeInitialBalance() {
        CreateAccountRequest request = new CreateAccountRequest(
            "John Doe",
            new BigDecimal("1000000.99")
        );

        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Large initial balance should pass validation");
    }

    @Test
    @DisplayName("Request with whitespace-only holder name fails validation")
    void testWhitespaceOnlyHolderName() {
        CreateAccountRequest request = new CreateAccountRequest(
            "   ",
            new BigDecimal("5000.00")
        );

        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Whitespace-only holder name should fail validation");
    }

    @Test
    @DisplayName("Request with decimal initial balance passes validation")
    void testDecimalInitialBalance() {
        CreateAccountRequest request = new CreateAccountRequest(
            "John Doe",
            new BigDecimal("5000.50")
        );

        Set<ConstraintViolation<CreateAccountRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Decimal initial balance should pass validation");
    }
}
