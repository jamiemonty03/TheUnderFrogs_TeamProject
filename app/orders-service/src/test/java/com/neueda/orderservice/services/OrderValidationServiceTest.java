package com.neueda.orderservice.services;

import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.exceptions.AccountNotActiveException;
import com.neueda.orderservice.exceptions.InstrumentNotFoundException;
import com.neueda.orderservice.exceptions.InsufficientFundsException;
import com.neueda.orderservice.exceptions.InsufficientHoldingsException;
import com.neueda.orderservice.exceptions.InvalidOrderException;
import com.neueda.orderservice.exceptions.TradingException;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Position;
import com.neueda.orderservice.repositories.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("OrderValidationService Tests")
public class OrderValidationServiceTest {

    private OrderValidationService validationService;

    @Mock
    private PositionRepository positionRepository;

    private Account activeAccount;
    private Account inactiveAccount;
    private Instrument tradableInstrument;
    private Instrument untradableInstrument;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validationService = new OrderValidationService(positionRepository);

        activeAccount = new Account("ACC001", "John Doe", new BigDecimal("20000.00"), AccountStatus.ACTIVE);
        inactiveAccount = new Account("ACC002", "Jane Roe", new BigDecimal("20000.00"), AccountStatus.SUSPENDED);
        tradableInstrument = new Instrument("AAPL", "Apple Inc.", new BigDecimal("150.00"), true);
        untradableInstrument = new Instrument("HALT", "Halted Corp.", new BigDecimal("10.00"), false);

        when(positionRepository.findByAccountIdAndSymbol(anyString(), anyString()))
            .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Valid BUY order passes validation")
    void validBuyOrderPasses() throws TradingException {
        assertDoesNotThrow(() -> validationService.validateOrder(
            activeAccount, tradableInstrument, OrderSide.BUY,
            new BigDecimal("10"), new BigDecimal("100.00")
        ));
    }

    @Test
    @DisplayName("Valid SELL order passes validation when holdings are sufficient")
    void validSellOrderPasses() throws TradingException {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("50"), new BigDecimal("120.00"));
        when(positionRepository.findByAccountIdAndSymbol("ACC001", "AAPL"))
            .thenReturn(Optional.of(position));

        assertDoesNotThrow(() -> validationService.validateOrder(
            activeAccount, tradableInstrument, OrderSide.SELL,
            new BigDecimal("10"), new BigDecimal("100.00")
        ));
    }

    @Test
    @DisplayName("Order rejected when account is not active")
    void rejectsInactiveAccount() {
        assertThrows(AccountNotActiveException.class, () -> validationService.validateOrder(
            inactiveAccount, tradableInstrument, OrderSide.BUY,
            new BigDecimal("10"), new BigDecimal("100.00")
        ));
    }

    @Test
    @DisplayName("Order rejected when account is null")
    void rejectsNullAccount() {
        assertThrows(InvalidOrderException.class, () -> validationService.validateOrder(
            null, tradableInstrument, OrderSide.BUY,
            new BigDecimal("10"), new BigDecimal("100.00")
        ));
    }

    @Test
    @DisplayName("Order rejected when instrument is not found")
    void rejectsMissingInstrument() {
        assertThrows(InstrumentNotFoundException.class, () -> validationService.validateOrder(
            activeAccount, null, OrderSide.BUY,
            new BigDecimal("10"), new BigDecimal("100.00")
        ));
    }

    @Test
    @DisplayName("Order rejected when instrument is not tradable")
    void rejectsUntradableInstrument() {
        assertThrows(TradingException.class, () -> validationService.validateOrder(
            activeAccount, untradableInstrument, OrderSide.BUY,
            new BigDecimal("10"), new BigDecimal("100.00")
        ));
    }

    @Test
    @DisplayName("BUY order rejected when quantity is zero or negative")
    void rejectsNonPositiveQuantityOnBuy() {
        assertThrows(InvalidOrderException.class, () -> validationService.validateOrder(
            activeAccount, tradableInstrument, OrderSide.BUY,
            BigDecimal.ZERO, new BigDecimal("100.00")
        ));
        assertThrows(InvalidOrderException.class, () -> validationService.validateOrder(
            activeAccount, tradableInstrument, OrderSide.BUY,
            new BigDecimal("-5"), new BigDecimal("100.00")
        ));
    }

    @Test
    @DisplayName("BUY order rejected when price is zero or negative")
    void rejectsNonPositivePriceOnBuy() {
        assertThrows(InvalidOrderException.class, () -> validationService.validateOrder(
            activeAccount, tradableInstrument, OrderSide.BUY,
            new BigDecimal("10"), BigDecimal.ZERO
        ));
    }

    @Test
    @DisplayName("BUY order rejected when cash balance is insufficient")
    void rejectsInsufficientFundsOnBuy() {
        assertThrows(InsufficientFundsException.class, () -> validationService.validateOrder(
            activeAccount, tradableInstrument, OrderSide.BUY,
            new BigDecimal("1000"), new BigDecimal("100.00")
        ));
    }

    @Test
    @DisplayName("SELL order rejected when quantity is zero or negative")
    void rejectsNonPositiveQuantityOnSell() {
        assertThrows(InvalidOrderException.class, () -> validationService.validateOrder(
            activeAccount, tradableInstrument, OrderSide.SELL,
            BigDecimal.ZERO, new BigDecimal("100.00")
        ));
    }

    @Test
    @DisplayName("SELL order rejected when holdings are insufficient")
    void rejectsInsufficientHoldingsOnSell() {
        Position position = new Position("ACC001", "AAPL", new BigDecimal("5"), new BigDecimal("120.00"));
        when(positionRepository.findByAccountIdAndSymbol("ACC001", "AAPL"))
            .thenReturn(Optional.of(position));

        assertThrows(InsufficientHoldingsException.class, () -> validationService.validateOrder(
            activeAccount, tradableInstrument, OrderSide.SELL,
            new BigDecimal("10"), new BigDecimal("100.00")
        ));
    }

    @Test
    @DisplayName("SELL order rejected when no position is held")
    void rejectsSellWithNoPosition() {
        assertThrows(InsufficientHoldingsException.class, () -> validationService.validateOrder(
            activeAccount, tradableInstrument, OrderSide.SELL,
            new BigDecimal("10"), new BigDecimal("100.00")
        ));
    }
}
