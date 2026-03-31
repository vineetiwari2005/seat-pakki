// Indian Cities Mock Data
export const cities = [
  { id: 1, name: 'Mumbai', state: 'Maharashtra' },
  { id: 2, name: 'Delhi', state: 'Delhi' },
  { id: 3, name: 'Bangalore', state: 'Karnataka' },
  { id: 4, name: 'Chennai', state: 'Tamil Nadu' },
  { id: 5, name: 'Kolkata', state: 'West Bengal' },
  { id: 6, name: 'Hyderabad', state: 'Telangana' },
  { id: 7, name: 'Pune', state: 'Maharashtra' }
];

// Indian Theaters Mock Data (5-10 per city)
export const theaters = [
  // Mumbai
  { id: 1, name: 'PVR Phoenix Palladium', address: 'Lower Parel, Mumbai', city: 'Mumbai', screens: 8 },
  { id: 2, name: 'INOX Nariman Point', address: 'Nariman Point, Mumbai', city: 'Mumbai', screens: 6 },
  { id: 3, name: 'Cinepolis Andheri', address: 'Andheri West, Mumbai', city: 'Mumbai', screens: 5 },
  { id: 4, name: 'Carnival Imax Wadala', address: 'Wadala, Mumbai', city: 'Mumbai', screens: 7 },
  { id: 5, name: 'PVR ICON Versova', address: 'Versova, Mumbai', city: 'Mumbai', screens: 4 },
  
  // Delhi
  { id: 6, name: 'PVR Select Citywalk', address: 'Saket, Delhi', city: 'Delhi', screens: 11 },
  { id: 7, name: 'INOX Nehru Place', address: 'Nehru Place, Delhi', city: 'Delhi', screens: 6 },
  { id: 8, name: 'Cinepolis DLF Place', address: 'Saket, Delhi', city: 'Delhi', screens: 5 },
  { id: 9, name: 'PVR Priya Vasant Vihar', address: 'Vasant Vihar, Delhi', city: 'Delhi', screens: 4 },
  { id: 10, name: 'Carnival Cinemas Rohini', address: 'Rohini, Delhi', city: 'Delhi', screens: 8 },
  
  // Bangalore
  { id: 11, name: 'PVR Forum Mall', address: 'Koramangala, Bangalore', city: 'Bangalore', screens: 7 },
  { id: 12, name: 'INOX Garuda Mall', address: 'Magrath Road, Bangalore', city: 'Bangalore', screens: 5 },
  { id: 13, name: 'Cinepolis Royal Meenakshi', address: 'Bannerghatta Road, Bangalore', city: 'Bangalore', screens: 9 },
  { id: 14, name: 'PVR Orion Mall', address: 'Rajajinagar, Bangalore', city: 'Bangalore', screens: 6 },
  { id: 15, name: 'INOX Mantri Square', address: 'Malleswaram, Bangalore', city: 'Bangalore', screens: 4 },
  
  // Chennai
  { id: 16, name: 'PVR Grand Galada', address: 'Pallavaram, Chennai', city: 'Chennai', screens: 5 },
  { id: 17, name: 'INOX Escape', address: 'OMR, Chennai', city: 'Chennai', screens: 6 },
  { id: 18, name: 'Sathyam Cinemas', address: 'Royapettah, Chennai', city: 'Chennai', screens: 5 },
  { id: 19, name: 'PVR VR Chennai', address: 'Anna Nagar, Chennai', city: 'Chennai', screens: 8 },
  { id: 20, name: 'Cinepolis Ampa Skywalk', address: 'Aminjikarai, Chennai', city: 'Chennai', screens: 4 },
  
  // Kolkata
  { id: 21, name: 'PVR South City', address: 'South City Mall, Kolkata', city: 'Kolkata', screens: 4 },
  { id: 22, name: 'INOX Quest', address: 'Park Circus, Kolkata', city: 'Kolkata', screens: 6 },
  { id: 23, name: 'Cinepolis Lake Mall', address: 'Rash Behari, Kolkata', city: 'Kolkata', screens: 5 },
  { id: 24, name: 'PVR Avani Riverside', address: 'Howrah, Kolkata', city: 'Kolkata', screens: 7 },
  { id: 25, name: 'INOX Forum', address: 'Elgin Road, Kolkata', city: 'Kolkata', screens: 3 },
  
  // Hyderabad
  { id: 26, name: 'PVR Next Galleria', address: 'Punjagutta, Hyderabad', city: 'Hyderabad', screens: 6 },
  { id: 27, name: 'INOX GVK One', address: 'Banjara Hills, Hyderabad', city: 'Hyderabad', screens: 5 },
  { id: 28, name: 'Prasads IMAX', address: 'Necklace Road, Hyderabad', city: 'Hyderabad', screens: 3 },
  { id: 29, name: 'AMB Cinemas', address: 'Gachibowli, Hyderabad', city: 'Hyderabad', screens: 7 },
  { id: 30, name: 'Asian Lal Bagh', address: 'Moosarambagh, Hyderabad', city: 'Hyderabad', screens: 4 },
  
  // Pune
  { id: 31, name: 'PVR Pavilion Mall', address: 'Shivajinagar, Pune', city: 'Pune', screens: 6 },
  { id: 32, name: 'INOX Amanora', address: 'Hadapsar, Pune', city: 'Pune', screens: 5 },
  { id: 33, name: 'Cinepolis Westend Mall', address: 'Aundh, Pune', city: 'Pune', screens: 7 },
  { id: 34, name: 'E-Square Multiplex', address: 'Pimpri, Pune', city: 'Pune', screens: 4 },
  { id: 35, name: 'PVR Market City', address: 'Vimannagar, Pune', city: 'Pune', screens: 5 }
];

// Indian Movies Mock Data (20 movies with realistic details)
export const movies = [
  {
    id: 1,
    name: 'Jawan',
    genre: ['ACTION', 'THRILLER'],
    language: 'HINDI',
    duration: 169,
    rating: 8.5,
    releaseDate: '2023-09-07',
    posterUrl: 'https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400',
    description: 'A high-octane action thriller with Shah Rukh Khan in a dual role',
    cast: ['Shah Rukh Khan', 'Nayanthara', 'Vijay Sethupathi'],
    director: 'Atlee',
    nowShowing: true
  },
  {
    id: 2,
    name: 'Pathaan',
    genre: ['ACTION', 'THRILLER'],
    language: 'HINDI',
    duration: 146,
    rating: 8.2,
    releaseDate: '2023-01-25',
    posterUrl: 'https://images.unsplash.com/photo-1485846234645-a62644f84728?w=400',
    description: 'An Indian spy must race against time to protect the nation',
    cast: ['Shah Rukh Khan', 'Deepika Padukone', 'John Abraham'],
    director: 'Siddharth Anand',
    nowShowing: true
  },
  {
    id: 3,
    name: 'RRR',
    genre: ['ACTION', 'DRAMA'],
    language: 'TELUGU',
    duration: 182,
    rating: 9.1,
    releaseDate: '2022-03-25',
    posterUrl: 'https://images.unsplash.com/photo-1594908900066-3f47337549d8?w=400',
    description: 'A fictional story about two legendary revolutionaries',
    cast: ['N.T. Rama Rao Jr.', 'Ram Charan', 'Alia Bhatt'],
    director: 'S.S. Rajamouli',
    nowShowing: true
  },
  {
    id: 4,
    name: 'KGF Chapter 2',
    genre: ['ACTION', 'DRAMA'],
    language: 'KANNADA',
    duration: 168,
    rating: 8.4,
    releaseDate: '2022-04-14',
    posterUrl: 'https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=400',
    description: 'Rocky rises to unprecedented power in the Kolar Gold Fields',
    cast: ['Yash', 'Sanjay Dutt', 'Raveena Tandon'],
    director: 'Prashanth Neel',
    nowShowing: true
  },
  {
    id: 5,
    name: 'Brahmastra',
    genre: ['FANTASY', 'ADVENTURE'],
    language: 'HINDI',
    duration: 167,
    rating: 7.8,
    releaseDate: '2022-09-09',
    posterUrl: 'https://images.unsplash.com/photo-1533613220915-609f661a6fe1?w=400',
    description: 'A young man discovers his connection to a secret society of guardians',
    cast: ['Ranbir Kapoor', 'Alia Bhatt', 'Amitabh Bachchan'],
    director: 'Ayan Mukerji',
    nowShowing: true
  },
  {
    id: 6,
    name: 'Vikram',
    genre: ['ACTION', 'THRILLER'],
    language: 'TAMIL',
    duration: 174,
    rating: 8.7,
    releaseDate: '2022-06-03',
    posterUrl: 'https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=400',
    description: 'Elite agents hunt down a masked vigilante',
    cast: ['Kamal Haasan', 'Vijay Sethupathi', 'Fahadh Faasil'],
    director: 'Lokesh Kanagaraj',
    nowShowing: true
  },
  {
    id: 7,
    name: 'Drishyam 2',
    genre: ['THRILLER', 'DRAMA'],
    language: 'HINDI',
    duration: 143,
    rating: 8.3,
    releaseDate: '2022-11-18',
    posterUrl: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=400',
    description: 'A gripping sequel to the mystery thriller',
    cast: ['Ajay Devgn', 'Tabu', 'Shriya Saran'],
    director: 'Abhishek Pathak',
    nowShowing: true
  },
  {
    id: 8,
    name: 'Ponniyin Selvan',
    genre: ['DRAMA', 'HISTORICAL'],
    language: 'TAMIL',
    duration: 167,
    rating: 8.0,
    releaseDate: '2022-09-30',
    posterUrl: 'https://images.unsplash.com/photo-1518676590629-3dcbd9c5a5c9?w=400',
    description: 'Epic historical drama set in the Chola dynasty',
    cast: ['Vikram', 'Aishwarya Rai', 'Karthi'],
    director: 'Mani Ratnam',
    nowShowing: true
  },
  {
    id: 9,
    name: 'Kantara',
    genre: ['THRILLER', 'DRAMA'],
    language: 'KANNADA',
    duration: 148,
    rating: 9.0,
    releaseDate: '2022-09-30',
    posterUrl: 'https://images.unsplash.com/photo-1574267432644-f610fa1d8b93?w=400',
    description: 'A mythical thriller set in a small coastal village',
    cast: ['Rishab Shetty', 'Sapthami Gowda'],
    director: 'Rishab Shetty',
    nowShowing: true
  },
  {
    id: 10,
    name: 'Animal',
    genre: ['ACTION', 'DRAMA'],
    language: 'HINDI',
    duration: 201,
    rating: 7.9,
    releaseDate: '2023-12-01',
    posterUrl: 'https://images.unsplash.com/photo-1571847140471-1d7766e825ea?w=400',
    description: 'A son seeks revenge for an assassination attempt on his father',
    cast: ['Ranbir Kapoor', 'Rashmika Mandanna', 'Anil Kapoor'],
    director: 'Sandeep Reddy Vanga',
    nowShowing: true
  },
  {
    id: 11,
    name: 'Dunki',
    genre: ['COMEDY', 'DRAMA'],
    language: 'HINDI',
    duration: 161,
    rating: 8.1,
    releaseDate: '2023-12-21',
    posterUrl: 'https://images.unsplash.com/photo-1598899134739-24c46f58b8c0?w=400',
    description: 'A comedy-drama about illegal immigration',
    cast: ['Shah Rukh Khan', 'Taapsee Pannu', 'Vicky Kaushal'],
    director: 'Rajkumar Hirani',
    nowShowing: true
  },
  {
    id: 12,
    name: 'Salaar',
    genre: ['ACTION', 'THRILLER'],
    language: 'KANNADA',
    duration: 175,
    rating: 8.6,
    releaseDate: '2023-12-22',
    posterUrl: 'https://images.unsplash.com/photo-1542204165-65bf26472b9b?w=400',
    description: 'A violent action thriller set in a dystopian world',
    cast: ['Prabhas', 'Prithviraj Sukumaran', 'Shruti Haasan'],
    director: 'Prashanth Neel',
    nowShowing: true
  },
  {
    id: 13,
    name: 'Tiger 3',
    genre: ['ACTION', 'THRILLER'],
    language: 'HINDI',
    duration: 155,
    rating: 7.5,
    releaseDate: '2023-11-12',
    posterUrl: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=400',
    description: 'Tiger faces a new international threat',
    cast: ['Salman Khan', 'Katrina Kaif', 'Emraan Hashmi'],
    director: 'Maneesh Sharma',
    nowShowing: true
  },
  {
    id: 14,
    name: 'Varisu',
    genre: ['ACTION', 'DRAMA'],
    language: 'TAMIL',
    duration: 169,
    rating: 7.2,
    releaseDate: '2023-01-12',
    posterUrl: 'https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=400',
    description: 'Family drama with corporate warfare',
    cast: ['Vijay', 'Rashmika Mandanna', 'Prakash Raj'],
    director: 'Vamshi Paidipally',
    nowShowing: true
  },
  {
    id: 15,
    name: 'Bholaa',
    genre: ['ACTION', 'THRILLER'],
    language: 'HINDI',
    duration: 144,
    rating: 7.4,
    releaseDate: '2023-03-30',
    posterUrl: 'https://images.unsplash.com/photo-1616530940355-351fabd9524b?w=400',
    description: 'An ex-convict fights to save trapped cops',
    cast: ['Ajay Devgn', 'Tabu', 'Deepak Dobriyal'],
    director: 'Ajay Devgn',
    nowShowing: true
  },
  {
    id: 16,
    name: 'Leo',
    genre: ['ACTION', 'THRILLER'],
    language: 'TAMIL',
    duration: 164,
    rating: 8.2,
    releaseDate: '2023-10-19',
    posterUrl: 'https://images.unsplash.com/photo-1574267432644-fd686cf0b1b6?w=400',
    description: "A cafe owner's dark past catches up with him",
    cast: ['Vijay', 'Trisha', 'Sanjay Dutt'],
    director: 'Lokesh Kanagaraj',
    nowShowing: true
  },
  {
    id: 17,
    name: 'Jailer',
    genre: ['ACTION', 'COMEDY'],
    language: 'TAMIL',
    duration: 168,
    rating: 8.4,
    releaseDate: '2023-08-10',
    posterUrl: 'https://images.unsplash.com/photo-1478720568477-152d9b164e26?w=400',
    description: 'A retired jailer hunts down criminals',
    cast: ['Rajinikanth', 'Mohanlal', 'Jackie Shroff'],
    director: 'Nelson Dilipkumar',
    nowShowing: true
  },
  {
    id: 18,
    name: '12th Fail',
    genre: ['DRAMA', 'BIOGRAPHY'],
    language: 'HINDI',
    duration: 147,
    rating: 9.2,
    releaseDate: '2023-10-27',
    posterUrl: 'https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=400',
    description: 'Inspiring story of UPSC aspirant',
    cast: ['Vikrant Massey', 'Medha Shankr'],
    director: 'Vidhu Vinod Chopra',
    nowShowing: true
  },
  {
    id: 19,
    name: 'Sam Bahadur',
    genre: ['BIOGRAPHY', 'DRAMA'],
    language: 'HINDI',
    duration: 150,
    rating: 8.0,
    releaseDate: '2023-12-01',
    posterUrl: 'https://images.unsplash.com/photo-1509347528160-9a9e33742cdb?w=400',
    description: 'Biopic of Field Marshal Sam Manekshaw',
    cast: ['Vicky Kaushal', 'Sanya Malhotra', 'Fatima Sana Shaikh'],
    director: 'Meghna Gulzar',
    nowShowing: true
  },
  {
    id: 20,
    name: 'Adipurush',
    genre: ['FANTASY', 'MYTHOLOGY'],
    language: 'HINDI',
    duration: 179,
    rating: 6.5,
    releaseDate: '2023-06-16',
    posterUrl: 'https://images.unsplash.com/photo-1518676590629-3dcbd9c5a5c9?w=400',
    description: 'Mythological saga based on the Ramayana',
    cast: ['Prabhas', 'Kriti Sanon', 'Saif Ali Khan'],
    director: 'Om Raut',
    nowShowing: true
  }
];

// Indian Showtimes (realistic timings)
export const showtimes = ['10:00', '13:30', '16:00', '19:00', '22:00'];

// Seat layout configuration with NOVELTY FEATURE: Couple Seats (2-seater couches)
export const seatLayout = {
  rows: ['A', 'B', 'C', 'D', 'E', 'F', 'G'],
  seatsPerRow: 12,
  seatTypes: {
    'A': 'COUPLE',      // 🆕 NOVELTY: Couple Seats (2-seater luxury couches)
    'B': 'COUPLE',      // 🆕 NOVELTY: Couple Seats (2-seater luxury couches)
    'C': 'PREMIUM',     // Premium recliners
    'D': 'PREMIUM',     // Premium recliners
    'E': 'GOLD',        // Gold seats
    'F': 'SILVER',      // Silver seats
    'G': 'SILVER'       // Silver seats
  },
  pricing: {
    'COUPLE': 600,      // 🆕 NOVELTY: Premium couple seating (price per person, must book 2)
    'PREMIUM': 350,
    'GOLD': 250,
    'SILVER': 150
  },
  // 🆕 NOVELTY FEATURE: Couple seats are always booked in pairs
  coupleSeatPairs: [
    ['A1', 'A2'], ['A3', 'A4'], ['A5', 'A6'], ['A7', 'A8'], ['A9', 'A10'], ['A11', 'A12'],
    ['B1', 'B2'], ['B3', 'B4'], ['B5', 'B6'], ['B7', 'B8'], ['B9', 'B10'], ['B11', 'B12']
  ]
};

// Generate shows for all theaters and movies
export const generateShows = () => {
  const shows = [];
  let showId = 1;
  
  theaters.forEach(theater => {
    // Get movies for this city
    const cityMovies = movies.slice(0, 10); // First 10 movies in each theater
    
    cityMovies.forEach(movie => {
      // Generate shows for next 7 days
      const today = new Date();
      for (let day = 0; day < 7; day++) {
        const showDate = new Date(today);
        showDate.setDate(today.getDate() + day);
        
        // Random screen (1 to theater.screens)
        const screenNumber = Math.floor(Math.random() * theater.screens) + 1;
        
        // Generate 3-4 random showtimes per day
        const numShows = 3 + Math.floor(Math.random() * 2);
        const selectedTimes = showtimes.slice(0, numShows);
        
        selectedTimes.forEach(time => {
          shows.push({
            id: showId++,
            movieId: movie.id,
            movieName: movie.name,
            theaterId: theater.id,
            theaterName: theater.name,
            theaterAddress: theater.address,
            city: theater.city,
            screenNumber: screenNumber,
            showDate: showDate.toISOString().split('T')[0],
            showTime: time,
            language: movie.language,
            availableSeats: 50,
            totalSeats: 50,
            seatLayout: seatLayout
          });
        });
      }
    });
  });
  
  return shows;
};

// Generate seat data for a show
export const generateSeatsForShow = (showId) => {
  const seats = [];
  let seatId = 1;
  
  seatLayout.rows.forEach(row => {
    for (let num = 1; num <= seatLayout.seatsPerRow; num++) {
      const seatNumber = `${row}${num}`;
      const seatType = seatLayout.seatTypes[row];
      const price = seatLayout.pricing[seatType];
      
      // Randomly mark some seats as booked (20% chance)
      const isBooked = Math.random() < 0.2;
      
      seats.push({
        id: seatId++,
        showId: showId,
        seatNumber: seatNumber,
        seatType: seatType,
        price: price,
        isBooked: isBooked,
        isLocked: false
      });
    }
  });
  
  return seats;
};

// Admin mock data
export const adminStats = {
  totalBookings: 15847,
  totalRevenue: 4521000,
  totalUsers: 45632,
  activeMovies: 20,
  totalTheaters: 35,
  todayBookings: 342,
  todayRevenue: 95400,
  recentBookings: [
    { id: 1, user: 'Raj Kumar', movie: 'Jawan', theater: 'PVR Phoenix', amount: 1400, date: '2026-01-05' },
    { id: 2, user: 'Priya Sharma', movie: 'Pathaan', theater: 'INOX Nariman', amount: 1000, date: '2026-01-05' },
    { id: 3, user: 'Amit Patel', movie: 'RRR', theater: 'Cinepolis Andheri', amount: 1200, date: '2026-01-05' }
  ],
  popularMovies: [
    { name: 'Jawan', bookings: 2456 },
    { name: 'Pathaan', bookings: 2134 },
    { name: 'RRR', bookings: 1987 },
    { name: 'KGF Chapter 2', bookings: 1765 }
  ]
};

export default {
  cities,
  theaters,
  movies,
  showtimes,
  seatLayout,
  generateShows,
  generateSeatsForShow,
  adminStats
};
