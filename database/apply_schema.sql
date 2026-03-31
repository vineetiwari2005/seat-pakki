-- =====================================================
-- APPLY ALL SCHEMA CHANGES
-- Run this file to create all new tables
-- =====================================================

-- Execute from MySQL command line:
-- mysql -u springuser -pspringpass123 bookmyshow < apply_schema.sql

-- Or run each script separately:
-- SOURCE parking_schema.sql;
-- SOURCE food_schema.sql;

USE bookmyshow;

-- =====================================================
-- PARKING SCHEMA
-- =====================================================

CREATE TABLE IF NOT EXISTS parking_lots (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    total_slots INT NOT NULL,
    available_slots INT NOT NULL,
    theater_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (theater_id) REFERENCES theaters(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS parking_slots (
    id INT PRIMARY KEY AUTO_INCREMENT,
    slot_number VARCHAR(50) NOT NULL,
    vehicle_type ENUM('TWO_WHEELER', 'FOUR_WHEELER', 'EV') NOT NULL,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    hourly_rate INT NOT NULL,
    parking_lot_id INT NOT NULL,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (parking_lot_id) REFERENCES parking_lots(id) ON DELETE CASCADE,
    UNIQUE KEY uk_parking_slot (parking_lot_id, slot_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS parking_tickets (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    vehicle_number VARCHAR(50) NOT NULL,
    vehicle_type ENUM('TWO_WHEELER', 'FOUR_WHEELER', 'EV') NOT NULL,
    parking_slot_id INT NOT NULL,
    movie_ticket_id INT NULL,
    entry_time TIMESTAMP NOT NULL,
    exit_time TIMESTAMP NULL,
    amount_paid INT NOT NULL,
    status ENUM('BOOKED', 'ACTIVE', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'BOOKED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (parking_slot_id) REFERENCES parking_slots(id),
    FOREIGN KEY (movie_ticket_id) REFERENCES tickets(ticket_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX IF NOT EXISTS idx_parking_availability ON parking_slots(parking_lot_id, vehicle_type, is_occupied);
CREATE INDEX IF NOT EXISTS idx_parking_status ON parking_tickets(status, created_at);
CREATE INDEX IF NOT EXISTS idx_parking_ticket_lookup ON parking_tickets(ticket_number);

-- =====================================================
-- FOOD ORDERING SCHEMA
-- =====================================================

CREATE TABLE IF NOT EXISTS food_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    item_name VARCHAR(255) NOT NULL,
    description TEXT,
    price INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(500),
    theater_id INT NOT NULL,
    is_vegetarian BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (theater_id) REFERENCES theaters(id) ON DELETE CASCADE,
    UNIQUE KEY uk_theater_food_item (theater_id, item_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS food_orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    ticket_id INT NULL,
    seat_numbers VARCHAR(255),
    total_amount INT NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'PREPARING', 'DELIVERED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    delivery_instructions TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS food_order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    food_item_id INT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    price INT NOT NULL,
    special_instructions TEXT,
    FOREIGN KEY (order_id) REFERENCES food_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (food_item_id) REFERENCES food_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX IF NOT EXISTS idx_food_theater ON food_items(theater_id, is_available);
CREATE INDEX IF NOT EXISTS idx_food_category ON food_items(category, is_available);
CREATE INDEX IF NOT EXISTS idx_order_user ON food_orders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_order_ticket ON food_orders(ticket_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON food_orders(status, created_at);
CREATE INDEX IF NOT EXISTS idx_order_lookup ON food_orders(order_number);

-- =====================================================
-- VERIFY SCHEMA
-- =====================================================

SELECT 'Schema creation completed!' AS status;
SHOW TABLES;
