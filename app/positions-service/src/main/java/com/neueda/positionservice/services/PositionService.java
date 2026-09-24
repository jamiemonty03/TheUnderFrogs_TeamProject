package com.neueda.positionservice.services;
import org.springframework.stereotype.Service;
import java.util.List;
import com.neueda.positionservice.dtos.requests.UpdatePositionRequest;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.models.PositionId;
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
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("AccountId cannot be null or empty");
        }
        return positionRepository.findByAccountIdOrderBySymbol(accountId);
    }

    public Optional<Position> getPosition(String accountId, String symbol) {
        return positionRepository.findById(new PositionId(accountId, symbol));
    }

    public Position savePosition(Position position) {
        return positionRepository.save(position);
    }

    public boolean deletePosition(String accountId, String symbol) {
        PositionId id = new PositionId(accountId, symbol);
        if (!positionRepository.existsById(id)) {
            return false;
        }
        positionRepository.deleteById(id);
        return true;
    }

    public Position updatePosition(String accountId, String symbol, Position position) {
        return applyUpdate(accountId, symbol,
            position.getQuantity(), position.getAverageCost(), position.getUpdatedBy());
    }

    public Position patchPosition(String accountId, String symbol, UpdatePositionRequest request) {
        return applyUpdate(accountId, symbol,
            request.quantity(), request.averageCost(), request.updatedBy());
    }

    private Position applyUpdate(String accountId, String symbol,
                                 BigDecimal quantity, BigDecimal averageCost, String updatedBy) {
        Position existing = getPosition(accountId, symbol)
            .orElseThrow(() -> new PositionNotFoundException(accountId, symbol));

        if (quantity != null) {
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            existing.setQuantity(quantity);
        }
        if (averageCost != null) {
            if (averageCost.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Average cost cannot be negative");
            }
            existing.setAverageCost(averageCost);
        }
        if (updatedBy != null) {
            existing.setUpdatedBy(updatedBy);
        }

        existing.setVersion(existing.getVersion() + 1);
        existing.setLastUpdated(LocalDateTime.now());

        return positionRepository.save(existing);
    }

    public Position getOrCreatePosition(String accountId, String symbol) {

        Optional<Position> existing = getPosition(accountId, symbol);
        if (existing.isPresent()) {
            return existing.get();
        }

        Position newPosition = new Position(accountId, symbol, BigDecimal.ZERO, BigDecimal.ZERO);
        positionRepository.save(newPosition);
        return newPosition;
    }

    public boolean hasPosition(String accountId, String symbol) {
        return getPosition(accountId, symbol)
            .map(pos -> pos.getQuantity().compareTo(BigDecimal.ZERO) > 0)
            .orElse(false);
    }

    public int getTotalQuantity(String accountId, String symbol) {
        return getPosition(accountId, symbol)
            .map(pos -> pos.getQuantity().intValue())
            .orElse(0);
    }

    public BigDecimal getAverageCost(String accountId, String symbol) {
        return getPosition(accountId, symbol)
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

    public Position updatePositionAfterBuy(String accountId, String symbol, int quantity, BigDecimal price) {
        Position position = getOrCreatePosition(accountId, symbol);
        applyBuy(position, BigDecimal.valueOf(quantity), price);
        return positionRepository.save(position);
    }

    public Position updatePositionAfterSell(String accountId, String symbol, int quantity)
            throws InsufficientHoldingsException {
        Position position = getPosition(accountId, symbol)
            .orElseThrow(() -> new InsufficientHoldingsException(
                symbol,
                BigDecimal.valueOf(quantity),
                BigDecimal.ZERO,
                accountId
            ));

        applySell(position, BigDecimal.valueOf(quantity));

        if (position.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            positionRepository.deleteById(new PositionId(accountId, symbol));
            return position;
        }
        return positionRepository.save(position);
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



