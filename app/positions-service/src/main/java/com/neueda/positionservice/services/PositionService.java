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
import org.springframework.transaction.annotation.Transactional;
import com.neueda.positionservice.enums.MovementType;
import com.neueda.positionservice.models.PositionMovement;
import com.neueda.positionservice.repositories.PositionMovementRepository;

@Service
public class PositionService {

    private static final int DECIMAL_PLACES = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final PositionRepository positionRepository;
    private final PositionMovementRepository positionMovementRepository;

    public PositionService(PositionRepository positionRepository, PositionMovementRepository positionMovementRepository) {
        this.positionRepository = positionRepository;
        this.positionMovementRepository = positionMovementRepository;
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

    @Transactional(rollbackFor = Exception.class)
    public Position updatePositionAfterBuy(String accountId, String symbol, int quantity, BigDecimal price) {
        return updatePositionAfterBuy(accountId, symbol, quantity, price, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Position updatePositionAfterBuy(String accountId, String symbol, int quantity, BigDecimal price,
                                           String orderId) {
        BigDecimal shares = BigDecimal.valueOf(quantity);
        requirePositive(shares, "Quantity and price must be greater than zero");
        requirePositive(price, "Quantity and price must be greater than zero");

        Position position = getPosition(accountId, symbol)
            .orElseGet(() -> emptyPosition(accountId, symbol));

        if (isRepeat(orderId, MovementType.BUY, accountId, symbol, shares, price)) {
            return position;
        }

        applyBuy(position, shares, price);
        return positionRepository.save(position);
    }

    @Transactional(rollbackFor = Exception.class)
    public Position updatePositionAfterSell(String accountId, String symbol, int quantity)
            throws InsufficientHoldingsException {
        return updatePositionAfterSell(accountId, symbol, quantity, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Position updatePositionAfterSell(String accountId, String symbol, int quantity, String orderId)
            throws InsufficientHoldingsException {
        BigDecimal shares = BigDecimal.valueOf(quantity);
        requirePositive(shares, "Quantity must be greater than zero");

        Optional<Position> existing = getPosition(accountId, symbol);
        BigDecimal averageCost = existing.map(Position::getAverageCost).orElse(BigDecimal.ZERO);

        if (isRepeat(orderId, MovementType.SELL, accountId, symbol, shares, averageCost)) {
            return existing.orElseGet(() -> emptyPosition(accountId, symbol));
        }

        Position position = existing.orElseThrow(() -> new InsufficientHoldingsException(
            symbol, shares, BigDecimal.ZERO, accountId));

        applySell(position, shares);

        if (position.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            positionRepository.deleteById(new PositionId(accountId, symbol));
            return position;
        }
        return positionRepository.save(position);
    }

    @Transactional(rollbackFor = Exception.class)
    public Position reverse(String accountId, String symbol, String orderId) throws InsufficientHoldingsException {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required for a reversal");
        }

        Position position = getPosition(accountId, symbol)
            .orElseGet(() -> emptyPosition(accountId, symbol));

        List<PositionMovement> originals = positionMovementRepository.findByOrderId(orderId).stream()
            .filter(movement -> movement.getMovementType() != MovementType.REVERSAL)
            .toList();

        if (originals.isEmpty()) {
            return position;
        }
        if (originals.size() > 1) {
            throw new IllegalArgumentException("Order " + orderId + " has more than one position movement to reverse");
        }

        PositionMovement original = originals.get(0);
        if (!original.getAccountId().equals(accountId) || !original.getSymbol().equals(symbol)) {
            throw new IllegalArgumentException("Order " + orderId + " belongs to a different position");
        }

        if (isRepeat(orderId, MovementType.REVERSAL, accountId, symbol, original.getQuantity(), original.getPrice())) {
            return position;
        }

        if (original.getMovementType() == MovementType.SELL) {
            addShares(position, original.getQuantity(), original.getPrice());
            return positionRepository.save(position);
        }
        return removeShares(position, original.getQuantity(), original.getPrice());
    }

    public void applyBuy(Position position, BigDecimal quantity, BigDecimal price) {
        requirePosition(position);
        requirePositive(quantity, "Quantity and price must be greater than zero");
        requirePositive(price, "Quantity and price must be greater than zero");
        addShares(position, quantity, price);
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
    
    private boolean isRepeat(String orderId, MovementType type, String accountId, String symbol, BigDecimal quantity, BigDecimal price) {
        return orderId != null && positionMovementRepository.insertIfAbsent(
                orderId, type.name(), accountId, symbol, quantity, price) == 0;
    }

    private static void addShares(Position position, BigDecimal quantity, BigDecimal price) {
        BigDecimal currentCostBasis = position.getAverageCost().multiply(position.getQuantity());
        BigDecimal newQuantity = position.getQuantity().add(quantity);

        position.setQuantity(newQuantity);
        position.setAverageCost(currentCostBasis.add(price.multiply(quantity))
            .divide(newQuantity, DECIMAL_PLACES, ROUNDING_MODE));
    }

    private Position removeShares(Position position, BigDecimal quantity, BigDecimal price)
            throws InsufficientHoldingsException {
        if (quantity.compareTo(position.getQuantity()) > 0) {
            throw new InsufficientHoldingsException(
                position.getSymbol(), quantity, position.getQuantity(), position.getAccountId());
        }

        BigDecimal remaining = position.getQuantity().subtract(quantity);
        if (remaining.signum() == 0) {
            positionRepository.deleteById(new PositionId(position.getAccountId(), position.getSymbol()));
            position.setQuantity(BigDecimal.ZERO);
            return position;
        }

        BigDecimal remainingCost = position.getAverageCost().multiply(position.getQuantity())
            .subtract(price.multiply(quantity))
            .max(BigDecimal.ZERO);
        position.setQuantity(remaining);
        position.setAverageCost(remainingCost.divide(remaining, DECIMAL_PLACES, ROUNDING_MODE));
        return positionRepository.save(position);
    }

    private static Position emptyPosition(String accountId, String symbol) {
        return new Position(accountId, symbol, BigDecimal.ZERO, BigDecimal.ZERO);
    }

}
