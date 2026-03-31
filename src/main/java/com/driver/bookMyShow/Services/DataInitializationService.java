package com.driver.bookMyShow.Services;

import com.driver.bookMyShow.Enums.Gender;
import com.driver.bookMyShow.Enums.Genre;
import com.driver.bookMyShow.Enums.Language;
import com.driver.bookMyShow.Enums.SeatType;
import com.driver.bookMyShow.Enums.TransactionType;
import com.driver.bookMyShow.Enums.UserRole;
import com.driver.bookMyShow.Models.*;
import com.driver.bookMyShow.Repositories.*;
import com.driver.bookMyShow.modules.food.entity.FoodItem;
import com.driver.bookMyShow.modules.food.repository.FoodItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitializationService implements CommandLineRunner {

    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final CityRepository cityRepository;
    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final TheaterSeatRepository theaterSeatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final TicketRepository ticketRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final FoodItemRepository foodItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Only initialize if database is empty
        if (movieRepository.count() == 0) {
            initializeMovies();
        }
        if (theaterRepository.count() == 0) {
            initializeTheaters();
        }
        // Always ensure admin users exist (check by email, not by count)
        ensureAdminUsersExist();

        if (userRepository.count() <= 2) {
            initializeUsers();
        }
        // NOTE: Theater seats and show seats are NO LONGER auto-initialized.
        // Admin must manage seats dynamically via /admin/theaters/{id}/seats and /admin/shows/{id}/seats endpoints.
        if (showRepository.count() == 0) {
            initializeShows();
        } else {
            // Refresh shows if all existing shows are in the past
            refreshStaleShows();
        }
        
        // Initialize sample bookings and transactions
        if (ticketRepository.count() == 0) {
            log.info("🎫 Creating sample bookings and wallet transactions...");
            initializeSampleBookingsAndTransactions();
        }
        
        // Initialize food menu items for all theaters
        if (foodItemRepository.count() == 0) {
            log.info("🍿 Creating food menu items for theaters...");
            initializeFoodItems();
        }
    }

    private void initializeMovies() {
        Movie[] movies = {
            createMovie("Jawan", "2023-09-07", 169, Genre.ACTION, Language.HINDI, 8.5,
                "A man is driven by a personal vendetta to rectify the wrongs in society.",
                "Atlee", "Shah Rukh Khan, Nayanthara, Vijay Sethupathi",
                "https://via.placeholder.com/300x450/FF6B6B/FFFFFF?text=Jawan", 
                "https://youtube.com/watch?v=jawan"),

            createMovie("Pathaan", "2023-01-25", 146, Genre.ACTION, Language.HINDI, 8.2,
                "An Indian spy takes on the leader of a terrorist organization.",
                "Siddharth Anand", "Shah Rukh Khan, Deepika Padukone, John Abraham",
                "https://via.placeholder.com/300x450/4ECDC4/FFFFFF?text=Pathaan", 
                "https://youtube.com/watch?v=pathaan"),

            createMovie("RRR", "2022-03-25", 187, Genre.ACTION, Language.TELUGU, 8.8,
                "A fictitious story of two legendary revolutionaries.",
                "S.S. Rajamouli", "N.T. Rama Rao Jr., Ram Charan, Alia Bhatt",
                "https://via.placeholder.com/300x450/FFE66D/000000?text=RRR", 
                "https://youtube.com/watch?v=rrr"),

            createMovie("KGF Chapter 2", "2022-04-14", 166, Genre.ACTION, Language.KANNADA, 8.4,
                "Rocky continues his journey to be the most powerful.",
                "Prashanth Neel", "Yash, Sanjay Dutt, Raveena Tandon",
                "https://via.placeholder.com/300x450/A8E6CF/000000?text=KGF+2", 
                "https://youtube.com/watch?v=kgf2"),

            createMovie("Salaar", "2023-12-22", 175, Genre.ACTION, Language.TELUGU, 8.1,
                "A gang leader tries to keep a promise made to his dying friend.",
                "Prashanth Neel", "Prabhas, Prithviraj Sukumaran, Shruti Haasan",
                "https://v3img.voot.com/resizeMedium,w_810,h_1080/v3Storage/assets/salaar-16x9-1703843534684.jpg", 
                "https://youtube.com/watch?v=salaar"),

            createMovie("Leo", "2023-10-19", 164, Genre.ACTION, Language.TAMIL, 7.9,
                "A mild-mannered cafe owner's peaceful life is shaken.",
                "Lokesh Kanagaraj", "Vijay, Trisha, Sanjay Dutt",
                "https://m.media-amazon.com/images/M/MV5BYjFkMTlkYWUtZWFhNy00M2FmLThiOTYtYTRiYjVlZWYxNmJkXkEyXkFqcGdeQXVyMTUzNTgzNzM0._V1_.jpg", 
                "https://youtube.com/watch?v=leo"),

            createMovie("Jailer", "2023-08-10", 168, Genre.ACTION, Language.TAMIL, 8.3,
                "A retired jailer goes on a manhunt to find his son's killers.",
                "Nelson Dilipkumar", "Rajinikanth, Mohanlal, Jackie Shroff",
                "https://m.media-amazon.com/images/M/MV5BYjFjMTQzY2EtZjQ5MC00NGUyLWJiYWMtZDhmNzJmNGYyODAyXkEyXkFqcGdeQXVyMTUzNTgzNzM0._V1_FMjpg_UX1000_.jpg", 
                "https://youtube.com/watch?v=jailer"),

            createMovie("Vikram", "2022-06-03", 174, Genre.ACTION, Language.TAMIL, 8.5,
                "Members of a black ops team must track and eliminate a gang of masked murderers.",
                "Lokesh Kanagaraj", "Kamal Haasan, Vijay Sethupathi, Fahadh Faasil",
                "https://m.media-amazon.com/images/M/MV5BMmJhYTYxMGEtNjQ5NS00MWZiLWEwN2ItYjJmMWE2YTU1YWYxXkEyXkFqcGdeQXVyMTEzNzg0Mjkx._V1_.jpg", 
                "https://youtube.com/watch?v=vikram"),

            createMovie("Pushpa: The Rise", "2021-12-17", 179, Genre.ACTION, Language.TELUGU, 7.8,
                "A laborer rises through the ranks of a red sandalwood smuggling syndicate.",
                "Sukumar", "Allu Arjun, Rashmika Mandanna, Fahadh Faasil",
                "https://m.media-amazon.com/images/M/MV5BY2FmYTY1NTctNzkiMi00M2VlLWI2ZGQtMTMxNzk3Njg0NWY4XkEyXkFqcGdeQXVyMTMzNDE5NDM2._V1_.jpg", 
                "https://youtube.com/watch?v=pushpa"),

            createMovie("Tiger 3", "2023-11-12", 155, Genre.ACTION, Language.HINDI, 7.5,
                "Tiger and Zoya face their biggest threat yet.",
                "Maneesh Sharma", "Salman Khan, Katrina Kaif, Emraan Hashmi",
                "https://m.media-amazon.com/images/M/MV5BYmRiNjg2OTgtYzNkZC00MGNkLWFjNzAtNThkYTdhNzdkNGNjXkEyXkFqcGdeQXVyMTUzNTgzNzM0._V1_FMjpg_UX1000_.jpg", 
                "https://youtube.com/watch?v=tiger3"),

            createMovie("Dunki", "2023-12-21", 161, Genre.DRAMA, Language.HINDI, 7.6,
                "A story of illegal immigration and the struggles of those seeking a better life.",
                "Rajkumar Hirani", "Shah Rukh Khan, Taapsee Pannu, Vicky Kaushal",
                "https://m.media-amazon.com/images/M/MV5BN2QxY2U5M2MtNTJhZi00ZmQ0LTg1M2YtNTdlOTdmNTkyY2NlXkEyXkFqcGdeQXVyMTUzNTgzNzM0._V1_.jpg", 
                "https://youtube.com/watch?v=dunki"),

            createMovie("12th Fail", "2023-10-27", 147, Genre.DRAMA, Language.HINDI, 9.1,
                "Based on the true story of an IPS officer's journey.",
                "Vidhu Vinod Chopra", "Vikrant Massey, Medha Shankar",
                "https://m.media-amazon.com/images/M/MV5BYjEyOTdhNjYtMDg5Yy00ZTNlLTk3YjAtNTE5NjNjZDk0MmI3XkEyXkFqcGdeQXVyMTUzNTgzNzM0._V1_.jpg", 
                "https://youtube.com/watch?v=12thfail"),

            createMovie("Animal", "2023-12-01", 201, Genre.THRILLER, Language.HINDI, 7.7,
                "A son's journey to avenge the attempted assassination of his father.",
                "Sandeep Reddy Vanga", "Ranbir Kapoor, Anil Kapoor, Bobby Deol",
                "https://m.media-amazon.com/images/M/MV5BODU4NmQ2MDktZDJkNy00ZjJlLWJkN2EtNjg0Y2VkNWE4MDk3XkEyXkFqcGdeQXVyMTUzNTgzNzM0._V1_.jpg", 
                "https://youtube.com/watch?v=animal"),

            createMovie("Gadar 2", "2023-08-11", 170, Genre.ROMANTIC, Language.HINDI, 7.3,
                "Tara Singh goes to Pakistan to rescue his son.",
                "Anil Sharma", "Sunny Deol, Ameesha Patel, Utkarsh Sharma",
                "https://m.media-amazon.com/images/M/MV5BOTMyMTU3NzcxMl5BMl5BanBnXkFtZTgwODQ3ODYwMzI@._V1_FMjpg_UX1000_.jpg", 
                "https://youtube.com/watch?v=gadar2"),

            createMovie("OMG 2", "2023-08-11", 155, Genre.SOCIAL, Language.HINDI, 8.0,
                "A satire on sex education and the Indian education system.",
                "Amit Rai", "Akshay Kumar, Pankaj Tripathi, Yami Gautam",
                "https://m.media-amazon.com/images/M/MV5BZDNjOGNhYTQtYmY0OS00YTI3LWFjMWQtZmJjMWJkNWZmNzJiXkEyXkFqcGdeQXVyMTUyNjIwMDEw._V1_.jpg", 
                "https://youtube.com/watch?v=omg2"),

            createMovie("Oppenheimer", "2023-07-21", 180, Genre.HISTORICAL, Language.ENGLISH, 8.6,
                "The story of J. Robert Oppenheimer's role in developing the atomic bomb.",
                "Christopher Nolan", "Cillian Murphy, Emily Blunt, Robert Downey Jr.",
                "https://m.media-amazon.com/images/M/MV5BMDBmYTZjNjUtN2M1MS00MTQ2LTk2ODgtNzc2M2QyZGE5NTVjXkEyXkFqcGdeQXVyNzAwMjU2MTY@._V1_FMjpg_UX1000_.jpg", 
                "https://youtube.com/watch?v=oppenheimer"),

            createMovie("Barbie", "2023-07-21", 114, Genre.COMEDY, Language.ENGLISH, 7.4,
                "Barbie and Ken are having the time of their lives in Barbie Land.",
                "Greta Gerwig", "Margot Robbie, Ryan Gosling, Will Ferrell",
                "https://m.media-amazon.com/images/M/MV5BNjU3N2QxNzYtMjk1NC00MTc4LTk1NTQtMmUxNTljM2I0NDA5XkEyXkFqcGdeQXVyODE5NzE3OTE@._V1_FMjpg_UX1000_.jpg", 
                "https://youtube.com/watch?v=barbie"),

            createMovie("Kantara", "2022-09-30", 148, Genre.THRILLER, Language.KANNADA, 8.4,
                "A local Kambala champion faces a dispute with a forest officer.",
                "Rishab Shetty", "Rishab Shetty, Sapthami Gowda, Kishore",
                "https://m.media-amazon.com/images/M/MV5BYjEyNzY4NTAtMDNhNi00OGVlLWJlZTUtNDA2MWI5MTU5ODg0XkEyXkFqcGdeQXVyMTEzNzg0Mjkx._V1_.jpg", 
                "https://youtube.com/watch?v=kantara"),

            createMovie("Ponniyin Selvan 1", "2022-09-30", 167, Genre.HISTORICAL, Language.TAMIL, 7.8,
                "The early life of Chola prince Arulmozhi Varman.",
                "Mani Ratnam", "Vikram, Aishwarya Rai, Jayam Ravi",
                "https://m.media-amazon.com/images/M/MV5BYjFkMWMxNjAtZGMxNi00ZTQ0LWI0ODktNTg4ZTY3NjY3NmFlXkEyXkFqcGdeQXVyMTE0MzY0NjE1._V1_.jpg", 
                "https://youtube.com/watch?v=ps1"),

            createMovie("Brahmastra", "2022-09-09", 167, Genre.ANIMATION, Language.HINDI, 6.8,
                "A DJ discovers his strange connection to the elemental forces of nature.",
                "Ayan Mukerji", "Ranbir Kapoor, Alia Bhatt, Amitabh Bachchan",
                "https://m.media-amazon.com/images/M/MV5BYjg5NmIyY2ItOGFjMi00M2ZjLThkZWUtM2I2NWYzMDAxZmIwXkEyXkFqcGdeQXVyMTIyNzY0NTMx._V1_.jpg", 
                "https://youtube.com/watch?v=brahmastra")
        };

        for (Movie movie : movies) {
            movieRepository.save(movie);
        }
        System.out.println("✅ Initialized " + movies.length + " movies");
    }

    private void initializeTheaters() {
        String[][] theaterData = {
            // Mumbai
            {"PVR Phoenix Palladium", "Lower Parel, Mumbai", "Mumbai"},
            {"INOX Nariman Point", "Nariman Point, Mumbai", "Mumbai"},
            {"Cinepolis Andheri", "Andheri West, Mumbai", "Mumbai"},
            {"PVR ECX Chembur", "Chembur, Mumbai", "Mumbai"},
            {"INOX Megaplex Inorbit Mall", "Malad, Mumbai", "Mumbai"},

            // Delhi
            {"PVR Saket", "Select Citywalk Mall, Saket, New Delhi", "Delhi"},
            {"INOX Nehru Place", "Nehru Place, New Delhi", "Delhi"},
            {"Cinepolis DLF Place", "DLF Place Mall, Saket, New Delhi", "Delhi"},
            {"PVR Priya", "Vasant Vihar, New Delhi", "Delhi"},
            {"INOX Connaught Place", "Connaught Place, New Delhi", "Delhi"},

            // Bangalore
            {"PVR Forum Mall", "Koramangala, Bangalore", "Bangalore"},
            {"INOX Garuda Mall", "Magrath Road, Bangalore", "Bangalore"},
            {"Cinepolis Nexus Shantiniketan", "Whitefield, Bangalore", "Bangalore"},
            {"PVR Orion Mall", "Rajajinagar, Bangalore", "Bangalore"},
            {"INOX Lido Mall", "MG Road, Bangalore", "Bangalore"},

            // Chennai
            {"PVR Grand Galleria", "Pallavaram, Chennai", "Chennai"},
            {"INOX Escape", "Express Avenue, Chennai", "Chennai"},
            {"Cinepolis INORBIT Mall", "Malumichampatti, Chennai", "Chennai"},
            {"PVR Heritage RSL", "Anna Salai, Chennai", "Chennai"},
            {"INOX National", "Arcot Road, Chennai", "Chennai"},

            // Hyderabad
            {"PVR Next Galleria", "Panjagutta, Hyderabad", "Hyderabad"},
            {"INOX GSM Mall", "Masab Tank, Hyderabad", "Hyderabad"},
            {"Cinepolis Mantra Mall", "Attapur, Hyderabad", "Hyderabad"},
            {"PVR Irrum Manzil", "Somajiguda, Hyderabad", "Hyderabad"},
            {"INOX GVK One", "Banjara Hills, Hyderabad", "Hyderabad"},

            // Kolkata
            {"PVR Avani Riverside", "Howrah, Kolkata", "Kolkata"},
            {"INOX South City", "Prince Anwar Shah Road, Kolkata", "Kolkata"},
            {"Cinepolis Lake Mall", "Jessore Road, Kolkata", "Kolkata"},
            {"PVR Mani Square", "EM Bypass, Kolkata", "Kolkata"},
            {"INOX Forum", "Elgin Road, Kolkata", "Kolkata"},

            // Pune
            {"PVR Phoenix Marketcity", "Viman Nagar, Pune", "Pune"},
            {"INOX Bund Garden", "Bund Garden Road, Pune", "Pune"},
            {"Cinepolis Westend Mall", "Aundh, Pune", "Pune"},
            {"PVR Pavillion Mall", "Shivajinagar, Pune", "Pune"},
            {"INOX SGS Mall", "Camp, Pune", "Pune"}
        };

        for (String[] data : theaterData) {
            // Find or create city
            City city = cityRepository.findByName(data[2])
                    .orElseGet(() -> {
                        City newCity = City.builder()
                                .name(data[2])
                                .state("Maharashtra")
                                .country("India")
                                .build();
                        return cityRepository.save(newCity);
                    });
            
            Theater theater = Theater.builder()
                    .name(data[0])
                    .address(data[1])
                    .city(city)
                    .build();
            theaterRepository.save(theater);
        }
        System.out.println("✅ Initialized " + theaterData.length + " theaters");
    }

    /**
     * Ensures admin users always exist (creates or resets password).
     * Called every startup to guarantee admin login always works.
     */
    private void ensureAdminUsersExist() {
        String mainAdminEmail = environment.getProperty("MAIN_ADMIN_EMAIL", "admin@gmail.com");
        String mainAdminPassword = environment.getProperty("MAIN_ADMIN_PASSWORD", "password123");
        String theatreAdminEmail = environment.getProperty("THEATRE_ADMIN_EMAIL", "tadmin@gmail.com");
        String theatreAdminPassword = environment.getProperty("THEATRE_ADMIN_PASSWORD", "password123");

        // Main Admin - create or reset
        User existingAdmin = userRepository.findByEmailId(mainAdminEmail);
        if (existingAdmin == null) {
            User mainAdmin = User.builder()
                    .name("Main Admin")
                    .emailId(mainAdminEmail)
                    .password(passwordEncoder.encode(mainAdminPassword))
                    .mobileNo("9999999999")
                    .age(30)
                    .gender(Gender.MALE)
                    .role(UserRole.ADMIN)
                    .isActive(true)
                    .walletBalance(0.0)
                    .build();
            userRepository.save(mainAdmin);
            System.out.println("✅ Created Main Admin: " + mainAdminEmail);
        } else {
            // Reset password and ensure role is ADMIN
            existingAdmin.setPassword(passwordEncoder.encode(mainAdminPassword));
            existingAdmin.setRole(UserRole.ADMIN);
            existingAdmin.setIsActive(true);
            userRepository.save(existingAdmin);
            System.out.println("✅ Reset Main Admin password: " + mainAdminEmail);
        }

        // Theatre Admin - create or reset
        User existingTAdmin = userRepository.findByEmailId(theatreAdminEmail);
        if (existingTAdmin == null) {
            User theatreAdmin = User.builder()
                    .name("Theatre Admin")
                    .emailId(theatreAdminEmail)
                    .password(passwordEncoder.encode(theatreAdminPassword))
                    .mobileNo("9999999998")
                    .age(28)
                    .gender(Gender.MALE)
                    .role(UserRole.THEATER_OWNER)
                    .isActive(true)
                    .walletBalance(0.0)
                    .build();
            userRepository.save(theatreAdmin);
            System.out.println("✅ Created Theatre Admin: " + theatreAdminEmail);
        } else {
            existingTAdmin.setPassword(passwordEncoder.encode(theatreAdminPassword));
            existingTAdmin.setRole(UserRole.THEATER_OWNER);
            existingTAdmin.setIsActive(true);
            userRepository.save(existingTAdmin);
            System.out.println("✅ Reset Theatre Admin password: " + theatreAdminEmail);
        }

        // Assign theatre admin to first theatre if not assigned
        List<Theater> theaters = theaterRepository.findAll();
        User tAdmin = userRepository.findByEmailId(theatreAdminEmail);
        if (tAdmin != null && !theaters.isEmpty()) {
            boolean hasTheatre = theaters.stream().anyMatch(t -> t.getAdmin() != null && t.getAdmin().getId().equals(tAdmin.getId()));
            if (!hasTheatre) {
                Theater firstTheatre = theaters.get(0);
                firstTheatre.setAdmin(tAdmin);
                theaterRepository.save(firstTheatre);
                System.out.println("✅ Assigned Theatre Admin to: " + firstTheatre.getName());
            }
        }
    }

    /**
     * Refreshes shows if all existing shows have past dates.
     * Updates old show dates to current/future dates instead of deleting
     * (to preserve FK references from tickets/payments).
     */
    private void refreshStaleShows() {
        LocalDate today = LocalDate.now();
        List<Show> allShows = showRepository.findAll();
        
        long futureShows = allShows.stream()
                .filter(s -> s.getDate() != null && s.getDate().toLocalDate().isAfter(today.minusDays(1)))
                .count();
        
        if (futureShows == 0 && !allShows.isEmpty()) {
            System.out.println("⚠️ All shows are in the past. Updating dates to current week...");
            int dayOffset = 0;
            for (int i = 0; i < allShows.size(); i++) {
                Show show = allShows.get(i);
                // Spread shows across next 7 days
                dayOffset = i % 7;
                show.setDate(Date.valueOf(today.plusDays(dayOffset)));
                showRepository.save(show);
            }
            System.out.println("✅ Updated " + allShows.size() + " shows to current dates");
        }
    }

    private void initializeUsers() {
        // Read credentials from .env (via Spring Environment)
        String mainAdminEmail = environment.getProperty("MAIN_ADMIN_EMAIL", "admin@gmail.com");
        String mainAdminPassword = environment.getProperty("MAIN_ADMIN_PASSWORD", "password123");
        String theatreAdminEmail = environment.getProperty("THEATRE_ADMIN_EMAIL", "tadmin@gmail.com");
        String theatreAdminPassword = environment.getProperty("THEATRE_ADMIN_PASSWORD", "password123");

        // Regular test user (admins already created by ensureAdminUsersExist)
        User testUser = User.builder()
                .name("Test User")
                .emailId("test@example.com")
                .password(passwordEncoder.encode("test123"))
                .mobileNo("8888888888")
                .age(25)
                .gender(Gender.MALE)
                .role(UserRole.USER)
                .isActive(true)
                .walletBalance(500.0)
                .build();

        // Only save test user if not exists
        if (userRepository.findByEmailId("test@example.com") == null) {
            userRepository.save(testUser);
        }

        log.info("✅ Initialized test user");
    }

    private Movie createMovie(String name, String releaseDate, int duration, Genre genre,
                             Language language, double rating, String description,
                             String director, String cast, String posterUrl, String trailerUrl) {
        return Movie.builder()
                .movieName(name)
                .releaseDate(Date.valueOf(LocalDate.parse(releaseDate)))
                .duration(duration)
                .genre(genre)
                .language(language)
                .rating(rating)
                .description(description)
                .director(director)
                .cast(cast)
                .posterUrl(posterUrl)
                .trailerUrl(trailerUrl)
                .nowShowing(true)
                .build();
    }

    // initializeTheaterSeats() REMOVED - Admin manages theater seats dynamically via admin panel

    private void initializeShows() {
        List<Movie> movies = movieRepository.findAll();
        List<Theater> theaters = theaterRepository.findAll();
        Random random = new Random(42); // Fixed seed for consistency
        
        int totalShows = 0;
        LocalDate today = LocalDate.now();
        
        // Show timings: 4 shows per day
        Time[] showTimes = {
            Time.valueOf(LocalTime.of(10, 0)),  // 10:00 AM
            Time.valueOf(LocalTime.of(14, 0)),  // 2:00 PM
            Time.valueOf(LocalTime.of(18, 0)),  // 6:00 PM
            Time.valueOf(LocalTime.of(21, 30))  // 9:30 PM
        };
        
        // Create shows for next 7 days
        for (int day = 0; day < 7; day++) {
            LocalDate showDate = today.plusDays(day);
            
            // Each movie gets shows in 3-5 random theaters per day
            for (Movie movie : movies) {
                int theaterCount = 3 + random.nextInt(3); // 3-5 theaters
                List<Theater> selectedTheaters = getRandomTheaters(theaters, theaterCount, random);
                
                for (Theater theater : selectedTheaters) {
                    // Each theater shows the movie 2-4 times per day
                    int timeslotCount = 2 + random.nextInt(3); // 2-4 timeslots
                    
                    for (int t = 0; t < timeslotCount; t++) {
                        Show show = Show.builder()
                                .date(Date.valueOf(showDate))
                                .time(showTimes[t])
                                .movie(movie)
                                .theater(theater)
                                .build();
                        showRepository.save(show);
                        totalShows++;
                    }
                }
            }
        }
        
        System.out.println("✅ Initialized " + totalShows + " shows for next 7 days");
        System.out.println("   Each movie has shows in 3-5 theaters per day with 2-4 timeslots each");
    }

    // initializeShowSeats() REMOVED - Show seats are auto-generated from theater seats when admin adds a show,
    // or can be manually managed via /admin/shows/{showId}/seats endpoints

    private List<Theater> getRandomTheaters(List<Theater> allTheaters, int count, Random random) {
        List<Theater> shuffled = new ArrayList<>(allTheaters);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Theater temp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, temp);
        }
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    /**
     * Create sample bookings and wallet transactions for testing
     * This populates booking history and transaction history with real data from DB
     */
    private void initializeSampleBookingsAndTransactions() {
        try {
            // Get the test user (should exist from initializeUsers)
            User testUser = userRepository.findByEmailId("test@example.com");
            if (testUser == null) {
                log.error("Test user not found");
                return;
            }
            
            // Ensure user has wallet balance
            if (testUser.getWalletBalance() == null || testUser.getWalletBalance() < 1000.00) {
                testUser.setWalletBalance(1000.00);
                testUser = userRepository.save(testUser);
                log.info("✅ Updated test user wallet balance to ₹1000");
            }
            
            // Get some shows from database
            List<Show> shows = showRepository.findAll();
            if (shows.isEmpty()) {
                log.warn("⚠️ No shows found. Cannot create sample bookings.");
                return;
            }
            
            // Create 3-5 sample bookings
            int bookingCount = 0;
            LocalDateTime now = LocalDateTime.now();
            
            for (int i = 0; i < Math.min(5, shows.size()); i++) {
                Show show = shows.get(i);
                
                // Create ticket
                Ticket ticket = Ticket.builder()
                    .user(testUser)
                    .show(show)
                    .bookedSeats("A1,A2,A3") // Sample seats
                    .totalTicketsPrice(BigDecimal.valueOf(450.00 + (i * 50)).intValue()) // Varying prices
                    .bookedAt(now.minusDays(i + 1)) // Bookings from different days
                    .qrCodeData("QR_" + System.currentTimeMillis() + "_" + i)
                    .build();
                
                ticketRepository.save(ticket);
                bookingCount++;
                
                // Create corresponding wallet transaction (DEBIT for payment)
                Double balanceBefore = testUser.getWalletBalance();
                Double balanceAfter = balanceBefore - ticket.getTotalTicketsPrice().doubleValue();
                
                WalletTransaction debitTxn = WalletTransaction.builder()
                    .user(testUser)
                    .amount(ticket.getTotalTicketsPrice().doubleValue())
                    .transactionType(TransactionType.DEBIT)
                    .transactionReference("BOOKING_PAYMENT_" + ticket.getId())
                    .description("Payment for " + show.getMovie().getMovieName() + " at " + show.getTheater().getName())
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .createdAt(ticket.getBookedAt())
                    .build();
                
                walletTransactionRepository.save(debitTxn);
                
                // Update user balance
                testUser.setWalletBalance(balanceAfter);
                userRepository.save(testUser);
                
                log.info("📋 Created booking #{}: {} at {} (₹{})", 
                    i + 1, show.getMovie().getMovieName(), show.getTheater().getName(), 
                    ticket.getTotalTicketsPrice());
            }
            
            // Create some additional wallet transactions (credits from refunds, add money, etc.)
            Double currentBalance = testUser.getWalletBalance();
            Double refundAmount = 250.00;
            Double balanceAfterRefund = currentBalance + refundAmount;
            
            WalletTransaction creditTxn1 = WalletTransaction.builder()
                .user(testUser)
                .amount(refundAmount)
                .transactionType(TransactionType.CREDIT)
                .transactionReference("REFUND_" + System.currentTimeMillis())
                .description("Refund for cancelled booking")
                .balanceBefore(currentBalance)
                .balanceAfter(balanceAfterRefund)
                .createdAt(now.minusDays(2))
                .build();
            walletTransactionRepository.save(creditTxn1);
            
            testUser.setWalletBalance(balanceAfterRefund);
            userRepository.save(testUser);
            
            currentBalance = balanceAfterRefund;
            Double addMoneyAmount = 500.00;
            Double balanceAfterAddMoney = currentBalance + addMoneyAmount;
            
            WalletTransaction creditTxn2 = WalletTransaction.builder()
                .user(testUser)
                .amount(addMoneyAmount)
                .transactionType(TransactionType.CREDIT)
                .transactionReference("ADD_MONEY_" + System.currentTimeMillis())
                .description("Money added to wallet")
                .balanceBefore(currentBalance)
                .balanceAfter(balanceAfterAddMoney)
                .createdAt(now.minusDays(5))
                .build();
            walletTransactionRepository.save(creditTxn2);
            
            testUser.setWalletBalance(balanceAfterAddMoney);
            userRepository.save(testUser);
            
            long totalTransactions = walletTransactionRepository.countByUser(testUser);
            
            log.info("✅ Sample data created:");
            log.info("   📋 Bookings: {}", bookingCount);
            log.info("   💳 Wallet Transactions: {}", totalTransactions);
            log.info("   👤 User: {} (ID: {})", testUser.getName(), testUser.getId());
            
        } catch (Exception e) {
            log.error("❌ Error creating sample bookings: {}", e.getMessage(), e);
        }
    }

    private void initializeFoodItems() {
        try {
            List<Theater> theaters = theaterRepository.findAll();
            if (theaters.isEmpty()) {
                log.warn("No theaters found — skipping food initialization");
                return;
            }

            // Menu items to add for every theater
            Object[][] menuData = {
                // {name, description, price, category, imageUrl, isVeg}
                {"Large Popcorn", "Freshly popped buttery popcorn (large tub)", 350, "POPCORN", "🍿", true},
                {"Regular Popcorn", "Classic salted popcorn (regular tub)", 200, "POPCORN", "🍿", true},
                {"Caramel Popcorn", "Sweet caramel coated popcorn", 300, "POPCORN", "🍿", true},
                {"Cheese Popcorn", "Cheddar cheese flavored popcorn", 280, "POPCORN", "🧀", true},
                {"Pepsi Large", "Chilled Pepsi 750ml", 200, "BEVERAGE", "🥤", true},
                {"Pepsi Regular", "Chilled Pepsi 400ml", 120, "BEVERAGE", "🥤", true},
                {"Masala Lemonade", "Tangy masala lemonade", 150, "BEVERAGE", "🍋", true},
                {"Cold Coffee", "Creamy iced coffee", 180, "BEVERAGE", "☕", true},
                {"Mineral Water", "Packaged drinking water 500ml", 50, "BEVERAGE", "💧", true},
                {"Nachos with Salsa", "Crispy nachos with chunky salsa dip", 250, "SNACK", "🌮", true},
                {"Nachos with Cheese", "Nachos loaded with cheese sauce", 280, "SNACK", "🧀", true},
                {"French Fries", "Golden crispy french fries", 180, "SNACK", "🍟", true},
                {"Samosa (2 pcs)", "Classic punjabi samosa", 100, "SNACK", "🥟", true},
                {"Veg Burger", "Crispy veg patty burger with lettuce", 200, "SNACK", "🍔", true},
                {"Chicken Burger", "Juicy chicken patty burger", 250, "SNACK", "🍔", false},
                {"Chicken Wings (4 pcs)", "Spicy fried chicken wings", 300, "SNACK", "🍗", false},
                {"Combo 1: Popcorn + Pepsi", "Large popcorn + Large Pepsi", 450, "COMBO", "🎬", true},
                {"Combo 2: Nachos + Pepsi", "Nachos with cheese + Regular Pepsi", 350, "COMBO", "🎬", true},
                {"Combo 3: Burger + Fries + Pepsi", "Veg burger + Fries + Regular Pepsi", 400, "COMBO", "🎬", true},
                {"Brownie Sundae", "Warm brownie with vanilla ice cream", 220, "DESSERT", "🍫", true},
                {"Ice Cream Cup", "Vanilla or chocolate ice cream cup", 120, "DESSERT", "🍨", true},
            };

            int totalAdded = 0;
            for (Theater theater : theaters) {
                for (Object[] item : menuData) {
                    FoodItem foodItem = FoodItem.builder()
                            .itemName((String) item[0])
                            .description((String) item[1])
                            .price((Integer) item[2])
                            .category((String) item[3])
                            .imageUrl((String) item[4])
                            .isVegetarian((Boolean) item[5])
                            .isAvailable(true)
                            .theater(theater)
                            .build();
                    try {
                        foodItemRepository.save(foodItem);
                        totalAdded++;
                    } catch (Exception e) {
                        // Duplicate — skip silently
                    }
                }
            }
            log.info("✅ Food items initialized: {} items across {} theaters", totalAdded, theaters.size());
        } catch (Exception e) {
            log.error("❌ Error initializing food items: {}", e.getMessage(), e);
        }
    }
}
