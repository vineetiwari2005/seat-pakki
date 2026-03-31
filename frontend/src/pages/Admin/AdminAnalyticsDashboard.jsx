import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminService } from '../../services';
import {
  FaUsers, FaFilm, FaTheaterMasks, FaTicketAlt, FaRupeeSign, FaCity,
  FaPlus, FaEdit, FaTrash, FaChartLine, FaChartBar, FaChartPie,
  FaDownload, FaFilter, FaTrophy, FaFireAlt, FaPercentage, FaStar,
  FaCalendarAlt, FaToggleOn, FaToggleOff, FaChair, FaCogs, FaWallet,
  FaUserMinus, FaArrowUp, FaArrowDown, FaExchangeAlt, FaTh
} from 'react-icons/fa';
import {
  BarChart, Bar, LineChart, Line, PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer,
  AreaChart, Area, ComposedChart
} from 'recharts';
import './AdminAnalytics.scss';

const COLORS = ['#e23744', '#f97316', '#eab308', '#22c55e', '#3b82f6', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16', '#f43f5e'];

const formatCurrency = (val) => {
  if (val == null) return '₹0';
  if (val >= 10000000) return `₹${(val / 10000000).toFixed(1)}Cr`;
  if (val >= 100000) return `₹${(val / 100000).toFixed(1)}L`;
  if (val >= 1000) return `₹${(val / 1000).toFixed(1)}K`;
  return `₹${val.toFixed(0)}`;
};

const downloadCSV = (data, filename) => {
  if (!data || data.length === 0) return;
  const headers = Object.keys(data[0]);
  const csvContent = [
    headers.join(','),
    ...data.map(row => headers.map(h => {
      const val = row[h];
      const str = val != null ? String(val) : '';
      return str.includes(',') || str.includes('"') ? `"${str.replace(/"/g, '""')}"` : str;
    }).join(','))
  ].join('\n');

  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = `${filename}_${new Date().toISOString().split('T')[0]}.csv`;
  link.click();
};

const AdminAnalyticsDashboard = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('overview');
  const [loading, setLoading] = useState(false);

  // Data states
  const [dashboardData, setDashboardData] = useState(null);
  const [cityAnalytics, setCityAnalytics] = useState([]);
  const [movieAnalytics, setMovieAnalytics] = useState([]);
  const [theaterRankings, setTheaterRankings] = useState([]);
  const [heatmapData, setHeatmapData] = useState(null);
  const [userAnalytics, setUserAnalytics] = useState(null);
  const [revenueTrends, setRevenueTrends] = useState([]);
  const [occupancyTrends, setOccupancyTrends] = useState([]);
  const [cancellationTrends, setCancellationTrends] = useState([]);
  const [distributions, setDistributions] = useState(null);

  // For existing management
  const [movies, setMovies] = useState([]);
  const [theaters, setTheaters] = useState([]);
  const [shows, setShows] = useState([]);
  const [users, setUsers] = useState([]);
  const [cities, setCities] = useState([]);

  // Seat management
  const [theaterSeats, setTheaterSeats] = useState([]);
  const [showSeats, setShowSeats] = useState([]);
  const [selectedTheaterId, setSelectedTheaterId] = useState('');
  const [selectedShowId, setSelectedShowId] = useState('');
  const [seatLoading, setSeatLoading] = useState(false);

  // Filter states
  const [movieFilters, setMovieFilters] = useState({ genre: '', language: '', dateFrom: '', dateTo: '' });
  const [trendPeriod, setTrendPeriod] = useState('daily');
  const [trendDateFrom, setTrendDateFrom] = useState('');
  const [trendDateTo, setTrendDateTo] = useState('');
  const [theaterSortBy, setTheaterSortBy] = useState('revenue');
  const [heatmapTheaterId, setHeatmapTheaterId] = useState('');
  const [heatmapMovieId, setHeatmapMovieId] = useState('');

  // Comparison
  const [compareMovieIds, setCompareMovieIds] = useState([]);
  const [comparisonData, setComparisonData] = useState([]);

  // Modal & CRUD
  const [showModal, setShowModal] = useState(false);
  const [modalType, setModalType] = useState('');
  const [currentItem, setCurrentItem] = useState(null);
  const [formData, setFormData] = useState({});

  // Export
  const [exportType, setExportType] = useState('revenue');
  const [exportDateFrom, setExportDateFrom] = useState('');
  const [exportDateTo, setExportDateTo] = useState('');

  // Theatre Admin management
  const [theatreAdmins, setTheatreAdmins] = useState([]);
  const [assignAdminForm, setAssignAdminForm] = useState({ theaterId: '', adminUserId: '' });
  const [taCity, setTaCity] = useState('');

  // Recommendations
  const [recommendations, setRecommendations] = useState([]);
  const [recForm, setRecForm] = useState({ movieId: '', theaterId: '', cityId: '', message: '', mode: 'theatre' });

  // Bookings & Payments
  const [bookings, setBookings] = useState([]);
  const [payments, setPayments] = useState([]);
  const [walletTransactions, setWalletTransactions] = useState([]);
  const [paymentStatusFilter, setPaymentStatusFilter] = useState('');

  // Food & Parking
  const [foodItems, setFoodItems] = useState([]);
  const [parkingSlots, setParkingSlots] = useState([]);
  const [foodForm, setFoodForm] = useState({ itemName: '', description: '', price: '', category: 'SNACKS', theater: { id: '' }, isAvailable: true });
  const [editingFoodId, setEditingFoodId] = useState(null);
  const [foodTheaterFilter, setFoodTheaterFilter] = useState('');
  const [parkingTheaterFilter, setParkingTheaterFilter] = useState('');

  // User detail analytics modal
  const [selectedUserDetail, setSelectedUserDetail] = useState(null);
  const [showUserDetailModal, setShowUserDetailModal] = useState(false);
  const [userDetailLoading, setUserDetailLoading] = useState(false);

  // Parking bulk pricing
  const [parkingCityFilter, setParkingCityFilter] = useState('');
  const [parkingBulkVehicleType, setParkingBulkVehicleType] = useState('');
  const [parkingBulkRate, setParkingBulkRate] = useState('');
  const [parkingBulkTheaterId, setParkingBulkTheaterId] = useState('');

  // =====================================================
  // DATA FETCHING
  // =====================================================

  const fetchDashboardData = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminService.getEnhancedDashboard();
      setDashboardData(data);
    } catch (error) {
      console.error('Error fetching dashboard:', error);
      // Fall back to basic dashboard
      try {
        const basic = await adminService.getDashboard();
        setDashboardData(basic);
      } catch (e) { console.error(e); }
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchCityAnalytics = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminService.getCityAnalytics();
      setCityAnalytics(Array.isArray(data) ? data : []);
    } catch (error) { console.error(error); setCityAnalytics([]); }
    finally { setLoading(false); }
  }, []);

  const fetchMovieAnalytics = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminService.getMovieAnalytics(movieFilters);
      setMovieAnalytics(Array.isArray(data) ? data : []);
    } catch (error) { console.error(error); setMovieAnalytics([]); }
    finally { setLoading(false); }
  }, [movieFilters]);

  const fetchTheaterRankings = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminService.getTheaterRankings(theaterSortBy);
      setTheaterRankings(Array.isArray(data) ? data : []);
    } catch (error) { console.error(error); setTheaterRankings([]); }
    finally { setLoading(false); }
  }, [theaterSortBy]);

  const fetchHeatmap = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminService.getShowOccupancyHeatmap(
        heatmapTheaterId || null, heatmapMovieId || null
      );
      setHeatmapData(data);
    } catch (error) { console.error(error); setHeatmapData(null); }
    finally { setLoading(false); }
  }, [heatmapTheaterId, heatmapMovieId]);

  const fetchUserAnalytics = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminService.getUserAnalytics();
      setUserAnalytics(data);
    } catch (error) { console.error(error); setUserAnalytics(null); }
    finally { setLoading(false); }
  }, []);

  const fetchRevenueTrends = useCallback(async () => {
    try {
      setLoading(true);
      const data = await adminService.getRevenueTrends(trendPeriod, trendDateFrom || undefined, trendDateTo || undefined);
      setRevenueTrends(Array.isArray(data) ? data : []);
    } catch (error) { console.error(error); setRevenueTrends([]); }
    finally { setLoading(false); }
  }, [trendPeriod, trendDateFrom, trendDateTo]);

  const fetchOccupancyTrends = useCallback(async () => {
    try {
      const data = await adminService.getOccupancyTrends(trendDateFrom || undefined, trendDateTo || undefined);
      setOccupancyTrends(Array.isArray(data) ? data : []);
    } catch (error) { console.error(error); setOccupancyTrends([]); }
  }, [trendDateFrom, trendDateTo]);

  const fetchCancellationTrends = useCallback(async () => {
    try {
      const data = await adminService.getCancellationTrends(trendDateFrom || undefined, trendDateTo || undefined);
      setCancellationTrends(Array.isArray(data) ? data : []);
    } catch (error) { console.error(error); setCancellationTrends([]); }
  }, [trendDateFrom, trendDateTo]);

  const fetchDistributions = useCallback(async () => {
    try {
      const data = await adminService.getDistributions();
      setDistributions(data);
    } catch (error) { console.error(error); setDistributions(null); }
  }, []);

  // Management fetches
  const fetchMovies = async () => { try { const d = await adminService.getAllMovies(); setMovies(Array.isArray(d) ? d : []); } catch(e) { setMovies([]); }};
  const fetchTheaters = async () => { try { const d = await adminService.getAllTheaters(); setTheaters(Array.isArray(d) ? d : []); } catch(e) { setTheaters([]); }};
  const fetchShows = async () => { try { const d = await adminService.getAllShows(); setShows(Array.isArray(d) ? d : []); } catch(e) { setShows([]); }};
  const fetchUsers = async () => { try { const d = await adminService.getAllUsers(); setUsers(Array.isArray(d) ? d : []); } catch(e) { setUsers([]); }};
  const fetchCities = async () => { try { const d = await adminService.getAllCities(); setCities(Array.isArray(d) ? d : []); } catch(e) { setCities([]); }};

  // New management fetches
  const fetchTheatreAdmins = async () => { try { const d = await adminService.getTheatreAdmins(); setTheatreAdmins(Array.isArray(d) ? d : []); } catch(e) { setTheatreAdmins([]); }};
  const fetchRecommendations = async () => { try { const d = await adminService.getRecommendations(); setRecommendations(Array.isArray(d) ? d : []); } catch(e) { setRecommendations([]); }};
  const fetchBookings = async () => { try { const d = await adminService.getDetailedBookings().catch(() => adminService.getAllBookings()); setBookings(Array.isArray(d) ? d : []); } catch(e) { setBookings([]); }};
  const fetchPayments = async (status) => { try { const d = await adminService.getDetailedPayments(status || undefined).catch(() => adminService.getAllPayments(status || undefined)); setPayments(Array.isArray(d) ? d : []); } catch(e) { setPayments([]); }};
  const fetchWalletTransactions = async () => { try { const d = await adminService.getWalletTransactions(); setWalletTransactions(Array.isArray(d) ? d : []); } catch(e) { setWalletTransactions([]); }};
  const fetchFoodItems = async () => { try { const d = await adminService.getAllFoodItems(); setFoodItems(Array.isArray(d) ? d : []); } catch(e) { setFoodItems([]); }};
  const fetchParkingSlots = async () => { try { const d = await adminService.getAllParkingSlots(); setParkingSlots(Array.isArray(d) ? d : []); } catch(e) { setParkingSlots([]); }};

  const fetchTheaterSeats = async (theaterId) => {
    if (!theaterId) return;
    try { setSeatLoading(true); const d = await adminService.getTheaterSeats(theaterId); setTheaterSeats(Array.isArray(d) ? d : []); }
    catch(e) { setTheaterSeats([]); } finally { setSeatLoading(false); }
  };
  const fetchShowSeats = async (showId) => {
    if (!showId) return;
    try { setSeatLoading(true); const d = await adminService.getShowSeats(showId); setShowSeats(Array.isArray(d) ? d : []); }
    catch(e) { setShowSeats([]); } finally { setSeatLoading(false); }
  };

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  useEffect(() => {
    switch (activeTab) {
      case 'overview': fetchDashboardData(); break;
      case 'city-analytics': fetchCityAnalytics(); fetchCities(); break;
      case 'movie-analytics': fetchMovieAnalytics(); break;
      case 'theater-rankings': fetchTheaterRankings(); break;
      case 'heatmap': fetchHeatmap(); fetchTheaters(); fetchMovies(); break;
      case 'user-analytics': fetchUserAnalytics(); break;
      case 'charts': fetchRevenueTrends(); fetchOccupancyTrends(); fetchCancellationTrends(); fetchDistributions(); break;
      case 'export': break;
      case 'movies': fetchMovies(); break;
      case 'theaters': fetchTheaters(); fetchCities(); fetchUsers(); break;
      case 'shows': fetchShows(); break;
      case 'users': fetchUsers(); break;
      case 'cities': fetchCities(); break;
      case 'seats': fetchTheaters(); fetchShows(); break;

      case 'recommendations': fetchRecommendations(); fetchMovies(); fetchTheaters(); fetchCities(); break;
      case 'bookings-payments': fetchBookings(); fetchPayments(paymentStatusFilter); fetchWalletTransactions(); break;
      case 'food': fetchFoodItems(); fetchTheaters(); break;
      case 'parking': fetchParkingSlots(); fetchTheaters(); fetchCities(); break;
    }
  }, [activeTab]);

  // =====================================================
  // CRUD HANDLERS (reusing existing logic)
  // =====================================================

  const handleDelete = async (type, id) => {
    if (!window.confirm(`Are you sure you want to delete this ${type}?`)) return;
    try {
      switch (type) {
        case 'movie': await adminService.deleteMovie(id); fetchMovies(); break;
        case 'theater': await adminService.deleteTheater(id); fetchTheaters(); break;
        case 'show': await adminService.deleteShow(id); fetchShows(); break;
        case 'user': await adminService.deleteUser(id); fetchUsers(); break;
        case 'city': await adminService.deleteCity(id); fetchCities(); break;
      }
    } catch (error) {
      alert(error.response?.data?.error || error.message);
    }
  };

  const handleToggleUserStatus = async (userId, currentStatus) => {
    try {
      await adminService.updateUserStatus(userId, !currentStatus);
      fetchUsers();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const handleCitySubmit = async (e) => {
    e.preventDefault();
    try {
      if (currentItem) {
        await adminService.updateCity(currentItem.id, formData);
      } else {
        await adminService.addCity(formData);
      }
      setShowModal(false);
      fetchCities();
      fetchCityAnalytics();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const handleMovieSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        movieName: formData.movieName,
        duration: parseInt(formData.duration) || 0,
        rating: parseFloat(formData.rating) || 0,
        releaseDate: formData.releaseDate || null,
        genre: formData.genre,
        language: formData.language,
        description: formData.description || '',
        director: formData.director || '',
        cast: formData.cast || '',
        posterUrl: formData.posterUrl || '',
        trailerUrl: formData.trailerUrl || '',
        nowShowing: formData.nowShowing !== false
      };
      if (currentItem) {
        await adminService.updateMovie(currentItem.id, payload);
      } else {
        await adminService.addMovie(payload);
      }
      setShowModal(false);
      fetchMovies();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const handleTheaterSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        name: formData.name,
        address: formData.address,
        cityId: parseInt(formData.cityId)
      };
      if (formData.adminUserId) {
        payload.adminUserId = parseInt(formData.adminUserId);
      }
      if (currentItem) {
        await adminService.updateTheater(currentItem.id, payload);
      } else {
        await adminService.addTheater(payload);
      }
      setShowModal(false);
      fetchTheaters();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const handleShowSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        movieId: parseInt(formData.movieId),
        theaterId: parseInt(formData.theaterId),
        showDate: formData.showDate,
        showStartTime: formData.showStartTime ? formData.showStartTime + ':00' : ''
      };
      if (currentItem) {
        await adminService.updateShow(currentItem.id, payload);
      } else {
        await adminService.addShow(payload);
      }
      setShowModal(false);
      fetchShows();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const openMovieModal = (movie = null) => {
    setCurrentItem(movie);
    setFormData(movie ? {
      movieName: movie.movieName || '', duration: movie.duration || '', rating: movie.rating || '',
      releaseDate: movie.releaseDate || '', genre: movie.genre || '', language: movie.language || '',
      description: movie.description || '', director: movie.director || '', cast: movie.cast || '',
      posterUrl: movie.posterUrl || '', trailerUrl: movie.trailerUrl || '', nowShowing: movie.nowShowing !== false
    } : { movieName: '', duration: '', rating: '', releaseDate: '', genre: '', language: '', description: '', director: '', cast: '', posterUrl: '', trailerUrl: '', nowShowing: true });
    setModalType('movie');
    setShowModal(true);
  };

  const openTheaterModal = (theater = null) => {
    setCurrentItem(theater);
    setFormData(theater ? {
      name: theater.name || '', address: theater.address || '',
      cityId: theater.city?.id || theater.cityId || '',
      adminUserId: theater.admin?.id || ''
    } : { name: '', address: '', cityId: '', adminUserId: '' });
    setModalType('theater');
    setShowModal(true);
  };

  const openShowModal = (show = null) => {
    setCurrentItem(show);
    setFormData(show ? {
      movieId: show.movie?.id || show.movieId || '',
      theaterId: show.theater?.id || show.theaterId || '',
      showDate: show.date || show.showDate || '',
      showStartTime: show.time ? show.time.substring(0, 5) : (show.showStartTime ? show.showStartTime.substring(0, 5) : '')
    } : { movieId: '', theaterId: '', showDate: '', showStartTime: '' });
    setModalType('show');
    setShowModal(true);
  };

  const handleExport = async () => {
    try {
      setLoading(true);
      const data = await adminService.getExportData(exportType, exportDateFrom || undefined, exportDateTo || undefined);
      if (data && data.length > 0) {
        downloadCSV(data, `bookmyshow_${exportType}`);
      } else {
        alert('No data to export for the selected criteria.');
      }
    } catch (error) { alert('Export failed: ' + (error.message || 'Unknown error')); }
    finally { setLoading(false); }
  };

  const handleCompareMovies = async () => {
    if (compareMovieIds.length < 2) { alert('Select at least 2 movies to compare'); return; }
    try {
      setLoading(true);
      const data = await adminService.compareMovies(compareMovieIds);
      setComparisonData(Array.isArray(data) ? data : []);
    } catch (error) { console.error(error); }
    finally { setLoading(false); }
  };

  // =====================================================
  // TAB NAVIGATION
  // =====================================================

  const tabs = [
    { key: 'overview', label: 'Overview', icon: <FaChartLine /> },
    { key: 'city-analytics', label: 'Cities', icon: <FaCity /> },
    { key: 'movie-analytics', label: 'Movies', icon: <FaFilm /> },
    { key: 'theater-rankings', label: 'Theatres', icon: <FaTrophy /> },
    { key: 'heatmap', label: 'Heatmap', icon: <FaTh /> },
    { key: 'user-analytics', label: 'Users', icon: <FaUsers /> },
    { key: 'charts', label: 'Charts', icon: <FaChartBar /> },
    { key: 'export', label: 'Export', icon: <FaDownload /> },
    { key: 'movies', label: 'Manage Movies', icon: <FaFilm /> },
    { key: 'theaters', label: 'Manage Theatres', icon: <FaTheaterMasks /> },
    { key: 'shows', label: 'Show Analytics', icon: <FaTicketAlt /> },
    { key: 'users', label: 'Manage Users', icon: <FaUsers /> },
    { key: 'cities', label: 'Manage Cities', icon: <FaCity /> },
    { key: 'seats', label: 'Seat Analytics', icon: <FaChair /> },

    { key: 'recommendations', label: 'Recommendations', icon: <FaStar /> },
    { key: 'bookings-payments', label: 'Bookings & Payments', icon: <FaWallet /> },
    { key: 'food', label: 'Food Items', icon: <FaFireAlt /> },
    { key: 'parking', label: 'Parking', icon: <FaCogs /> },
  ];

  // =====================================================
  // RENDER HELPERS
  // =====================================================

  const StatCard = ({ icon, label, value, subValue, color }) => (
    <div className="stat-card" style={{ borderLeft: `4px solid ${color || '#e23744'}` }}>
      <div className="stat-icon" style={{ color: color || '#e23744' }}>{icon}</div>
      <div className="stat-info">
        <h3>{value}</h3>
        <p>{label}</p>
        {subValue && <span className="sub-value">{subValue}</span>}
      </div>
    </div>
  );

  // =====================================================
  // OVERVIEW TAB
  // =====================================================

  const renderOverview = () => {
    if (!dashboardData) return <div className="loading">Loading dashboard...</div>;
    const d = dashboardData;
    return (
      <div className="overview-tab">
        <div className="stats-grid">
          <StatCard icon={<FaRupeeSign />} label="Total Revenue" value={formatCurrency(d.totalRevenue || 0)} subValue={`Today: ${formatCurrency(d.todayRevenue || 0)}`} color="#22c55e" />
          <StatCard icon={<FaTicketAlt />} label="Total Bookings" value={d.totalBookings || 0} subValue={`Today: ${d.todayBookings || 0}`} color="#3b82f6" />
          <StatCard icon={<FaUsers />} label="Total Users" value={d.totalUsers || 0} color="#8b5cf6" />
          <StatCard icon={<FaFilm />} label="Total Movies" value={d.totalMovies || 0} color="#f97316" />
          <StatCard icon={<FaTheaterMasks />} label="Total Theatres" value={d.totalTheaters || 0} color="#ec4899" />
          <StatCard icon={<FaTicketAlt />} label="Total Shows" value={d.totalShows || 0} color="#06b6d4" />
          <StatCard icon={<FaCity />} label="Total Cities" value={d.totalCities || 0} color="#eab308" />
          <StatCard icon={<FaPercentage />} label="Occupancy" value={`${d.overallOccupancy || 0}%`} color="#84cc16" />
          <StatCard icon={<FaExchangeAlt />} label="Cancellation Rate" value={`${d.cancellationRate || 0}%`} subValue={`${d.totalCancellations || 0} cancelled`} color="#f43f5e" />
          <StatCard icon={<FaRupeeSign />} label="Avg Revenue/Booking" value={formatCurrency(d.avgRevenuePerBooking || 0)} color="#22c55e" />
        </div>

        {d.paymentMethods && Object.keys(d.paymentMethods).length > 0 && (
          <div className="chart-container">
            <h3><FaChartPie /> Payment Methods</h3>
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie data={Object.entries(d.paymentMethods).map(([k, v]) => ({ name: k.replace(/_/g, ' '), value: v }))}
                  cx="50%" cy="50%" outerRadius={100} dataKey="value" label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
                  {Object.keys(d.paymentMethods).map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}

        <div className="quick-actions">
          <h3>Quick Actions</h3>
          <div className="action-grid">
            <button onClick={() => navigate('/admin/movies/add')}><FaPlus /> Add Movie</button>
            <button onClick={() => navigate('/admin/theaters/add')}><FaPlus /> Add Theatre</button>
            <button onClick={() => navigate('/admin/shows/add')}><FaPlus /> Add Show</button>
            <button onClick={() => setActiveTab('export')}><FaDownload /> Export Data</button>
          </div>
        </div>
      </div>
    );
  };

  // =====================================================
  // CITY ANALYTICS TAB
  // =====================================================

  const renderCityAnalytics = () => (
    <div className="analytics-section">
      <div className="section-header">
        <h2><FaCity /> City Analytics</h2>
        <button className="export-btn" onClick={() => { setExportType('cities'); setActiveTab('export'); }}><FaDownload /> Export</button>
      </div>

      {cityAnalytics.length > 0 && (
        <div className="chart-container">
          <h3>Revenue by City</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={cityAnalytics.filter(c => c.totalRevenue > 0)}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="cityName" />
              <YAxis tickFormatter={formatCurrency} />
              <Tooltip formatter={(val) => formatCurrency(val)} />
              <Bar dataKey="totalRevenue" fill="#e23744" name="Revenue" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      <div className="data-table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>City</th>
              <th>State</th>
              <th>Theatres</th>
              <th>Shows</th>
              <th>Bookings</th>
              <th>Revenue</th>
              <th>Active Users</th>
            </tr>
          </thead>
          <tbody>
            {cityAnalytics.map((city, i) => (
              <tr key={i}>
                <td className="city-name">{city.cityName}</td>
                <td>{city.state || '-'}</td>
                <td>{city.theaterCount}</td>
                <td>{city.showCount}</td>
                <td>{city.bookingCount}</td>
                <td className="revenue">{formatCurrency(city.totalRevenue)}</td>
                <td>{city.activeUsers}</td>
              </tr>
            ))}
            {cityAnalytics.length === 0 && <tr><td colSpan="7" className="no-data">No city data found</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );

  // =====================================================
  // MOVIE ANALYTICS TAB
  // =====================================================

  const renderMovieAnalytics = () => (
    <div className="analytics-section">
      <div className="section-header">
        <h2><FaFilm /> Movie Analytics</h2>
        <button className="export-btn" onClick={() => { setExportType('movies'); setActiveTab('export'); }}><FaDownload /> Export</button>
      </div>

      <div className="filter-bar">
        <select value={movieFilters.genre} onChange={e => setMovieFilters(prev => ({ ...prev, genre: e.target.value }))}>
          <option value="">All Genres</option>
          {['DRAMA','THRILLER','ACTION','ROMANTIC','COMEDY','HISTORICAL','ANIMATION','SPORTS','SOCIAL','WAR'].map(g =>
            <option key={g} value={g}>{g}</option>)}
        </select>
        <select value={movieFilters.language} onChange={e => setMovieFilters(prev => ({ ...prev, language: e.target.value }))}>
          <option value="">All Languages</option>
          {['HINDI','ENGLISH','TELUGU','TAMIL','MARATHI','PUNJAB','KANNADA'].map(l =>
            <option key={l} value={l}>{l}</option>)}
        </select>
        <input type="date" value={movieFilters.dateFrom} onChange={e => setMovieFilters(prev => ({ ...prev, dateFrom: e.target.value }))} placeholder="From" />
        <input type="date" value={movieFilters.dateTo} onChange={e => setMovieFilters(prev => ({ ...prev, dateTo: e.target.value }))} placeholder="To" />
        <button className="apply-btn" onClick={fetchMovieAnalytics}><FaFilter /> Apply</button>
      </div>

      {movieAnalytics.length > 0 && (
        <div className="charts-row">
          <div className="chart-container half">
            <h3>Revenue by Movie (Top 10)</h3>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={movieAnalytics.slice(0, 10)} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis type="number" tickFormatter={formatCurrency} />
                <YAxis type="category" dataKey="movieName" width={120} tick={{ fontSize: 12 }} />
                <Tooltip formatter={(val) => formatCurrency(val)} />
                <Bar dataKey="totalRevenue" fill="#e23744" name="Revenue" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="chart-container half">
            <h3>Occupancy Rate by Movie</h3>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={movieAnalytics.slice(0, 10)}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="movieName" tick={{ fontSize: 11 }} angle={-20} textAnchor="end" height={60} />
                <YAxis domain={[0, 100]} unit="%" />
                <Tooltip formatter={(val) => `${val}%`} />
                <Bar dataKey="occupancyRate" fill="#3b82f6" name="Occupancy %" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {/* Movie Comparison */}
      <div className="comparison-section">
        <h3><FaExchangeAlt /> Compare Movies</h3>
        <div className="compare-controls">
          <select multiple value={compareMovieIds} onChange={e => {
            const selected = Array.from(e.target.selectedOptions, o => parseInt(o.value));
            setCompareMovieIds(selected);
          }} className="multi-select">
            {movieAnalytics.map(m => <option key={m.movieId} value={m.movieId}>{m.movieName}</option>)}
          </select>
          <button onClick={handleCompareMovies} className="apply-btn"><FaExchangeAlt /> Compare</button>
        </div>
        {comparisonData.length > 0 && (
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={comparisonData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="movieName" />
                <YAxis yAxisId="left" tickFormatter={formatCurrency} />
                <YAxis yAxisId="right" orientation="right" domain={[0, 100]} unit="%" />
                <Tooltip />
                <Legend />
                <Bar yAxisId="left" dataKey="totalRevenue" fill="#e23744" name="Revenue" radius={[4, 4, 0, 0]} />
                <Bar yAxisId="right" dataKey="occupancyRate" fill="#3b82f6" name="Occupancy %" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>

      <div className="data-table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Movie</th>
              <th>Genre</th>
              <th>Language</th>
              <th>Rating</th>
              <th>Shows</th>
              <th>Bookings</th>
              <th>Occupancy</th>
              <th>Revenue</th>
              <th>Avg Price</th>
              <th>Theatres</th>
            </tr>
          </thead>
          <tbody>
            {movieAnalytics.map((m, i) => (
              <tr key={i}>
                <td className="movie-name">{m.movieName}</td>
                <td><span className="badge">{m.genre || '-'}</span></td>
                <td>{m.language || '-'}</td>
                <td><FaStar className="star-icon" /> {m.rating || '-'}</td>
                <td>{m.totalShows}</td>
                <td>{m.totalBookings} <span className="sub-text">({m.cancelledCount || 0} cancelled)</span></td>
                <td><span className={`occupancy-badge ${m.occupancyRate >= 70 ? 'high' : m.occupancyRate >= 40 ? 'med' : 'low'}`}>{m.occupancyRate}%</span></td>
                <td className="revenue">{formatCurrency(m.totalRevenue)}</td>
                <td>{formatCurrency(m.avgTicketPrice)}</td>
                <td>{m.theaterCount}</td>
              </tr>
            ))}
            {movieAnalytics.length === 0 && <tr><td colSpan="10" className="no-data">No movie data</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );

  // =====================================================
  // THEATRE RANKINGS TAB
  // =====================================================

  const renderTheaterRankings = () => (
    <div className="analytics-section">
      <div className="section-header">
        <h2><FaTrophy /> Theatre Performance Rankings</h2>
        <button className="export-btn" onClick={() => { setExportType('theaters'); setActiveTab('export'); }}><FaDownload /> Export</button>
      </div>

      <div className="filter-bar">
        <label>Sort by:</label>
        <select value={theaterSortBy} onChange={e => { setTheaterSortBy(e.target.value); }}>
          <option value="revenue">Revenue</option>
          <option value="occupancy">Occupancy</option>
          <option value="bookings">Bookings</option>
        </select>
        <button className="apply-btn" onClick={fetchTheaterRankings}><FaFilter /> Apply</button>
      </div>

      {theaterRankings.length > 0 && (
        <div className="chart-container">
          <ResponsiveContainer width="100%" height={350}>
            <ComposedChart data={theaterRankings.slice(0, 10)}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="theaterName" tick={{ fontSize: 11 }} angle={-15} textAnchor="end" height={60} />
              <YAxis yAxisId="left" tickFormatter={formatCurrency} />
              <YAxis yAxisId="right" orientation="right" domain={[0, 100]} unit="%" />
              <Tooltip />
              <Legend />
              <Bar yAxisId="left" dataKey="totalRevenue" fill="#e23744" name="Revenue" radius={[4, 4, 0, 0]} />
              <Line yAxisId="right" type="monotone" dataKey="occupancyRate" stroke="#3b82f6" name="Occupancy %" strokeWidth={2} />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      )}

      <div className="data-table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Rank</th>
              <th>Theatre</th>
              <th>City</th>
              <th>Shows</th>
              <th>Bookings</th>
              <th>Occupancy</th>
              <th>Revenue</th>
              <th>Movies</th>
              <th>Cancel Rate</th>
            </tr>
          </thead>
          <tbody>
            {theaterRankings.map((t, i) => (
              <tr key={i} className={i < 3 ? 'top-rank' : ''}>
                <td className="rank">
                  {t.rank <= 3 ? <FaTrophy className={`trophy rank-${t.rank}`} /> : t.rank}
                </td>
                <td className="theater-name">{t.theaterName}</td>
                <td>{t.cityName}</td>
                <td>{t.totalShows}</td>
                <td>{t.totalBookings}</td>
                <td><span className={`occupancy-badge ${t.occupancyRate >= 70 ? 'high' : t.occupancyRate >= 40 ? 'med' : 'low'}`}>{t.occupancyRate}%</span></td>
                <td className="revenue">{formatCurrency(t.totalRevenue)}</td>
                <td>{t.moviesShown}</td>
                <td><span className={`cancel-badge ${t.cancellationRate > 20 ? 'high' : ''}`}>{t.cancellationRate}%</span></td>
              </tr>
            ))}
            {theaterRankings.length === 0 && <tr><td colSpan="9" className="no-data">No theatre data</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );

  // =====================================================
  // HEATMAP TAB
  // =====================================================

  const getHeatmapColor = (value) => {
    if (value === 0) return '#f3f4f6';
    if (value < 25) return '#d1fae5';
    if (value < 50) return '#fef08a';
    if (value < 75) return '#fb923c';
    return '#dc2626';
  };

  const getHeatmapTextColor = (value) => {
    if (value >= 75) return '#ffffff';
    return '#1f2937';
  };

  const renderHeatmap = () => (
    <div className="analytics-section">
      <div className="section-header">
        <h2><FaTh /> Show Occupancy Heatmap</h2>
      </div>

      <div className="filter-bar">
        <select value={heatmapTheaterId} onChange={e => setHeatmapTheaterId(e.target.value)}>
          <option value="">All Theatres</option>
          {theaters.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select>
        <select value={heatmapMovieId} onChange={e => setHeatmapMovieId(e.target.value)}>
          <option value="">All Movies</option>
          {movies.map(m => <option key={m.id} value={m.id}>{m.movieName}</option>)}
        </select>
        <button className="apply-btn" onClick={fetchHeatmap}><FaFilter /> Apply</button>
      </div>

      {heatmapData && heatmapData.heatmapData && (
        <div className="heatmap-container">
          <div className="heatmap-grid">
            <div className="heatmap-header">
              <div className="heatmap-label"></div>
              {heatmapData.timeSlots && heatmapData.timeSlots.map((slot, i) => (
                <div key={i} className="heatmap-col-header">{slot}</div>
              ))}
            </div>
            {heatmapData.days && heatmapData.days.map((day, dayIdx) => (
              <div key={dayIdx} className="heatmap-row">
                <div className="heatmap-row-header">{day}</div>
                {heatmapData.timeSlots.map((slot, slotIdx) => {
                  const cell = heatmapData.heatmapData.find(c => c.day === day && c.timeSlot === slot);
                  const val = cell ? cell.avgOccupancy : 0;
                  const count = cell ? cell.showCount : 0;
                  return (
                    <div key={slotIdx} className="heatmap-cell"
                      style={{ backgroundColor: getHeatmapColor(val), color: getHeatmapTextColor(val) }}
                      title={`${day} ${slot}: ${val}% occupancy (${count} shows)`}>
                      <span className="cell-value">{val > 0 ? `${val}%` : '-'}</span>
                      <span className="cell-count">{count > 0 ? `${count} shows` : ''}</span>
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
          <div className="heatmap-legend">
            <span>Low</span>
            <div className="legend-scale">
              {[0, 25, 50, 75, 100].map(v => (
                <div key={v} className="legend-item" style={{ backgroundColor: getHeatmapColor(v), color: getHeatmapTextColor(v) }}>{v}%</div>
              ))}
            </div>
            <span>High</span>
          </div>
        </div>
      )}
    </div>
  );

  // =====================================================
  // USER ANALYTICS TAB
  // =====================================================

  const renderUserAnalytics = () => {
    if (!userAnalytics) return <div className="loading">Loading user analytics...</div>;
    const ua = userAnalytics;
    return (
      <div className="analytics-section">
        <div className="section-header">
          <h2><FaUsers /> User Analytics</h2>
          <button className="export-btn" onClick={() => { setExportType('users'); setActiveTab('export'); }}><FaDownload /> Export</button>
        </div>

        <div className="stats-grid small">
          <StatCard icon={<FaUsers />} label="Total Users" value={ua.totalUsers || 0} color="#3b82f6" />
          <StatCard icon={<FaToggleOn />} label="Active" value={ua.activeUsers || 0} color="#22c55e" />
          <StatCard icon={<FaToggleOff />} label="Inactive" value={ua.inactiveUsers || 0} color="#f43f5e" />
          <StatCard icon={<FaTicketAlt />} label="Avg Bookings/User" value={ua.avgBookingsPerUser || 0} color="#8b5cf6" />
        </div>

        <div className="charts-row">
          {ua.roleDistribution && Object.keys(ua.roleDistribution).length > 0 && (
            <div className="chart-container half">
              <h3>Role Distribution</h3>
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie data={Object.entries(ua.roleDistribution).map(([k, v]) => ({ name: k, value: v }))}
                    cx="50%" cy="50%" outerRadius={80} dataKey="value" label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
                    {Object.keys(ua.roleDistribution).map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
          {ua.genderDistribution && Object.keys(ua.genderDistribution).length > 0 && (
            <div className="chart-container half">
              <h3>Gender Distribution</h3>
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie data={Object.entries(ua.genderDistribution).map(([k, v]) => ({ name: k, value: v }))}
                    cx="50%" cy="50%" outerRadius={80} dataKey="value" label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
                    {Object.keys(ua.genderDistribution).map((_, i) => <Cell key={i} fill={COLORS[(i + 3) % COLORS.length]} />)}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        {ua.registrationTrend && ua.registrationTrend.length > 0 && (
          <div className="chart-container">
            <h3>User Registration Trend (Last 30 Days)</h3>
            <ResponsiveContainer width="100%" height={250}>
              <AreaChart data={ua.registrationTrend}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" tick={{ fontSize: 10 }} angle={-45} textAnchor="end" height={60} />
                <YAxis />
                <Tooltip />
                <Area type="monotone" dataKey="count" stroke="#8b5cf6" fill="#8b5cf680" name="Registrations" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}

        {ua.topUsers && ua.topUsers.length > 0 && (
          <div className="top-users-section">
            <h3><FaTrophy /> Top Users by Bookings</h3>
            <div className="data-table-wrapper">
              <table className="data-table">
                <thead>
                  <tr><th>#</th><th>Name</th><th>Email</th><th>Bookings</th><th>Total Spent</th></tr>
                </thead>
                <tbody>
                  {ua.topUsers.map((u, i) => (
                    <tr key={i}>
                      <td>{i + 1}</td>
                      <td>{u.name}</td>
                      <td>{u.email}</td>
                      <td>{u.bookingCount}</td>
                      <td className="revenue">{formatCurrency(u.totalSpent)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    );
  };

  // =====================================================
  // CHARTS TAB (Revenue, Occupancy, Cancellation, Distributions)
  // =====================================================

  const renderCharts = () => (
    <div className="analytics-section">
      <div className="section-header">
        <h2><FaChartBar /> Graph Analytics</h2>
      </div>

      <div className="filter-bar">
        <label>Period:</label>
        <select value={trendPeriod} onChange={e => setTrendPeriod(e.target.value)}>
          <option value="daily">Daily</option>
          <option value="weekly">Weekly</option>
          <option value="monthly">Monthly</option>
        </select>
        <input type="date" value={trendDateFrom} onChange={e => setTrendDateFrom(e.target.value)} />
        <input type="date" value={trendDateTo} onChange={e => setTrendDateTo(e.target.value)} />
        <button className="apply-btn" onClick={() => { fetchRevenueTrends(); fetchOccupancyTrends(); fetchCancellationTrends(); }}><FaFilter /> Apply</button>
      </div>

      {/* Revenue Trend */}
      <div className="chart-container">
        <h3><FaRupeeSign /> Revenue Trend</h3>
        <ResponsiveContainer width="100%" height={300}>
          <ComposedChart data={revenueTrends}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="period" tick={{ fontSize: 10 }} angle={-45} textAnchor="end" height={60} />
            <YAxis yAxisId="left" tickFormatter={formatCurrency} />
            <YAxis yAxisId="right" orientation="right" />
            <Tooltip formatter={(val, name) => name === 'Revenue' ? formatCurrency(val) : val} />
            <Legend />
            <Area yAxisId="left" type="monotone" dataKey="revenue" stroke="#22c55e" fill="#22c55e40" name="Revenue" />
            <Line yAxisId="right" type="monotone" dataKey="transactions" stroke="#3b82f6" name="Transactions" strokeWidth={2} />
          </ComposedChart>
        </ResponsiveContainer>
      </div>

      {/* Occupancy Trend */}
      <div className="chart-container">
        <h3><FaPercentage /> Occupancy Trend</h3>
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={occupancyTrends}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" tick={{ fontSize: 10 }} angle={-45} textAnchor="end" height={60} />
            <YAxis domain={[0, 100]} unit="%" />
            <Tooltip formatter={(val) => `${val}%`} />
            <Area type="monotone" dataKey="occupancyRate" stroke="#3b82f6" fill="#3b82f640" name="Occupancy %" />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      {/* Cancellation Trend */}
      <div className="chart-container">
        <h3><FaFireAlt /> Cancellation Trend</h3>
        <ResponsiveContainer width="100%" height={300}>
          <ComposedChart data={cancellationTrends}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" tick={{ fontSize: 10 }} angle={-45} textAnchor="end" height={60} />
            <YAxis yAxisId="left" />
            <YAxis yAxisId="right" orientation="right" tickFormatter={formatCurrency} />
            <Tooltip />
            <Legend />
            <Bar yAxisId="left" dataKey="cancellations" fill="#f43f5e" name="Cancellations" radius={[4, 4, 0, 0]} />
            <Bar yAxisId="left" dataKey="totalBookings" fill="#3b82f640" name="Total Bookings" radius={[4, 4, 0, 0]} />
            <Line yAxisId="right" type="monotone" dataKey="refundAmount" stroke="#f97316" name="Refund Amount" strokeWidth={2} />
          </ComposedChart>
        </ResponsiveContainer>
      </div>

      {/* Distribution Charts */}
      {distributions && (
        <>
          {distributions.genreDistribution && distributions.genreDistribution.length > 0 && (
            <div className="charts-row">
              <div className="chart-container half">
                <h3>Revenue by Genre</h3>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie data={distributions.genreDistribution.map(g => ({ name: g.genre, value: g.revenue }))}
                      cx="50%" cy="50%" outerRadius={100} dataKey="value"
                      label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
                      {distributions.genreDistribution.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                    </Pie>
                    <Tooltip formatter={(val) => formatCurrency(val)} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <div className="chart-container half">
                <h3>Revenue by Language</h3>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie data={(distributions.languageDistribution || []).map(l => ({ name: l.language, value: l.revenue }))}
                      cx="50%" cy="50%" outerRadius={100} dataKey="value"
                      label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
                      {(distributions.languageDistribution || []).map((_, i) => <Cell key={i} fill={COLORS[(i + 3) % COLORS.length]} />)}
                    </Pie>
                    <Tooltip formatter={(val) => formatCurrency(val)} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            </div>
          )}

          {distributions.paymentMethodDistribution && distributions.paymentMethodDistribution.length > 0 && (
            <div className="chart-container">
              <h3>Payment Methods</h3>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={distributions.paymentMethodDistribution}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="method" tick={{ fontSize: 11 }} />
                  <YAxis tickFormatter={formatCurrency} />
                  <Tooltip formatter={(val) => formatCurrency(val)} />
                  <Bar dataKey="revenue" fill="#8b5cf6" name="Revenue" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </>
      )}
    </div>
  );

  // =====================================================
  // EXPORT TAB
  // =====================================================

  const renderExport = () => (
    <div className="analytics-section">
      <div className="section-header">
        <h2><FaDownload /> Export Data (CSV)</h2>
      </div>

      <div className="export-form">
        <div className="form-group">
          <label>Data Type</label>
          <select value={exportType} onChange={e => setExportType(e.target.value)}>
            <option value="revenue">Revenue Report</option>
            <option value="bookings">Bookings Report</option>
            <option value="movies">Movie Analytics</option>
            <option value="theaters">Theatre Analytics</option>
            <option value="users">User Data</option>
            <option value="cities">City Analytics</option>
          </select>
        </div>
        <div className="form-group">
          <label>Date From (Optional)</label>
          <input type="date" value={exportDateFrom} onChange={e => setExportDateFrom(e.target.value)} />
        </div>
        <div className="form-group">
          <label>Date To (Optional)</label>
          <input type="date" value={exportDateTo} onChange={e => setExportDateTo(e.target.value)} />
        </div>
        <button className="export-download-btn" onClick={handleExport} disabled={loading}>
          <FaDownload /> {loading ? 'Exporting...' : 'Download CSV'}
        </button>
      </div>

      <div className="export-info">
        <h4>Available Export Types:</h4>
        <ul>
          <li><strong>Revenue Report:</strong> Transaction details, amounts, payment methods, movie/theatre info</li>
          <li><strong>Bookings Report:</strong> Ticket details, seats, prices, status, cancellations</li>
          <li><strong>Movie Analytics:</strong> Revenue, bookings, occupancy per movie</li>
          <li><strong>Theatre Analytics:</strong> Revenue, rankings, occupancy per theatre</li>
          <li><strong>User Data:</strong> User details, booking counts, wallet balance</li>
          <li><strong>City Analytics:</strong> City-wise theatre count, revenue, bookings</li>
        </ul>
      </div>
    </div>
  );

  // =====================================================
  // MANAGEMENT TABS (Movies, Theatres, Shows, Users, Cities, Seats)
  // =====================================================

  const renderManageMovies = () => (
    <div className="management-section">
      <div className="section-header">
        <h2><FaFilm /> Manage Movies</h2>
        <button className="add-btn" onClick={() => openMovieModal()}><FaPlus /> Add Movie</button>
      </div>
      <div className="data-table-wrapper">
        <table className="data-table">
          <thead>
            <tr><th>ID</th><th>Poster</th><th>Name</th><th>Genre</th><th>Language</th><th>Rating</th><th>Duration</th><th>Release Date</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {movies.map(m => (
              <tr key={m.id}>
                <td>{m.id}</td>
                <td>{m.posterUrl ? <img src={m.posterUrl} alt={m.movieName} style={{ width: '40px', height: '56px', objectFit: 'cover', borderRadius: '4px' }} /> : <span style={{color:'#9ca3af'}}>—</span>}</td>
                <td>{m.movieName}</td>
                <td><span className="badge">{m.genre}</span></td>
                <td>{m.language}</td>
                <td><FaStar className="star-icon" /> {m.rating}</td>
                <td>{m.duration} min</td>
                <td>{m.releaseDate}</td>
                <td className="actions">
                  <button className="edit-btn" onClick={() => openMovieModal(m)}><FaEdit /></button>
                  <button className="delete-btn" onClick={() => handleDelete('movie', m.id)}><FaTrash /></button>
                </td>
              </tr>
            ))}
            {movies.length === 0 && <tr><td colSpan="9" className="no-data">No movies</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );

  const renderManageTheaters = () => (
    <div className="management-section">
      <div className="section-header">
        <h2><FaTheaterMasks /> Manage Theatres</h2>
        <button className="add-btn" onClick={() => openTheaterModal()}><FaPlus /> Add Theatre</button>
      </div>
      <div className="data-table-wrapper">
        <table className="data-table">
          <thead>
            <tr><th>ID</th><th>Name</th><th>Address</th><th>City</th><th>Admin</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {theaters.map(t => (
              <tr key={t.id}>
                <td>{t.id}</td>
                <td>{t.name}</td>
                <td>{t.address}</td>
                <td>{t.city?.name || t.cityName || '-'}</td>
                <td>{t.admin?.name || <span style={{color:'#9ca3af'}}>Unassigned</span>}</td>
                <td className="actions">
                  <button className="edit-btn" onClick={() => openTheaterModal(t)}><FaEdit /></button>
                  <button className="delete-btn" onClick={() => handleDelete('theater', t.id)}><FaTrash /></button>
                </td>
              </tr>
            ))}
            {theaters.length === 0 && <tr><td colSpan="6" className="no-data">No theatres</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );

  const renderManageShows = () => {
    const upcomingShows = shows.filter(s => new Date(s.date) >= new Date());
    const uniqueMovies = [...new Set(shows.map(s => s.movie?.id).filter(Boolean))];
    const theatersWithShows = [...new Set(shows.map(s => s.theater?.id).filter(Boolean))];

    return (
      <div className="management-section">
        <div className="section-header">
          <h2><FaTicketAlt /> Show Analytics</h2>
          <span className="subtitle" style={{ fontSize: '0.85rem', color: '#6b7280', marginLeft: '1rem' }}>View only — managed by Theatre Admins</span>
        </div>

        <div className="stats-grid">
          <StatCard icon={<FaTicketAlt />} label="Total Shows" value={shows.length} color="#e23744" />
          <StatCard icon={<FaCalendarAlt />} label="Upcoming" value={upcomingShows.length} color="#22c55e" />
          <StatCard icon={<FaFilm />} label="Unique Movies" value={uniqueMovies.length} color="#3b82f6" />
          <StatCard icon={<FaTheaterMasks />} label="Theatres with Shows" value={theatersWithShows.length} color="#8b5cf6" />
        </div>

        <div className="data-table-wrapper">
          <table className="data-table">
            <thead>
              <tr><th>ID</th><th>Movie</th><th>Theatre</th><th>City</th><th>Date</th><th>Time</th></tr>
            </thead>
            <tbody>
              {shows.map(s => (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td>{s.movie?.movieName || '-'}</td>
                  <td>{s.theater?.name || '-'}</td>
                  <td>{s.theater?.cityName || s.theater?.city?.name || '-'}</td>
                  <td>{s.date}</td>
                  <td>{s.time}</td>
                </tr>
              ))}
              {shows.length === 0 && <tr><td colSpan="6" className="no-data">No shows</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  const renderManageUsers = () => {
    // Role distribution for PieChart
    const roleCounts = {};
    users.forEach(u => { const role = u.role || 'USER'; roleCounts[role] = (roleCounts[role] || 0) + 1; });
    const roleData = Object.entries(roleCounts).map(([name, value]) => ({ name, value }));

    // Wallet balance distribution for BarChart (top 10 users by wallet)
    const walletData = [...users]
      .sort((a, b) => (b.walletBalance || 0) - (a.walletBalance || 0))
      .slice(0, 10)
      .map(u => ({ name: u.name?.split(' ')[0] || `#${u.id}`, wallet: u.walletBalance || 0 }));

    // Stats
    const totalUsers = users.length;
    const activeUsers = users.filter(u => u.isActive).length;
    const inactiveUsers = totalUsers - activeUsers;
    const totalWalletBalance = users.reduce((sum, u) => sum + (u.walletBalance || 0), 0);

    return (
      <div className="management-section">
        <div className="section-header">
          <h2><FaUsers /> Manage Users</h2>
          <span className="subtitle" style={{ fontSize: '0.85rem', color: '#6b7280', marginLeft: '1rem' }}>Click a user row to view spending & booking analytics</span>
        </div>

        {/* Stats Cards */}
        <div className="stats-grid">
          <StatCard icon={<FaUsers />} label="Total Users" value={totalUsers} color="#3b82f6" />
          <StatCard icon={<FaToggleOn />} label="Active" value={activeUsers} color="#22c55e" />
          <StatCard icon={<FaToggleOff />} label="Inactive" value={inactiveUsers} color="#e23744" />
          <StatCard icon={<FaWallet />} label="Total Wallet Balance" value={formatCurrency(totalWalletBalance)} color="#8b5cf6" />
        </div>

        {/* Charts Row */}
        {users.length > 0 && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
            {/* Role Distribution Pie Chart */}
            <div className="chart-card" style={{ background: '#fff', borderRadius: '12px', padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
              <h3 style={{ fontSize: '1rem', marginBottom: '0.75rem', color: '#1f2937' }}><FaChartPie style={{ marginRight: '0.5rem' }} />Role Distribution</h3>
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie data={roleData} cx="50%" cy="50%" outerRadius={80} dataKey="value" label={({ name, percent }) => `${name} (${(percent * 100).toFixed(0)}%)`}>
                    {roleData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* Top Wallet Balances Bar Chart */}
            <div className="chart-card" style={{ background: '#fff', borderRadius: '12px', padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
              <h3 style={{ fontSize: '1rem', marginBottom: '0.75rem', color: '#1f2937' }}><FaChartBar style={{ marginRight: '0.5rem' }} />Top Wallet Balances</h3>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={walletData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                  <YAxis tickFormatter={v => formatCurrency(v)} tick={{ fontSize: 11 }} />
                  <Tooltip formatter={v => formatCurrency(v)} />
                  <Bar dataKey="wallet" fill="#8b5cf6" radius={[6, 6, 0, 0]} name="Wallet Balance" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        )}

        {/* User Table */}
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead>
              <tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Active</th><th>Wallet</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id} onClick={() => handleUserDetailClick(u.id)} style={{ cursor: 'pointer' }} title="Click to view analytics">
                  <td>{u.id}</td>
                  <td>{u.name}</td>
                  <td>{u.emailId}</td>
                  <td><span className={`role-badge ${u.role?.toLowerCase()}`}>{u.role}</span></td>
                  <td>
                    <button className={`toggle-btn ${u.isActive ? 'active' : 'inactive'}`}
                      onClick={(e) => { e.stopPropagation(); handleToggleUserStatus(u.id, u.isActive); }}>
                      {u.isActive ? <FaToggleOn /> : <FaToggleOff />}
                    </button>
                  </td>
                  <td>{formatCurrency(u.walletBalance || 0)}</td>
                  <td className="actions">
                    {u.role !== 'ADMIN' && (
                      <button className="delete-btn" onClick={(e) => { e.stopPropagation(); handleDelete('user', u.id); }}><FaTrash /></button>
                    )}
                  </td>
                </tr>
              ))}
              {users.length === 0 && <tr><td colSpan="7" className="no-data">No users</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  const renderManageCities = () => (
    <div className="management-section">
      <div className="section-header">
        <h2><FaCity /> Manage Cities</h2>
        <button className="add-btn" onClick={() => { setCurrentItem(null); setFormData({ name: '', state: '', country: 'India' }); setModalType('city'); setShowModal(true); }}><FaPlus /> Add City</button>
      </div>
      <div className="data-table-wrapper">
        <table className="data-table">
          <thead>
            <tr><th>ID</th><th>Name</th><th>State</th><th>Country</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {cities.map(c => (
              <tr key={c.id}>
                <td>{c.id}</td>
                <td>{c.name}</td>
                <td>{c.state || '-'}</td>
                <td>{c.country || '-'}</td>
                <td className="actions">
                  <button className="edit-btn" onClick={() => { setCurrentItem(c); setFormData({ name: c.name, state: c.state || '', country: c.country || '' }); setModalType('city'); setShowModal(true); }}><FaEdit /></button>
                  <button className="delete-btn" onClick={() => handleDelete('city', c.id)}><FaTrash /></button>
                </td>
              </tr>
            ))}
            {cities.length === 0 && <tr><td colSpan="5" className="no-data">No cities</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );

  const renderManageSeats = () => {
    // Seat type breakdown from theater seats
    const seatTypeCount = {};
    theaterSeats.forEach(s => { seatTypeCount[s.seatType] = (seatTypeCount[s.seatType] || 0) + 1; });
    const seatTypeData = Object.entries(seatTypeCount).map(([type, count]) => ({ type, count }));

    // Show seat occupancy
    const totalShowSeats = showSeats.length;
    const bookedShowSeats = showSeats.filter(s => !s.isAvailable).length;
    const occupancyRate = totalShowSeats > 0 ? ((bookedShowSeats / totalShowSeats) * 100).toFixed(1) : 0;

    return (
      <div className="management-section">
        <div className="section-header">
          <h2><FaChair /> Seat Analytics</h2>
          <span className="subtitle" style={{ fontSize: '0.85rem', color: '#6b7280', marginLeft: '1rem' }}>View only — managed by Theatre Admins</span>
        </div>

        <div className="stats-grid">
          <StatCard icon={<FaChair />} label="Theatre Seats Loaded" value={theaterSeats.length} color="#3b82f6" />
          <StatCard icon={<FaChair />} label="Show Seats Loaded" value={totalShowSeats} color="#8b5cf6" />
          <StatCard icon={<FaPercentage />} label="Occupancy Rate" value={`${occupancyRate}%`} color="#e23744" />
          <StatCard icon={<FaTicketAlt />} label="Booked Seats" value={bookedShowSeats} color="#22c55e" />
        </div>

        <div className="seats-section">
          <h3>Theatre Seats (View Only)</h3>
          <div className="filter-bar">
            <select value={selectedTheaterId} onChange={e => { setSelectedTheaterId(e.target.value); if (e.target.value) fetchTheaterSeats(e.target.value); }}>
              <option value="">Select Theatre</option>
              {theaters.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          </div>
          {selectedTheaterId && seatTypeData.length > 0 && (
            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
              {seatTypeData.map(st => (
                <span key={st.type} className="badge" style={{ padding: '0.3rem 0.75rem' }}>{st.type}: {st.count}</span>
              ))}
            </div>
          )}
          {selectedTheaterId && (
            <div className="data-table-wrapper">
              <table className="data-table compact">
                <thead><tr><th>Seat No</th><th>Type</th></tr></thead>
                <tbody>
                  {theaterSeats.map(s => (
                    <tr key={s.id}><td>{s.seatNo}</td><td><span className="badge">{s.seatType}</span></td></tr>
                  ))}
                  {theaterSeats.length === 0 && <tr><td colSpan="2" className="no-data">No seats</td></tr>}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="seats-section">
          <h3>Show Seats (View Only)</h3>
          <div className="filter-bar">
            <select value={selectedShowId} onChange={e => { setSelectedShowId(e.target.value); if (e.target.value) fetchShowSeats(e.target.value); }}>
              <option value="">Select Show</option>
              {shows.map(s => <option key={s.id} value={s.id}>#{s.id} - {s.movie?.movieName || 'Unknown'} @ {s.theater?.name || 'Unknown'} ({s.date})</option>)}
            </select>
          </div>
          {selectedShowId && (
            <div className="data-table-wrapper">
              <table className="data-table compact">
                <thead><tr><th>Seat No</th><th>Type</th><th>Price</th><th>Available</th></tr></thead>
                <tbody>
                  {showSeats.map(s => (
                    <tr key={s.id}>
                      <td>{s.seatNo}</td>
                      <td><span className="badge">{s.seatType}</span></td>
                      <td>{formatCurrency(s.price)}</td>
                      <td><span className={`status-dot ${s.isAvailable ? 'available' : 'booked'}`}>{s.isAvailable ? 'Yes' : 'No'}</span></td>
                    </tr>
                  ))}
                  {showSeats.length === 0 && <tr><td colSpan="4" className="no-data">No seats</td></tr>}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    );
  };

  // =====================================================
  // NEW TAB: THEATRE ADMINS
  // =====================================================

  const handleAssignAdmin = async (e) => {
    e.preventDefault();
    try {
      await adminService.assignTheatreAdmin(parseInt(assignAdminForm.theaterId), parseInt(assignAdminForm.adminUserId));
      alert('Theatre admin assigned successfully!');
      setAssignAdminForm({ theaterId: '', adminUserId: '' });
      fetchTheatreAdmins();
      fetchTheaters();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const handleRemoveAdmin = async (theaterId) => {
    if (!window.confirm('Remove admin from this theatre?')) return;
    try {
      await adminService.removeTheatreAdmin(theaterId);
      alert('Admin removed successfully!');
      fetchTheatreAdmins();
      fetchTheaters();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const renderManageTheatreAdmins = () => {
    const theaterOwners = users.filter(u => u.role === 'THEATER_OWNER');
    const theatersWithAdmin = theaters.filter(t => t.admin);
    const theatersWithoutAdmin = theaters.filter(t => !t.admin);

    // Filter theatres by selected city
    const filteredTheatres = taCity
      ? theaters.filter(t => t.city?.id?.toString() === taCity)
      : theaters;
    const filteredWithAdmin = filteredTheatres.filter(t => t.admin);
    const filteredWithoutAdmin = filteredTheatres.filter(t => !t.admin);

    return (
      <div className="tab-content">
        <div className="section-header"><h2><FaUserMinus /> Theatre Admin Management</h2></div>

        <div className="stats-grid">
          <StatCard icon={<FaUserMinus />} label="Theatre Admins" value={theatreAdmins.length} color="#8b5cf6" />
          <StatCard icon={<FaTheaterMasks />} label="Assigned Theatres" value={theatersWithAdmin.length} color="#22c55e" />
          <StatCard icon={<FaTheaterMasks />} label="Unassigned Theatres" value={theatersWithoutAdmin.length} color="#f97316" />
        </div>

        {/* City Filter */}
        <div className="filter-bar" style={{marginBottom:'1rem'}}>
          <label>Filter by City:</label>
          <select value={taCity} onChange={e => { setTaCity(e.target.value); setAssignAdminForm({ theaterId: '', adminUserId: '' }); }}>
            <option value="">All Cities</option>
            {cities.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>

        {/* Assignment Form */}
        <div className="section-header" style={{marginTop:'0.5rem'}}><h3><FaPlus /> Assign Admin to Theatre</h3></div>
        <form onSubmit={handleAssignAdmin} className="filter-bar" style={{marginBottom:'1.5rem'}}>
          <select value={assignAdminForm.theaterId} onChange={e => setAssignAdminForm(p => ({...p, theaterId: e.target.value}))} required>
            <option value="">Select Theatre...</option>
            {filteredWithoutAdmin.map(t => (
              <option key={t.id} value={t.id}>{t.name} ({t.city?.name || 'N/A'})</option>
            ))}
          </select>
          <select value={assignAdminForm.adminUserId} onChange={e => setAssignAdminForm(p => ({...p, adminUserId: e.target.value}))} required>
            <option value="">Select Theatre Admin User...</option>
            {theaterOwners.map(u => (
              <option key={u.id} value={u.id}>{u.name} ({u.email})</option>
            ))}
          </select>
          <button type="submit" className="btn-primary"><FaPlus /> Assign</button>
        </form>

        {/* All Theatres in Selected City with Status */}
        <div className="section-header"><h3><FaTheaterMasks /> Theatres {taCity ? `in ${cities.find(c => c.id?.toString() === taCity)?.name || 'Selected City'}` : '(All Cities)'}</h3></div>
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>Theatre</th><th>City</th><th>Admin</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {filteredTheatres.length > 0 ? filteredTheatres.map(t => (
                <tr key={t.id}>
                  <td style={{ fontWeight: 600 }}>{t.name}</td>
                  <td>{t.city?.name || 'N/A'}</td>
                  <td>{t.admin?.name || t.admin?.email || '-'}</td>
                  <td><span className={`status-badge ${t.admin ? 'confirmed' : 'pending'}`}>{t.admin ? 'Assigned' : 'Not Assigned'}</span></td>
                  <td>
                    {t.admin ? (
                      <button className="btn-danger" onClick={() => handleRemoveAdmin(t.id)}><FaTrash /> Remove</button>
                    ) : (
                      <span style={{ color: '#a0a0b0', fontSize: '0.82rem' }}>Select above to assign</span>
                    )}
                  </td>
                </tr>
              )) : <tr><td colSpan="5" className="no-data">No theatres found</td></tr>}
            </tbody>
          </table>
        </div>

        {/* Current Theatre Admin Users */}
        <div className="section-header" style={{marginTop:'1.5rem'}}><h3><FaUsers /> Theatre Admin Users</h3></div>
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Assigned Theatre</th><th>City</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {theatreAdmins.length > 0 ? theatreAdmins.map((ta, i) => (
                <tr key={i}>
                  <td style={{ fontWeight: 600, color: '#6366f1' }}>#{ta.id}</td>
                  <td>{ta.name}</td>
                  <td>{ta.email}</td>
                  <td>{ta.assignedTheatre?.name || '-'}</td>
                  <td>{ta.assignedTheatre?.cityName || '-'}</td>
                  <td><span className={`status-badge ${ta.assignedTheatre ? 'confirmed' : 'pending'}`}>{ta.assignedTheatre ? 'Assigned' : 'Not Assigned'}</span></td>
                  <td>
                    {ta.assignedTheatre && <button className="btn-danger" onClick={() => handleRemoveAdmin(ta.assignedTheatre.id)}>
                      <FaTrash /> Remove
                    </button>}
                  </td>
                </tr>
              )) : <tr><td colSpan="7" className="no-data">No theatre admins found</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  // =====================================================
  // NEW TAB: RECOMMENDATIONS
  // =====================================================

  const handleRecommendMovie = async (e) => {
    e.preventDefault();
    try {
      if (recForm.mode === 'theatre') {
        await adminService.recommendMovie(parseInt(recForm.movieId), parseInt(recForm.theaterId), recForm.message);
        alert('Movie recommended to theatre successfully!');
      } else {
        await adminService.recommendMovieToCity(parseInt(recForm.movieId), parseInt(recForm.cityId), recForm.message);
        alert('Movie recommended to all theatres in city!');
      }
      setRecForm({ movieId: '', theaterId: '', cityId: '', message: '', mode: recForm.mode });
      fetchRecommendations();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const renderManageRecommendations = () => (
    <div className="tab-content">
      <div className="section-header"><h2><FaStar /> Movie Recommendations</h2></div>

      <div className="stats-grid">
        <StatCard icon={<FaStar />} label="Total Recommendations" value={recommendations.length} color="#eab308" />
        <StatCard icon={<FaFilm />} label="Movies Available" value={movies.length} color="#3b82f6" />
        <StatCard icon={<FaTheaterMasks />} label="Theatres" value={theaters.length} color="#22c55e" />
      </div>

      {/* Recommendation Form */}
      <div className="section-header" style={{marginTop:'1.5rem'}}><h3>Recommend a Movie</h3></div>
      <form onSubmit={handleRecommendMovie} className="filter-bar" style={{marginBottom:'1.5rem', flexWrap:'wrap', gap:'0.5rem'}}>
        <select value={recForm.mode} onChange={e => setRecForm(p => ({...p, mode: e.target.value}))}>
          <option value="theatre">To Specific Theatre</option>
          <option value="city">To All Theatres in City</option>
        </select>
        <select value={recForm.movieId} onChange={e => setRecForm(p => ({...p, movieId: e.target.value}))} required>
          <option value="">Select Movie...</option>
          {movies.map(m => <option key={m.id} value={m.id}>{m.movieName || m.name}</option>)}
        </select>
        {recForm.mode === 'theatre' ? (
          <select value={recForm.theaterId} onChange={e => setRecForm(p => ({...p, theaterId: e.target.value}))} required>
            <option value="">Select Theatre...</option>
            {theaters.map(t => <option key={t.id} value={t.id}>{t.name} ({t.city?.name || 'N/A'})</option>)}
          </select>
        ) : (
          <select value={recForm.cityId} onChange={e => setRecForm(p => ({...p, cityId: e.target.value}))} required>
            <option value="">Select City...</option>
            {cities.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        )}
        <input type="text" placeholder="Message (optional)" value={recForm.message}
          onChange={e => setRecForm(p => ({...p, message: e.target.value}))} style={{minWidth:'200px'}} />
        <button type="submit" className="btn-primary"><FaPlus /> Recommend</button>
      </form>

      {/* Existing Recommendations */}
      <div className="section-header"><h3>All Recommendations</h3></div>
      <div className="data-table-wrapper">
        <table className="data-table">
          <thead><tr><th>ID</th><th>Movie</th><th>Theatre</th><th>Status</th><th>Message</th><th>Date</th></tr></thead>
          <tbody>
            {recommendations.length > 0 ? recommendations.map((r, i) => (
              <tr key={i}>
                <td>{r.id}</td>
                <td>{r.movieName || r.movie?.movieName || 'N/A'}</td>
                <td>{r.theaterName || r.theater?.name || 'N/A'}</td>
                <td><span className={`status-badge ${(r.status || 'PENDING').toLowerCase()}`}>{r.status || 'PENDING'}</span></td>
                <td>{r.message || '-'}</td>
                <td>{r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '-'}</td>
              </tr>
            )) : <tr><td colSpan="6" style={{textAlign:'center'}}>No recommendations found</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );

  // =====================================================
  // NEW TAB: BOOKINGS & PAYMENTS
  // =====================================================

  const handleWalletAdjust = async (userId) => {
    const amount = prompt('Enter amount (positive to credit, negative to debit):');
    if (!amount) return;
    const reason = prompt('Enter reason:') || 'Admin adjustment';
    try {
      await adminService.adjustUserWallet(userId, parseFloat(amount), reason);
      alert('Wallet adjusted successfully!');
      fetchWalletTransactions();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const renderBookingsPayments = () => {
    const totalRevenue = payments.reduce((sum, p) => sum + (p.amount || 0), 0);
    const totalWalletUsed = payments.reduce((sum, p) => sum + (p.walletAmount || 0), 0);
    const totalCardUsed = payments.reduce((sum, p) => sum + (p.cardAmount || 0), 0);
    const totalDiscount = payments.reduce((sum, p) => sum + (p.discountAmount || 0), 0);
    const totalTax = payments.reduce((sum, p) => sum + (p.tax || 0), 0);
    const totalConvFee = payments.reduce((sum, p) => sum + (p.convenienceFee || 0), 0);
    const activeBookings = bookings.filter(b => (b.status || 'CONFIRMED') === 'CONFIRMED').length;
    const cancelledBookings = bookings.filter(b => (b.status || '') === 'CANCELLED').length;

    // Payment method distribution for PieChart
    const methodCounts = {};
    payments.forEach(p => { const m = p.paymentMethod || p.method || 'Unknown'; methodCounts[m] = (methodCounts[m] || 0) + 1; });
    const methodData = Object.entries(methodCounts).map(([name, value]) => ({ name: name.replace('_', ' '), value }));

    // Revenue breakdown for PieChart
    const revenueBreakdown = [
      { name: 'Card', value: totalCardUsed },
      { name: 'Wallet', value: totalWalletUsed },
    ].filter(d => d.value > 0);

    // Booking status for PieChart
    const bookingStatusData = [
      { name: 'Confirmed', value: activeBookings },
      { name: 'Cancelled', value: cancelledBookings },
    ].filter(d => d.value > 0);

    // Daily revenue trend (last 7 entries)
    const dailyRevenue = {};
    payments.forEach(p => {
      if (p.createdAt) {
        const day = new Date(p.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' });
        dailyRevenue[day] = (dailyRevenue[day] || 0) + (p.amount || 0);
      }
    });
    const revenueTrendData = Object.entries(dailyRevenue).slice(-7).map(([date, revenue]) => ({ date, revenue }));

    const cardStyle = { background: '#16213e', borderRadius: '10px', padding: '1.25rem', border: '1px solid #2a2a4a' };
    const sectionTitle = { fontSize: '1rem', marginBottom: '0.75rem', color: '#fff', display: 'flex', alignItems: 'center', gap: '0.5rem' };

    return (
      <div className="tab-content">
        <div className="section-header"><h2><FaWallet /> Bookings, Payments & Wallet</h2></div>

        {/* Stats Row */}
        <div className="stats-grid">
          <StatCard icon={<FaTicketAlt />} label="Total Bookings" value={bookings.length} color="#e23744" />
          <StatCard icon={<FaTicketAlt />} label="Active" value={activeBookings} color="#22c55e" />
          <StatCard icon={<FaTicketAlt />} label="Cancelled" value={cancelledBookings} color="#f97316" />
          <StatCard icon={<FaRupeeSign />} label="Total Revenue" value={formatCurrency(totalRevenue)} color="#8b5cf6" />
          <StatCard icon={<FaWallet />} label="Wallet Used" value={formatCurrency(totalWalletUsed)} color="#3b82f6" />
          <StatCard icon={<FaRupeeSign />} label="Card Used" value={formatCurrency(totalCardUsed)} color="#06b6d4" />
          <StatCard icon={<FaPercentage />} label="Tax Collected" value={formatCurrency(totalTax)} color="#eab308" />
          <StatCard icon={<FaArrowDown />} label="Discounts Given" value={formatCurrency(totalDiscount)} color="#ec4899" />
        </div>

        {/* Charts Row */}
        {(payments.length > 0 || bookings.length > 0) && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem', margin: '1.5rem 0' }}>
            {/* Revenue Trend */}
            {revenueTrendData.length > 1 && (
              <div style={cardStyle}>
                <h3 style={sectionTitle}><FaChartLine /> Revenue Trend</h3>
                <ResponsiveContainer width="100%" height={220}>
                  <AreaChart data={revenueTrendData}>
                    <defs>
                      <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.3} />
                        <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                      </linearGradient>
                    </defs>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                    <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#a0a0b0' }} />
                    <YAxis tickFormatter={v => formatCurrency(v)} tick={{ fontSize: 11, fill: '#a0a0b0' }} />
                    <Tooltip formatter={v => formatCurrency(v)} />
                    <Area type="monotone" dataKey="revenue" stroke="#8b5cf6" fill="url(#revGrad)" strokeWidth={2} name="Revenue" />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            )}
            {/* Payment Method Distribution */}
            {methodData.length > 0 && (
              <div style={cardStyle}>
                <h3 style={sectionTitle}><FaChartPie /> Payment Methods</h3>
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie data={methodData} cx="50%" cy="50%" outerRadius={70} dataKey="value" label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`} labelLine={{ stroke: '#a0a0b0' }}>
                      {methodData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                    </Pie>
                    <Tooltip />
                    <Legend wrapperStyle={{ fontSize: 12, color: '#a0a0b0' }} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            )}
            {/* Revenue Split */}
            {revenueBreakdown.length > 0 && (
              <div style={cardStyle}>
                <h3 style={sectionTitle}><FaWallet /> Revenue Split (Card vs Wallet)</h3>
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie data={revenueBreakdown} cx="50%" cy="50%" innerRadius={40} outerRadius={70} dataKey="value" label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`} labelLine={{ stroke: '#a0a0b0' }}>
                      <Cell fill="#06b6d4" />
                      <Cell fill="#3b82f6" />
                    </Pie>
                    <Tooltip formatter={v => formatCurrency(v)} />
                    <Legend wrapperStyle={{ fontSize: 12, color: '#a0a0b0' }} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>
        )}

        {/* Revenue Summary Bar */}
        <div style={{ background: 'linear-gradient(135deg, #1e1b4b 0%, #312e81 100%)', borderRadius: '12px', padding: '1.25rem 1.5rem', margin: '0 0 1.5rem', color: '#fff', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '1rem' }}>
          {[
            { label: 'Gross Revenue', val: totalRevenue, color: '#a78bfa' },
            { label: 'Base Amount', val: totalRevenue - totalTax - totalConvFee, color: '#67e8f9' },
            { label: 'Tax Collected', val: totalTax, color: '#fbbf24' },
            { label: 'Conv. Fees', val: totalConvFee, color: '#fb923c' },
            { label: 'Discounts', val: totalDiscount, color: '#f472b6' },
          ].map((item, i) => (
            <div key={i} style={{ textAlign: 'center' }}>
              <div style={{ fontSize: '0.75rem', opacity: 0.8, marginBottom: '0.25rem', textTransform: 'uppercase', letterSpacing: '0.5px' }}>{item.label}</div>
              <div style={{ fontSize: '1.35rem', fontWeight: 700, color: item.color }}>{formatCurrency(item.val)}</div>
            </div>
          ))}
        </div>

        {/* Bookings Table */}
        <div style={{ ...cardStyle, marginBottom: '1.5rem' }}>
          <h3 style={sectionTitle}><FaTicketAlt /> Recent Bookings <span style={{ fontSize: '0.75rem', color: '#9ca3af', fontWeight: 400 }}>({bookings.length} total)</span></h3>
          <div className="data-table-wrapper" style={{ maxHeight: '400px', overflowY: 'auto' }}>
            <table className="data-table">
              <thead><tr><th>ID</th><th>User</th><th>Movie</th><th>Theatre</th><th>City</th><th>Seats</th><th>Amount</th><th>Status</th><th>Show Date</th><th>Booked At</th></tr></thead>
              <tbody>
                {bookings.length > 0 ? bookings.slice(0, 50).map(b => (
                  <tr key={b.id}>
                    <td style={{ fontWeight: 600, color: '#6366f1' }}>#{b.id}</td>
                    <td>{b.userName || b.user?.name || 'N/A'}</td>
                    <td style={{ fontWeight: 500 }}>{b.movieName || b.show?.movie?.movieName || 'N/A'}</td>
                    <td>{b.theaterName || b.show?.theater?.name || 'N/A'}</td>
                    <td>{b.cityName || '-'}</td>
                    <td><span style={{ background: 'rgba(255,255,255,0.08)', padding: '2px 8px', borderRadius: '4px', fontSize: '0.85rem' }}>{b.bookedSeats?.map ? b.bookedSeats.map(s => s.seatNo || s).join(', ') : (b.seatCount || '-')}</span></td>
                    <td style={{ fontWeight: 600 }}>{formatCurrency(b.totalAmount || b.amount || 0)}</td>
                    <td><span className={`status-badge ${(b.status || 'CONFIRMED').toLowerCase()}`}>{b.status || 'CONFIRMED'}</span></td>
                    <td>{b.showDate || b.show?.date || '-'} {b.showTime || b.show?.time || ''}</td>
                    <td style={{ fontSize: '0.82rem', color: '#a0a0b0' }}>{b.bookedAt ? new Date(b.bookedAt).toLocaleString() : '-'}</td>
                  </tr>
                )) : <tr><td colSpan="10" style={{textAlign:'center', padding: '2rem', color: '#9ca3af'}}>No bookings found</td></tr>}
              </tbody>
            </table>
          </div>
        </div>

        {/* Payments Table with Split Details */}
        <div style={{ ...cardStyle, marginBottom: '1.5rem' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem', flexWrap: 'wrap', gap: '0.5rem' }}>
            <h3 style={{ ...sectionTitle, marginBottom: 0 }}><FaRupeeSign /> Payments <span style={{ fontSize: '0.75rem', color: '#9ca3af', fontWeight: 400 }}>({payments.length} total)</span></h3>
            <select value={paymentStatusFilter} onChange={e => { setPaymentStatusFilter(e.target.value); fetchPayments(e.target.value); }}
              style={{ padding: '0.4rem 0.75rem', borderRadius: '8px', border: '1px solid #2a2a4a', fontSize: '0.85rem', background: '#1a1a2e', color: '#e0e0e0' }}>
              <option value="">All Statuses</option>
              <option value="COMPLETED">Completed</option>
              <option value="PENDING">Pending</option>
              <option value="FAILED">Failed</option>
              <option value="REFUNDED">Refunded</option>
            </select>
          </div>
          <div className="data-table-wrapper" style={{ maxHeight: '400px', overflowY: 'auto' }}>
            <table className="data-table">
              <thead><tr><th>ID</th><th>User</th><th>Base Amt</th><th>Tax</th><th>Conv. Fee</th><th>Wallet</th><th>Card</th><th>Total</th><th>Promo</th><th>Discount</th><th>Method</th><th>Status</th><th>Date</th></tr></thead>
              <tbody>
                {payments.length > 0 ? payments.slice(0, 50).map(p => (
                  <tr key={p.id}>
                    <td style={{ fontWeight: 600, color: '#6366f1' }}>#{p.id}</td>
                    <td>{p.userName || p.user?.name || 'N/A'}</td>
                    <td>{formatCurrency(p.baseAmount || 0)}</td>
                    <td style={{ color: '#eab308' }}>{formatCurrency(p.tax || 0)}</td>
                    <td style={{ color: '#f97316' }}>{formatCurrency(p.convenienceFee || 0)}</td>
                    <td style={{ color: '#3b82f6' }}>{formatCurrency(p.walletAmount || 0)}</td>
                    <td style={{ color: '#06b6d4' }}>{formatCurrency(p.cardAmount || 0)}</td>
                    <td style={{ fontWeight: 700, color: '#fff' }}>{formatCurrency(p.amount || 0)}</td>
                    <td>{p.promoCode ? <span style={{ background: 'rgba(34,197,94,0.15)', color: '#22c55e', padding: '2px 6px', borderRadius: '4px', fontSize: '0.8rem' }}>{p.promoCode}</span> : '-'}</td>
                    <td>{p.discountAmount ? <span style={{ color: '#ec4899' }}>{formatCurrency(p.discountAmount)}</span> : '-'}</td>
                    <td><span style={{ background: 'rgba(255,255,255,0.08)', padding: '2px 8px', borderRadius: '4px', fontSize: '0.82rem' }}>{p.paymentMethod || p.method || 'N/A'}</span></td>
                    <td><span className={`status-badge ${(p.status || 'PENDING').toLowerCase()}`}>{p.status || 'PENDING'}</span></td>
                    <td style={{ fontSize: '0.82rem', color: '#a0a0b0' }}>{p.createdAt ? new Date(p.createdAt).toLocaleString() : '-'}</td>
                  </tr>
                )) : <tr><td colSpan="13" style={{textAlign:'center', padding: '2rem', color: '#a0a0b0'}}>No payments found</td></tr>}
              </tbody>
            </table>
          </div>
        </div>

        {/* Wallet Transactions */}
        <div style={cardStyle}>
          <h3 style={sectionTitle}><FaWallet /> Wallet Transactions <span style={{ fontSize: '0.75rem', color: '#9ca3af', fontWeight: 400 }}>({walletTransactions.length} total)</span></h3>
          <div className="data-table-wrapper" style={{ maxHeight: '350px', overflowY: 'auto' }}>
            <table className="data-table">
              <thead><tr><th>ID</th><th>User</th><th>Type</th><th>Amount</th><th>Description</th><th>Date</th></tr></thead>
              <tbody>
                {walletTransactions.length > 0 ? walletTransactions.slice(0, 50).map(wt => (
                  <tr key={wt.id}>
                    <td style={{ fontWeight: 600, color: '#6366f1' }}>#{wt.id}</td>
                    <td>{wt.user?.name || wt.userName || 'N/A'}</td>
                    <td><span className={`status-badge ${(wt.transactionType || wt.type || 'CREDIT').toLowerCase()}`}>{wt.transactionType || wt.type || 'N/A'}</span></td>
                    <td style={{ fontWeight: 600 }}>{formatCurrency(wt.amount || 0)}</td>
                    <td style={{ fontSize: '0.85rem', color: '#a0a0b0' }}>{wt.description || '-'}</td>
                    <td style={{ fontSize: '0.82rem', color: '#a0a0b0' }}>{wt.createdAt ? new Date(wt.createdAt).toLocaleString() : '-'}</td>
                  </tr>
                )) : <tr><td colSpan="6" style={{textAlign:'center', padding: '2rem', color: '#9ca3af'}}>No wallet transactions found</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    );
  };

  // =====================================================
  // NEW TAB: FOOD ITEMS
  // =====================================================

  const handleFoodSubmit = async (e) => {
    e.preventDefault();
    try {
      if (editingFoodId) {
        await adminService.updateFoodItem(editingFoodId, {
          itemName: foodForm.itemName,
          description: foodForm.description,
          price: parseFloat(foodForm.price),
          category: foodForm.category,
          isAvailable: foodForm.isAvailable
        });
        alert('Food item updated!');
      } else {
        await adminService.addFoodItem({
          itemName: foodForm.itemName,
          description: foodForm.description,
          price: parseFloat(foodForm.price),
          category: foodForm.category,
          theater: { id: parseInt(foodForm.theater.id) },
          isAvailable: foodForm.isAvailable
        });
        alert('Food item added!');
      }
      setFoodForm({ itemName: '', description: '', price: '', category: 'SNACKS', theater: { id: '' }, isAvailable: true });
      setEditingFoodId(null);
      fetchFoodItems();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const handleDeleteFood = async (foodId) => {
    if (!window.confirm('Delete this food item?')) return;
    try { await adminService.deleteFoodItem(foodId); fetchFoodItems(); }
    catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const renderManageFood = () => {
    const filteredFood = foodTheaterFilter
      ? foodItems.filter(f => f.theater?.id?.toString() === foodTheaterFilter)
      : foodItems;

    return (
      <div className="tab-content">
        <div className="section-header"><h2><FaFireAlt /> Food Item Management</h2></div>

        <div className="stats-grid">
          <StatCard icon={<FaFireAlt />} label="Total Food Items" value={foodItems.length} color="#f97316" />
          <StatCard icon={<FaToggleOn />} label="Available" value={foodItems.filter(f => f.isAvailable).length} color="#22c55e" />
          <StatCard icon={<FaToggleOff />} label="Unavailable" value={foodItems.filter(f => !f.isAvailable).length} color="#e23744" />
        </div>

        {/* Add/Edit Food Form */}
        <div className="section-header" style={{marginTop:'1.5rem'}}><h3>{editingFoodId ? 'Edit' : 'Add'} Food Item</h3></div>
        <form onSubmit={handleFoodSubmit} className="filter-bar" style={{marginBottom:'1.5rem', flexWrap:'wrap', gap:'0.5rem'}}>
          <input type="text" placeholder="Item Name" value={foodForm.itemName}
            onChange={e => setFoodForm(p => ({...p, itemName: e.target.value}))} required />
          <input type="text" placeholder="Description" value={foodForm.description}
            onChange={e => setFoodForm(p => ({...p, description: e.target.value}))} />
          <input type="number" placeholder="Price" value={foodForm.price} step="0.01"
            onChange={e => setFoodForm(p => ({...p, price: e.target.value}))} required />
          <select value={foodForm.category} onChange={e => setFoodForm(p => ({...p, category: e.target.value}))}>
            <option value="SNACKS">Snacks</option>
            <option value="BEVERAGES">Beverages</option>
            <option value="COMBO">Combo</option>
            <option value="MEALS">Meals</option>
          </select>
          {!editingFoodId && (
            <select value={foodForm.theater.id} onChange={e => setFoodForm(p => ({...p, theater: {id: e.target.value}}))} required>
              <option value="">Select Theatre...</option>
              {theaters.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select>
          )}
          <label style={{display:'flex',alignItems:'center',gap:'0.3rem'}}>
            <input type="checkbox" checked={foodForm.isAvailable}
              onChange={e => setFoodForm(p => ({...p, isAvailable: e.target.checked}))} /> Available
          </label>
          <button type="submit" className="btn-primary">{editingFoodId ? <><FaEdit /> Update</> : <><FaPlus /> Add</>}</button>
          {editingFoodId && <button type="button" className="btn-secondary" onClick={() => {
            setEditingFoodId(null);
            setFoodForm({ itemName: '', description: '', price: '', category: 'SNACKS', theater: { id: '' }, isAvailable: true });
          }}>Cancel</button>}
        </form>

        {/* Filter by Theater */}
        <div className="filter-bar" style={{marginBottom:'1rem'}}>
          <select value={foodTheaterFilter} onChange={e => setFoodTheaterFilter(e.target.value)}>
            <option value="">All Theatres</option>
            {theaters.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </div>

        {/* Food Table */}
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>ID</th><th>Name</th><th>Description</th><th>Price</th><th>Category</th><th>Theatre</th><th>Available</th><th>Actions</th></tr></thead>
            <tbody>
              {filteredFood.length > 0 ? filteredFood.map(f => (
                <tr key={f.id}>
                  <td>{f.id}</td>
                  <td>{f.itemName}</td>
                  <td>{f.description || '-'}</td>
                  <td>{formatCurrency(f.price || 0)}</td>
                  <td>{f.category || '-'}</td>
                  <td>{f.theater?.name || 'N/A'}</td>
                  <td>{f.isAvailable ? <FaToggleOn style={{color:'#22c55e'}} /> : <FaToggleOff style={{color:'#e23744'}} />}</td>
                  <td>
                    <button className="btn-edit" onClick={() => {
                      setEditingFoodId(f.id);
                      setFoodForm({ itemName: f.itemName, description: f.description || '', price: f.price || '', category: f.category || 'SNACKS', theater: { id: f.theater?.id || '' }, isAvailable: f.isAvailable !== false });
                    }}><FaEdit /></button>
                    <button className="btn-danger" onClick={() => handleDeleteFood(f.id)}><FaTrash /></button>
                  </td>
                </tr>
              )) : <tr><td colSpan="8" style={{textAlign:'center'}}>No food items found</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  // =====================================================
  // USER DETAIL ANALYTICS MODAL
  // =====================================================

  const handleUserDetailClick = async (userId) => {
    try {
      setUserDetailLoading(true);
      setShowUserDetailModal(true);
      const data = await adminService.getUserDetailAnalytics(userId);
      setSelectedUserDetail(data);
    } catch (error) {
      console.error('Failed to fetch user analytics:', error);
      setSelectedUserDetail({ error: 'Failed to load user analytics' });
    } finally {
      setUserDetailLoading(false);
    }
  };

  const renderUserDetailModal = () => {
    if (!showUserDetailModal) return null;
    const d = selectedUserDetail;
    return (
      <div className="modal-overlay" onClick={() => { setShowUserDetailModal(false); setSelectedUserDetail(null); }}>
        <div className="modal-content modal-lg" onClick={e => e.stopPropagation()} style={{ maxWidth: '700px' }}>
          <h3><FaUsers /> User Analytics</h3>
          {userDetailLoading ? (
            <div style={{ textAlign: 'center', padding: '2rem' }}>Loading...</div>
          ) : d?.error ? (
            <div style={{ textAlign: 'center', padding: '2rem', color: '#e23744' }}>{d.error}</div>
          ) : d ? (
            <div>
              <div style={{ marginBottom: '1rem', padding: '0.75rem', background: '#f9fafb', borderRadius: '8px' }}>
                <strong>{d.name || 'N/A'}</strong> — {d.email || 'N/A'}
                <span className={`role-badge ${(d.role || '').toLowerCase()}`} style={{ marginLeft: '0.5rem' }}>{d.role}</span>
              </div>
              <div className="stats-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))' }}>
                <StatCard icon={<FaTicketAlt />} label="Total Bookings" value={d.totalBookings || 0} color="#e23744" />
                <StatCard icon={<FaTicketAlt />} label="Active" value={d.activeBookings || 0} color="#22c55e" />
                <StatCard icon={<FaTicketAlt />} label="Cancelled" value={d.cancelledBookings || 0} color="#f97316" />
                <StatCard icon={<FaRupeeSign />} label="Total Spent" value={formatCurrency(d.totalSpending || 0)} color="#8b5cf6" />
                <StatCard icon={<FaWallet />} label="Wallet Paid" value={formatCurrency(d.walletPaid || 0)} color="#3b82f6" />
                <StatCard icon={<FaRupeeSign />} label="Card Paid" value={formatCurrency(d.cardPaid || 0)} color="#06b6d4" />
                <StatCard icon={<FaWallet />} label="Wallet Balance" value={formatCurrency(d.walletBalance || 0)} color="#eab308" />
              </div>
              {d.recentBookings && d.recentBookings.length > 0 && (
                <>
                  <h4 style={{ marginTop: '1rem', marginBottom: '0.5rem' }}>Recent Bookings</h4>
                  <div className="data-table-wrapper">
                    <table className="data-table compact">
                      <thead><tr><th>Movie</th><th>Theatre</th><th>Amount</th><th>Booked At</th></tr></thead>
                      <tbody>
                        {d.recentBookings.map((b, i) => (
                          <tr key={i}>
                            <td>{b.movieName || '-'}</td>
                            <td>{b.theaterName || '-'}</td>
                            <td>{formatCurrency(b.amount || 0)}</td>
                            <td>{b.bookedAt ? new Date(b.bookedAt).toLocaleString() : '-'}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
              <div className="modal-actions" style={{ marginTop: '1rem' }}>
                <button type="button" className="cancel-btn" onClick={() => { setShowUserDetailModal(false); setSelectedUserDetail(null); }}>Close</button>
              </div>
            </div>
          ) : null}
        </div>
      </div>
    );
  };

  // =====================================================
  // NEW TAB: PARKING
  // =====================================================

  const handleParkingStatusToggle = async (slotId, currentStatus) => {
    try {
      await adminService.updateParkingSlotStatus(slotId, !currentStatus);
      fetchParkingSlots();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const handleParkingRateUpdate = async (slotId) => {
    const rate = prompt('Enter new hourly rate:');
    if (!rate) return;
    try {
      await adminService.updateParkingSlot(slotId, { hourlyRate: parseFloat(rate) });
      alert('Rate updated!');
      fetchParkingSlots();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const handleBulkPriceUpdate = async () => {
    if (!parkingBulkTheaterId || !parkingBulkRate) {
      alert('Please select a theatre and enter a rate');
      return;
    }
    try {
      const res = await adminService.updateParkingPriceByTheater(
        parseInt(parkingBulkTheaterId),
        parkingBulkVehicleType || 'ALL',
        parseInt(parkingBulkRate)
      );
      alert(res?.message || 'Parking prices updated!');
      setParkingBulkRate('');
      fetchParkingSlots();
    } catch (error) { alert(error.response?.data?.error || error.message); }
  };

  const renderManageParking = () => {
    // Filter theaters by selected city
    const theatersInCity = parkingCityFilter
      ? theaters.filter(t => (t.city?.id?.toString() === parkingCityFilter || t.cityId?.toString() === parkingCityFilter))
      : theaters;

    // Filter parking by theater (which is already filtered by city)
    const filteredParking = parkingTheaterFilter
      ? parkingSlots.filter(p => p.parkingLot?.theater?.id?.toString() === parkingTheaterFilter)
      : parkingCityFilter
        ? parkingSlots.filter(p => {
            const theaterIds = theatersInCity.map(t => t.id?.toString());
            return theaterIds.includes(p.parkingLot?.theater?.id?.toString());
          })
        : parkingSlots;

    return (
      <div className="tab-content">
        <div className="section-header"><h2><FaCogs /> Parking Management</h2></div>

        <div className="stats-grid">
          <StatCard icon={<FaCogs />} label="Total Slots" value={parkingSlots.length} color="#3b82f6" />
          <StatCard icon={<FaToggleOn />} label="Occupied" value={parkingSlots.filter(p => p.isOccupied).length} color="#e23744" />
          <StatCard icon={<FaToggleOff />} label="Available" value={parkingSlots.filter(p => !p.isOccupied).length} color="#22c55e" />
        </div>

        {/* Bulk Price Update: City → Theatre → Vehicle Type → Rate */}
        <div className="section-header" style={{marginTop:'1.5rem'}}><h3><FaRupeeSign /> Bulk Price Update</h3></div>
        <div className="filter-bar" style={{marginBottom:'1.5rem', flexWrap:'wrap', gap:'0.5rem'}}>
          <select value={parkingCityFilter} onChange={e => { setParkingCityFilter(e.target.value); setParkingTheaterFilter(''); setParkingBulkTheaterId(''); }}>
            <option value="">Select City</option>
            {cities.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <select value={parkingBulkTheaterId} onChange={e => { setParkingBulkTheaterId(e.target.value); setParkingTheaterFilter(e.target.value); }}>
            <option value="">Select Theatre</option>
            {theatersInCity.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
          <select value={parkingBulkVehicleType} onChange={e => setParkingBulkVehicleType(e.target.value)}>
            <option value="">All Vehicle Types</option>
            <option value="TWO_WHEELER">Two Wheeler</option>
            <option value="FOUR_WHEELER">Four Wheeler</option>
            <option value="SUV">SUV</option>
          </select>
          <input type="number" placeholder="Hourly Rate (₹)" value={parkingBulkRate}
            onChange={e => setParkingBulkRate(e.target.value)} step="0.5" min="0" style={{width:'140px'}} />
          <button className="apply-btn" onClick={handleBulkPriceUpdate} disabled={!parkingBulkTheaterId || !parkingBulkRate}>
            <FaRupeeSign /> Update Price
          </button>
        </div>

        {/* Filter & Table */}
        <div className="filter-bar" style={{marginBottom:'1rem'}}>
          <select value={parkingCityFilter} onChange={e => { setParkingCityFilter(e.target.value); setParkingTheaterFilter(''); }}>
            <option value="">All Cities</option>
            {cities.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <select value={parkingTheaterFilter} onChange={e => setParkingTheaterFilter(e.target.value)}>
            <option value="">All Theatres</option>
            {theatersInCity.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </div>

        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>ID</th><th>Slot No</th><th>City</th><th>Theatre</th><th>Type</th><th>Rate/hr</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>
              {filteredParking.length > 0 ? filteredParking.map(p => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{p.slotNumber || p.slotNo || '-'}</td>
                  <td>{p.parkingLot?.theater?.city?.name || p.parkingLot?.theater?.cityName || '-'}</td>
                  <td>{p.parkingLot?.theater?.name || 'N/A'}</td>
                  <td>{p.slotType || p.vehicleType || '-'}</td>
                  <td>{p.hourlyRate ? formatCurrency(p.hourlyRate) : '-'}</td>
                  <td>
                    <span className={`status-badge ${p.isOccupied ? 'occupied' : 'available'}`}>
                      {p.isOccupied ? 'Occupied' : 'Available'}
                    </span>
                  </td>
                  <td>
                    <button className="btn-edit" onClick={() => handleParkingStatusToggle(p.id, p.isOccupied)}>
                      {p.isOccupied ? <FaToggleOff /> : <FaToggleOn />}
                    </button>
                    <button className="btn-edit" onClick={() => handleParkingRateUpdate(p.id)}><FaRupeeSign /></button>
                  </td>
                </tr>
              )) : <tr><td colSpan="8" style={{textAlign:'center'}}>No parking slots found</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  // =====================================================
  // MODAL
  // =====================================================

  const renderModal = () => {
    if (!showModal) return null;
    const modalTitle = modalType === 'city' ? 'City' : modalType === 'movie' ? 'Movie' : modalType === 'theater' ? 'Theatre' : modalType === 'show' ? 'Show' : modalType;
    return (
      <div className="modal-overlay" onClick={() => setShowModal(false)}>
        <div className="modal-content modal-lg" onClick={e => e.stopPropagation()}>
          <h3>{currentItem ? 'Edit' : 'Add'} {modalTitle}</h3>

          {modalType === 'city' && (
            <form onSubmit={handleCitySubmit}>
              <div className="form-group">
                <label>City Name *</label>
                <input type="text" value={formData.name || ''} onChange={e => setFormData(p => ({ ...p, name: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label>State</label>
                <input type="text" value={formData.state || ''} onChange={e => setFormData(p => ({ ...p, state: e.target.value }))} />
              </div>
              <div className="form-group">
                <label>Country</label>
                <input type="text" value={formData.country || ''} onChange={e => setFormData(p => ({ ...p, country: e.target.value }))} />
              </div>
              <div className="modal-actions">
                <button type="submit" className="save-btn">{currentItem ? 'Update' : 'Add'}</button>
                <button type="button" className="cancel-btn" onClick={() => setShowModal(false)}>Cancel</button>
              </div>
            </form>
          )}

          {modalType === 'movie' && (
            <form onSubmit={handleMovieSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label>Movie Name *</label>
                  <input type="text" value={formData.movieName || ''} onChange={e => setFormData(p => ({ ...p, movieName: e.target.value }))} required />
                </div>
                <div className="form-group">
                  <label>Genre *</label>
                  <select value={formData.genre || ''} onChange={e => setFormData(p => ({ ...p, genre: e.target.value }))} required>
                    <option value="">Select Genre</option>
                    {['ACTION', 'COMEDY', 'DRAMA', 'HORROR', 'ROMANCE', 'SCI_FI', 'THRILLER', 'ANIMATION', 'DOCUMENTARY', 'ADVENTURE', 'FANTASY', 'MYSTERY', 'CRIME', 'HISTORICAL', 'MUSICAL', 'FAMILY', 'SPORTS', 'BIOGRAPHY'].map(g => (
                      <option key={g} value={g}>{g.replace(/_/g, ' ')}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Language *</label>
                  <select value={formData.language || ''} onChange={e => setFormData(p => ({ ...p, language: e.target.value }))} required>
                    <option value="">Select Language</option>
                    {['HINDI', 'ENGLISH', 'TAMIL', 'TELUGU', 'KANNADA', 'MALAYALAM', 'MARATHI', 'BENGALI', 'PUNJABI', 'GUJARATI'].map(l => (
                      <option key={l} value={l}>{l}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>Duration (mins) *</label>
                  <input type="number" min="1" value={formData.duration || ''} onChange={e => setFormData(p => ({ ...p, duration: e.target.value }))} required />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Rating</label>
                  <input type="number" step="0.1" min="0" max="10" value={formData.rating || ''} onChange={e => setFormData(p => ({ ...p, rating: e.target.value }))} />
                </div>
                <div className="form-group">
                  <label>Release Date</label>
                  <input type="date" value={formData.releaseDate || ''} onChange={e => setFormData(p => ({ ...p, releaseDate: e.target.value }))} />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Director</label>
                  <input type="text" value={formData.director || ''} onChange={e => setFormData(p => ({ ...p, director: e.target.value }))} />
                </div>
                <div className="form-group">
                  <label>Cast</label>
                  <input type="text" value={formData.cast || ''} onChange={e => setFormData(p => ({ ...p, cast: e.target.value }))} placeholder="Comma separated" />
                </div>
              </div>
              <div className="form-group">
                <label>Description</label>
                <textarea rows="3" value={formData.description || ''} onChange={e => setFormData(p => ({ ...p, description: e.target.value }))} />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Poster URL</label>
                  <input type="text" value={formData.posterUrl || ''} onChange={e => setFormData(p => ({ ...p, posterUrl: e.target.value }))} placeholder="https://..." />
                </div>
                <div className="form-group">
                  <label>Trailer URL</label>
                  <input type="text" value={formData.trailerUrl || ''} onChange={e => setFormData(p => ({ ...p, trailerUrl: e.target.value }))} placeholder="https://..." />
                </div>
              </div>
              <div className="form-group checkbox-group">
                <label>
                  <input type="checkbox" checked={formData.nowShowing !== false} onChange={e => setFormData(p => ({ ...p, nowShowing: e.target.checked }))} />
                  Now Showing
                </label>
              </div>
              <div className="modal-actions">
                <button type="submit" className="save-btn">{currentItem ? 'Update' : 'Add'} Movie</button>
                <button type="button" className="cancel-btn" onClick={() => setShowModal(false)}>Cancel</button>
              </div>
            </form>
          )}

          {modalType === 'theater' && (
            <form onSubmit={handleTheaterSubmit}>
              <div className="form-group">
                <label>Theatre Name *</label>
                <input type="text" value={formData.name || ''} onChange={e => setFormData(p => ({ ...p, name: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label>Address *</label>
                <input type="text" value={formData.address || ''} onChange={e => setFormData(p => ({ ...p, address: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label>City *</label>
                <select value={formData.cityId || ''} onChange={e => setFormData(p => ({ ...p, cityId: e.target.value }))} required>
                  <option value="">Select City</option>
                  {cities.map(c => (
                    <option key={c.id} value={c.id}>{c.name}{c.state ? ` (${c.state})` : ''}</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Theatre Admin (Optional)</label>
                <select value={formData.adminUserId || ''} onChange={e => setFormData(p => ({ ...p, adminUserId: e.target.value }))}>
                  <option value="">No Admin Assigned</option>
                  {users.filter(u => u.role === 'THEATER_OWNER').map(u => (
                    <option key={u.id} value={u.id}>{u.name} ({u.emailId})</option>
                  ))}
                </select>
              </div>
              <div className="modal-actions">
                <button type="submit" className="save-btn">{currentItem ? 'Update' : 'Add'} Theatre</button>
                <button type="button" className="cancel-btn" onClick={() => setShowModal(false)}>Cancel</button>
              </div>
            </form>
          )}

          {modalType === 'show' && (
            <form onSubmit={handleShowSubmit}>
              <div className="form-group">
                <label>Movie *</label>
                <select value={formData.movieId || ''} onChange={e => setFormData(p => ({ ...p, movieId: e.target.value }))} required>
                  <option value="">Select Movie</option>
                  {movies.map(m => (
                    <option key={m.id} value={m.id}>{m.movieName} ({m.language})</option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label>Theatre *</label>
                <select value={formData.theaterId || ''} onChange={e => setFormData(p => ({ ...p, theaterId: e.target.value }))} required>
                  <option value="">Select Theatre</option>
                  {theaters.map(t => (
                    <option key={t.id} value={t.id}>{t.name} — {t.city?.name || t.cityName || 'Unknown City'}</option>
                  ))}
                </select>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>Show Date *</label>
                  <input type="date" value={formData.showDate || ''} onChange={e => setFormData(p => ({ ...p, showDate: e.target.value }))} required />
                </div>
                <div className="form-group">
                  <label>Show Time *</label>
                  <input type="time" value={formData.showStartTime || ''} onChange={e => setFormData(p => ({ ...p, showStartTime: e.target.value }))} required />
                </div>
              </div>
              <div className="modal-actions">
                <button type="submit" className="save-btn">{currentItem ? 'Update' : 'Add'} Show</button>
                <button type="button" className="cancel-btn" onClick={() => setShowModal(false)}>Cancel</button>
              </div>
            </form>
          )}

        </div>
      </div>
    );
  };

  // =====================================================
  // MAIN RENDER
  // =====================================================

  const renderActiveTab = () => {
    switch (activeTab) {
      case 'overview': return renderOverview();
      case 'city-analytics': return renderCityAnalytics();
      case 'movie-analytics': return renderMovieAnalytics();
      case 'theater-rankings': return renderTheaterRankings();
      case 'heatmap': return renderHeatmap();
      case 'user-analytics': return renderUserAnalytics();
      case 'charts': return renderCharts();
      case 'export': return renderExport();
      case 'movies': return renderManageMovies();
      case 'theaters': return renderManageTheaters();
      case 'shows': return renderManageShows();
      case 'users': return renderManageUsers();
      case 'cities': return renderManageCities();
      case 'seats': return renderManageSeats();

      case 'recommendations': return renderManageRecommendations();
      case 'bookings-payments': return renderBookingsPayments();
      case 'food': return renderManageFood();
      case 'parking': return renderManageParking();
      default: return renderOverview();
    }
  };

  return (
    <div className="admin-analytics-dashboard">
      <div className="dashboard-header">
        <h1>Admin Dashboard</h1>
        <span className="subtitle">SeatPakki Analytics & Management</span>
      </div>

      <div className="dashboard-layout">
        <nav className="sidebar">
          <div className="nav-section">
            <h4>Analytics</h4>
            {tabs.slice(0, 8).map(tab => (
              <button key={tab.key} className={`nav-item ${activeTab === tab.key ? 'active' : ''}`}
                onClick={() => setActiveTab(tab.key)}>
                {tab.icon} <span>{tab.label}</span>
              </button>
            ))}
          </div>
          <div className="nav-section">
            <h4>Management</h4>
            {tabs.slice(8).map(tab => (
              <button key={tab.key} className={`nav-item ${activeTab === tab.key ? 'active' : ''}`}
                onClick={() => setActiveTab(tab.key)}>
                {tab.icon} <span>{tab.label}</span>
              </button>
            ))}
          </div>
        </nav>

        <main className="dashboard-content">
          {loading && <div className="loading-bar"></div>}
          {renderActiveTab()}
        </main>
      </div>

      {renderModal()}
      {renderUserDetailModal()}
    </div>
  );
};

export default AdminAnalyticsDashboard;
