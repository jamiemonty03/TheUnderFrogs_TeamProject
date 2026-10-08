package com.neueda.accountservice.exceptions;

public class DuplicateAccountException extends TradingException {

    public DuplicateAccountException(String accountId) {
        super("Account already exists: " + accountId);
    }
}
