-- Game Rewards Tables

-- Daily Game Logs Table: Tracks user plays to enforce one-play-per-day rule
CREATE TABLE IF NOT EXISTS daily_game_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  score INT NOT NULL DEFAULT 0,
  played_date DATE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user(id),
  UNIQUE KEY unique_user_daily_play (user_id, played_date),
  INDEX idx_user_id (user_id),
  INDEX idx_played_date (played_date)
);

-- Temporary Wallet Table: Stores cashback rewards with 10-day expiration
CREATE TABLE IF NOT EXISTS temporary_wallet (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  amount DECIMAL(10, 2) NOT NULL,
  earned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,
  is_expired BOOLEAN DEFAULT FALSE,
  is_used BOOLEAN DEFAULT FALSE,
  used_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user(id),
  INDEX idx_user_id (user_id),
  INDEX idx_expires_at (expires_at),
  INDEX idx_is_expired (is_expired),
  INDEX idx_is_used (is_used)
);

-- Game Leaderboard Table: Tracks highest scores and average scores per month
CREATE TABLE IF NOT EXISTS game_leaderboard (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  month_year VARCHAR(7) NOT NULL,
  user_id BIGINT NOT NULL,
  highest_score INT NOT NULL,
  total_plays INT NOT NULL DEFAULT 1,
  average_score DECIMAL(10, 2) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user(id),
  UNIQUE KEY unique_user_month (user_id, month_year),
  INDEX idx_month_year (month_year),
  INDEX idx_highest_score (highest_score)
);

-- Add this index to support monthly aggregation queries
CREATE TABLE IF NOT EXISTS monthly_game_stats (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  month_year VARCHAR(7) NOT NULL UNIQUE,
  highest_score INT NOT NULL,
  average_score DECIMAL(10, 2) NOT NULL,
  total_players INT NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_month_year (month_year)
);
