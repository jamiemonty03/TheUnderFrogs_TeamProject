package com.neueda.tradeexecutor.dtos;

import java.math.BigDecimal;
import com.neueda.tradeexecutor.enums.OrderStatus;

public record StatusUpdateRequest(
    OrderStatus expectedStatus,
    OrderStatus newStatus,
    BigDecimal fillPrice,
    String reason
) {}
