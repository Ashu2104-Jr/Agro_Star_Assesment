-- Create database if not exists
CREATE DATABASE IF NOT EXISTS inventory_db;
USE inventory_db;

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(8) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Inventory table with optimistic locking and cascade delete
CREATE TABLE IF NOT EXISTS inventory (
    product_id VARCHAR(8) PRIMARY KEY,
    total_stock INT NOT NULL DEFAULT 0,
    available_stock INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_product_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Reservations table
CREATE TABLE IF NOT EXISTS reservations (
    id VARCHAR(8) PRIMARY KEY,
    order_id VARCHAR(8) NOT NULL,
    product_id VARCHAR(8) NOT NULL,
    quantity INT NOT NULL,
    status ENUM('RESERVED', 'EXPIRED', 'CONFIRMED') NOT NULL DEFAULT 'RESERVED',
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    INDEX idx_order_status (order_id, status),
    INDEX idx_expires_at (expires_at)
);

-- Orders table with product reference and cascade delete
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(8) PRIMARY KEY,
    product_id VARCHAR(8) NOT NULL,
    quantity INT NOT NULL,
    status ENUM('CREATED', 'CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_product_id FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);