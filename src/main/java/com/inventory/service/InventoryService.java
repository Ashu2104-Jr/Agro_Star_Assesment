package com.inventory.service;

import com.inventory.entity.*;
import com.inventory.dto.ReservationStatus;
import com.inventory.repository.InventoryRepository;
import com.inventory.dto.*;
import com.inventory.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

@Service
public class InventoryService {
    
    private final InventoryRepository repository;
    
    @Autowired
    public InventoryService(final InventoryRepository repository) {
        this.repository = repository;
    }
    
    @Transactional
    public ProductOutput addProduct(final ProductInput input) {
        if (input == null || input.name() == null || input.stock() == null) {
            throw new InvalidRequestException("Invalid input data");
        }
        
        final Optional<Product> existingProduct = repository.findByName(input.name());
        if (existingProduct.isPresent()) {
            throw new InvalidRequestException("Product with name '" + input.name() + "' already exists");
        }
        
        final Product product = Product.builder().name(input.name()).build();
        final Product savedProduct = repository.save(product);
        
        repository.insertInventory(savedProduct.getId(), input.stock());
        
        return new ProductOutput(savedProduct.getId(), savedProduct.getName());
    }
    
    @Transactional
    public StockUpdateOutput updateStock(final String productId, final StockInput input) {
        if (productId == null || input == null || input.stock() == null) {
            throw new InvalidRequestException("Invalid input data");
        }
        
        cleanupExpiredReservations();
        
        if (!repository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        
        final Inventory currentInventory = repository.findInventoryByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product"));
        
        repository.updateTotalStock(productId, input.stock());
        
        final Integer newTotalStock = currentInventory.getTotalStock() + input.stock();
        
        return new StockUpdateOutput(productId, "Stock updated successfully", input.stock(), newTotalStock);
    }
    
    @Transactional
    public ReservationOutput reserveStock(final ReservationInput input) {
        if (input == null || input.productId() == null || input.quantity() == null) {
            throw new InvalidRequestException("Invalid input data");
        }
        
        cleanupExpiredReservations();
        
        try {
            final Inventory inventory = repository.findInventoryByProductId(input.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            
            if (inventory.getAvailableStock() < input.quantity()) {
                throw new InvalidRequestException("Out of stock. Your quantity: " + input.quantity() + ", Available: " + inventory.getAvailableStock());
            }
            
            inventory.setAvailableStock(inventory.getAvailableStock() - input.quantity());
            repository.saveInventory(inventory);
            
            final String orderId = UUID.randomUUID().toString().split("-")[0];
            final LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
            
            final Reservation savedReservation = repository.insertReservation(orderId, input.productId(), input.quantity(), expiresAt);
            
            return new ReservationOutput(savedReservation.getId(), orderId, input.productId(), 
                input.quantity(), expiresAt.toString(), "RESERVED");
            
        } catch (final OptimisticLockingFailureException e) {
            throw new InternalServerException("Stock reservation failed due to concurrent modification. Please retry.");
        } catch (final Exception e) {
            throw new InternalServerException(e.getMessage());
        }
    }
    
    @Transactional
    public OrderOutput confirmOrder(final OrderInput input) {
        if (input == null || input.orderId() == null) {
            throw new InvalidRequestException("Invalid input data");
        }
        
        cleanupExpiredReservations();
        
        final Reservation reservation = repository.findReservationByOrderId(input.orderId())
            .orElseThrow(() -> new ResourceNotFoundException("No reservation found"));
        
        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new InvalidRequestException("Timeout. Please retry.");
        }
        
        reservation.setStatus(ReservationStatus.CONFIRMED);
        repository.save(reservation);
        repository.insertOrder(input.orderId(), reservation.getProductId(), reservation.getQuantity());
        
        return new OrderOutput(input.orderId(), "CONFIRMED");
    }
    
    public StockOutput getAvailableStock(final String productId) {
        if (productId == null) {
            throw new InvalidRequestException("Product ID is required");
        }
        
        cleanupExpiredReservations();
        
        final Product product = repository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        final Inventory inventory = repository.findInventoryByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product"));
        
        return new StockOutput(productId, product.getName(), inventory.getAvailableStock());
    }
    
    @Transactional
    public void cleanupExpiredReservations() {
        final LocalDateTime now = LocalDateTime.now();
        final var expiredReservations = repository.findExpiredReservations(now);
        
        for (final Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            repository.save(reservation);
            repository.releaseStock(reservation.getProductId(), reservation.getQuantity());
        }
    }
}