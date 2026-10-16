package com.neueda.positionservice.dtos.requests;

import com.neueda.positionservice.enums.PortfolioRisk;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePortfolioRequest(
    @Size(max = 100, message = "Portfolio name must be 100 characters or fewer")
    @Pattern(regexp = ".*\\S.*", message = "Portfolio name cannot be blank")
    String name,

    PortfolioRisk risk,

    @Size(max = 100, message = "Benchmark must be 100 characters or fewer")
    String benchmark
) {}
