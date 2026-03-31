-- ============================================================================
-- Payment Add-ons Schema
-- Purpose: Support optional parking and food add-ons during payment stage
-- Design: Non-breaking extension to existing schema
-- ============================================================================

-- Payment Add-ons Table
CREATE TABLE IF NOT EXISTS payment_addons (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    payment_id INT,
    user_id INT NOT NULL,
    addon_type ENUM('PARKING', 'FOOD_BEVERAGE') NOT NULL,
    status ENUM('SELECTED', 'CONFIRMED', 'CANCELLED', 'FAILED') NOT NULL,
    amount DOUBLE NOT NULL,
    reference_id INT COMMENT 'ParkingTicket ID or FoodOrder ID',
    metadata TEXT COMMENT 'JSON metadata for addon details',
    failure_reason VARCHAR(500),
    created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    INDEX idx_session_id (session_id),
    INDEX idx_payment_id (payment_id),
    INDEX idx_user_id (user_id),
    INDEX idx_addon_type (addon_type),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    
    CONSTRAINT fk_payment_addon_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_addon_payment FOREIGN KEY (payment_id) 
        REFERENCES payments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- Indexes for Performance
-- ============================================================================

-- Session-based queries (most common)
CREATE INDEX idx_session_addon_type ON payment_addons(session_id, addon_type);

-- Payment reconciliation
CREATE INDEX idx_payment_status ON payment_addons(payment_id, status);

-- User history
CREATE INDEX idx_user_created ON payment_addons(user_id, created_at DESC);

-- ============================================================================
-- Views for Reporting
-- ============================================================================

-- Add-on revenue summary
CREATE OR REPLACE VIEW v_addon_revenue_summary AS
SELECT 
    DATE(created_at) as date,
    addon_type,
    status,
    COUNT(*) as count,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount
FROM payment_addons
GROUP BY DATE(created_at), addon_type, status;

-- ============================================================================
-- Sample Queries
-- ============================================================================

-- Get all add-ons for a session
-- SELECT * FROM payment_addons WHERE session_id = 'SESSION_123';

-- Get confirmed add-ons for a payment
-- SELECT * FROM payment_addons WHERE payment_id = 1 AND status = 'CONFIRMED';

-- Failed add-ons needing manual review
-- SELECT * FROM payment_addons WHERE status = 'FAILED' ORDER BY created_at DESC;

-- ============================================================================
