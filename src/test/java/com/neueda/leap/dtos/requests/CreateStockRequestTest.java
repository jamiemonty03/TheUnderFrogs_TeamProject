package com.neueda.leap.dtos.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateStockRequest Validation Tests")
class CreateStockRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid request with required fields and nulls for optional fields passes validation")
    void testValidRequestWithNulls() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            "Apple Inc.",
            null,
            null,
            null,
            null,
            null,
            null
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request with nulls should have no violations");
    }

    @Test
    @DisplayName("Valid request with all fields populated passes validation")
    void testValidRequestWithAllFields() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            "Apple Inc.",
            new BigDecimal("150.25"),
            LocalDateTime.now(),
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request with all fields should pass validation");
    }

    @Test
    @DisplayName("Request with blank symbol fails validation")
    void testBlankSymbol() {
        CreateStockRequest request = new CreateStockRequest(
            "",
            "Apple Inc.",
            new BigDecimal("150.25"),
            LocalDateTime.now(),
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank symbol should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Symbol")));
    }

    @Test
    @DisplayName("Request with null symbol fails validation")
    void testNullSymbol() {
        CreateStockRequest request = new CreateStockRequest(
            null,
            "Apple Inc.",
            new BigDecimal("150.25"),
            LocalDateTime.now(),
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null symbol should fail validation");
    }

    @Test
    @DisplayName("Request with whitespace-only symbol fails validation")
    void testWhitespaceOnlySymbol() {
        CreateStockRequest request = new CreateStockRequest(
            "   ",
            "Apple Inc.",
            new BigDecimal("150.25"),
            LocalDateTime.now(),
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Whitespace-only symbol should fail validation");
    }

    @Test
    @DisplayName("Request with blank name fails validation")
    void testBlankName() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            "",
            new BigDecimal("150.25"),
            LocalDateTime.now(),
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank name should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Name")));
    }

    @Test
    @DisplayName("Request with null name fails validation")
    void testNullName() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            null,
            new BigDecimal("150.25"),
            LocalDateTime.now(),
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null name should fail validation");
    }

    @Test
    @DisplayName("Request with whitespace-only name fails validation")
    void testWhitespaceOnlyName() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            "   ",
            new BigDecimal("150.25"),
            LocalDateTime.now(),
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Whitespace-only name should fail validation");
    }

    @Test
    @DisplayName("Request with positive price passes validation")
    void testPositivePrice() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            "Apple Inc.",
            new BigDecimal("150.25"),
            LocalDateTime.now(),
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Positive price should pass validation");
    }

    @Test
    @DisplayName("Request with zero price passes validation")
    void testZeroPrice() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            "Apple Inc.",
            new BigDecimal("0.00"),
            LocalDateTime.now(),
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Zero price is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with null price passes validation")
    void testNullPrice() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            "Apple Inc.",
            null,
            LocalDateTime.now(),
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null price is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with null tradeDate passes validation")
    void testNullTradeDate() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            "Apple Inc.",
            new BigDecimal("150.25"),
            null,
            "Technology",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null tradeDate is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with null sector passes validation")
    void testNullSector() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            "Apple Inc.",
            new BigDecimal("150.25"),
            LocalDateTime.now(),
            null,
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null sector is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with empty string sector passes validation")
    void testEmptySector() {
        CreateStockRequest request = new CreateStockRequest(
            "AAPL",
            "Apple Inc.",
            new BigDecimal("150.25"),
            LocalDateTime.now(),
            "",
            "Consumer Electronics",
            "United States",
            "https://www.apple.com"
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Empty string sector is allowed (optional field, no @NotBlank)");
    }

    @Test
    @DisplayName("Request with all optional fields as null passes validation")
    void testAllOptionalFieldsNull() {
        CreateStockRequest request = new CreateStockRequest(
            "MSFT",
            "Microsoft Corporation",
            null,
            null,
            null,
            null,
            null,
            null
        );

        Set<ConstraintViolation<CreateStockRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "All optional fields as null should pass validation");
    }
}
