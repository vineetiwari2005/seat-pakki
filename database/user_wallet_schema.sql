-- Create user_wallets table for DB-driven wallet management
-- This table stores wallet balance for each user separately
-- Replaces the hardcoded wallet_balance column in users table

CREATE TABLE IF NOT EXISTS user_wallets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNIQUE NOT NULL,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    version BIGINT DEFAULT 0, -- For optimistic locking
    last_credited_at DATETIME NULL,
    last_debited_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_wallet_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
        
    CONSTRAINT chk_wallet_balance_non_negative 
        CHECK (balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create index for faster user lookups
CREATE INDEX idx_user_wallet_user_id ON user_wallets(user_id);

-- Migrate existing wallet balances from users table to user_wallets table
INSERT INTO user_wallets (user_id, balance, created_at)
SELECT id, wallet_balance, created_at
FROM users
WHERE id NOT IN (SELECT user_id FROM user_wallets)
ON DUPLICATE KEY UPDATE balance = VALUES(balance);

-- Add comment to table
ALTER TABLE user_wallets COMMENT = 'Stores wallet balance for each user with optimistic locking support';
