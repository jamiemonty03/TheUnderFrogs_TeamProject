package com.neueda.positionservice.dtos.requests;

import com.neueda.positionservice.enums.PortfolioRisk;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePortfolioRequest(
    @NotBlank(message = "Portfolio name is required")
    @Size(max = 100, message = "Portfolio name must be 100 characters or fewer")
    String name,

    @NotNull(message = "Risk must be LOW, MEDIUM or HIGH")
    PortfolioRisk risk,

    @Size(max = 100, message = "Benchmark must be 100 characters or fewer")
    String benchmark
) {}
