-- ============================================================
-- BookMyShow - Insert Shows for Testing
-- ============================================================
-- This script adds shows for movies so you can test booking

-- Get today's date for show scheduling
SET @today = CURDATE();

-- ============================================================
-- INSERT SHOWS FOR VIKRAM (Movie ID needs to be verified)
-- ============================================================
-- First, let's find Vikram's movie ID
SELECT @vikram_id := id FROM movies WHERE movie_name = 'Vikram' LIMIT 1;

-- Insert shows for Vikram across multiple theaters
-- Today's shows
INSERT INTO shows (date, time, movie_id, theater_id, created_at) VALUES
(@today, '10:00:00', @vikram_id, 1, NOW()),
(@today, '13:30:00', @vikram_id, 1, NOW()),
(@today, '17:00:00', @vikram_id, 1, NOW()),
(@today, '20:30:00', @vikram_id, 1, NOW()),
(@today, '11:00:00', @vikram_id, 2, NOW()),
(@today, '14:30:00', @vikram_id, 2, NOW()),
(@today, '18:00:00', @vikram_id, 2, NOW()),
(@today, '21:30:00', @vikram_id, 2, NOW()),
(@today, '10:30:00', @vikram_id, 3, NOW()),
(@today, '14:00:00', @vikram_id, 3, NOW()),
(@today, '17:30:00', @vikram_id, 3, NOW()),
(@today, '21:00:00', @vikram_id, 3, NOW());

-- Tomorrow's shows
INSERT INTO shows (date, time, movie_id, theater_id, created_at) VALUES
(DATE_ADD(@today, INTERVAL 1 DAY), '10:00:00', @vikram_id, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '13:30:00', @vikram_id, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '17:00:00', @vikram_id, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '20:30:00', @vikram_id, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '11:00:00', @vikram_id, 2, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '14:30:00', @vikram_id, 2, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '18:00:00', @vikram_id, 2, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '21:30:00', @vikram_id, 2, NOW());

-- Day after tomorrow
INSERT INTO shows (date, time, movie_id, theater_id, created_at) VALUES
(DATE_ADD(@today, INTERVAL 2 DAY), '10:00:00', @vikram_id, 1, NOW()),
(DATE_ADD(@today, INTERVAL 2 DAY), '13:30:00', @vikram_id, 1, NOW()),
(DATE_ADD(@today, INTERVAL 2 DAY), '17:00:00', @vikram_id, 1, NOW()),
(DATE_ADD(@today, INTERVAL 2 DAY), '20:30:00', @vikram_id, 1, NOW());

-- ============================================================
-- INSERT SHOWS FOR OTHER POPULAR MOVIES
-- ============================================================

-- RRR Shows
SELECT @rrr_id := id FROM movies WHERE movie_name = 'RRR' LIMIT 1;
INSERT INTO shows (date, time, movie_id, theater_id, created_at) VALUES
(@today, '09:30:00', @rrr_id, 4, NOW()),
(@today, '13:00:00', @rrr_id, 4, NOW()),
(@today, '16:30:00', @rrr_id, 4, NOW()),
(@today, '20:00:00', @rrr_id, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '09:30:00', @rrr_id, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '13:00:00', @rrr_id, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '16:30:00', @rrr_id, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '20:00:00', @rrr_id, 4, NOW());

-- Jawan Shows
SELECT @jawan_id := id FROM movies WHERE movie_name = 'Jawan' LIMIT 1;
INSERT INTO shows (date, time, movie_id, theater_id, created_at) VALUES
(@today, '10:00:00', @jawan_id, 5, NOW()),
(@today, '13:30:00', @jawan_id, 5, NOW()),
(@today, '17:00:00', @jawan_id, 5, NOW()),
(@today, '20:30:00', @jawan_id, 5, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '10:00:00', @jawan_id, 5, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '13:30:00', @jawan_id, 5, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '17:00:00', @jawan_id, 5, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '20:30:00', @jawan_id, 5, NOW());

-- Pathaan Shows
SELECT @pathaan_id := id FROM movies WHERE movie_name = 'Pathaan' LIMIT 1;
INSERT INTO shows (date, time, movie_id, theater_id, created_at) VALUES
(@today, '11:00:00', @pathaan_id, 6, NOW()),
(@today, '14:30:00', @pathaan_id, 6, NOW()),
(@today, '18:00:00', @pathaan_id, 6, NOW()),
(@today, '21:30:00', @pathaan_id, 6, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '11:00:00', @pathaan_id, 6, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '14:30:00', @pathaan_id, 6, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '18:00:00', @pathaan_id, 6, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '21:30:00', @pathaan_id, 6, NOW());

-- Animal Shows
SELECT @animal_id := id FROM movies WHERE movie_name = 'Animal' LIMIT 1;
INSERT INTO shows (date, time, movie_id, theater_id, created_at) VALUES
(@today, '09:00:00', @animal_id, 7, NOW()),
(@today, '12:30:00', @animal_id, 7, NOW()),
(@today, '16:00:00', @animal_id, 7, NOW()),
(@today, '19:30:00', @animal_id, 7, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '09:00:00', @animal_id, 7, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '12:30:00', @animal_id, 7, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '16:00:00', @animal_id, 7, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '19:30:00', @animal_id, 7, NOW());

-- 12th Fail Shows
SELECT @fail_id := id FROM movies WHERE movie_name = '12th Fail' LIMIT 1;
INSERT INTO shows (date, time, movie_id, theater_id, created_at) VALUES
(@today, '10:30:00', @fail_id, 8, NOW()),
(@today, '14:00:00', @fail_id, 8, NOW()),
(@today, '17:30:00', @fail_id, 8, NOW()),
(@today, '21:00:00', @fail_id, 8, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '10:30:00', @fail_id, 8, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '14:00:00', @fail_id, 8, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '17:30:00', @fail_id, 8, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '21:00:00', @fail_id, 8, NOW());

-- KGF Chapter 2 Shows
SELECT @kgf_id := id FROM movies WHERE movie_name = 'KGF Chapter 2' LIMIT 1;
INSERT INTO shows (date, time, movie_id, theater_id, created_at) VALUES
(@today, '11:00:00', @kgf_id, 9, NOW()),
(@today, '14:30:00', @kgf_id, 9, NOW()),
(@today, '18:00:00', @kgf_id, 9, NOW()),
(@today, '21:30:00', @kgf_id, 9, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '11:00:00', @kgf_id, 9, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '14:30:00', @kgf_id, 9, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '18:00:00', @kgf_id, 9, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '21:30:00', @kgf_id, 9, NOW());

-- Leo Shows
SELECT @leo_id := id FROM movies WHERE movie_name = 'Leo' LIMIT 1;
INSERT INTO shows (date, time, movie_id, theater_id, created_at) VALUES
(@today, '10:00:00', @leo_id, 10, NOW()),
(@today, '13:30:00', @leo_id, 10, NOW()),
(@today, '17:00:00', @leo_id, 10, NOW()),
(@today, '20:30:00', @leo_id, 10, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '10:00:00', @leo_id, 10, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '13:30:00', @leo_id, 10, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '17:00:00', @leo_id, 10, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '20:30:00', @leo_id, 10, NOW());

COMMIT;

-- ============================================================
-- Verification
-- ============================================================
SELECT CONCAT('Total shows inserted: ', COUNT(*)) as result FROM shows;
SELECT m.movie_name, t.name as theater_name, s.date, s.time 
FROM shows s 
JOIN movies m ON s.movie_id = m.id 
JOIN theaters t ON s.theater_id = t.id 
ORDER BY s.date, s.time 
LIMIT 20;

('GOLD', 450.00, 1, NOW()),
('GOLD', 450.00, 1, NOW()),
('GOLD', 450.00, 1, NOW()),
('GOLD', 450.00, 1, NOW()),
('GOLD', 450.00, 1, NOW()),
('GOLD', 450.00, 1, NOW()),
('GOLD', 450.00, 1, NOW()),
('GOLD', 450.00, 1, NOW()),
('GOLD', 450.00, 1, NOW()),
('GOLD', 450.00, 1, NOW()),
('SILVER', 300.00, 1, NOW()),
('SILVER', 300.00, 1, NOW()),
('SILVER', 300.00, 1, NOW()),
('SILVER', 300.00, 1, NOW()),
('SILVER', 300.00, 1, NOW()),
('SILVER', 300.00, 1, NOW()),
('SILVER', 300.00, 1, NOW()),
('SILVER', 300.00, 1, NOW()),
('SILVER', 300.00, 1, NOW()),
('SILVER', 300.00, 1, NOW()),
('PREMIUM', 600.00, 1, NOW()),
('PREMIUM', 600.00, 1, NOW()),
('PREMIUM', 600.00, 1, NOW()),
('PREMIUM', 600.00, 1, NOW()),
('PREMIUM', 600.00, 1, NOW()),
('PREMIUM', 600.00, 1, NOW()),
('PREMIUM', 600.00, 1, NOW()),
('PREMIUM', 600.00, 1, NOW()),
('PREMIUM', 600.00, 1, NOW()),
('PREMIUM', 600.00, 1, NOW());

-- Add seats for 5 more Mumbai theaters (theaters 2-6)
INSERT INTO theater_seat (seat_type, rate, theater_id, created_at)
SELECT 'GOLD', 450.00, theater_id, NOW()
FROM (SELECT 2 AS theater_id UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS theaters
CROSS JOIN (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS seats;

INSERT INTO theater_seat (seat_type, rate, theater_id, created_at)
SELECT 'SILVER', 300.00, theater_id, NOW()
FROM (SELECT 2 AS theater_id UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS theaters
CROSS JOIN (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS seats;

INSERT INTO theater_seat (seat_type, rate, theater_id, created_at)
SELECT 'PREMIUM', 600.00, theater_id, NOW()
FROM (SELECT 2 AS theater_id UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS theaters
CROSS JOIN (SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS seats;

-- ============================================================
-- INSERT SHOWS
-- ============================================================
-- Creating shows for the next 7 days for popular movies

-- Get today's date for show scheduling
SET @today = CURDATE();

-- Jawan (Movie ID 1) - Multiple shows across theaters
INSERT INTO shows (show_date, show_start_time, movie_id, theater_id, created_at) VALUES
-- Today
(@today, '10:00:00', 1, 1, NOW()),
(@today, '13:30:00', 1, 1, NOW()),
(@today, '17:00:00', 1, 1, NOW()),
(@today, '20:30:00', 1, 1, NOW()),
(@today, '11:00:00', 1, 2, NOW()),
(@today, '14:30:00', 1, 2, NOW()),
(@today, '18:00:00', 1, 2, NOW()),
(@today, '21:30:00', 1, 2, NOW()),
-- Tomorrow
(DATE_ADD(@today, INTERVAL 1 DAY), '10:00:00', 1, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '13:30:00', 1, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '17:00:00', 1, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '20:30:00', 1, 1, NOW()),
-- Day after tomorrow
(DATE_ADD(@today, INTERVAL 2 DAY), '10:00:00', 1, 1, NOW()),
(DATE_ADD(@today, INTERVAL 2 DAY), '13:30:00', 1, 1, NOW()),
(DATE_ADD(@today, INTERVAL 2 DAY), '17:00:00', 1, 1, NOW()),
(DATE_ADD(@today, INTERVAL 2 DAY), '20:30:00', 1, 1, NOW());

-- Pathaan (Movie ID 2)
INSERT INTO shows (show_date, show_start_time, movie_id, theater_id, created_at) VALUES
(@today, '10:30:00', 2, 3, NOW()),
(@today, '14:00:00', 2, 3, NOW()),
(@today, '17:30:00', 2, 3, NOW()),
(@today, '21:00:00', 2, 3, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '10:30:00', 2, 3, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '14:00:00', 2, 3, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '17:30:00', 2, 3, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '21:00:00', 2, 3, NOW());

-- RRR (Movie ID 8)
INSERT INTO shows (show_date, show_start_time, movie_id, theater_id, created_at) VALUES
(@today, '09:30:00', 8, 4, NOW()),
(@today, '13:00:00', 8, 4, NOW()),
(@today, '16:30:00', 8, 4, NOW()),
(@today, '20:00:00', 8, 4, NOW()),
(@today, '10:00:00', 8, 5, NOW()),
(@today, '13:30:00', 8, 5, NOW()),
(@today, '17:00:00', 8, 5, NOW()),
(@today, '20:30:00', 8, 5, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '09:30:00', 8, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '13:00:00', 8, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '16:30:00', 8, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '20:00:00', 8, 4, NOW());

-- Vikram (Movie ID 13) - The one you're testing
INSERT INTO shows (show_date, show_start_time, movie_id, theater_id, created_at) VALUES
(@today, '10:00:00', 13, 1, NOW()),
(@today, '13:30:00', 13, 1, NOW()),
(@today, '17:00:00', 13, 1, NOW()),
(@today, '20:30:00', 13, 1, NOW()),
(@today, '11:00:00', 13, 2, NOW()),
(@today, '14:30:00', 13, 2, NOW()),
(@today, '18:00:00', 13, 2, NOW()),
(@today, '21:30:00', 13, 2, NOW()),
(@today, '10:30:00', 13, 3, NOW()),
(@today, '14:00:00', 13, 3, NOW()),
(@today, '17:30:00', 13, 3, NOW()),
(@today, '21:00:00', 13, 3, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '10:00:00', 13, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '13:30:00', 13, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '17:00:00', 13, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '20:30:00', 13, 1, NOW()),
(DATE_ADD(@today, INTERVAL 2 DAY), '10:00:00', 13, 1, NOW()),
(DATE_ADD(@today, INTERVAL 2 DAY), '13:30:00', 13, 1, NOW()),
(DATE_ADD(@today, INTERVAL 2 DAY), '17:00:00', 13, 1, NOW()),
(DATE_ADD(@today, INTERVAL 2 DAY), '20:30:00', 13, 1, NOW());

-- Animal (Movie ID 6)
INSERT INTO shows (show_date, show_start_time, movie_id, theater_id, created_at) VALUES
(@today, '09:00:00', 6, 1, NOW()),
(@today, '12:30:00', 6, 1, NOW()),
(@today, '16:00:00', 6, 1, NOW()),
(@today, '19:30:00', 6, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '09:00:00', 6, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '12:30:00', 6, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '16:00:00', 6, 1, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '19:30:00', 6, 1, NOW());

-- 12th Fail (Movie ID 7)
INSERT INTO shows (show_date, show_start_time, movie_id, theater_id, created_at) VALUES
(@today, '11:00:00', 7, 2, NOW()),
(@today, '14:30:00', 7, 2, NOW()),
(@today, '18:00:00', 7, 2, NOW()),
(@today, '21:30:00', 7, 2, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '11:00:00', 7, 2, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '14:30:00', 7, 2, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '18:00:00', 7, 2, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '21:30:00', 7, 2, NOW());

-- KGF Chapter 2 (Movie ID 9)
INSERT INTO shows (show_date, show_start_time, movie_id, theater_id, created_at) VALUES
(@today, '10:00:00', 9, 3, NOW()),
(@today, '13:30:00', 9, 3, NOW()),
(@today, '17:00:00', 9, 3, NOW()),
(@today, '20:30:00', 9, 3, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '10:00:00', 9, 3, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '13:30:00', 9, 3, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '17:00:00', 9, 3, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '20:30:00', 9, 3, NOW());

-- Leo (Movie ID 11)
INSERT INTO shows (show_date, show_start_time, movie_id, theater_id, created_at) VALUES
(@today, '11:00:00', 11, 4, NOW()),
(@today, '14:30:00', 11, 4, NOW()),
(@today, '18:00:00', 11, 4, NOW()),
(@today, '21:30:00', 11, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '11:00:00', 11, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '14:30:00', 11, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '18:00:00', 11, 4, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '21:30:00', 11, 4, NOW());

-- Oppenheimer (Movie ID 19)
INSERT INTO shows (show_date, show_start_time, movie_id, theater_id, created_at) VALUES
(@today, '10:30:00', 19, 5, NOW()),
(@today, '14:00:00', 19, 5, NOW()),
(@today, '17:30:00', 19, 5, NOW()),
(@today, '21:00:00', 19, 5, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '10:30:00', 19, 5, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '14:00:00', 19, 5, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '17:30:00', 19, 5, NOW()),
(DATE_ADD(@today, INTERVAL 1 DAY), '21:00:00', 19, 5, NOW());

COMMIT;

-- ============================================================
-- Verification Queries
-- ============================================================
-- SELECT COUNT(*) FROM shows;  -- Should return many shows
-- SELECT COUNT(*) FROM theater_seat;  -- Should return seats
-- SELECT m.movie_name, t.name, s.show_date, s.show_start_time 
-- FROM shows s 
-- JOIN movie m ON s.movie_id = m.id 
-- JOIN theater t ON s.theater_id = t.id 
-- ORDER BY s.show_date, s.show_start_time;

