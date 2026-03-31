-- Add wallet_amount and card_amount columns to payments table for split payment tracking
-- This enables DB-centric storage of payment breakdown

USE bookmyshow;

-- Add wallet_amount column (amount paid via wallet)
ALTER TABLE payments 
ADD COLUMN wallet_amount DECIMAL(10,2) DEFAULT 0.0 AFTER payment_method;

-- Add card_amount column (amount paid via card)
ALTER TABLE payments 
ADD COLUMN card_amount DECIMAL(10,2) DEFAULT 0.0 AFTER wallet_amount;

-- Update existing payments to set default values
-- For WALLET payments, set wallet_amount = total_amount
UPDATE payments 
SET wallet_amount = total_amount, 
    card_amount = 0.0 
WHERE payment_method = 'WALLET';

-- For STRIPE/other card payments, set card_amount = total_amount
UPDATE payments 
SET wallet_amount = 0.0, 
    card_amount = total_amount 
WHERE payment_method IN ('STRIPE', 'CREDIT_CARD', 'DEBIT_CARD', 'UPI');

-- For WALLET_CARD_SPLIT payments, amounts are already set by application
-- (No need to update as they are correctly set during payment processing)

SELECT 'Split payment columns added successfully!' AS status;
