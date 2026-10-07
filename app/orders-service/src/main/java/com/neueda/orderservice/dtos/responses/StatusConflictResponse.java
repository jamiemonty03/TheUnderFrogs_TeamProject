package com.neueda.orderservice.dtos.responses;

import com.neueda.orderservice.enums.OrderStatus;

public record StatusConflictResponse(String errorCode, String message, OrderStatus currentStatus) {}
