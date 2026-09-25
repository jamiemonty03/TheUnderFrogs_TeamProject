package com.neueda.orderservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.exceptions.InvalidOrderException;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;

class OrderValidationEdgeCaseTest {

    private final OrderValidationService validationService = new OrderValidationService();
    private final Account account = new Account("ACC001", "John Doe", new BigDecimal("1000000.00"), AccountStatus.ACTIVE);
    private final Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);

    @Test
    @DisplayName("Null quantity is rejected")
    void rejectsNullQuantity() {
        InvalidOrderException ex = assertThrows(InvalidOrderException.class, () ->
            validationService.validateOrder(account, instrument, OrderSide.SELL, null, BigDecimal.TEN));
        assertEquals("Quantity must be positive", ex.getMessage());
    }

    @Test
    @DisplayName("Null price is rejected")
    void rejectsNullPrice() {
        InvalidOrderException ex = assertThrows(InvalidOrderException.class, () ->
            validationService.validateOrder(account, instrument, OrderSide.SELL, BigDecimal.ONE, null));
        assertEquals("Price must be positive", ex.getMessage());
    }

    @Test
    @DisplayName("Quantity larger than Integer.MAX_VALUE is rejected")
    void rejectsQuantityAboveIntRange() {
        BigDecimal tooLarge = BigDecimal.valueOf(Integer.MAX_VALUE).add(BigDecimal.ONE);
        InvalidOrderException ex = assertThrows(InvalidOrderException.class, () ->
            validationService.validateOrder(account, instrument, OrderSide.SELL, tooLarge, BigDecimal.TEN));
        assertEquals("Quantity exceeds the maximum supported value", ex.getMessage());
    }

    @Test
    @DisplayName("Null order side is rejected")
    void rejectsNullSide() {
        InvalidOrderException ex = assertThrows(InvalidOrderException.class, () ->
            validationService.validateOrder(account, instrument, null, BigDecimal.ONE, BigDecimal.TEN));
        assertEquals("Invalid order side: null", ex.getMessage());
    }
}
