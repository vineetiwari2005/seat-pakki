-- Temporary wallet credit schema for Change Date flow
-- Credit is valid for 15 days and consumed during next booking payments

CREATE TABLE IF NOT EXISTS temporary_wallet_credits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    ticket_id INT NULL,
    source_type VARCHAR(30) NOT NULL DEFAULT 'DATE_CHANGE',
    total_amount DOUBLE NOT NULL,
    remaining_amount DOUBLE NOT NULL,
    expires_at DATETIME NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    last_used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tmp_wallet_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_tmp_wallet_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id)
);

SET @cst=(SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='temporary_wallet_credits' AND COLUMN_NAME='ticket_id');
SET @sst=IF(@cst='NO','ALTER TABLE temporary_wallet_credits MODIFY COLUMN ticket_id INT NULL','SELECT 1');
PREPARE stst FROM @sst;
EXECUTE stst;
DEALLOCATE PREPARE stst;

SET @csrc=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='temporary_wallet_credits' AND COLUMN_NAME='source_type');
SET @ssrc=IF(@csrc=0,'ALTER TABLE temporary_wallet_credits ADD COLUMN source_type VARCHAR(30) NOT NULL DEFAULT ''DATE_CHANGE''','SELECT 1');
PREPARE stsrc FROM @ssrc;
EXECUTE stsrc;
DEALLOCATE PREPARE stsrc;

SET @i1=(SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='temporary_wallet_credits' AND INDEX_NAME='idx_tmp_wallet_user_expiry');
SET @si1=IF(@i1=0,'CREATE INDEX idx_tmp_wallet_user_expiry ON temporary_wallet_credits(user_id, expires_at)','SELECT 1');
PREPARE sti1 FROM @si1;
EXECUTE sti1;
DEALLOCATE PREPARE sti1;

SET @i2=(SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='temporary_wallet_credits' AND INDEX_NAME='idx_tmp_wallet_ticket');
SET @si2=IF(@i2=0,'CREATE INDEX idx_tmp_wallet_ticket ON temporary_wallet_credits(ticket_id)','SELECT 1');
PREPARE sti2 FROM @si2;
EXECUTE sti2;
DEALLOCATE PREPARE sti2;

SET @c1=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payments' AND COLUMN_NAME='temporary_wallet_amount');
SET @s1=IF(@c1=0,'ALTER TABLE payments ADD COLUMN temporary_wallet_amount DOUBLE NULL','SELECT 1');
PREPARE st1 FROM @s1;
EXECUTE st1;
DEALLOCATE PREPARE st1;

SET @c2=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payments' AND COLUMN_NAME='payable_amount');
SET @s2=IF(@c2=0,'ALTER TABLE payments ADD COLUMN payable_amount DOUBLE NULL','SELECT 1');
PREPARE st2 FROM @s2;
EXECUTE st2;
DEALLOCATE PREPARE st2;

CREATE TABLE IF NOT EXISTS game_play_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    score INT NOT NULL,
    played_date DATE NOT NULL,
    played_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_play_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_game_play_user_day UNIQUE (user_id, played_date)
);

CREATE TABLE IF NOT EXISTS game_monthly_leaderboard (
    month_key VARCHAR(7) PRIMARY KEY,
    highest_score INT NOT NULL,
    highest_user_id INT NULL,
    average_score DOUBLE NOT NULL,
    plays_count INT NOT NULL,
    total_score BIGINT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS game_reward_credits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    game_play_log_id INT NOT NULL,
    total_amount DOUBLE NOT NULL,
    remaining_amount DOUBLE NOT NULL,
    expires_at DATETIME NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    last_used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_game_reward_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_game_reward_play_log FOREIGN KEY (game_play_log_id) REFERENCES game_play_logs(id)
);

SET @i3=(SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='game_reward_credits' AND INDEX_NAME='idx_game_reward_user_expiry');
SET @si3=IF(@i3=0,'CREATE INDEX idx_game_reward_user_expiry ON game_reward_credits(user_id, expires_at)','SELECT 1');
PREPARE sti3 FROM @si3;
EXECUTE sti3;
DEALLOCATE PREPARE sti3;
