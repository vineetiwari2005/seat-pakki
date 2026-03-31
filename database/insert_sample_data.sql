-- BookMyShow Database - Sample Data Insert Script
-- This script populates the database with Indian movies and theaters

-- ==============================================
-- INSERT MOVIES
-- ==============================================
-- Table structure: id, movie_name, release_date, duration, genre, language, rating

INSERT INTO movies (movie_name, release_date, duration, genre, language, rating) VALUES
('Jawan', '2023-09-07', 169, 'ACTION', 'HINDI', 8.5),
('Pathaan', '2023-01-25', 146, 'ACTION', 'HINDI', 8.0),
('Dunki', '2023-12-21', 161, 'DRAMA', 'HINDI', 7.8),
('Tiger 3', '2023-11-12', 155, 'THRILLER', 'HINDI', 7.5),
('Gadar 2', '2023-08-11', 170, 'ACTION', 'HINDI', 7.2),
('Animal', '2023-12-01', 201, 'ACTION', 'HINDI', 7.9),
('12th Fail', '2023-10-27', 147, 'DRAMA', 'HINDI', 9.2),
('RRR', '2022-03-25', 187, 'ACTION', 'TELUGU', 8.8),
('KGF Chapter 2', '2022-04-14', 168, 'ACTION', 'KANNADA', 8.3),
('Salaar', '2023-12-22', 175, 'THRILLER', 'KANNADA', 7.7),
('Leo', '2023-10-19', 164, 'ACTION', 'TAMIL', 7.3),
('Jailer', '2023-08-10', 168, 'COMEDY', 'TAMIL', 7.8),
('Vikram', '2022-06-03', 174, 'THRILLER', 'TAMIL', 8.3),
('Ponniyin Selvan 2', '2023-04-28', 164, 'DRAMA', 'TAMIL', 7.5),
('Varisu', '2023-01-11', 169, 'ACTION', 'TAMIL', 6.8),
('Brahmastra', '2022-09-09', 167, 'ACTION', 'HINDI', 7.5),
('Adipurush', '2023-06-16', 179, 'HISTORICAL', 'HINDI', 6.2),
('Mission Impossible 7', '2023-07-12', 163, 'ACTION', 'ENGLISH', 8.0),
('Oppenheimer', '2023-07-21', 180, 'DRAMA', 'ENGLISH', 8.6),
('Barbie', '2023-07-21', 114, 'COMEDY', 'ENGLISH', 7.8);

-- ==============================================
-- INSERT THEATERS
-- ==============================================
-- Table structure: id, name, address

INSERT INTO theaters (name, address) VALUES
-- Mumbai
('PVR Phoenix Palladium', 'Lower Parel, Mumbai'),
('INOX Nariman Point', 'Nariman Point, Mumbai'),
('Cinepolis Andheri', 'Andheri West, Mumbai'),
('Carnival IMAX Wadala', 'Wadala, Mumbai'),
('PVR ICON Versova', 'Versova, Mumbai'),

-- Delhi
('PVR Select Citywalk', 'Saket, Delhi'),
('INOX Nehru Place', 'Nehru Place, Delhi'),
('Cinepolis DLF Place', 'Saket, Delhi'),
('PVR Priya Vasant Vihar', 'Vasant Vihar, Delhi'),
('Carnival Cinemas Rohini', 'Rohini, Delhi'),

-- Bangalore
('PVR Forum Mall', 'Koramangala, Bangalore'),
('INOX Garuda Mall', 'Magrath Road, Bangalore'),
('Cinepolis Royal Meenakshi', 'Bannerghatta Road, Bangalore'),
('PVR Orion Mall', 'Rajajinagar, Bangalore'),
('INOX Mantri Square', 'Malleswaram, Bangalore'),

-- Chennai
('PVR Grand Galada', 'Pallavaram, Chennai'),
('INOX Escape', 'OMR, Chennai'),
('Sathyam Cinemas', 'Royapettah, Chennai'),
('PVR VR Chennai', 'Anna Nagar, Chennai'),
('Cinepolis Ampa Skywalk', 'Aminjikarai, Chennai'),

-- Hyderabad
('PVR Next Galleria', 'Punjagutta, Hyderabad'),
('INOX GVK One', 'Banjara Hills, Hyderabad'),
('Cinepolis Manjeera', 'KPHB, Hyderabad'),
('AMB Cinemas', 'Gachibowli, Hyderabad'),
('PVR Panjagutta', 'Panjagutta, Hyderabad'),

-- Kolkata
('PVR South City', 'South City Mall, Kolkata'),
('INOX Quest', 'Park Circus, Kolkata'),
('Cinepolis Lake Mall', 'Rash Behari, Kolkata'),
('PVR Avani Riverside', 'Howrah, Kolkata'),
('INOX Forum', 'Elgin Road, Kolkata'),

-- Pune
('PVR Phoenix Market City', 'Viman Nagar, Pune'),
('INOX Bund Garden', 'Bund Garden Road, Pune'),
('Cinepolis Westend Mall', 'Aundh, Pune'),
('PVR Pavilion Mall', 'Shivajinagar, Pune'),
('City Pride Multiplex', 'Kothrud, Pune');

COMMIT;

-- ==============================================
-- Verification Queries
-- ==============================================
-- SELECT COUNT(*) FROM movies;  -- Should return 20
-- SELECT COUNT(*) FROM theaters; -- Should return 35
-- SELECT movie_name, language, rating FROM movies ORDER BY rating DESC;
-- SELECT name, address FROM theaters;

('Gadar 2', '2023-08-11', 170, 'ACTION,DRAMA,ROMANCE', 'HINDI', 7.2, 'During the Indo-Pakistani War of 1971, Tara Singh returns to Pakistan to bring his son, Charanjeet, back home.', 'Anil Sharma', 'Sunny Deol, Ameesha Patel, Utkarsh Sharma', 'https://image.tmdb.org/t/p/w500/gadar2.jpg', 'https://youtube.com/watch?v=gadar2', true, NOW()),

('Animal', '2023-12-01', 201, 'ACTION,CRIME,DRAMA', 'HINDI', 7.9, 'A son''s love for his father turns into an obsession when he discovers a threat to his father''s life.', 'Sandeep Reddy Vanga', 'Ranbir Kapoor, Anil Kapoor, Bobby Deol, Rashmika Mandanna', 'https://image.tmdb.org/t/p/w500/animal.jpg', 'https://youtube.com/watch?v=animal', true, NOW()),

('12th Fail', '2023-10-27', 147, 'DRAMA', 'HINDI', 9.2, 'Based on the true story of an IPS officer who overcomes hardships and failures to achieve his dream.', 'Vidhu Vinod Chopra', 'Vikrant Massey, Medha Shankar', 'https://image.tmdb.org/t/p/w500/12thfail.jpg', 'https://youtube.com/watch?v=12thfail', true, NOW()),

-- South Indian Movies
('RRR', '2022-03-25', 187, 'ACTION,DRAMA', 'TELUGU', 8.8, 'A fictional story about two legendary revolutionaries and their journey away from home before they started fighting for their country in 1920s.', 'S.S. Rajamouli', 'N.T. Rama Rao Jr., Ram Charan, Alia Bhatt', 'https://image.tmdb.org/t/p/w500/rrr.jpg', 'https://youtube.com/watch?v=rrr', true, NOW()),

('KGF Chapter 2', '2022-04-14', 168, 'ACTION,THRILLER', 'KANNADA', 8.3, 'In the blood-soaked Kolar Gold Fields, Rocky''s name strikes fear into his foes, while the government sees him as a threat to law and order.', 'Prashanth Neel', 'Yash, Sanjay Dutt, Raveena Tandon', 'https://image.tmdb.org/t/p/w500/kgf2.jpg', 'https://youtube.com/watch?v=kgf2', true, NOW()),

('Salaar', '2023-12-22', 175, 'ACTION,THRILLER', 'KANNADA', 7.7, 'The fate of a violently contested kingdom hangs on the fraught bond between two friends-turned-foes in this saga of power, bloodshed and betrayal.', 'Prashanth Neel', 'Prabhas, Prithviraj Sukumaran, Shruti Haasan', 'https://image.tmdb.org/t/p/w500/salaar.jpg', 'https://youtube.com/watch?v=salaar', true, NOW()),

('Leo', '2023-10-19', 164, 'ACTION,THRILLER', 'TAMIL', 7.3, 'A mild-mannered cafe owner in a hill station is forced to confront his violent past when gangsters arrive claiming he is their long-lost leader.', 'Lokesh Kanagaraj', 'Vijay, Sanjay Dutt, Trisha Krishnan', 'https://image.tmdb.org/t/p/w500/leo.jpg', 'https://youtube.com/watch?v=leo', true, NOW()),

('Jailer', '2023-08-10', 168, 'ACTION,COMEDY,THRILLER', 'TAMIL', 7.8, 'A retired jailer goes on a manhunt to find his son''s killers. But the road leads him to a familiar, albeit a bit darker place.', 'Nelson Dilipkumar', 'Rajinikanth, Mohanlal, Shiva Rajkumar', 'https://image.tmdb.org/t/p/w500/jailer.jpg', 'https://youtube.com/watch?v=jailer', true, NOW()),

('Vikram', '2022-06-03', 174, 'ACTION,THRILLER', 'TAMIL', 8.3, 'Members of a black ops team must track and eliminate a gang of masked murderers.', 'Lokesh Kanagaraj', 'Kamal Haasan, Vijay Sethupathi, Fahadh Faasil', 'https://image.tmdb.org/t/p/w500/vikram.jpg', 'https://youtube.com/watch?v=vikram', true, NOW()),

('Ponniyin Selvan: Part 2', '2023-04-28', 164, 'ACTION,ADVENTURE,DRAMA', 'TAMIL', 7.5, 'Arulmozhi Varman continues on his journey to become Rajaraja I, the greatest ruler of the historic Chola empire of south India.', 'Mani Ratnam', 'Vikram, Aishwarya Rai, Karthi, Jayam Ravi', 'https://image.tmdb.org/t/p/w500/ps2.jpg', 'https://youtube.com/watch?v=ps2', true, NOW()),

('Varisu', '2023-01-11', 169, 'ACTION,DRAMA', 'TAMIL', 6.8, 'A son steps up to protect his father''s business empire from his siblings and regain the family''s lost glory.', 'Vamshi Paidipally', 'Vijay, Rashmika Mandanna, Prakash Raj', 'https://image.tmdb.org/t/p/w500/varisu.jpg', 'https://youtube.com/watch?v=varisu', true, NOW()),

-- English/Hindi Dubbed
('Brahmastra: Part One', '2022-09-09', 167, 'ACTION,ADVENTURE,FANTASY', 'HINDI', 7.5, 'A DJ with a connection to fire discovers his power while falling for a woman who can control water, as they fight ancient forces.', 'Ayan Mukerji', 'Ranbir Kapoor, Alia Bhatt, Amitabh Bachchan', 'https://image.tmdb.org/t/p/w500/brahmastra.jpg', 'https://youtube.com/watch?v=brahmastra', true, NOW()),

('Adipurush', '2023-06-16', 179, 'ACTION,ADVENTURE,DRAMA', 'HINDI', 6.2, 'A modern adaptation of the Indian epic Ramayana which follows the exiled prince Raghav''s journey to rescue his wife Janaki from the raakshash king Lankesh.', 'Om Raut', 'Prabhas, Kriti Sanon, Saif Ali Khan', 'https://image.tmdb.org/t/p/w500/adipurush.jpg', 'https://youtube.com/watch?v=adipurush', true, NOW()),

('Mission Impossible: Dead Reckoning', '2023-07-12', 163, 'ACTION,THRILLER', 'ENGLISH', 8.0, 'Ethan Hunt and his IMF team must track down a dangerous weapon before it falls into the wrong hands.', 'Christopher McQuarrie', 'Tom Cruise, Hayley Atwell, Ving Rhames', 'https://image.tmdb.org/t/p/w500/mi7.jpg', 'https://youtube.com/watch?v=mi7', true, NOW()),

('Oppenheimer', '2023-07-21', 180, 'BIOGRAPHY,DRAMA,HISTORY', 'ENGLISH', 8.6, 'The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb.', 'Christopher Nolan', 'Cillian Murphy, Emily Blunt, Robert Downey Jr.', 'https://image.tmdb.org/t/p/w500/oppenheimer.jpg', 'https://youtube.com/watch?v=oppenheimer', true, NOW()),

('Barbie', '2023-07-21', 114, 'COMEDY,FANTASY', 'ENGLISH', 7.8, 'Barbie and Ken are having the time of their lives in Barbie Land until they discover what it means to be human.', 'Greta Gerwig', 'Margot Robbie, Ryan Gosling, Will Ferrell', 'https://image.tmdb.org/t/p/w500/barbie.jpg', 'https://youtube.com/watch?v=barbie', true, NOW());

-- ==============================================
-- INSERT THEATERS (Sample theaters in major cities)
-- ==============================================

-- Mumbai Theaters
INSERT INTO theater (name, location, city, created_at) VALUES
('PVR Phoenix Palladium', 'Lower Parel', 'Mumbai', NOW()),
('INOX Nariman Point', 'Nariman Point', 'Mumbai', NOW()),
('Cinepolis Andheri', 'Andheri West', 'Mumbai', NOW()),
('Carnival IMAX Wadala', 'Wadala', 'Mumbai', NOW()),
('PVR ICON Versova', 'Versova', 'Mumbai', NOW());

-- Delhi Theaters
INSERT INTO theater (name, location, city, created_at) VALUES
('PVR Select Citywalk', 'Saket', 'Delhi', NOW()),
('INOX Nehru Place', 'Nehru Place', 'Delhi', NOW()),
('Cinepolis DLF Place', 'Saket', 'Delhi', NOW()),
('PVR Priya Vasant Vihar', 'Vasant Vihar', 'Delhi', NOW()),
('Carnival Cinemas Rohini', 'Rohini', 'Delhi', NOW());

-- Bangalore Theaters
INSERT INTO theater (name, location, city, created_at) VALUES
('PVR Forum Mall', 'Koramangala', 'Bangalore', NOW()),
('INOX Garuda Mall', 'Magrath Road', 'Bangalore', NOW()),
('Cinepolis Royal Meenakshi', 'Bannerghatta Road', 'Bangalore', NOW()),
('PVR Orion Mall', 'Rajajinagar', 'Bangalore', NOW()),
('INOX Mantri Square', 'Malleswaram', 'Bangalore', NOW());

-- Chennai Theaters
INSERT INTO theater (name, location, city, created_at) VALUES
('PVR Grand Galada', 'Pallavaram', 'Chennai', NOW()),
('INOX Escape', 'OMR', 'Chennai', NOW()),
('Sathyam Cinemas', 'Royapettah', 'Chennai', NOW()),
('PVR VR Chennai', 'Anna Nagar', 'Chennai', NOW()),
('Cinepolis Ampa Skywalk', 'Aminjikarai', 'Chennai', NOW());

-- Hyderabad Theaters
INSERT INTO theater (name, location, city, created_at) VALUES
('PVR Next Galleria', 'Punjagutta', 'Hyderabad', NOW()),
('INOX GVK One', 'Banjara Hills', 'Hyderabad', NOW()),
('Cinepolis Manjeera', 'KPHB', 'Hyderabad', NOW()),
('AMB Cinemas', 'Gachibowli', 'Hyderabad', NOW()),
('PVR Panjagutta', 'Panjagutta', 'Hyderabad', NOW());

-- Kolkata Theaters
INSERT INTO theater (name, location, city, created_at) VALUES
('PVR South City', 'South City Mall', 'Kolkata', NOW()),
('INOX Quest', 'Park Circus', 'Kolkata', NOW()),
('Cinepolis Lake Mall', 'Rash Behari', 'Kolkata', NOW()),
('PVR Avani Riverside', 'Howrah', 'Kolkata', NOW()),
('INOX Forum', 'Elgin Road', 'Kolkata', NOW());

-- Pune Theaters
INSERT INTO theater (name, location, city, created_at) VALUES
('PVR Phoenix Market City', 'Viman Nagar', 'Pune', NOW()),
('INOX Bund Garden', 'Bund Garden Road', 'Pune', NOW()),
('Cinepolis Westend Mall', 'Aundh', 'Pune', NOW()),
('PVR Pavilion Mall', 'Shivajinagar', 'Pune', NOW()),
('City Pride Multiplex', 'Kothrud', 'Pune', NOW());

-- ==============================================
-- Note: Theater Seats and Shows
-- ==============================================
-- Theater seats and shows should be created through your application's
-- admin panel or via the REST API endpoints as they have complex 
-- relationships and seat configurations.

-- For testing, you can create shows using POST requests to:
-- /show/addNew with showDate, startTime, movieId, theaterId

-- Theater seats can be added via:
-- /theater/addTheaterSeat with rate, seatType, theaterId

COMMIT;

-- ==============================================
-- Verification Queries
-- ==============================================
-- SELECT COUNT(*) FROM movie;  -- Should return 20
-- SELECT COUNT(*) FROM theater; -- Should return 35
-- SELECT name, language, rating FROM movie ORDER BY rating DESC;
-- SELECT name, city FROM theater ORDER BY city, name;
