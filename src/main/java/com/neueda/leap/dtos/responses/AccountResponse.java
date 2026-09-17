package com.neueda.leap.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.neueda.leap.enums.AccountStatus;

public record AccountResponse(
    String accountId,
    String holderName,
    BigDecimal cashBalance,
    AccountStatus status,
    LocalDateTime lastUpdated
) {}
