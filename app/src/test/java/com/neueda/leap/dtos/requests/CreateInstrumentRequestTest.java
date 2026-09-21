package com.neueda.leap.dtos.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import com.neueda.leap.dtos.requests.CreateInstrumentRequest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CreateInstrumentRequest Validation Tests")
class CreateInstrumentRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid request with all required fields passes validation")
    void testValidRequest() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "AAPL",
            "Apple Inc.",
            "EQUITY",
            "USD",
            "NASDAQ",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    @DisplayName("Valid request with tradable=false passes validation")
    void testValidRequestNotTradable() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "INVALID",
            "Invalid Instrument",
            "EQUITY",
            "USD",
            "NYSE",
            false
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Request with tradable=false should pass validation");
    }

    @Test
    @DisplayName("Request with blank symbol fails validation")
    void testBlankSymbol() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "",
            "Apple Inc.",
            "EQUITY",
            "USD",
            "NASDAQ",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank symbol should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Symbol")));
    }

    @Test
    @DisplayName("Request with null symbol fails validation")
    void testNullSymbol() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            null,
            "Apple Inc.",
            "EQUITY",
            "USD",
            "NASDAQ",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null symbol should fail validation");
    }

    @Test
    @DisplayName("Request with whitespace-only symbol fails validation")
    void testWhitespaceOnlySymbol() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "   ",
            "Apple Inc.",
            "EQUITY",
            "USD",
            "NASDAQ",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Whitespace-only symbol should fail validation");
    }

    @Test
    @DisplayName("Request with blank name fails validation")
    void testBlankName() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "AAPL",
            "",
            "EQUITY",
            "USD",
            "NASDAQ",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank name should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Name")));
    }

    @Test
    @DisplayName("Request with null name fails validation")
    void testNullName() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "AAPL",
            null,
            "EQUITY",
            "USD",
            "NASDAQ",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null name should fail validation");
    }

    @Test
    @DisplayName("Request with blank assetClass fails validation")
    void testBlankAssetClass() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "AAPL",
            "Apple Inc.",
            "",
            "USD",
            "NASDAQ",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank asset class should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Asset class")));
    }

    @Test
    @DisplayName("Request with null assetClass fails validation")
    void testNullAssetClass() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "AAPL",
            "Apple Inc.",
            null,
            "USD",
            "NASDAQ",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null asset class should fail validation");
    }

    @Test
    @DisplayName("Request with blank currency fails validation")
    void testBlankCurrency() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "AAPL",
            "Apple Inc.",
            "EQUITY",
            "",
            "NASDAQ",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank currency should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Currency")));
    }

    @Test
    @DisplayName("Request with null currency fails validation")
    void testNullCurrency() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "AAPL",
            "Apple Inc.",
            "EQUITY",
            null,
            "NASDAQ",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null currency should fail validation");
    }

    @Test
    @DisplayName("Request with blank exchange fails validation")
    void testBlankExchange() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "AAPL",
            "Apple Inc.",
            "EQUITY",
            "USD",
            "",
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank exchange should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Exchange")));
    }

    @Test
    @DisplayName("Request with null exchange fails validation")
    void testNullExchange() {
        CreateInstrumentRequest request = new CreateInstrumentRequest(
            "AAPL",
            "Apple Inc.",
            "EQUITY",
            "USD",
            null,
            true
        );

        Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null exchange should fail validation");
    }

    @Test
    @DisplayName("Request with different asset classes passes validation")
    void testDifferentAssetClasses() {
        String[] assetClasses = {"EQUITY", "BOND", "ETF", "COMMODITY", "FOREX"};
        
        for (String assetClass : assetClasses) {
            CreateInstrumentRequest request = new CreateInstrumentRequest(
                "TEST",
                "Test Instrument",
                assetClass,
                "USD",
                "NASDAQ",
                true
            );

            Set<ConstraintViolation<CreateInstrumentRequest>> violations = validator.validate(request);
            assertTrue(violations.isEmpty(), "Asset class '" + assetClass + "' should pass validation");
        }
    }
}
