package com.neueda.tradeexecutor.dtos;

import com.neueda.tradeexecutor.enums.OrderStatus;

public record StatusUpdateResult(
    boolean updated,
    OrderStatus currentStatus
) {}
