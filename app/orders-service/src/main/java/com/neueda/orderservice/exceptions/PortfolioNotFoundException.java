package com.neueda.orderservice.exceptions;

public class PortfolioNotFoundException extends TradingException {

    public PortfolioNotFoundException(String accountId, String portfolioId) {
        super("Portfolio " + portfolioId + " not found for account " + accountId);
    }
}
