# Inventory Stock Management System

## Approach

The system implements a reservation-based inventory management approach with automatic expiry handling:

- **Product Creation**: System generates unique 8-character UUID IDs for all products
- **Stock Reservation**: Temporary reservations with 1-minute expiry to prevent stock blocking
- **Order Confirmation**: Converts valid reservations to confirmed orders
- **Automatic Cleanup**: Expired reservations release stock back to available inventory
- **Concurrency Control**: Optimistic locking prevents race conditions during stock updates
- **Database Cascade**: Product deletion automatically removes associated inventory records

## APIs

### 1. Create Product
```http
POST /products
{
  "name": "Sample Product",
  "stock": 100
}
```
Response: `{"productId": "a1b2c3d4", "name": "Sample Product"}`

### 2. Update Stock
```http
PUT /products/stock/{productId}
{
  "stock": 50
}
```
Response: `{"productId": "a1b2c3d4", "message": "Stock updated successfully", "stockChange": 50, "newTotalStock": 150}`

### 3. Reserve Stock
```http
POST /products/reservation
{
  "productId": "a1b2c3d4",
  "quantity": 10
}
```
Response: `{"reservationId": "b2c3d4e5", "orderId": "c3d4e5f6", "productId": "a1b2c3d4", "quantity": 10, "expiresAt": "2024-01-15T10:31:00", "status": "RESERVED"}`

### 4. Confirm Order
```http
POST /products/order
{
  "orderId": "c3d4e5f6"
}
```
Response: `{"orderId": "c3d4e5f6", "status": "CONFIRMED"}`

### 5. Get Available Stock
```http
GET /products/stock/{productId}
```
Response: `{"productId": "a1b2c3d4", "name": "Sample Product", "availableStock": 90}`

## DB Schema

### Tables

**products**
- `id` VARCHAR(8) PRIMARY KEY
- `name` VARCHAR(255) NOT NULL
- `created_at` TIMESTAMP

**inventory**
- `product_id` VARCHAR(8) PRIMARY KEY
- `total_stock` INT NOT NULL
- `available_stock` INT NOT NULL
- `version` BIGINT (optimistic locking)
- `updated_at` TIMESTAMP
- FOREIGN KEY (`product_id`) REFERENCES `products(id)` ON DELETE CASCADE

**reservations**
- `id` VARCHAR(8) PRIMARY KEY
- `order_id` VARCHAR(8) NOT NULL
- `product_id` VARCHAR(8) NOT NULL
- `quantity` INT NOT NULL
- `status` ENUM('RESERVED', 'EXPIRED', 'CONFIRMED')
- `expires_at` TIMESTAMP NOT NULL
- `created_at` TIMESTAMP
- FOREIGN KEY (`product_id`) REFERENCES `products(id)`

**orders**
- `id` VARCHAR(8) PRIMARY KEY
- `product_id` VARCHAR(8) NOT NULL
- `quantity` INT NOT NULL
- `created_at` TIMESTAMP
- FOREIGN KEY (`product_id`) REFERENCES `products(id)` ON DELETE CASCADE

### Relationships
- Product → Inventory: One-to-One (CASCADE DELETE)
- Product → Orders: One-to-Many (CASCADE DELETE)
- Product → Reservations: One-to-Many

## Assumptions

1. **Product IDs**: Always system-generated, never user-provided
2. **Reservation Expiry**: Fixed 10-minute timeout for all reservations
3. **Stock Updates**: Can be positive (add) or negative (reduce) increments
4. **Order Creation**: Only happens when reservation is confirmed
5. **Cleanup Timing**: Expired reservations cleaned before each stock operation
6. **Concurrency**: Optimistic locking sufficient for expected load
7. **Database Cascade**: Only inventory and orders cascade delete with products
8. **UUID Format**: First 8 characters of UUID used for all entity IDs