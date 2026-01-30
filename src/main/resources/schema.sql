-- Create database if not exists
CREATE DATABASE IF NOT EXISTS inventory_db;
USE inventory_db;

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(8) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Inventory table with optimistic locking
CREATE TABLE IF NOT EXISTS inventory (
    product_id VARCHAR(8) PRIMARY KEY,
    total_stock INT NOT NULL DEFAULT 0,
    available_stock INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id)
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

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(8) PRIMARY KEY,
    status ENUM('CREATED', 'CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample data
INSERT IGNORE INTO products (id, name) VALUES 
('12345678', 'Sample Product 1'),
('87654321', 'Sample Product 2'),
('11111111', 'Sample Product 3');

INSERT IGNORE INTO inventory (product_id, total_stock, available_stock) VALUES 
('12345678', 100, 100),
('87654321', 50, 50),
('11111111', 200, 200);