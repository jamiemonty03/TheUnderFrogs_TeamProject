package com.neueda.instrumentservice.exceptions;

import java.math.BigDecimal;

public class InsufficientFundsException extends TradingException {
    

    public InsufficientFundsException(BigDecimal required, BigDecimal available, String accountId) {
        super("Insufficient funds in account " + accountId + 
              ": required " + required + ", available " + available);
    }

    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException() {
        super("Insufficient funds");
    }
}
