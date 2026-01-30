package com.inventory.dto;

public record StockUpdateOutput(String productId, String message, Integer addedStock, Integer newTotalStock) {}