package com.neueda.positionservice.dtos.requests;

import java.math.BigDecimal;

public class UpdatePositionRequest {
    
    private BigDecimal quantity;
    
    private BigDecimal averageCost;
    
    private String updatedBy;
    
    // Default constructor
    public UpdatePositionRequest() {
    }
    
    // Constructor with parameters
    public UpdatePositionRequest(BigDecimal quantity, BigDecimal averageCost, String updatedBy) {
        this.quantity = quantity;
        this.averageCost = averageCost;
        this.updatedBy = updatedBy;
    }
    
    // Getters and Setters
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public BigDecimal getAverageCost() {
        return averageCost;
    }
    
    public void setAverageCost(BigDecimal averageCost) {
        this.averageCost = averageCost;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    @Override
    public String toString() {
        return "UpdatePositionRequest{" +
                "quantity=" + quantity +
                ", averageCost=" + averageCost +
                ", updatedBy='" + updatedBy + '\'' +
                '}';
    }
}
