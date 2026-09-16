package com.neueda.leap.dto.requests;

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

@DisplayName("CreateBondRequest Validation Tests")
class CreateBondRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid request with required fields and nulls for optional fields passes validation")
    void testValidRequestWithNulls() {
        CreateBondRequest request = new CreateBondRequest(
            "BND001",
            "Corporate Bond",
            null,
            null,
            null,
            null,
            null
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request with nulls should have no violations");
    }

    @Test
    @DisplayName("Valid request with all fields populated passes validation")
    void testValidRequestWithAllFields() {
        CreateBondRequest request = new CreateBondRequest(
            "BND001",
            "US Treasury Bond",
            new BigDecimal("102.50"),
            LocalDateTime.now(),
            "Government",
            "US Treasury",
            "Federal"
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid request with all fields should pass validation");
    }

    @Test
    @DisplayName("Request with blank symbol fails validation")
    void testBlankSymbol() {
        CreateBondRequest request = new CreateBondRequest(
            "",
            "US Treasury Bond",
            new BigDecimal("102.50"),
            LocalDateTime.now(),
            "Government",
            "US Treasury",
            "Federal"
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank symbol should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Symbol")));
    }

    @Test
    @DisplayName("Request with null symbol fails validation")
    void testNullSymbol() {
        CreateBondRequest request = new CreateBondRequest(
            null,
            "US Treasury Bond",
            new BigDecimal("102.50"),
            LocalDateTime.now(),
            "Government",
            "US Treasury",
            "Federal"
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null symbol should fail validation");
    }

    @Test
    @DisplayName("Request with blank name fails validation")
    void testBlankName() {
        CreateBondRequest request = new CreateBondRequest(
            "BND001",
            "",
            new BigDecimal("102.50"),
            LocalDateTime.now(),
            "Government",
            "US Treasury",
            "Federal"
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Blank name should fail validation");
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Name")));
    }

    @Test
    @DisplayName("Request with null name fails validation")
    void testNullName() {
        CreateBondRequest request = new CreateBondRequest(
            "BND001",
            null,
            new BigDecimal("102.50"),
            LocalDateTime.now(),
            "Government",
            "US Treasury",
            "Federal"
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Null name should fail validation");
    }

    @Test
    @DisplayName("Request with null price passes validation (optional field)")
    void testNullPrice() {
        CreateBondRequest request = new CreateBondRequest(
            "BND001",
            "US Treasury Bond",
            null,
            LocalDateTime.now(),
            "Government",
            "US Treasury",
            "Federal"
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null price is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with null tradeDate passes validation (optional field)")
    void testNullTradeDate() {
        CreateBondRequest request = new CreateBondRequest(
            "BND001",
            "US Treasury Bond",
            new BigDecimal("102.50"),
            null,
            "Government",
            "US Treasury",
            "Federal"
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null tradeDate is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with null category passes validation (optional field)")
    void testNullCategory() {
        CreateBondRequest request = new CreateBondRequest(
            "BND001",
            "US Treasury Bond",
            new BigDecimal("102.50"),
            LocalDateTime.now(),
            null,
            "US Treasury",
            "Federal"
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null category is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with null fundFamily passes validation (optional field)")
    void testNullFundFamily() {
        CreateBondRequest request = new CreateBondRequest(
            "BND001",
            "US Treasury Bond",
            new BigDecimal("102.50"),
            LocalDateTime.now(),
            "Government",
            null,
            "Federal"
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null fundFamily is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with null legalType passes validation (optional field)")
    void testNullLegalType() {
        CreateBondRequest request = new CreateBondRequest(
            "BND001",
            "US Treasury Bond",
            new BigDecimal("102.50"),
            LocalDateTime.now(),
            "Government",
            "US Treasury",
            null
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Null legalType is allowed (optional field)");
    }

    @Test
    @DisplayName("Request with all optional fields as null passes validation")
    void testAllOptionalFieldsNull() {
        CreateBondRequest request = new CreateBondRequest(
            "BND002",
            "Corporate Bond",
            null,
            null,
            null,
            null,
            null
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "All optional fields as null should pass validation");
    }

    @Test
    @DisplayName("Request with empty string optional fields passes validation")
    void testEmptyStringOptionalFields() {
        CreateBondRequest request = new CreateBondRequest(
            "BND001",
            "US Treasury Bond",
            new BigDecimal("102.50"),
            LocalDateTime.now(),
            "",
            "",
            ""
        );

        Set<ConstraintViolation<CreateBondRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Empty string optional fields are allowed (no @NotBlank)");
    }
}
