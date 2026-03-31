-- Fix existing shows that don't have seats
-- This script creates seats for shows that are missing them

-- Note: Run this if you have shows created before the auto-seat-creation fix
-- This will create seats for each show based on the theater's seat configuration

-- You may need to customize the pricing based on your theater's actual prices
-- Current default prices: COUPLE=600, PREMIUM=350, GOLD=250, CLASSIC/SILVER=150

-- To execute this, you would need to create a stored procedure
-- or manually run seat creation for each show via the associateShowSeats endpoint

-- Quick fix: Delete shows without seats and recreate them
-- The new addShow() method will automatically create seats

SELECT 
    s.show_id,
    s.date,
    s.time,
    m.movie_name,
    t.theater_name,
    COUNT(ss.id) as seat_count
FROM shows s
LEFT JOIN movies m ON s.movie_id = m.movie_id  
LEFT JOIN theaters t ON s.theater_id = t.theater_id
LEFT JOIN show_seats ss ON s.show_id = ss.show_id
GROUP BY s.show_id, s.date, s.time, m.movie_name, t.theater_name
HAVING seat_count = 0
ORDER BY s.date, s.time;

-- If you see shows with 0 seats, you can either:
-- 1. Delete them: DELETE FROM shows WHERE show_id IN (select ids from above query);
-- 2. Or use the backend API to associate seats: POST /api/admin/show-seats with ShowSeatEntryDto
