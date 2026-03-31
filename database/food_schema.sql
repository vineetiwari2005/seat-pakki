-- =====================================================
-- FOOD ORDERING MODULE SCHEMA
-- =====================================================

-- Drop existing tables if recreating
-- DROP TABLE IF EXISTS food_order_items;
-- DROP TABLE IF EXISTS food_orders;
-- DROP TABLE IF EXISTS food_items;

-- Food Items (menu per theater)
CREATE TABLE food_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    item_name VARCHAR(255) NOT NULL,
    description TEXT,
    price INT NOT NULL,
    category VARCHAR(50) NOT NULL COMMENT 'COMBO, POPCORN, BEVERAGE, SNACK, DESSERT',
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(500),
    theater_id INT NOT NULL,
    is_vegetarian BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (theater_id) REFERENCES theaters(id) ON DELETE CASCADE,
    UNIQUE KEY uk_theater_food_item (theater_id, item_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Food Orders
CREATE TABLE food_orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    ticket_id INT NULL COMMENT 'Optional - can order without ticket',
    seat_numbers VARCHAR(255) COMMENT 'Delivery seats: A12,A13',
    total_amount INT NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'PREPARING', 'DELIVERED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    delivery_instructions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Food Order Items (line items)
CREATE TABLE food_order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    food_item_id INT NOT NULL,
    item_name VARCHAR(255) NOT NULL COMMENT 'Snapshot at order time',
    quantity INT NOT NULL,
    price INT NOT NULL COMMENT 'Snapshot at order time',
    special_instructions TEXT,
    FOREIGN KEY (order_id) REFERENCES food_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (food_item_id) REFERENCES food_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Indexes for performance
CREATE INDEX idx_food_theater ON food_items(theater_id, is_available);
CREATE INDEX idx_food_category ON food_items(category, is_available);
CREATE INDEX idx_order_user ON food_orders(user_id, status);
CREATE INDEX idx_order_ticket ON food_orders(ticket_id);
CREATE INDEX idx_order_status ON food_orders(status, created_at);
CREATE INDEX idx_order_lookup ON food_orders(order_number);

-- Sample data insert (run after theater data is loaded)
-- This will be handled by DataInitializationService in Spring Boot
