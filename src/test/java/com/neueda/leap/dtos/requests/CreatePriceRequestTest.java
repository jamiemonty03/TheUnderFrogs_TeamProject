package com.neueda.leap.dto.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreatePriceRequest Validation Tests")
class CreatePriceRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid request with all fields populated passes validation")
    void testValidRequest() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    @DisplayName("Valid request with zero prices passes validation")
    void testValidRequestWithZeroPrices() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00"),
            new BigDecimal("0.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Zero prices should pass validation (minimum is 0)");
    }

    @Test
    @DisplayName("Request with blank symbol fails validation")
    void testBlankSymbol() {
        CreatePriceRequest request = new CreatePriceRequest(
            "",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank symbol should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Symbol")));
    }

    @Test
    @DisplayName("Request with null symbol fails validation")
    void testNullSymbol() {
        CreatePriceRequest request = new CreatePriceRequest(
            null,
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null symbol should fail validation");
    }

    @Test
    @DisplayName("Request with whitespace-only symbol fails validation")
    void testWhitespaceOnlySymbol() {
        CreatePriceRequest request = new CreatePriceRequest(
            "   ",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Whitespace-only symbol should fail validation");
    }

    @Test
    @DisplayName("Request with null tradeDate passes validation (optional field)")
    void testNullTradeDate() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            null,
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null tradeDate is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with negative open price fails validation")
    void testNegativeOpenPrice() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("-150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Negative open price should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Open")));
    }

    @Test
    @DisplayName("Request with negative high price fails validation")
    void testNegativeHighPrice() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("-155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Negative high price should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("High")));
    }

    @Test
    @DisplayName("Request with negative low price fails validation")
    void testNegativeLowPrice() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("-149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Negative low price should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Low")));
    }

    @Test
    @DisplayName("Request with negative close price fails validation")
    void testNegativeClosePrice() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("-154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Negative close price should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Close")));
    }

    @Test
    @DisplayName("Request with negative volume fails validation")
    void testNegativeVolume() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("-1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Negative volume should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Volume")));
    }

    @Test
    @DisplayName("Request with null open price fails validation")
    void testNullOpenPrice() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            null,
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null open price should fail validation");
    }

    @Test
    @DisplayName("Request with null high price fails validation")
    void testNullHighPrice() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.00"),
            null,
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null high price should fail validation");
    }

    @Test
    @DisplayName("Request with null low price fails validation")
    void testNullLowPrice() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            null,
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null low price should fail validation");
    }

    @Test
    @DisplayName("Request with null close price fails validation")
    void testNullClosePrice() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            null,
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null close price should fail validation");
    }

    @Test
    @DisplayName("Request with null volume fails validation")
    void testNullVolume() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            null
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null volume should fail validation");
    }

    @Test
    @DisplayName("Request with decimal prices passes validation")
    void testDecimalPrices() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.25"),
            new BigDecimal("155.75"),
            new BigDecimal("149.50"),
            new BigDecimal("154.33"),
            new BigDecimal("1234567.89")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Decimal prices should pass validation");
    }

    @Test
    @DisplayName("Request with historical tradeDate passes validation")
    void testHistoricalTradeDate() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.of(2023, 1, 15),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("1000000.00")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Historical tradeDate should pass validation");
    }

    @Test
    @DisplayName("Request with large volume passes validation")
    void testLargeVolume() {
        CreatePriceRequest request = new CreatePriceRequest(
            "AAPL",
            LocalDate.now(),
            new BigDecimal("150.00"),
            new BigDecimal("155.00"),
            new BigDecimal("149.00"),
            new BigDecimal("154.50"),
            new BigDecimal("999999999999.99")
        );

        Set<ConstraintViolation<CreatePriceRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Large volume should pass validation");
    }
}
