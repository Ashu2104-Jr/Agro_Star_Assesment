package com.inventory.repository;

import com.inventory.entity.*;
import com.inventory.dto.ReservationStatus;
import com.inventory.exception.InternalServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class InventoryRepository {
    
    private final EntityManager entityManager;
    
    @Autowired
    public InventoryRepository(final EntityManager entityManager) {
        this.entityManager = entityManager;
    }
    
    public Product save(final Product product) {
        try {
            if (product.getId() == null) {
                entityManager.persist(product);
                return product;
            } else {
                return entityManager.merge(product);
            }
        } catch (final Exception e) {
            throw new InternalServerException("Failed to save product: " + e.getMessage());
        }
    }
    
    public boolean existsById(final String productId) {
        try {
            return entityManager.find(Product.class, productId) != null;
        } catch (final Exception e) {
            throw new InternalServerException("Failed to check product existence: " + e.getMessage());
        }
    }
    
    public Optional<Product> findById(final String productId) {
        try {
            final Product product = entityManager.find(Product.class, productId);
            return Optional.ofNullable(product);
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
    
    public Optional<Product> findByName(final String name) {
        try {
            final Product product = entityManager.createQuery("SELECT p FROM Product p WHERE p.name = :name", Product.class)
                .setParameter("name", name)
                .getSingleResult();
            return Optional.of(product);
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
    
    public Optional<Inventory> findInventoryByProductId(final String productId) {
        try {
            final Inventory inventory = entityManager.createQuery("SELECT i FROM Inventory i WHERE i.productId = :productId", Inventory.class)
                .setParameter("productId", productId)
                .getSingleResult();
            return Optional.of(inventory);
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
    
    public int updateTotalStock(final String productId, final Integer stock) {
        try {
            return entityManager.createQuery("UPDATE Inventory i SET i.totalStock = i.totalStock + :stock, i.availableStock = i.availableStock + :stock WHERE i.productId = :productId")
                .setParameter("productId", productId)
                .setParameter("stock", stock)
                .executeUpdate();
        } catch (final Exception e) {
            throw new InternalServerException("Failed to update total stock: " + e.getMessage());
        }
    }
    
    public int insertInventory(final String productId, final Integer stock) {
        try {
            return entityManager.createNativeQuery("INSERT INTO inventory (product_id, total_stock, available_stock, version) VALUES (?, ?, ?, 0)")
                .setParameter(1, productId)
                .setParameter(2, stock)
                .setParameter(3, stock)
                .executeUpdate();
        } catch (final Exception e) {
            throw new InternalServerException("Failed to create inventory: " + e.getMessage());
        }
    }
    
    public Optional<Reservation> findReservationByOrderId(final String orderId) {
        try {
            final Reservation reservation = entityManager.createQuery("SELECT r FROM Reservation r WHERE r.orderId = :orderId", Reservation.class)
                .setParameter("orderId", orderId)
                .getSingleResult();
            return Optional.of(reservation);
        } catch (final Exception e) {
            return Optional.empty();
        }
    }
    
    public List<Reservation> findExpiredReservations(final LocalDateTime now) {
        try {
            return entityManager.createQuery("SELECT r FROM Reservation r WHERE r.status = :reservedStatus AND r.expiresAt < :now", Reservation.class)
                .setParameter("reservedStatus", ReservationStatus.RESERVED)
                .setParameter("now", now)
                .getResultList();
        } catch (final Exception e) {
            throw new InternalServerException("Failed to find expired reservations: " + e.getMessage());
        }
    }
    
    public int releaseStock(final String productId, final Integer quantity) {
        try {
            return entityManager.createQuery("UPDATE Inventory i SET i.availableStock = i.availableStock + :quantity WHERE i.productId = :productId")
                .setParameter("productId", productId)
                .setParameter("quantity", quantity)
                .executeUpdate();
        } catch (final Exception e) {
            throw new InternalServerException("Failed to release stock: " + e.getMessage());
        }
    }
    
    public Reservation insertReservation(final String orderId, final String productId, final Integer quantity, final LocalDateTime expiresAt) {
        try {
            final Reservation reservation = Reservation.builder()
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .status(ReservationStatus.RESERVED)
                .expiresAt(expiresAt)
                .build();
            return save(reservation);
        } catch (final Exception e) {
            throw new InternalServerException("Failed to create reservation: " + e.getMessage());
        }
    }
    
    public String insertOrder(final String orderId, final String productId, final Integer quantity) {
        try {
            final Order order = Order.builder()
                .id(orderId)
                .productId(productId)
                .quantity(quantity)
                .build();
            final Order savedOrder = saveOrder(order);
            return savedOrder.getId();
        } catch (final Exception e) {
            throw new InternalServerException("Failed to create order: " + e.getMessage());
        }
    }
    
    public Order saveOrder(final Order order) {
        try {
            if (order.getId() == null) {
                entityManager.persist(order);
                return order;
            } else {
                return entityManager.merge(order);
            }
        } catch (final Exception e) {
            throw new InternalServerException("Failed to save order: " + e.getMessage());
        }
    }
    
    public Inventory saveInventory(final Inventory inventory) {
        try {
            return entityManager.merge(inventory);
        } catch (final OptimisticLockingFailureException e) {
            throw new InternalServerException("Stock update failed due to concurrent access. Please retry.");
        } catch (final Exception e) {
            throw new InternalServerException("Failed to save inventory: " + e.getMessage());
        }
    }
    
    public Reservation save(final Reservation reservation) {
        try {
            if (reservation.getId() == null) {
                entityManager.persist(reservation);
                return reservation;
            } else {
                return entityManager.merge(reservation);
            }
        } catch (final Exception e) {
            throw new InternalServerException("Failed to save reservation: " + e.getMessage());
        }
    }
}