package com.neueda.leap.services;

import java.math.BigDecimal;
import java.util.Optional;
import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.models.Position;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.exceptions.AccountNotActiveException;
import com.neueda.leap.exceptions.InstrumentNotFoundException;
import com.neueda.leap.exceptions.InsufficientFundsException;
import com.neueda.leap.exceptions.InsufficientHoldingsException;
import com.neueda.leap.exceptions.TradingException;
import com.neueda.leap.repositories.PositionRepository;

/**
 * Service for validating orders against business rules.
 * 
 * Enforces the following validation rules:
 * - BUY: Account balance >= (Quantity × Price)
 * - SELL: Holdings >= Quantity
 * - Instrument must be tradable
 * - Account must be ACTIVE
 * 
 * Throws appropriate exceptions if any rule is violated.
 */
public class OrderValidationService {
    
    private final PositionRepository positionRepository;

    public OrderValidationService(PositionRepository positionRepository) {
        if (positionRepository == null) {
            throw new IllegalArgumentException("PositionRepository cannot be null");
        }
        this.positionRepository = positionRepository;
    }

    public void validateAccount(Account account) throws AccountNotActiveException {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (!account.isActive()) {
            throw new AccountNotActiveException(
                "Cannot place order: Account " + account.getAccountId() + 
                " status is " + account.getStatus()
            );
        }
    }

    public void validateInstrument(Instrument instrument) 
            throws InstrumentNotFoundException, TradingException {
        if (instrument == null) {
            throw new InstrumentNotFoundException("Instrument not found");
        }
        if (!instrument.isTradable()) {
            throw new TradingException(
                "Instrument " + instrument.getSymbol() + 
                " is not tradable"
            );
        }
    }

    public void validateBuyOrder(Account account, BigDecimal quantity, BigDecimal price) 
            throws InsufficientFundsException {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        
        BigDecimal requiredBalance = quantity.multiply(price);
        BigDecimal availableBalance = account.getCashBalance();
        
        if (availableBalance.compareTo(requiredBalance) < 0) {
            throw new InsufficientFundsException(
                "Insufficient funds for BUY order. Required: $" + requiredBalance + 
                ", Available: $" + availableBalance
            );
        }
    }

    public void validateSellOrder(Account account, String instrumentSymbol, BigDecimal quantity) 
            throws InsufficientHoldingsException {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        // Get position from repository
        Optional<Position> positionOpt = positionRepository.findByAccountAndSymbol(
            account.getAccountId(), 
            instrumentSymbol
        );
        
        // If no position exists, holdings are zero
        BigDecimal holdingQuantity = positionOpt
            .map(Position::getQuantity)
            .orElse(BigDecimal.ZERO);
        
        // Validate that holdings >= sell quantity
        if (holdingQuantity.compareTo(quantity) < 0) {
            throw new InsufficientHoldingsException(
                "Insufficient holdings for SELL order. Required: " + quantity + 
                " shares, Available: " + holdingQuantity + " shares of " + instrumentSymbol
            );
        }
    }

    public void validateOrder(Account account, Instrument instrument, OrderSide side,
                             BigDecimal quantity, BigDecimal price) 
            throws AccountNotActiveException, InstrumentNotFoundException, 
                   TradingException, InsufficientFundsException, InsufficientHoldingsException {
        
        // Rule 1: Validate account is ACTIVE
        validateAccount(account);
        
        // Rule 2: Validate instrument is tradable
        validateInstrument(instrument);
        
        // Rule 3 & 4: Validate based on order side
        if (side == OrderSide.BUY) {
            validateBuyOrder(account, quantity, price);
        } else if (side == OrderSide.SELL) {
            validateSellOrder(account, instrument.getSymbol(), quantity);
        } else {
            throw new IllegalArgumentException("Invalid order side: " + side);
        }
    }
}
