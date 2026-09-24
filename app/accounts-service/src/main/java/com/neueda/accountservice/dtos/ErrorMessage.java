package com.neueda.accountservice.dtos;

public record ErrorMessage(
    String code,
    String message,
    int status
) {
    public ErrorMessage(String code, String message) {
        this(code, message, 422);
    }
}
