package com.neueda.leap.services;

import com.neueda.leap.models.Position;
import com.neueda.leap.exceptions.InsufficientHoldingsException;
import java.math.BigDecimal;
import java.math.RoundingMode;



public class PositionService {
    
    public void applyBuy(Position position, BigDecimal quantity, BigDecimal price) {

        if (position == null || quantity == null || price == null) {
            throw new IllegalArgumentException("Position, quantity, and price must not be null");
        }

        if (quantity.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity and price must be greater than zero");
        }

        BigDecimal currentQuantity = position.getQuantity();
        BigDecimal currentAverageCost = position.getAverageCost();

        BigDecimal currentCostBasis = currentAverageCost.multiply(currentQuantity);

        BigDecimal purchaseCost = price.multiply(quantity);
        BigDecimal newQuantity = currentQuantity.add(quantity);
        BigDecimal newAverageCost = currentCostBasis.add(purchaseCost).divide(newQuantity, 4, RoundingMode.HALF_UP);


        position.setQuantity(newQuantity);
        position.setAverageCost(newAverageCost);
    }

    public void applySell(Position position, BigDecimal quantity) throws InsufficientHoldingsException {

        if (position == null || quantity == null) {
            throw new IllegalArgumentException("Position and quantity must not be null");
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        BigDecimal currentQuantity = position.getQuantity();

        if (quantity.compareTo(currentQuantity) > 0) {
            throw new InsufficientHoldingsException(position.getSymbol(), quantity, currentQuantity, position.getAccountId());
        }

        BigDecimal newQuantity = currentQuantity.subtract(quantity);
        position.setQuantity(newQuantity);
    }
}



