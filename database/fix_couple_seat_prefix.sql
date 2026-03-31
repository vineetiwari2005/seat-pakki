-- Fix duplicate 'C' prefix: Rename COUPLE seats from C-prefix to L-prefix
-- COUPLE seats currently use 'C' (same as CLASSIC), change to 'L' (Love seats)

-- Fix show_seats table
UPDATE show_seats 
SET seat_no = CONCAT('L', SUBSTRING(seat_no, 2))
WHERE seat_type = 'COUPLE' 
  AND seat_no LIKE 'C%';

-- Fix theater_seats table (template seats)
UPDATE theater_seats 
SET seat_no = CONCAT('L', SUBSTRING(seat_no, 2))
WHERE seat_type = 'COUPLE' 
  AND seat_no LIKE 'C%';

-- Verify the fix
SELECT 'show_seats' as tbl, seat_type, seat_no FROM show_seats 
WHERE seat_type IN ('COUPLE', 'CLASSIC') 
ORDER BY seat_type, seat_no;

SELECT 'theater_seats' as tbl, seat_type, seat_no FROM theater_seats 
WHERE seat_type IN ('COUPLE', 'CLASSIC') 
ORDER BY seat_type, seat_no;
