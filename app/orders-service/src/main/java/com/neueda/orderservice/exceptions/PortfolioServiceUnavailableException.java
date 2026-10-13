package com.neueda.orderservice.exceptions;

public class PortfolioServiceUnavailableException extends TradingException {

    public PortfolioServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
