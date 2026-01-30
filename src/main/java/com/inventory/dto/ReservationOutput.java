package com.inventory.dto;

public record ReservationOutput(
    String reservationId,
    String orderId,
    String productId,
    Integer quantity,
    String expiresAt,
    String status
) {}