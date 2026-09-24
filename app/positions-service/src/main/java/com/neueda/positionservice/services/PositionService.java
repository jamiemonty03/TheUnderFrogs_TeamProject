package com.neueda.positionservice.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.neueda.positionservice.dtos.requests.CreatePositionRequest;
import com.neueda.positionservice.dtos.requests.ReplacePositionRequest;
import com.neueda.positionservice.dtos.requests.UpdatePositionRequest;
import com.neueda.positionservice.exceptions.InsufficientHoldingsException;
import com.neueda.positionservice.exceptions.PositionNotFoundException;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.models.PositionId;
import com.neueda.positionservice.repositories.PositionRepository;

@Service
public class PositionService {

    private static final int DECIMAL_PLACES = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public List<Position> getPositionsByAccountId(String accountId) {
        return positionRepository.findByAccountIdOrderBySymbol(accountId);
    }

    public Optional<Position> getPosition(String accountId, String symbol) {
        return positionRepository.findById(new PositionId(accountId, symbol));
    }

    public Position createPosition(CreatePositionRequest request) {
        Optional<Position> existing = getPosition(request.accountId(), request.symbol());
        if (existing.isPresent()) {
            return applyUpdate(existing.get(), request.quantity(), request.averageCost());
        }
        return positionRepository.save(new Position(
            request.accountId(), request.symbol(), request.quantity(), request.averageCost()));
    }

    public void deletePosition(String accountId, String symbol) {
        PositionId id = new PositionId(accountId, symbol);
        if (!positionRepository.existsById(id)) {
            throw new PositionNotFoundException(accountId, symbol);
        }
        positionRepository.deleteById(id);
    }

    public Position updatePosition(String accountId, String symbol, ReplacePositionRequest request) {
        return applyUpdate(findExisting(accountId, symbol), request.quantity(), request.averageCost());
    }

    public Position patchPosition(String accountId, String symbol, UpdatePositionRequest request) {
        return applyUpdate(findExisting(accountId, symbol), request.quantity(), request.averageCost());
    }

    public Position updatePositionAfterBuy(String accountId, String symbol, int quantity, BigDecimal price) {
        Position position = getPosition(accountId, symbol)
            .orElseGet(() -> new Position(accountId, symbol, BigDecimal.ZERO, BigDecimal.ZERO));
        applyBuy(position, BigDecimal.valueOf(quantity), price);
        return positionRepository.save(position);
    }

    public Position updatePositionAfterSell(String accountId, String symbol, int quantity)
            throws InsufficientHoldingsException {
        Position position = getPosition(accountId, symbol)
            .orElseThrow(() -> new InsufficientHoldingsException(
                symbol, BigDecimal.valueOf(quantity), BigDecimal.ZERO, accountId));

        applySell(position, BigDecimal.valueOf(quantity));

        if (position.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            positionRepository.deleteById(new PositionId(accountId, symbol));
            return position;
        }
        return positionRepository.save(position);
    }

    public void applyBuy(Position position, BigDecimal quantity, BigDecimal price) {
        requirePosition(position);
        requirePositive(quantity, "Quantity and price must be greater than zero");
        requirePositive(price, "Quantity and price must be greater than zero");

        BigDecimal currentCostBasis = position.getAverageCost().multiply(position.getQuantity());
        BigDecimal purchaseCost = price.multiply(quantity);
        BigDecimal newQuantity = position.getQuantity().add(quantity);

        position.setQuantity(newQuantity);
        position.setAverageCost(currentCostBasis.add(purchaseCost)
            .divide(newQuantity, DECIMAL_PLACES, ROUNDING_MODE));
    }

    public void applySell(Position position, BigDecimal quantity) throws InsufficientHoldingsException {
        requirePosition(position);
        requirePositive(quantity, "Quantity must be greater than zero");

        if (quantity.compareTo(position.getQuantity()) > 0) {
            throw new InsufficientHoldingsException(
                position.getSymbol(), quantity, position.getQuantity(), position.getAccountId());
        }

        position.setQuantity(position.getQuantity().subtract(quantity));
    }

    private Position findExisting(String accountId, String symbol) {
        return getPosition(accountId, symbol)
            .orElseThrow(() -> new PositionNotFoundException(accountId, symbol));
    }

    private Position applyUpdate(Position existing, BigDecimal quantity, BigDecimal averageCost) {
        if (quantity != null) {
            requirePositive(quantity, "Quantity must be positive");
            existing.setQuantity(quantity);
        }
        if (averageCost != null) {
            if (averageCost.signum() < 0) {
                throw new IllegalArgumentException("Average cost cannot be negative");
            }
            existing.setAverageCost(averageCost);
        }
        return positionRepository.save(existing);
    }

    private static void requirePosition(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position must not be null");
        }
    }

    private static void requirePositive(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
