# Inventory Stock Management System

A Spring Boot application for managing product inventory with stock reservations, automatic expiry handling, and database-level cascade operations.

## Features

- **Product Management**: Create products with system-generated IDs
- **Stock Management**: Update product inventory levels
- **Stock Reservation**: Temporary stock reservations with automatic expiry
- **Order Confirmation**: Convert reservations to confirmed orders
- **Database Cascade**: Automatic cleanup of inventory when products are deleted at database level
- **Concurrency Safety**: Optimistic locking for thread-safe operations

## Tech Stack

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA / Hibernate
- MySQL 8.0
- Lombok
- Gradle

## Database Schema

### Tables
- `products`: Product master data
- `inventory`: Stock quantities with optimistic locking
- `reservations`: Temporary stock reservations with expiry
- `orders`: Confirmed order records

### Database Cascade Relationships
- **Product → Inventory**: One-to-One (Database CASCADE DELETE only)

## API Endpoints

### 1. Create Product
```http
POST /products
Content-Type: application/json

{
  "name": "Sample Product",
  "stock": 100
}
```

**Response:**
```json
{
  "productId": "a1b2c3d4",
  "name": "Sample Product"
}
```

### 2. Update Product Stock
```http
PUT /products/stock/{productId}
Content-Type: application/json

{
  "stock": 50
}
```

**Response:**
```json
{
  "productId": "a1b2c3d4",
  "message": "Stock updated successfully",
  "stockChange": 50,
  "newTotalStock": 150
}
```

### 3. Reserve Stock
```http
POST /products/reservation
Content-Type: application/json

{
  "productId": "a1b2c3d4",
  "quantity": 10
}
```

**Response:**
```json
{
  "reservationId": "b2c3d4e5",
  "orderId": "c3d4e5f6",
  "productId": "a1b2c3d4",
  "quantity": 10,
  "expiresAt": "2024-01-15T10:31:00",
  "status": "RESERVED"
}
```

### 4. Confirm Order
```http
POST /products/order
Content-Type: application/json

{
  "orderId": "c3d4e5f6"
}
```

**Response:**
```json
{
  "orderId": "c3d4e5f6",
  "status": "CONFIRMED"
}
```

### 5. Get Available Stock
```http
GET /products/stock/{productId}
```

**Response:**
```json
{
  "productId": "a1b2c3d4",
  "name": "Sample Product",
  "availableStock": 90
}
```

## Business Logic Flow

### 1. Product Creation Flow
```
1. User submits product name and initial stock
2. System generates 8-character UUID for product ID
3. Product record created in database
4. Inventory record created with total_stock = available_stock = initial stock
5. Return product ID and name to user
```

### 2. Stock Update Flow
```
1. Clean up expired reservations first
2. Validate product exists
3. Update both total_stock and available_stock by the increment/decrement amount
4. Return updated stock information
```

### 3. Stock Reservation Flow
```
1. Clean up expired reservations first
2. Validate product exists and has sufficient available stock
3. Reduce available_stock by requested quantity (optimistic locking)
4. Generate order ID and set 1-minute expiry
5. Create reservation record with RESERVED status
6. Return reservation details with expiry time
```

### 4. Order Confirmation Flow
```
1. Clean up expired reservations first
2. Find reservation by order ID
3. Validate reservation exists and is not expired
4. Update reservation status to CONFIRMED
5. Create order record with product ID and quantity
6. Return confirmed order status
```

### 5. Stock Availability Check Flow
```
1. Clean up expired reservations first
2. Validate product exists
3. Retrieve current available stock from inventory
4. Return product details with available stock
```

### 6. Automatic Expiry Cleanup Flow
```
1. Find all reservations with RESERVED status and expiry time < current time
2. For each expired reservation:
   - Update status to EXPIRED
   - Release reserved stock back to available_stock
3. Process runs automatically before each stock operation
```

## Error Responses

All errors return a consistent format:

```json
{
  "id": null,
  "message": "Error description",
  "errorCode": 400,
  "retryable": false
}
```

### Error Codes
- `400`: Bad Request (Invalid input, insufficient stock)
- `404`: Not Found (Product/reservation not found)
- `409`: Conflict (Concurrent modification)
- `500`: Internal Server Error

## Data Models

### ProductInput
```json
{
  "name": "string (required)",
  "stock": "integer (required)"
}
```

### StockInput
```json
{
  "stock": "integer (required)"
}
```

### ReservationInput
```json
{
  "productId": "string (required)",
  "quantity": "integer (required)"
}
```

### OrderInput
```json
{
  "orderId": "string (required)"
}
```

## Configuration

### Database Configuration (application.properties)
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/inventory_db
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

## Running the Application

1. **Setup MySQL Database:**
   ```sql
   CREATE DATABASE inventory_db;
   ```

2. **Run Application:**
   ```bash
   ./gradlew bootRun
   ```

3. **Run Tests:**
   ```bash
   ./gradlew test
   ```

## Concurrency Handling
- Uses JPA `@Version` for optimistic locking on inventory updates
- Handles concurrent stock modifications gracefully
- Provides retry suggestions for failed operations due to concurrent access

## Sample Usage Workflow

```bash
# 1. Create product
curl -X POST http://localhost:8080/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Product","stock":100}'

# 2. Reserve stock
curl -X POST http://localhost:8080/products/reservation \
  -H "Content-Type: application/json" \
  -d '{"productId":"a1b2c3d4","quantity":10}'

# 3. Check available stock
curl http://localhost:8080/products/stock/a1b2c3d4

# 4. Confirm order
curl -X POST http://localhost:8080/products/order \
  -H "Content-Type: application/json" \
  -d '{"orderId":"c3d4e5f6"}'

# 5. Update stock
curl -X PUT http://localhost:8080/products/stock/a1b2c3d4 \
  -H "Content-Type: application/json" \
  -d '{"stock":25}'
```

## Key Features

- **No Continuous Timers**: Expiry checked during operations only
- **Database Cascade**: Only inventory cascades with product deletion at database level
- **Thread Safety**: Optimistic locking prevents race conditions
- **Auto-Expiry**: Reservations expire automatically after 1 minute
- **UUID IDs**: 8-character UUID prefixes for all entities (system-generated)
- **Stock Integrity**: Available stock automatically managed through reservation lifecycle