package com.neueda.accountservice.dtos.responses;

import java.math.BigDecimal;

public record TopUpConfirmResponse(BigDecimal amount, BigDecimal cashBalance) {}
