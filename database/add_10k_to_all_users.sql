-- Set wallet balance to 10,000 for all existing users
-- This script creates wallet records for users who don't have one, then sets balance to 10,000

-- Step 1: Create user_wallets entries for ALL users who don't have one yet
INSERT INTO user_wallets (user_id, balance, created_at, updated_at, version)
SELECT 
    u.id,
    10000.00,
    NOW(),
    NOW(),
    0
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM user_wallets uw WHERE uw.user_id = u.id
);

-- Step 2: Update existing user_wallets - Set balance to 10,000
UPDATE user_wallets 
SET balance = 10000.00,
    last_credited_at = NOW(),
    updated_at = NOW();

-- Step 3: Update users table wallet_balance to match (for backward compatibility)
UPDATE users 
SET wallet_balance = 10000.00,
    updated_at = NOW();

-- Step 4: Create wallet transaction records for audit trail
INSERT INTO wallet_transactions (user_id, transaction_type, amount, balance_before, balance_after, transaction_reference, description, created_at)
SELECT 
    uw.user_id,
    'CREDIT',
    10000.00,
    0.00,
    10000.00,
    CONCAT('INIT_CREDIT_', uw.user_id, '_', UNIX_TIMESTAMP()),
    'Initial wallet balance set to 10,000',
    NOW()
FROM user_wallets uw
WHERE NOT EXISTS (
    SELECT 1 FROM wallet_transactions wt 
    WHERE wt.user_id = uw.user_id 
    AND wt.description = 'Initial wallet balance set to 10,000'
);

-- Verification queries
SELECT '=== VERIFICATION ===' as info;
SELECT 'Total users:' as info, COUNT(*) as count FROM users;
SELECT 'Total user wallets:' as info, COUNT(*) as count FROM user_wallets;
SELECT 'Wallets with 10k balance:' as info, COUNT(*) as count FROM user_wallets WHERE balance = 10000;

SELECT '' as '';
SELECT 'All User Wallets:' as info;
SELECT 
    uw.user_id,
    u.name,
    u.email_id,
    uw.balance,
    uw.last_credited_at 
FROM user_wallets uw
JOIN users u ON u.id = uw.user_id
ORDER BY uw.user_id;
