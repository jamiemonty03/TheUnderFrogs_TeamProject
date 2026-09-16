package com.neueda.leap.dto.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreditAccountRequest Validation Tests")
class CreditAccountRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid request with all required fields passes validation")
    void testValidRequest() {
        CreditAccountRequest request = new CreditAccountRequest(
            "ACC001",
            new BigDecimal("1000.00")
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    @DisplayName("Request with blank account ID fails validation")
    void testBlankAccountId() {
        CreditAccountRequest request = new CreditAccountRequest(
            "",
            new BigDecimal("500.00")
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank account ID should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Account")));
    }

    @Test
    @DisplayName("Request with null account ID fails validation")
    void testNullAccountId() {
        CreditAccountRequest request = new CreditAccountRequest(
            null,
            new BigDecimal("500.00")
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null account ID should fail validation");
    }

    @Test
    @DisplayName("Request with whitespace-only account ID fails validation")
    void testWhitespaceOnlyAccountId() {
        CreditAccountRequest request = new CreditAccountRequest(
            "   ",
            new BigDecimal("500.00")
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Whitespace-only account ID should fail validation");
    }

    @Test
    @DisplayName("Request with null amount fails validation")
    void testNullAmount() {
        CreditAccountRequest request = new CreditAccountRequest(
            "ACC001",
            null
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null amount should fail validation");
    }

    @Test
    @DisplayName("Request with zero amount fails validation")
    void testZeroAmount() {
        CreditAccountRequest request = new CreditAccountRequest(
            "ACC001",
            new BigDecimal("0.00")
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Zero amount should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("must be greater than 0")));
    }

    @Test
    @DisplayName("Request with amount below minimum fails validation")
    void testAmountBelowMinimum() {
        CreditAccountRequest request = new CreditAccountRequest(
            "ACC001",
            new BigDecimal("0.00")
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Amount below 0.01 should fail validation");
    }

    @Test
    @DisplayName("Request with negative amount fails validation")
    void testNegativeAmount() {
        CreditAccountRequest request = new CreditAccountRequest(
            "ACC001",
            new BigDecimal("-50.00")
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Negative amount should fail validation");
    }

    @Test
    @DisplayName("Request with minimum valid amount passes validation")
    void testMinimumValidAmount() {
        CreditAccountRequest request = new CreditAccountRequest(
            "ACC001",
            new BigDecimal("0.01")
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Minimum valid amount should pass validation");
    }

    @Test
    @DisplayName("Request with large amount passes validation")
    void testLargeAmount() {
        CreditAccountRequest request = new CreditAccountRequest(
            "ACC001",
            new BigDecimal("999999.99")
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Large amount should pass validation");
    }

    @Test
    @DisplayName("Request with decimal amount passes validation")
    void testDecimalAmount() {
        CreditAccountRequest request = new CreditAccountRequest(
            "ACC001",
            new BigDecimal("250.75")
        );

        Set<ConstraintViolation<CreditAccountRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Decimal amount should pass validation");
    }
}
