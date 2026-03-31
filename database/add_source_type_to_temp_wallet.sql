-- Add source_type column to temporary_wallet table
-- This tracks the source of the temporary wallet entry:
-- - GAME_REWARD: From spin game rewards
-- - TICKET_CANCELLATION: From booking cancellation refunds  
-- - TICKET_CHANGE_REFUND: From booking seat change refunds

ALTER TABLE temporary_wallet ADD COLUMN `source_type` VARCHAR(100) DEFAULT 'GAME_REWARD';

-- Update existing entries to mark them as GAME_REWARD (since they were all from spins before this change)
UPDATE temporary_wallet SET source_type = 'GAME_REWARD' WHERE source_type IS NULL;

-- For future entries from ticket operations, the application will set the appropriate source_type when creating the record
