package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductInput(
    @NotBlank(message = "Name is required") String name,
    @NotNull(message = "Stock is required") @Positive(message = "Stock must be positive") Integer stock
) {}