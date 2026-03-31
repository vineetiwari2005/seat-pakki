-- Add fresh shows for current week (Jan 21-28, 2026)
-- This will create shows for all movies in all theaters with proper seats

-- Clean up old shows (optional - safer approach without deleting tickets)
-- Instead of deleting, we'll just add new shows
-- Comment out the delete lines to keep old shows

-- DELETE FROM show_seats WHERE show_id IN (SELECT show_id FROM shows WHERE date < '2026-01-21');
-- DELETE FROM shows WHERE date < '2026-01-21';

-- Insert shows for Jan 21-28, 2026
-- We'll create 4 showtimes per day for each movie-theater combination
-- Showtimes: 10:00, 13:30, 16:30, 19:30

-- Jan 21, 2026 (Tuesday)
INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '10:00:00', '2026-01-21', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '13:30:00', '2026-01-21', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '16:30:00', '2026-01-21', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '19:30:00', '2026-01-21', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

-- Jan 22, 2026 (Wednesday)
INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '10:00:00', '2026-01-22', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '13:30:00', '2026-01-22', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '16:30:00', '2026-01-22', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '19:30:00', '2026-01-22', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

-- Jan 23, 2026 (Thursday)
INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '10:00:00', '2026-01-23', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '13:30:00', '2026-01-23', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '16:30:00', '2026-01-23', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '19:30:00', '2026-01-23', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

-- Jan 24, 2026 (Friday)
INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '10:00:00', '2026-01-24', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '13:30:00', '2026-01-24', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '16:30:00', '2026-01-24', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '19:30:00', '2026-01-24', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

-- Jan 25, 2026 (Saturday)
INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '10:00:00', '2026-01-25', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '13:30:00', '2026-01-25', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '16:30:00', '2026-01-25', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '19:30:00', '2026-01-25', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

-- Jan 26, 2026 (Sunday)
INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '10:00:00', '2026-01-26', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '13:30:00', '2026-01-26', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '16:30:00', '2026-01-26', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '19:30:00', '2026-01-26', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

-- Jan 27, 2026 (Monday)
INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '10:00:00', '2026-01-27', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '13:30:00', '2026-01-27', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '16:30:00', '2026-01-27', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '19:30:00', '2026-01-27', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

-- Jan 28, 2026 (Tuesday)
INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '10:00:00', '2026-01-28', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '13:30:00', '2026-01-28', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '16:30:00', '2026-01-28', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

INSERT INTO shows (time, date, movie_id, theater_id, created_at)
SELECT '19:30:00', '2026-01-28', m.id, t.id, NOW()
FROM movies m
CROSS JOIN theaters t
WHERE m.id <= 10 AND t.id <= 5
LIMIT 50;

-- Now create seats for all newly created shows
-- This will create seats for each show based on theater seats

INSERT INTO show_seats (seat_no, seat_type, price, show_id, is_available, is_food_contains)
SELECT 
    ts.seat_no,
    ts.seat_type,
    CASE 
        WHEN ts.seat_type = 'COUPLE' THEN 600
        WHEN ts.seat_type = 'PREMIUM' THEN 350
        WHEN ts.seat_type = 'GOLD' THEN 250
        ELSE 150
    END as price,
    s.show_id,
    TRUE,
    FALSE
FROM shows s
INNER JOIN theater_seats ts ON s.theater_id = ts.theater_id
WHERE s.date >= '2026-01-21' AND s.date <= '2026-01-28'
AND NOT EXISTS (
    SELECT 1 FROM show_seats ss WHERE ss.show_id = s.show_id
);

-- Verify the results
SELECT 
    DATE(s.date) as show_date,
    COUNT(DISTINCT s.show_id) as total_shows,
    COUNT(ss.id) as total_seats
FROM shows s
LEFT JOIN show_seats ss ON s.show_id = ss.show_id
WHERE s.date >= '2026-01-21' AND s.date <= '2026-01-28'
GROUP BY DATE(s.date)
ORDER BY show_date;
