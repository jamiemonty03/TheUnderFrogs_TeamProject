package com.neueda.positionservice.dtos.requests;

import java.math.BigDecimal;

public record UpdatePositionRequest(
    BigDecimal quantity,
    BigDecimal averageCost,
    String updatedBy
) {}
