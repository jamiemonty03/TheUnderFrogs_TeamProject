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
import com.neueda.leap.exceptions.InvalidOrderException;
import com.neueda.leap.exceptions.TradingException;
import com.neueda.leap.repositories.PositionRepository;

public class OrderValidationService {
    
    private final PositionRepository positionRepository;

    public OrderValidationService(PositionRepository positionRepository) {
        if (positionRepository == null) {
            throw new IllegalArgumentException("PositionRepository cannot be null");
        }
        this.positionRepository = positionRepository;
    }

    public void validateAccount(Account account) throws AccountNotActiveException, InvalidOrderException {
        if (account == null) {
            throw new InvalidOrderException("Account cannot be null");
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
            throws InsufficientFundsException, InvalidOrderException {
        validateQuantity(quantity);
        validatePrice(price);
        
        BigDecimal requiredBalance = quantity.multiply(price);
        BigDecimal availableBalance = account.getCashBalance();
        
        if (availableBalance.compareTo(requiredBalance) < 0) {
            throw new InsufficientFundsException(
                "Insufficient funds for BUY order. Required: $" + requiredBalance + 
                ", Available: $" + availableBalance
            );
        }
    }

    public void validateSellOrder(Account account, String instrumentSymbol, BigDecimal quantity, BigDecimal price)
            throws InsufficientHoldingsException, InvalidOrderException {
        validateQuantity(quantity);
        validatePrice(price);
        
        Optional<Position> positionOpt = positionRepository.findByAccountAndSymbol(
            account.getAccountId(), 
            instrumentSymbol
        );
        
        BigDecimal holdingQuantity = positionOpt
            .map(Position::getQuantity)
            .orElse(BigDecimal.ZERO);
        
        if (holdingQuantity.compareTo(quantity) < 0) {
            throw new InsufficientHoldingsException(
                "Insufficient holdings for SELL order. Required: " + quantity + 
                " shares, Available: " + holdingQuantity + " shares of " + instrumentSymbol
            );
        }
    }

    private void validateQuantity(BigDecimal quantity) throws InvalidOrderException {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Quantity must be positive");
        }
        if (quantity.stripTrailingZeros().scale() > 0) {
            throw new InvalidOrderException("Quantity must be a whole number of shares");
        }
        if (quantity.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
            throw new InvalidOrderException("Quantity exceeds the maximum supported value");
        }
    }

    private void validatePrice(BigDecimal price) throws InvalidOrderException {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Price must be positive");
        }
    }

    public void validateOrder(Account account, Instrument instrument, OrderSide side,
                             BigDecimal quantity, BigDecimal price)
            throws AccountNotActiveException, InstrumentNotFoundException,
                   TradingException, InsufficientFundsException, InsufficientHoldingsException,
                   InvalidOrderException {

        validateAccount(account);

        validateInstrument(instrument);

        if (side == OrderSide.BUY) {
            validateBuyOrder(account, quantity, price);
        } else if (side == OrderSide.SELL) {
            validateSellOrder(account, instrument.getSymbol(), quantity, price);
        } else {
            throw new InvalidOrderException("Invalid order side: " + side);
        }
    }
}
