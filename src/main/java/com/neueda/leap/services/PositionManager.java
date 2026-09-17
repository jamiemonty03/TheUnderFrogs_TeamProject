package com.neueda.leap.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import com.neueda.leap.models.Position;
import com.neueda.leap.repositories.PositionRepository;
import com.neueda.leap.exceptions.InsufficientHoldingsException;

/**
 * Manages positions using PositionRepository as the single source of truth.
 * 
 * Responsibilities:
 * - Load positions from repository
 * - Apply business logic via PositionService (buy/sell calculations)
 * - Persist updated positions back to repository
 * - Query holdings from repository
 * 
 */
public class PositionManager {

    private final PositionRepository positionRepository;
    private final PositionService positionService;

    public PositionManager(PositionRepository positionRepository, PositionService positionService) {
        if (positionRepository == null) {
            throw new IllegalArgumentException("PositionRepository cannot be null");
        }
        if (positionService == null) {
            throw new IllegalArgumentException("PositionService cannot be null");
        }
        this.positionRepository = positionRepository;
        this.positionService = positionService;
    }

    public Position getOrCreatePosition(String accountId, String symbol) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or blank");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }

        Optional<Position> existing = positionRepository.findByAccountAndSymbol(accountId, symbol);
        if (existing.isPresent()) {
            return existing.get();
        }

        Position newPosition = new Position(accountId, symbol, BigDecimal.ZERO, BigDecimal.ZERO);
        return positionRepository.save(newPosition);
    }

    public Optional<Position> getPosition(String accountId, String symbol) {
        return positionRepository.findByAccountAndSymbol(accountId, symbol);
    }

    public int getTotalQuantity(String accountId, String symbol) {
        return positionRepository.findByAccountAndSymbol(accountId, symbol)
            .map(pos -> pos.getQuantity().intValue())
            .orElse(0);
    }

    public BigDecimal getAverageCost(String accountId, String symbol) {
        return positionRepository.findByAccountAndSymbol(accountId, symbol)
            .map(Position::getAverageCost)
            .orElse(BigDecimal.ZERO);
    }

    public boolean hasPosition(String accountId, String symbol) {
        return positionRepository.findByAccountAndSymbol(accountId, symbol)
            .map(pos -> pos.getQuantity().compareTo(BigDecimal.ZERO) > 0)
            .orElse(false);
    }

    public void updatePositionAfterBuy(String accountId, String symbol, int buyQuantity, BigDecimal buyPrice) {
        Position position = getOrCreatePosition(accountId, symbol);
        positionService.applyBuy(position, BigDecimal.valueOf(buyQuantity), buyPrice);
        positionRepository.save(position);
    }

    public void updatePositionAfterSell(String accountId, String symbol, int sellQuantity) 
            throws InsufficientHoldingsException {
        Position position = positionRepository.findByAccountAndSymbol(accountId, symbol)
            .orElseThrow(() -> new InsufficientHoldingsException(
                symbol,
                BigDecimal.valueOf(sellQuantity),
                BigDecimal.ZERO,
                accountId
            ));
        positionService.applySell(position, BigDecimal.valueOf(sellQuantity));

        if (position.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            positionRepository.delete(accountId, symbol);
        } else {
            positionRepository.save(position);
        }
    }

}
}
