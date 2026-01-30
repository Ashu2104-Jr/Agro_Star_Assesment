-- Migration script to update orders table structure

-- Add product_id and quantity columns to orders table
ALTER TABLE orders 
ADD COLUMN product_id VARCHAR(8) NOT NULL,
ADD COLUMN quantity INT NOT NULL;

-- Drop status column since orders are only created when confirmed
ALTER TABLE orders 
DROP COLUMN status;

-- Add foreign key constraint for product_id
ALTER TABLE orders 
ADD CONSTRAINT fk_orders_product_id 
FOREIGN KEY (product_id) REFERENCES products(id);