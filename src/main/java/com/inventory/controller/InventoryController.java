package com.inventory.controller;

import com.inventory.dto.*;
import com.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/products")
public class InventoryController {
    
    private final InventoryService service;
    
    @Autowired
    public InventoryController(final InventoryService service) {
        this.service = service;
    }
    
    @PostMapping
    public ResponseEntity<ProductOutput> addProduct(@Valid @RequestBody final ProductInput input) {
        final ProductOutput result = service.addProduct(input);
        return ResponseEntity.ok(result);
    }
    
    @PutMapping("/stock/{productId}")
    public ResponseEntity<StockUpdateOutput> updateStock(@PathVariable final String productId, @Valid @RequestBody final StockInput input) {
        final StockUpdateOutput result = service.updateStock(productId, input);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/reservation")
    public ResponseEntity<ReservationOutput> reserveStock(@Valid @RequestBody final ReservationInput input) {
        final ReservationOutput result = service.reserveStock(input);
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/order")
    public ResponseEntity<OrderOutput> confirmOrder(@Valid @RequestBody final OrderInput input) {
        final OrderOutput result = service.confirmOrder(input);
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/stock/{productId}")
    public ResponseEntity<StockOutput> getAvailableStock(@PathVariable final String productId) {
        final StockOutput result = service.getAvailableStock(productId);
        return ResponseEntity.ok(result);
    }
}