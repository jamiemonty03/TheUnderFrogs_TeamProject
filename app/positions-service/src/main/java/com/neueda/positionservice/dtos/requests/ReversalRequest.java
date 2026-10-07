package com.neueda.positionservice.dtos.requests;

import jakarta.validation.constraints.NotBlank;

public record ReversalRequest(
    @NotBlank(message = "Order ID is required") String orderId
) {}
