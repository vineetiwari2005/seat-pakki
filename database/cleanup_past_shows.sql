-- ============================================================
-- BookMyShow - Cleanup Past Shows Script
-- ============================================================
-- This script removes shows that have already passed
-- Run this periodically or set up as a scheduled event

-- Display shows that will be deleted (for verification)
SELECT 
    s.id AS show_id,
    m.movie_name,
    t.name AS theater_name,
    s.show_date,
    s.show_start_time,
    CONCAT(s.show_date, ' ', s.show_start_time) AS show_datetime,
    NOW() AS current_time,
    CASE 
        WHEN CONCAT(s.show_date, ' ', s.show_start_time) < NOW() 
        THEN 'PAST - Will be deleted'
        ELSE 'FUTURE - Will be kept'
    END AS status
FROM shows s
JOIN movies m ON s.movie_id = m.id
JOIN theaters t ON s.theater_id = t.id
WHERE CONCAT(s.show_date, ' ', s.show_start_time) < NOW()
ORDER BY s.show_date DESC, s.show_start_time DESC;

-- Count of shows to be deleted
SELECT COUNT(*) AS shows_to_delete
FROM shows 
WHERE CONCAT(show_date, ' ', show_start_time) < NOW();

-- ============================================================
-- DELETE PAST SHOWS (Uncomment to execute)
-- ============================================================
-- WARNING: This will permanently delete past shows and related data
-- Make sure to backup your database before running this

-- Step 1: Delete show_seat records for past shows
-- DELETE ss FROM show_seat ss
-- JOIN shows s ON ss.show_id = s.id
-- WHERE CONCAT(s.show_date, ' ', s.show_start_time) < NOW();

-- Step 2: Delete seat_lock records for past shows
-- DELETE sl FROM seat_locks sl
-- JOIN shows s ON sl.show_id = s.id
-- WHERE CONCAT(s.show_date, ' ', s.show_start_time) < NOW();

-- Step 3: Delete the past shows themselves
-- NOTE: This will NOT delete tickets/payments for past shows (for historical records)
-- DELETE FROM shows 
-- WHERE CONCAT(show_date, ' ', show_start_time) < NOW();

-- ============================================================
-- ALTERNATIVE: Archive past shows instead of deleting
-- ============================================================
-- Create archive table (run once)
-- CREATE TABLE IF NOT EXISTS shows_archive LIKE shows;

-- Move past shows to archive
-- INSERT INTO shows_archive 
-- SELECT * FROM shows 
-- WHERE CONCAT(show_date, ' ', show_start_time) < NOW();

-- Then delete from main table
-- DELETE FROM shows 
-- WHERE CONCAT(show_date, ' ', show_start_time) < NOW();

-- ============================================================
-- SCHEDULED EVENT (Auto-cleanup) - MySQL 8.0+
-- ============================================================
-- This creates an event that runs daily to clean up past shows

-- First, enable event scheduler (run once as admin)
-- SET GLOBAL event_scheduler = ON;

-- Create the scheduled event (uncomment to activate)
/*
DELIMITER $$

CREATE EVENT IF NOT EXISTS cleanup_past_shows_daily
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 2 HOUR  -- Runs at 2 AM daily
DO
BEGIN
    -- Delete show_seats for past shows
    DELETE ss FROM show_seat ss
    JOIN shows s ON ss.show_id = s.id
    WHERE CONCAT(s.show_date, ' ', s.show_start_time) < NOW() - INTERVAL 7 DAY;
    
    -- Delete seat_locks for past shows
    DELETE sl FROM seat_locks sl
    JOIN shows s ON sl.show_id = s.id
    WHERE CONCAT(s.show_date, ' ', s.show_start_time) < NOW() - INTERVAL 7 DAY;
    
    -- Delete shows older than 7 days
    DELETE FROM shows 
    WHERE CONCAT(show_date, ' ', show_start_time) < NOW() - INTERVAL 7 DAY;
    
    -- Log the cleanup
    INSERT INTO system_logs (event_type, message, created_at)
    VALUES ('CLEANUP', CONCAT('Deleted past shows older than 7 days at ', NOW()), NOW());
END$$

DELIMITER ;
*/

-- View scheduled events
-- SHOW EVENTS;

-- Disable the event if needed
-- ALTER EVENT cleanup_past_shows_daily DISABLE;

-- Enable the event if disabled
-- ALTER EVENT cleanup_past_shows_daily ENABLE;

-- Drop the event if needed
-- DROP EVENT IF EXISTS cleanup_past_shows_daily;

-- ============================================================
-- VERIFICATION QUERIES
-- ============================================================

-- Check remaining shows
SELECT 
    COUNT(*) AS total_shows,
    SUM(CASE WHEN CONCAT(show_date, ' ', show_start_time) < NOW() THEN 1 ELSE 0 END) AS past_shows,
    SUM(CASE WHEN CONCAT(show_date, ' ', show_start_time) >= NOW() THEN 1 ELSE 0 END) AS future_shows
FROM shows;

-- Show date distribution
SELECT 
    show_date,
    COUNT(*) AS show_count,
    SUM(CASE WHEN CONCAT(show_date, ' ', show_start_time) < NOW() THEN 1 ELSE 0 END) AS past,
    SUM(CASE WHEN CONCAT(show_date, ' ', show_start_time) >= NOW() THEN 1 ELSE 0 END) AS future
FROM shows
GROUP BY show_date
ORDER BY show_date DESC;
