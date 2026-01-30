package com.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderInput(
    @NotBlank(message = "Order ID is required") String orderId
) {}