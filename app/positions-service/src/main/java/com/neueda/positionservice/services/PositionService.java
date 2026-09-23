package com.neueda.positionservice.services;
import org.springframework.stereotype.Service;
import java.util.List;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.exceptions.InsufficientHoldingsException;
import com.neueda.positionservice.exceptions.PositionNotFoundException;
import com.neueda.positionservice.repositories.PositionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PositionService {

    private static final int DECIMAL_PLACES = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        if (positionRepository == null) {
            throw new IllegalArgumentException("PositionRepository cannot be null");
        }
        this.positionRepository = positionRepository;
    }

     public List<Position> getPositionsByAccountId(String accountId) {
        return positionRepository.findByAccountId(accountId);
    }

    public Optional<Position> getPosition(String accountId, String symbol) {
        return positionRepository.findByAccountAndSymbol(accountId, symbol);
    }

    public Position savePosition(Position position) {
        if (positionRepository.exists(position.getAccountId(), position.getSymbol())) {
            positionRepository.update(position);
        } else {
            positionRepository.save(position);
        }
        return position;
    }
 
    public boolean deletePosition(String accountId, String symbol) {
        return positionRepository.delete(accountId, symbol);
    }

    public Position updatePosition(String accountId, String symbol, Position position) {
        Position existing = positionRepository.findByAccountAndSymbol(accountId, symbol)
            .orElseThrow(() -> new PositionNotFoundException(accountId, symbol));
        
        if (position.getQuantity() != null) {
            if (position.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            existing.setQuantity(position.getQuantity());
        }
        if (position.getAverageCost() != null) {
            if (position.getAverageCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Average cost cannot be negative");
            }
            existing.setAverageCost(position.getAverageCost());
        }
        if (position.getUpdatedBy() != null) {
            existing.setUpdatedBy(position.getUpdatedBy());
        }
        
        existing.setVersion(existing.getVersion() + 1);
        existing.setLastUpdated(LocalDateTime.now());
        
        positionRepository.update(existing);
        return existing;
    }

    public Position getOrCreatePosition(String accountId, String symbol) {

        Optional<Position> existing = positionRepository.findByAccountAndSymbol(accountId, symbol);
        if (existing.isPresent()) {
            return existing.get();
        }

        Position newPosition = new Position(accountId, symbol, BigDecimal.ZERO, BigDecimal.ZERO);
        positionRepository.save(newPosition);
        return newPosition;
    }

    public boolean hasPosition(String accountId, String symbol) {
        return positionRepository.findByAccountAndSymbol(accountId, symbol)
            .map(pos -> pos.getQuantity().compareTo(BigDecimal.ZERO) > 0)
            .orElse(false);
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

    public void applyBuy(Position position, BigDecimal quantity, BigDecimal price) {
        validatePositionAndAmount(position, quantity, price, "Quantity and price must be greater than zero");

        BigDecimal currentQuantity = position.getQuantity();
        BigDecimal currentAverageCost = position.getAverageCost();

        BigDecimal currentCostBasis = currentAverageCost.multiply(currentQuantity);
        BigDecimal purchaseCost = price.multiply(quantity);
        BigDecimal newQuantity = currentQuantity.add(quantity);

        BigDecimal newAverageCost = currentCostBasis.add(purchaseCost)
            .divide(newQuantity, DECIMAL_PLACES, ROUNDING_MODE);

        position.setQuantity(newQuantity);
        position.setAverageCost(newAverageCost);
        position.setVersion(position.getVersion() + 1);
        position.setLastUpdated(LocalDateTime.now());
    }

    public void applySell(Position position, BigDecimal quantity) throws InsufficientHoldingsException {
        validatePositionAndAmount(position, quantity, null, "Quantity must be greater than zero");

        BigDecimal currentQuantity = position.getQuantity();

        if (quantity.compareTo(currentQuantity) > 0) {
            throw new InsufficientHoldingsException(
                position.getSymbol(),
                quantity,
                currentQuantity,
                position.getAccountId()
            );
        }

        BigDecimal newQuantity = currentQuantity.subtract(quantity);
        position.setQuantity(newQuantity);
        position.setVersion(position.getVersion() + 1);
        position.setLastUpdated(LocalDateTime.now());
    }

    public void updatePositionAfterBuy(String accountId, String symbol, int quantity, BigDecimal price) {
        Position position = getOrCreatePosition(accountId, symbol);
        applyBuy(position, BigDecimal.valueOf(quantity), price);
        positionRepository.save(position);
    }

    public void updatePositionAfterSell(String accountId, String symbol, int quantity)
            throws InsufficientHoldingsException {
        Position position = positionRepository.findByAccountAndSymbol(accountId, symbol)
            .orElseThrow(() -> new InsufficientHoldingsException(
                symbol,
                BigDecimal.valueOf(quantity),
                BigDecimal.ZERO,
                accountId
            ));

        applySell(position, BigDecimal.valueOf(quantity));

        if (position.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            positionRepository.delete(accountId, symbol);
        } else {
            positionRepository.save(position);
        }
    }

    private void validatePositionAndAmount(Position position, BigDecimal quantity, BigDecimal price, String amountMessage) {
        if (position == null || quantity == null) {
            throw new IllegalArgumentException("Position and quantity must not be null");
        }

        if (price != null && price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(amountMessage);
        }

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(amountMessage);
        }
    }
}



