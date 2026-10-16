package com.neueda.positionservice.exceptions;

public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(String accountId, String portfolioId) {
        super("Portfolio " + portfolioId + " not found for account " + accountId);
    }
}
