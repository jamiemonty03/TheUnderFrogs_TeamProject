package com.neueda.tradeexecutor.dtos;

import com.neueda.tradeexecutor.enums.OrderStatus;

public record StatusUpdateRequest(
    OrderStatus expectedStatus,
    OrderStatus newStatus,
    String reason
) {}
