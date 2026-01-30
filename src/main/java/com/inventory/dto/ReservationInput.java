package com.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReservationInput(
    @NotNull(message = "Product ID is required") String productId,
    @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be positive") Integer quantity
) {}