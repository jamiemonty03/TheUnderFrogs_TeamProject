package com.neueda.tradeexecutor.exceptions;


public class SettlementRejectedException extends RuntimeException {
    public SettlementRejectedException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
