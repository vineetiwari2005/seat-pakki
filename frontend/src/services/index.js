import api from './api';
import gameService from './gameService';

// Public Services (no auth required)
export const publicService = {
  getCities: async () => {
    const response = await api.get('/api/public/cities');
    return response.data;
  }
};

// Authentication Services
export const authService = {
  signup: async (userData) => {
    // Map frontend field names to backend expected names
    const backendData = {
      name: userData.name,
      email: userData.email,
      password: userData.password,
      mobileNo: userData.mobileNumber, // Frontend uses mobileNumber, backend expects mobileNo
      age: parseInt(userData.age),
      gender: userData.gender,
      address: userData.address || '',
      role: userData.role || 'USER',
      cityId: userData.cityId ? parseInt(userData.cityId) : null
    };
    const response = await api.post('/auth/signup', backendData);
    return response.data;
  },

  login: async (credentials) => {
    const response = await api.post('/auth/login', credentials);
    return response.data;
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  },

  getCurrentUser: () => {
    const userStr = localStorage.getItem('user');
    if (!userStr || userStr === 'undefined' || userStr === 'null') {
      return null;
    }
    try {
      return JSON.parse(userStr);
    } catch (error) {
      console.error('Error parsing user data from localStorage:', error);
      localStorage.removeItem('user');
      return null;
    }
  },

  isAuthenticated: () => {
    return !!localStorage.getItem('accessToken');
  },

  getUserRole: () => {
    const user = authService.getCurrentUser();
    return user?.role || 'USER';
  }
};

// Movie Services
export const movieService = {
  getNowShowing: async () => {
    const response = await api.get('/api/movies/now-showing');
    return response.data;
  },

  getMovieByName: async (movieName) => {
    const response = await api.get(`/movie/${encodeURIComponent(movieName)}`);
    return response.data;
  },

  searchMovies: async (keyword) => {
    const response = await api.get(`/api/movies/search?keyword=${keyword}`);
    return response.data;
  },

  filterMovies: async (filters) => {
    const params = new URLSearchParams();
    if (filters.genre) params.append('genre', filters.genre);
    if (filters.language) params.append('language', filters.language);
    if (filters.minRating) params.append('minRating', filters.minRating);
    
    const response = await api.get(`/api/movies/filter/advanced?${params.toString()}`);
    return response.data;
  },

  getMoviesByCity: async (city) => {
    const response = await api.get(`/api/movies/city/${city}`);
    return response.data;
  },

  addMovie: async (movieData) => {
    const response = await api.post('/movie/addNew', movieData);
    return response.data;
  }
};

export const recommendationService = {
  getPersonalizedRecommendations: async (userId, limit = 5) => {
    const params = new URLSearchParams();
    if (userId) params.append('userId', userId);
    if (limit) params.append('limit', limit);

    const query = params.toString();
    const response = await api.get(`/api/movies/recommendations${query ? `?${query}` : ''}`);
    return response.data;
  }
};

// Theater Services
export const theaterService = {
  getAllTheaters: async () => {
    const response = await api.get('/theater/get-all-theaters');
    return response.data;
  },

  getTheatersByCity: async (city) => {
    const response = await api.get(`/theater/get-theaters-by-city/${city}`);
    return response.data;
  },

  addTheater: async (theaterData) => {
    const response = await api.post('/theater/add-theater', theaterData);
    return response.data;
  },

  addTheaterSeat: async (seatData) => {
    const response = await api.post('/theater/add-theater-seat', seatData);
    return response.data;
  }
};

// Show Services
export const showService = {
  addShow: async (showData) => {
    const response = await api.post('/show/add-show', showData);
    return response.data;
  },

  getShowsByMovie: async (movieId) => {
    const response = await api.get(`/show/by-movie/${movieId}`);
    return response.data;
  },

  getShowsByTheater: async (theaterId) => {
    const response = await api.get(`/show/by-theater/${theaterId}`);
    return response.data;
  },

  getShowById: async (showId) => {
    const response = await api.get(`/show/${showId}`);
    return response.data;
  }
};

// Seat Lock Services
export const seatLockService = {
  lockSeats: async (lockData) => {
    const response = await api.post('/api/seat-locks/lock', lockData);
    return response.data;
  },

  releaseLocks: async (sessionId) => {
    const response = await api.post(`/api/seat-locks/release/${sessionId}`);
    return response.data;
  },

  confirmLocks: async (sessionId) => {
    const response = await api.post(`/api/seat-locks/confirm/${sessionId}`);
    return response.data;
  }
};

// Payment Services
export const paymentService = {
  initiatePayment: async (paymentData) => {
    const response = await api.post('/api/payment/initiate', paymentData);
    return response.data;
  },

  processPayment: async (transactionId, otpData = {}) => {
    const response = await api.post(`/api/payment/process/${transactionId}`, otpData);
    return response.data;
  },

  getPaymentStatus: async (transactionId) => {
    const response = await api.get(`/api/payment/status/${transactionId}`);
    return response.data;
  },

  getPaymentHistory: async (userId) => {
    const response = await api.get(`/api/payment/user/${userId}/history`);
    return response.data.data || response.data;
  }
};

export const otpService = {
  sendOtp: async (payload) => {
    try {
      const response = await api.post('/api/otp/send', payload);
      return response.data;
    } catch (error) {
      const message = error?.response?.data?.error || 'Failed to send OTP';
      throw new Error(message);
    }
  }
};

// Ticket/Booking Services
export const bookingService = {
  bookTicket: async (bookingData) => {
    const response = await api.post('/ticket/book-ticket', bookingData);
    return response.data;
  },

  getUserBookings: async (userId) => {
    // Use the better API endpoint that returns properly formatted data
    const response = await api.get(`/api/bookings/user/${userId}`);
    // Extract data from ApiResponse wrapper
    return response.data.data || response.data;
  },

  getUpcomingBookings: async (userId) => {
    const response = await api.get(`/api/bookings/user/${userId}/upcoming`);
    return response.data.data || response.data;
  },

  getPastBookings: async (userId) => {
    const response = await api.get(`/api/bookings/user/${userId}/past`);
    return response.data.data || response.data;
  },

  getRefundEstimate: async (ticketId) => {
    const response = await api.get(`/api/bookings/${ticketId}/refund-estimate`);
    return response.data.data || response.data;
  },

  cancelBooking: async (ticketId, userId) => {
    // Backend uses POST /api/bookings/{ticketId}/cancel with userId param
    const response = await api.post(`/api/bookings/${ticketId}/cancel?userId=${userId}`);
    return response.data.data || response.data;
  },

  getChangeDateOptions: async (ticketId, userId) => {
    const response = await api.get(`/api/bookings/${ticketId}/change-date-options?userId=${userId}`);
    return response.data.data || response.data;
  },

  changeBookingDate: async (ticketId, userId) => {
    const response = await api.post(`/api/bookings/${ticketId}/change-date?userId=${userId}`);
    return response.data.data || response.data;
  },

  getBookingDetails: async (ticketId) => {
    const response = await api.get(`/api/booking/${ticketId}`);
    return response.data;
  }
};

// Admin Services
export const adminService = {
  getDashboard: async () => {
    const response = await api.get('/admin/dashboard');
    return response.data;
  },

  getAllUsers: async () => {
    const response = await api.get('/admin/users');
    return response.data;
  },

  updateUserStatus: async (userId, isActive) => {
    const response = await api.put(`/admin/users/${userId}/status?isActive=${isActive}`);
    return response.data;
  },

  deleteUser: async (userId) => {
    const response = await api.delete(`/admin/users/${userId}`);
    return response.data;
  },

  adjustUserWallet: async (userId, amount, reason = 'Admin adjustment') => {
    const response = await api.put(`/admin/wallet/${userId}/adjust?amount=${amount}&reason=${encodeURIComponent(reason)}`);
    return response.data;
  },

  getAnalytics: async (period) => {
    const response = await api.get(`/admin/analytics?period=${period}`);
    return response.data;
  },

  // City Management
  getAllCities: async () => {
    const response = await api.get('/admin/cities');
    return response.data;
  },

  addCity: async (cityData) => {
    const response = await api.post('/admin/cities', cityData);
    return response.data;
  },

  updateCity: async (cityId, cityData) => {
    const response = await api.put(`/admin/cities/${cityId}`, cityData);
    return response.data;
  },

  deleteCity: async (cityId) => {
    const response = await api.delete(`/admin/cities/${cityId}`);
    return response.data;
  },

  // Movie Management
  getAllMovies: async () => {
    const response = await api.get('/admin/movies');
    return response.data;
  },

  addMovie: async (movieData) => {
    const response = await api.post('/admin/movies', movieData);
    return response.data;
  },

  updateMovie: async (movieId, movieData) => {
    const response = await api.put(`/admin/movies/${movieId}`, movieData);
    return response.data;
  },

  deleteMovie: async (movieId) => {
    const response = await api.delete(`/admin/movies/${movieId}`);
    return response.data;
  },

  uploadMoviePoster: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/admin/movies/upload-poster', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // Theater Management
  getAllTheaters: async () => {
    const response = await api.get('/admin/theaters');
    return response.data;
  },

  addTheater: async (theaterData) => {
    const response = await api.post('/admin/theaters', theaterData);
    return response.data;
  },

  updateTheater: async (theaterId, theaterData) => {
    const response = await api.put(`/admin/theaters/${theaterId}`, theaterData);
    return response.data;
  },

  deleteTheater: async (theaterId) => {
    const response = await api.delete(`/admin/theaters/${theaterId}`);
    return response.data;
  },

  addTheaterSeats: async (seatData) => {
    const response = await api.post('/admin/theaters/seats', seatData);
    return response.data;
  },

  // Show Management
  getAllShows: async () => {
    const response = await api.get('/admin/shows');
    return response.data;
  },

  addShow: async (showData) => {
    const response = await api.post('/admin/shows', showData);
    return response.data;
  },

  updateShow: async (showId, showData) => {
    const response = await api.put(`/admin/shows/${showId}`, showData);
    return response.data;
  },

  deleteShow: async (showId) => {
    const response = await api.delete(`/admin/shows/${showId}`);
    return response.data;
  },

  // =====================================================
  // THEATER SEAT MANAGEMENT
  // =====================================================

  getTheaterSeats: async (theaterId) => {
    const response = await api.get(`/admin/theaters/${theaterId}/seats`);
    return response.data;
  },

  addTheaterSeatsRow: async (theaterId, rowPrefix, seatType, count) => {
    const response = await api.post(`/admin/theaters/${theaterId}/seats/row`, {
      rowPrefix, seatType, count
    });
    return response.data;
  },

  deleteTheaterSeat: async (theaterId, seatId) => {
    const response = await api.delete(`/admin/theaters/${theaterId}/seats/${seatId}`);
    return response.data;
  },

  // =====================================================
  // SHOW SEAT MANAGEMENT
  // =====================================================

  getShowSeats: async (showId) => {
    const response = await api.get(`/admin/shows/${showId}/seats`);
    return response.data;
  },

  addShowSeat: async (showId, seatNo, seatType, price) => {
    const response = await api.post(`/admin/shows/${showId}/seats`, {
      seatNo, seatType, price
    });
    return response.data;
  },

  addShowSeatsRow: async (showId, rowPrefix, seatType, count, price) => {
    const response = await api.post(`/admin/shows/${showId}/seats/row`, {
      rowPrefix, seatType, count, price
    });
    return response.data;
  },

  generateShowSeats: async (showId) => {
    const response = await api.post(`/admin/shows/${showId}/seats/generate`);
    return response.data;
  },

  deleteShowSeat: async (showId, seatId) => {
    const response = await api.delete(`/admin/shows/${showId}/seats/${seatId}`);
    return response.data;
  },

  toggleShowSeatAvailability: async (showId, seatId) => {
    const response = await api.put(`/admin/shows/${showId}/seats/${seatId}/availability`);
    return response.data;
  },

  updateShowSeatPrice: async (showId, seatId, price) => {
    const response = await api.put(`/admin/shows/${showId}/seats/${seatId}/price`, { price });
    return response.data;
  },

  // =====================================================
  // ANALYTICS API CALLS
  // =====================================================

  getEnhancedDashboard: async () => {
    const response = await api.get('/admin/analytics/dashboard');
    return response.data;
  },

  getCityAnalytics: async () => {
    const response = await api.get('/admin/analytics/cities');
    return response.data;
  },

  getMovieAnalytics: async (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.genre) params.append('genre', filters.genre);
    if (filters.language) params.append('language', filters.language);
    if (filters.dateFrom) params.append('dateFrom', filters.dateFrom);
    if (filters.dateTo) params.append('dateTo', filters.dateTo);
    const response = await api.get(`/admin/analytics/movies?${params.toString()}`);
    return response.data;
  },

  compareMovies: async (movieIds) => {
    const params = movieIds.map(id => `movieIds=${id}`).join('&');
    const response = await api.get(`/admin/analytics/movies/compare?${params}`);
    return response.data;
  },

  getTheaterRankings: async (sortBy = 'revenue') => {
    const response = await api.get(`/admin/analytics/theaters/rankings?sortBy=${sortBy}`);
    return response.data;
  },

  getShowOccupancyHeatmap: async (theaterId, movieId) => {
    const params = new URLSearchParams();
    if (theaterId) params.append('theaterId', theaterId);
    if (movieId) params.append('movieId', movieId);
    const response = await api.get(`/admin/analytics/shows/occupancy-heatmap?${params.toString()}`);
    return response.data;
  },

  getUserAnalytics: async () => {
    const response = await api.get('/admin/analytics/users');
    return response.data;
  },

  getRevenueTrends: async (period = 'daily', dateFrom, dateTo) => {
    const params = new URLSearchParams({ period });
    if (dateFrom) params.append('dateFrom', dateFrom);
    if (dateTo) params.append('dateTo', dateTo);
    const response = await api.get(`/admin/analytics/revenue/trends?${params.toString()}`);
    return response.data;
  },

  getOccupancyTrends: async (dateFrom, dateTo) => {
    const params = new URLSearchParams();
    if (dateFrom) params.append('dateFrom', dateFrom);
    if (dateTo) params.append('dateTo', dateTo);
    const response = await api.get(`/admin/analytics/occupancy/trends?${params.toString()}`);
    return response.data;
  },

  getCancellationTrends: async (dateFrom, dateTo) => {
    const params = new URLSearchParams();
    if (dateFrom) params.append('dateFrom', dateFrom);
    if (dateTo) params.append('dateTo', dateTo);
    const response = await api.get(`/admin/analytics/cancellation/trends?${params.toString()}`);
    return response.data;
  },

  getDistributions: async () => {
    const response = await api.get('/admin/analytics/distributions');
    return response.data;
  },

  getExportData: async (type, dateFrom, dateTo) => {
    const params = new URLSearchParams();
    if (dateFrom) params.append('dateFrom', dateFrom);
    if (dateTo) params.append('dateTo', dateTo);
    const response = await api.get(`/admin/analytics/export/${type}?${params.toString()}`);
    return response.data;
  },

  // =====================================================
  // THEATRE ADMIN MANAGEMENT
  // =====================================================

  getTheatreAdmins: async () => {
    const response = await api.get('/admin/theatre-admins');
    return response.data;
  },

  assignTheatreAdmin: async (theaterId, adminUserId) => {
    const response = await api.put(`/admin/theaters/${theaterId}/assign-admin`, { adminUserId });
    return response.data;
  },

  removeTheatreAdmin: async (theaterId) => {
    const response = await api.put(`/admin/theaters/${theaterId}/remove-admin`);
    return response.data;
  },

  // =====================================================
  // MOVIE RECOMMENDATIONS
  // =====================================================

  getRecommendations: async () => {
    const response = await api.get('/admin/recommendations');
    return response.data;
  },

  recommendMovie: async (movieId, theaterId, message) => {
    const response = await api.post('/admin/recommendations', { movieId, theaterId, message });
    return response.data;
  },

  recommendMovieToCity: async (movieId, cityId, message) => {
    const response = await api.post('/admin/recommendations/city', { movieId, cityId, message });
    return response.data;
  },

  // =====================================================
  // BOOKINGS, PAYMENTS, WALLET TRANSACTIONS
  // =====================================================

  getAllBookings: async () => {
    const response = await api.get('/admin/bookings');
    return response.data;
  },

  getAllPayments: async (status) => {
    const params = status ? `?status=${status}` : '';
    const response = await api.get(`/admin/payments${params}`);
    return response.data;
  },

  getWalletTransactions: async () => {
    const response = await api.get('/admin/wallet/transactions');
    return response.data;
  },

  getRevenueReport: async () => {
    const response = await api.get('/admin/revenue-report');
    return response.data;
  },

  // =====================================================
  // FOOD & PARKING MANAGEMENT
  // =====================================================

  getAllFoodItems: async () => {
    const response = await api.get('/admin/food');
    return response.data;
  },

  getFoodItemsByTheater: async (theaterId) => {
    const response = await api.get(`/admin/food/theater/${theaterId}`);
    return response.data;
  },

  addFoodItem: async (foodData) => {
    const response = await api.post('/admin/food', foodData);
    return response.data;
  },

  updateFoodItem: async (foodId, foodData) => {
    const response = await api.put(`/admin/food/${foodId}`, foodData);
    return response.data;
  },

  deleteFoodItem: async (foodId) => {
    const response = await api.delete(`/admin/food/${foodId}`);
    return response.data;
  },

  getAllParkingSlots: async () => {
    const response = await api.get('/admin/parking');
    return response.data;
  },

  getParkingSlotsByTheater: async (theaterId) => {
    const response = await api.get(`/admin/parking/theater/${theaterId}`);
    return response.data;
  },

  updateParkingSlot: async (slotId, slotData) => {
    const response = await api.put(`/admin/parking/${slotId}`, slotData);
    return response.data;
  },

  updateParkingSlotStatus: async (slotId, isOccupied) => {
    const response = await api.put(`/admin/parking/${slotId}/status?isOccupied=${isOccupied}`);
    return response.data;
  },

  // Bulk parking price update by theatre
  updateParkingPriceByTheater: async (theaterId, vehicleType, hourlyRate) => {
    const response = await api.put(`/admin/parking/theater/${theaterId}/price?vehicleType=${vehicleType}&hourlyRate=${hourlyRate}`);
    return response.data;
  },

  // User detail analytics
  getUserDetailAnalytics: async (userId) => {
    const response = await api.get(`/admin/users/${userId}/analytics`);
    return response.data;
  },

  // Detailed bookings & payments
  getDetailedBookings: async () => {
    const response = await api.get('/admin/bookings/detailed');
    return response.data;
  },

  getDetailedPayments: async (status) => {
    const params = status ? `?status=${status}` : '';
    const response = await api.get(`/admin/payments/detailed${params}`);
    return response.data;
  },

  getPopularMovies: async () => {
    const response = await api.get('/admin/movies/popular');
    return response.data;
  },

  // =====================================================
  // ENHANCED ANALYTICS (NEW)
  // =====================================================

  getRecommendationAnalytics: async () => {
    const response = await api.get('/admin/analytics/recommendations');
    return response.data;
  },

  getSeatTypeAnalytics: async () => {
    const response = await api.get('/admin/analytics/seat-types');
    return response.data;
  },

  getPeakTimeAnalytics: async () => {
    const response = await api.get('/admin/analytics/peak-times');
    return response.data;
  },

  getLanguageAnalytics: async () => {
    const response = await api.get('/admin/analytics/languages');
    return response.data;
  }
};

// User Profile Services
export const userService = {
  getProfile: async (userId) => {
    const response = await api.get(`/api/user/profile/${userId}`);
    return response.data;
  },

  updateProfile: async (userId, userData) => {
    const response = await api.put(`/api/user/profile/${userId}`, userData);
    return response.data;
  },

  getWalletBalance: async (userId) => {
    // Updated to use new UserWallet API
    const response = await api.get(`/api/user-wallet/${userId}/balance`);
    return response.data;
  }
};

// Wallet Services
export const walletService = {
  // Get wallet balance from DB via UserWallet API
  getBalance: async (userId) => {
    const response = await api.get(`/api/user-wallet/${userId}/balance`);
    return response.data;
  },

  getTemporaryCredit: async (userId) => {
    const response = await api.get(`/api/user-wallet/${userId}/temporary-credit`);
    return response.data;
  },

  getTemporaryCreditTransactions: async (userId) => {
    const response = await api.get(`/api/wallet/temporary-transactions/${userId}`);
    return response.data;
  },

  getTransactions: async (userId, type = null) => {
    const params = type ? `?type=${type}` : '';
    const response = await api.get(`/api/wallet/transactions/${userId}${params}`);
    return response.data;
  },

  // Add money to wallet via UserWallet API
  addMoney: async (userId, amount, description = 'Wallet recharge') => {
    const response = await api.post(`/api/user-wallet/${userId}/credit`, {
      amount: amount,
      description: description
    });
    return response.data;
  }
};

export const gameRewardsService = {
  getStatus: async (userId) => {
    const response = await api.get(`/api/games/rewards/${userId}/status`);
    return response.data;
  },

  submitPlay: async (userId, payload) => {
    const response = await api.post(`/api/games/rewards/${userId}/play`, payload);
    return response.data;
  },

  getHistory: async (userId) => {
    const response = await api.get(`/api/games/rewards/${userId}/history`);
    return response.data;
  }
};

// Theatre Owner Services (for THEATER_OWNER role)
export const theatreOwnerService = {
  getDashboard: async () => {
    const response = await api.get('/owner/dashboard');
    return response.data;
  },

  getTheatre: async () => {
    const response = await api.get('/owner/theatre');
    return response.data;
  },

  getRecommendations: async () => {
    const response = await api.get('/owner/recommendations');
    return response.data;
  },

  getPendingRecommendations: async () => {
    const response = await api.get('/owner/recommendations/pending');
    return response.data;
  },

  acceptRecommendation: async (id, message) => {
    const response = await api.post(`/owner/recommendations/${id}/accept`, { message });
    return response.data;
  },

  rejectRecommendation: async (id, reason) => {
    const response = await api.post(`/owner/recommendations/${id}/reject`, { reason });
    return response.data;
  },

  getShows: async () => {
    const response = await api.get('/owner/shows');
    return response.data;
  },

  addShow: async (showData) => {
    const response = await api.post('/owner/shows', showData);
    return response.data;
  },

  deleteShow: async (showId) => {
    const response = await api.delete(`/owner/shows/${showId}`);
    return response.data;
  },

  getShowBookings: async (showId) => {
    const response = await api.get(`/owner/shows/${showId}/bookings`);
    return response.data;
  },

  getSeats: async () => {
    const response = await api.get('/owner/seats');
    return response.data;
  },

  getShowSeats: async (showId) => {
    const response = await api.get(`/owner/shows/${showId}/seats`);
    return response.data;
  },

  getAnalytics: async () => {
    const response = await api.get('/owner/analytics');
    return response.data;
  },

  // =====================================================
  // SEAT MANAGEMENT
  // =====================================================

  addSeatsRow: async (rowPrefix, seatType, count) => {
    const response = await api.post('/owner/seats/row', { rowPrefix, seatType, count });
    return response.data;
  },

  deleteSeat: async (seatId) => {
    const response = await api.delete(`/owner/seats/${seatId}`);
    return response.data;
  },

  getSeatSummary: async () => {
    const response = await api.get('/owner/seats/summary');
    return response.data;
  },

  // =====================================================
  // ENHANCED ANALYTICS
  // =====================================================

  getSeatTypeRevenue: async () => {
    const response = await api.get('/owner/analytics/seat-revenue');
    return response.data;
  },

  getRecommendationStats: async () => {
    const response = await api.get('/owner/analytics/recommendations');
    return response.data;
  },

  getTimeSlotAnalytics: async () => {
    const response = await api.get('/owner/analytics/time-slots');
    return response.data;
  },

  getCancellationStats: async () => {
    const response = await api.get('/owner/analytics/cancellations');
    return response.data;
  },

  getWeeklyRevenueTrend: async () => {
    const response = await api.get('/owner/analytics/weekly-revenue');
    return response.data;
  },

  getGenrePerformance: async () => {
    const response = await api.get('/owner/analytics/genres');
    return response.data;
  },

  getPaymentAnalytics: async () => {
    const response = await api.get('/owner/analytics/payments');
    return response.data;
  }
};

export default {
  authService,
  movieService,
  recommendationService,
  theaterService,
  showService,
  seatLockService,
  paymentService,
  otpService,
  bookingService,
  adminService,
  theatreOwnerService,
  userService,
  walletService,
  publicService,
  gameService
};
