-- =====================================================
-- PARKING MODULE SCHEMA
-- =====================================================

-- Drop existing tables if recreating
-- DROP TABLE IF EXISTS parking_tickets;
-- DROP TABLE IF EXISTS parking_slots;
-- DROP TABLE IF EXISTS parking_lots;

-- Parking Lots (one per theater)
CREATE TABLE parking_lots (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    total_slots INT NOT NULL,
    available_slots INT NOT NULL,
    theater_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (theater_id) REFERENCES theaters(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Parking Slots (individual spaces)
CREATE TABLE parking_slots (
    id INT PRIMARY KEY AUTO_INCREMENT,
    slot_number VARCHAR(50) NOT NULL,
    vehicle_type ENUM('TWO_WHEELER', 'FOUR_WHEELER', 'EV') NOT NULL,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    hourly_rate INT NOT NULL,
    parking_lot_id INT NOT NULL,
    version BIGINT DEFAULT 0 COMMENT 'Optimistic locking version',
    FOREIGN KEY (parking_lot_id) REFERENCES parking_lots(id) ON DELETE CASCADE,
    UNIQUE KEY uk_parking_slot (parking_lot_id, slot_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Parking Tickets (bookings)
CREATE TABLE parking_tickets (
    id INT PRIMARY KEY AUTO_INCREMENT,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    vehicle_number VARCHAR(50) NOT NULL,
    vehicle_type ENUM('TWO_WHEELER', 'FOUR_WHEELER', 'EV') NOT NULL,
    parking_slot_id INT NOT NULL,
    movie_ticket_id INT NULL COMMENT 'Optional - can park without movie ticket',
    entry_time TIMESTAMP NOT NULL,
    exit_time TIMESTAMP NULL,
    amount_paid INT NOT NULL,
    status ENUM('BOOKED', 'ACTIVE', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'BOOKED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (parking_slot_id) REFERENCES parking_slots(id),
    FOREIGN KEY (movie_ticket_id) REFERENCES tickets(ticket_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Indexes for performance
CREATE INDEX idx_parking_availability ON parking_slots(parking_lot_id, vehicle_type, is_occupied);
CREATE INDEX idx_parking_status ON parking_tickets(status, created_at);
CREATE INDEX idx_parking_ticket_lookup ON parking_tickets(ticket_number);

-- Sample data insert (run after theater data is loaded)
-- This will be handled by DataInitializationService in Spring Boot
