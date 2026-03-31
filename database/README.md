# 🗄️ Database Setup Instructions

## Step 1: Ensure Database Exists

First, make sure your `bookmyshow` database is created and the Spring Boot application has run at least once to create all tables.

## Step 2: Run the Sample Data Script

### Option A: Using MySQL Command Line

```bash
# Navigate to the database directory
cd /Users/vineettiwari/Downloads/bookmyshow/Book-My-Show/database

# Run the SQL script
mysql -u root -p bookmyshow < insert_sample_data.sql

# Enter your MySQL password when prompted
```

### Option B: Using MySQL Workbench

1. Open MySQL Workbench
2. Connect to your MySQL server
3. Select the `bookmyshow` database
4. Go to **File → Open SQL Script**
5. Navigate to `/Users/vineettiwari/Downloads/bookmyshow/Book-My-Show/database/insert_sample_data.sql`
6. Click **Execute** (lightning bolt icon)

### Option C: Using IntelliJ Database Tool

1. In IntelliJ, open the **Database** tool window (View → Tool Windows → Database)
2. Connect to your MySQL database if not already connected
3. Right-click on the `bookmyshow` database
4. Select **Run SQL Script**
5. Choose the `insert_sample_data.sql` file
6. Click **Run**

## Step 3: Verify Data Insertion

Run these queries to verify:

```sql
-- Check movies count (should be 20)
SELECT COUNT(*) as total_movies FROM movie;

-- Check theaters count (should be 35)  
SELECT COUNT(*) as total_theaters FROM theater;

-- View all movies
SELECT id, name, language, rating, now_showing FROM movie ORDER BY rating DESC;

-- View theaters by city
SELECT id, name, location, city FROM theater ORDER BY city, name;

-- Top rated movies
SELECT name, language, rating FROM movie WHERE now_showing = true ORDER BY rating DESC LIMIT 10;
```

## Step 4: Restart Your Backend

After inserting data, restart your Spring Boot backend in IntelliJ to ensure all caches are cleared.

## What This Script Does

✅ Inserts **20 movies** including:
   - Bollywood hits (Jawan, Pathaan, Dunki, Tiger 3, Animal, 12th Fail)
   - South Indian blockbusters (RRR, KGF 2, Salaar, Leo, Jailer, Vikram)
   - Hollywood releases (Mission Impossible 7, Oppenheimer, Barbie)

✅ Creates **35 theaters** across 7 cities:
   - Mumbai: 5 theaters (PVR Phoenix, INOX Nariman, Cinepolis Andheri, etc.)
   - Delhi: 5 theaters
   - Bangalore: 5 theaters
   - Chennai: 5 theaters
   - Hyderabad: 5 theaters
   - Kolkata: 5 theaters
   - Pune: 5 theaters

## Next Steps

After inserting the data:

1. **Create Shows** (via your backend API or admin panel):
   ```bash
   POST /show/addNew
   {
     "showDate": "2026-01-10",
     "startTime": "10:00:00",
     "movieId": 1,
     "theaterId": 1
   }
   ```

2. **Add Theater Seats** (via backend API):
   ```bash
   POST /theater/addTheaterSeat
   {
     "theaterId": 1,
     "seatType": "PREMIUM",
     "rate": 350
   }
   ```

3. **Test Frontend**:
   - Open http://localhost:3001
   - You should see all 20 movies on the home page
   - Try searching, filtering by genre/language
   - Movies should load from database instead of mock data

## Troubleshooting

**Issue**: Script fails with "table doesn't exist"
**Solution**: Run your Spring Boot app first to create tables, then run this script

**Issue**: Duplicate entry errors
**Solution**: Database already has data. Either:
- Clear existing data: `TRUNCATE TABLE movie; TRUNCATE TABLE theater;`
- Or skip this script if you already have production data

**Issue**: Foreign key constraint fails
**Solution**: Make sure you're running Spring Boot with `spring.jpa.hibernate.ddl-auto=update` to create tables first

## Database Schema Reference

The script assumes these tables exist:
- `movie` (id, name, release_date, duration, genre, language, rating, etc.)
- `theater` (id, name, location, city, created_at)
- `theater_seat` (id, theater_id, seat_type, rate)
- `show` (id, show_date, start_time, movie_id, theater_id)
- `user` (id, name, email, password, role, etc.)
- `ticket` (id, user_id, show_id, booking_time, etc.)

Your Spring Boot app creates these automatically via JPA/Hibernate.

---

**Once data is inserted, your frontend will fetch real movies from the database! 🎉**
