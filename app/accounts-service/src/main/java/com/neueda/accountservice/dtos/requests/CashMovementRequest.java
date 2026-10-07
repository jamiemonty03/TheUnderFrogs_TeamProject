package com.neueda.accountservice.dtos.requests;

import java.math.BigDecimal;

public record CashMovementRequest(
    BigDecimal amount,
    String orderId
) {}
