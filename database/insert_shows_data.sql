-- BookMyShow - Insert Sample Shows Data
-- This creates shows for the next 7 days for all movies in all theaters

-- ==============================================
-- INSERT SHOWS FOR NEXT 7 DAYS
-- ==============================================
-- Shows for each movie at multiple times across different theaters

-- Jawan Shows (Movie ID will be 1)
INSERT INTO show (start_time, show_date, movie_id, theater_id, created_at) VALUES
-- Today
('10:00:00', CURDATE(), 1, 1, NOW()),
('13:30:00', CURDATE(), 1, 1, NOW()),
('17:00:00', CURDATE(), 1, 1, NOW()),
('20:30:00', CURDATE(), 1, 1, NOW()),

-- Tomorrow
('10:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 1, NOW()),
('13:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 1, NOW()),
('17:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 1, NOW()),
('20:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 1, 1, NOW()),

-- Day 2
('10:00:00', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, 1, NOW()),
('13:30:00', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, 1, NOW()),
('17:00:00', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, 1, NOW()),
('20:30:00', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 1, 1, NOW());

-- Pathaan Shows (Movie ID 2)
INSERT INTO show (start_time, show_date, movie_id, theater_id, created_at) VALUES
('11:00:00', CURDATE(), 2, 2, NOW()),
('14:30:00', CURDATE(), 2, 2, NOW()),
('18:00:00', CURDATE(), 2, 2, NOW()),
('21:30:00', CURDATE(), 2, 2, NOW()),

('11:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, 2, NOW()),
('14:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, 2, NOW()),
('18:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, 2, NOW()),
('21:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 2, 2, NOW());

-- RRR Shows (Movie ID 8)
INSERT INTO show (start_time, show_date, movie_id, theater_id, created_at) VALUES
('09:30:00', CURDATE(), 8, 3, NOW()),
('13:00:00', CURDATE(), 8, 3, NOW()),
('16:30:00', CURDATE(), 8, 3, NOW()),
('20:00:00', CURDATE(), 8, 3, NOW()),

('09:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 8, 3, NOW()),
('13:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 8, 3, NOW()),
('16:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 8, 3, NOW()),
('20:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 8, 3, NOW());

-- Vikram Shows (Movie ID 13)
INSERT INTO show (start_time, show_date, movie_id, theater_id, created_at) VALUES
('10:30:00', CURDATE(), 13, 4, NOW()),
('14:00:00', CURDATE(), 13, 4, NOW()),
('17:30:00', CURDATE(), 13, 4, NOW()),
('21:00:00', CURDATE(), 13, 4, NOW()),

('10:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 13, 4, NOW()),
('14:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 13, 4, NOW()),
('17:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 13, 4, NOW()),
('21:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 13, 4, NOW());

-- KGF Chapter 2 Shows (Movie ID 9)
INSERT INTO show (start_time, show_date, movie_id, theater_id, created_at) VALUES
('09:00:00', CURDATE(), 9, 5, NOW()),
('12:30:00', CURDATE(), 9, 5, NOW()),
('16:00:00', CURDATE(), 9, 5, NOW()),
('19:30:00', CURDATE(), 9, 5, NOW()),

('09:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 9, 5, NOW()),
('12:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 9, 5, NOW()),
('16:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 9, 5, NOW()),
('19:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 9, 5, NOW());

-- Jailer Shows (Movie ID 12)
INSERT INTO show (start_time, show_date, movie_id, theater_id, created_at) VALUES
('10:00:00', CURDATE(), 12, 6, NOW()),
('13:30:00', CURDATE(), 12, 6, NOW()),
('17:00:00', CURDATE(), 12, 6, NOW()),
('20:30:00', CURDATE(), 12, 6, NOW()),

('10:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 12, 6, NOW()),
('13:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 12, 6, NOW()),
('17:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 12, 6, NOW()),
('20:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 12, 6, NOW());

-- Animal Shows (Movie ID 6)
INSERT INTO show (start_time, show_date, movie_id, theater_id, created_at) VALUES
('11:30:00', CURDATE(), 6, 7, NOW()),
('15:00:00', CURDATE(), 6, 7, NOW()),
('18:30:00', CURDATE(), 6, 7, NOW()),
('22:00:00', CURDATE(), 6, 7, NOW()),

('11:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 6, 7, NOW()),
('15:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 6, 7, NOW()),
('18:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 6, 7, NOW()),
('22:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 6, 7, NOW());

-- 12th Fail Shows (Movie ID 7)
INSERT INTO show (start_time, show_date, movie_id, theater_id, created_at) VALUES
('12:00:00', CURDATE(), 7, 8, NOW()),
('15:30:00', CURDATE(), 7, 8, NOW()),
('19:00:00', CURDATE(), 7, 8, NOW()),

('12:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 7, 8, NOW()),
('15:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 7, 8, NOW()),
('19:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 7, 8, NOW());

-- Salaar Shows (Movie ID 10)
INSERT INTO show (start_time, show_date, movie_id, theater_id, created_at) VALUES
('10:00:00', CURDATE(), 10, 9, NOW()),
('13:30:00', CURDATE(), 10, 9, NOW()),
('17:00:00', CURDATE(), 10, 9, NOW()),
('20:30:00', CURDATE(), 10, 9, NOW()),

('10:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 10, 9, NOW()),
('13:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 10, 9, NOW()),
('17:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 10, 9, NOW()),
('20:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 10, 9, NOW());

-- Leo Shows (Movie ID 11)
INSERT INTO show (start_time, show_date, movie_id, theater_id, created_at) VALUES
('11:00:00', CURDATE(), 11, 10, NOW()),
('14:30:00', CURDATE(), 11, 10, NOW()),
('18:00:00', CURDATE(), 11, 10, NOW()),
('21:30:00', CURDATE(), 11, 10, NOW()),

('11:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 11, 10, NOW()),
('14:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 11, 10, NOW()),
('18:00:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 11, 10, NOW()),
('21:30:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY), 11, 10, NOW());

COMMIT;

-- ==============================================
-- Verification Query
-- ==============================================
-- SELECT COUNT(*) FROM show;  -- Should return many shows
-- SELECT s.id, m.movie_name, t.name as theater, s.show_date, s.start_time 
-- FROM show s 
-- JOIN movie m ON s.movie_id = m.id 
-- JOIN theater t ON s.theater_id = t.id
-- ORDER BY s.show_date, s.start_time;
