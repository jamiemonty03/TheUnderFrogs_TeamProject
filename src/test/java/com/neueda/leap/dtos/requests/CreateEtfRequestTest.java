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
import com.neueda.leap.dto.requests.CreateEtfRequest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateEtfRequest Validation Tests")
class CreateEtfRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid request with required fields and nulls for optional fields passes validation")
    void testValidRequestWithNulls() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            "SPDR S&P 500 ETF",
            null,
            null,
            null,
            null,
            null
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request with nulls should have no violations");
    }

    @Test
    @DisplayName("Valid request with all fields populated passes validation")
    void testValidRequestWithAllFields() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            "SPDR S&P 500 ETF",
            new BigDecimal("450.75"),
            LocalDateTime.now(),
            "Equity",
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request with all fields should pass validation");
    }

    @Test
    @DisplayName("Request with blank symbol fails validation")
    void testBlankSymbol() {
        CreateEtfRequest request = new CreateEtfRequest(
            "",
            "SPDR S&P 500 ETF",
            new BigDecimal("450.75"),
            LocalDateTime.now(),
            "Equity",
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank symbol should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Symbol")));
    }

    @Test
    @DisplayName("Request with null symbol fails validation")
    void testNullSymbol() {
        CreateEtfRequest request = new CreateEtfRequest(
            null,
            "SPDR S&P 500 ETF",
            new BigDecimal("450.75"),
            LocalDateTime.now(),
            "Equity",
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null symbol should fail validation");
    }

    @Test
    @DisplayName("Request with whitespace-only symbol fails validation")
    void testWhitespaceOnlySymbol() {
        CreateEtfRequest request = new CreateEtfRequest(
            "  \t  ",
            "SPDR S&P 500 ETF",
            new BigDecimal("450.75"),
            LocalDateTime.now(),
            "Equity",
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Whitespace-only symbol should fail validation");
    }

    @Test
    @DisplayName("Request with blank name fails validation")
    void testBlankName() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            "",
            new BigDecimal("450.75"),
            LocalDateTime.now(),
            "Equity",
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank name should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Name")));
    }

    @Test
    @DisplayName("Request with null name fails validation")
    void testNullName() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            null,
            new BigDecimal("450.75"),
            LocalDateTime.now(),
            "Equity",
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null name should fail validation");
    }

    @Test
    @DisplayName("Request with null price passes validation (optional field)")
    void testNullPrice() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            "SPDR S&P 500 ETF",
            null,
            LocalDateTime.now(),
            "Equity",
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null price is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with positive price passes validation")
    void testPositivePrice() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            "SPDR S&P 500 ETF",
            new BigDecimal("450.75"),
            LocalDateTime.now(),
            "Equity",
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Positive price should pass validation");
    }

    @Test
    @DisplayName("Request with null tradeDate passes validation (optional field)")
    void testNullTradeDate() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            "SPDR S&P 500 ETF",
            new BigDecimal("450.75"),
            null,
            "Equity",
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null tradeDate is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with null category passes validation (optional field)")
    void testNullCategory() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            "SPDR S&P 500 ETF",
            new BigDecimal("450.75"),
            LocalDateTime.now(),
            null,
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null category is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with null fundFamily passes validation (optional field)")
    void testNullFundFamily() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            "SPDR S&P 500 ETF",
            new BigDecimal("450.75"),
            LocalDateTime.now(),
            "Equity",
            null,
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null fundFamily is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with null legalType passes validation (optional field)")
    void testNullLegalType() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            "SPDR S&P 500 ETF",
            new BigDecimal("450.75"),
            LocalDateTime.now(),
            "Equity",
            "State Street",
            null
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null legalType is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with all optional fields as null passes validation")
    void testAllOptionalFieldsNull() {
        CreateEtfRequest request = new CreateEtfRequest(
            "QQQ",
            "Invesco QQQ Trust",
            null,
            null,
            null,
            null,
            null
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "All optional fields as null should pass validation");
    }

    @Test
    @DisplayName("Request with zero price passes validation")
    void testZeroPrice() {
        CreateEtfRequest request = new CreateEtfRequest(
            "SPY",
            "SPDR S&P 500 ETF",
            new BigDecimal("0.00"),
            LocalDateTime.now(),
            "Equity",
            "State Street",
            "Trust"
        );

        Set<ConstraintViolation<CreateEtfRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Zero price is allowed (optional field)");
    }
}
