import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminService } from '../../services';
import { 
  FaUsers, FaFilm, FaTheaterMasks, FaTicketAlt, FaRupeeSign, FaCity,
  FaPlus, FaEdit, FaTrash, FaChartLine, FaWallet, FaUserMinus, FaChair,
  FaToggleOn, FaToggleOff, FaCogs
} from 'react-icons/fa';
import './AdminDashboard.scss';

const AdminDashboard = () => {
  const navigate = useNavigate();
  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('overview');

  // State for CRUD operations
  const [movies, setMovies] = useState([]);
  const [theaters, setTheaters] = useState([]);
  const [shows, setShows] = useState([]);
  const [users, setUsers] = useState([]);
  const [cities, setCities] = useState([]);

  // Seat management states
  const [theaterSeats, setTheaterSeats] = useState([]);
  const [showSeats, setShowSeats] = useState([]);
  const [selectedTheaterId, setSelectedTheaterId] = useState('');
  const [selectedShowId, setSelectedShowId] = useState('');
  const [seatLoading, setSeatLoading] = useState(false);

  // Modal states
  const [showModal, setShowModal] = useState(false);
  const [modalType, setModalType] = useState('');
  const [currentItem, setCurrentItem] = useState(null);

  // Form states
  const [formData, setFormData] = useState({});
  const [posterFile, setPosterFile] = useState(null);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  useEffect(() => {
    if (activeTab === 'movies') fetchMovies();
    else if (activeTab === 'theaters') fetchTheaters();
    else if (activeTab === 'shows') fetchShows();
    else if (activeTab === 'users') fetchUsers();
    else if (activeTab === 'cities') fetchCities();
    else if (activeTab === 'seats') { fetchTheaters(); fetchShows(); }
  }, [activeTab]);

  const fetchDashboardData = async () => {
    try {
      setLoading(true);
      const data = await adminService.getDashboard();
      setDashboardData(data);
    } catch (error) {
      console.error('Error fetching dashboard:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchMovies = async () => {
    try {
      const data = await adminService.getAllMovies();
      setMovies(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching movies:', error);
      setMovies([]);
    }
  };

  const fetchTheaters = async () => {
    try {
      const data = await adminService.getAllTheaters();
      setTheaters(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching theaters:', error);
      setTheaters([]);
    }
  };

  const fetchShows = async () => {
    try {
      const data = await adminService.getAllShows();
      setShows(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching shows:', error);
      setShows([]);
    }
  };

  const fetchUsers = async () => {
    try {
      const data = await adminService.getAllUsers();
      setUsers(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching users:', error);
      setUsers([]);
    }
  };

  const fetchCities = async () => {
    try {
      const data = await adminService.getAllCities();
      setCities(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching cities:', error);
      setCities([]);
    }
  };

  const openModal = (type, item = null) => {
    setModalType(type);
    setCurrentItem(item);
    setFormData(item || {});
    setPosterFile(null);
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setModalType('');
    setCurrentItem(null);
    setFormData({});
    setPosterFile(null);
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleFileChange = (e) => {
    setPosterFile(e.target.files[0]);
  };

  const handleSubmitCity = async (e) => {
    e.preventDefault();
    try {
      if (currentItem) {
        await adminService.updateCity(currentItem.id, formData);
        alert('City updated successfully!');
      } else {
        await adminService.addCity(formData);
        alert('City added successfully!');
      }
      closeModal();
      fetchCities();
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleSubmitMovie = async (e) => {
    e.preventDefault();
    try {
      let posterUrl = formData.posterUrl;
      
      if (posterFile) {
        const uploadResult = await adminService.uploadMoviePoster(posterFile);
        posterUrl = uploadResult.url;
      }

      const movieData = { ...formData, posterUrl };

      if (currentItem) {
        await adminService.updateMovie(currentItem.id, movieData);
        alert('Movie updated successfully!');
      } else {
        await adminService.addMovie(movieData);
        alert('Movie added successfully!');
      }
      closeModal();
      fetchMovies();
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleSubmitTheater = async (e) => {
    e.preventDefault();
    try {
      if (currentItem) {
        await adminService.updateTheater(currentItem.id, formData);
        alert('Theater updated successfully!');
      } else {
        await adminService.addTheater(formData);
        alert('Theater added successfully!');
      }
      closeModal();
      fetchTheaters();
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleSubmitShow = async (e) => {
    e.preventDefault();
    try {
      const showData = {
        showDate: formData.showDate,
        showStartTime: formData.showStartTime,
        movieId: parseInt(formData.movieId),
        theaterId: parseInt(formData.theaterId)
      };

      if (currentItem) {
        await adminService.updateShow(currentItem.id, showData);
        alert('Show updated successfully!');
      } else {
        await adminService.addShow(showData);
        alert('Show added successfully!');
      }
      closeModal();
      fetchShows();
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleAdjustWallet = async (e) => {
    e.preventDefault();
    try {
      await adminService.adjustUserWallet(
        currentItem.id, 
        parseFloat(formData.amount),
        formData.reason || 'Admin adjustment'
      );
      alert('Wallet adjusted successfully!');
      closeModal();
      fetchUsers();
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleDeleteMovie = async (movieId) => {
    if (!window.confirm('Are you sure you want to delete this movie?')) return;
    try {
      await adminService.deleteMovie(movieId);
      fetchMovies();
      alert('Movie deleted successfully!');
    } catch (error) {
      alert('Failed to delete: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleDeleteTheater = async (theaterId) => {
    if (!window.confirm('Are you sure you want to delete this theater?')) return;
    try {
      await adminService.deleteTheater(theaterId);
      fetchTheaters();
      alert('Theater deleted successfully!');
    } catch (error) {
      alert('Failed to delete: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleDeleteShow = async (showId) => {
    if (!window.confirm('Are you sure you want to delete this show?')) return;
    try {
      await adminService.deleteShow(showId);
      fetchShows();
      alert('Show deleted successfully!');
    } catch (error) {
      alert('Failed to delete: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleDeleteCity = async (cityId) => {
    if (!window.confirm('Are you sure you want to delete this city?')) return;
    try {
      await adminService.deleteCity(cityId);
      fetchCities();
      alert('City deleted successfully!');
    } catch (error) {
      alert('Failed to delete: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleToggleUserStatus = async (userId, currentStatus) => {
    try {
      await adminService.updateUserStatus(userId, !currentStatus);
      fetchUsers();
      alert(`User ${!currentStatus ? 'activated' : 'deactivated'} successfully!`);
    } catch (error) {
      alert('Failed to update: ' + (error.response?.data?.error || error.message));
    }
  };

  // =====================================================
  // SEAT MANAGEMENT HANDLERS
  // =====================================================

  const fetchTheaterSeats = async (theaterId) => {
    if (!theaterId) { setTheaterSeats([]); return; }
    try {
      setSeatLoading(true);
      const data = await adminService.getTheaterSeats(theaterId);
      setTheaterSeats(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching theater seats:', error);
      setTheaterSeats([]);
    } finally { setSeatLoading(false); }
  };

  const fetchShowSeats = async (showId) => {
    if (!showId) { setShowSeats([]); return; }
    try {
      setSeatLoading(true);
      const data = await adminService.getShowSeats(showId);
      setShowSeats(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Error fetching show seats:', error);
      setShowSeats([]);
    } finally { setSeatLoading(false); }
  };

  const handleAddTheaterSeatsRow = async (e) => {
    e.preventDefault();
    try {
      await adminService.addTheaterSeatsRow(
        parseInt(selectedTheaterId),
        formData.rowPrefix,
        formData.seatType,
        parseInt(formData.count)
      );
      alert('Theater seats added successfully!');
      closeModal();
      fetchTheaterSeats(selectedTheaterId);
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleDeleteTheaterSeat = async (seatId) => {
    if (!window.confirm('Delete this theater seat?')) return;
    try {
      await adminService.deleteTheaterSeat(parseInt(selectedTheaterId), seatId);
      fetchTheaterSeats(selectedTheaterId);
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleAddShowSeat = async (e) => {
    e.preventDefault();
    try {
      await adminService.addShowSeat(
        parseInt(selectedShowId),
        formData.seatNo,
        formData.seatType,
        parseInt(formData.price)
      );
      alert('Show seat added successfully!');
      closeModal();
      fetchShowSeats(selectedShowId);
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleAddShowSeatsRow = async (e) => {
    e.preventDefault();
    try {
      await adminService.addShowSeatsRow(
        parseInt(selectedShowId),
        formData.rowPrefix,
        formData.seatType,
        parseInt(formData.count),
        parseInt(formData.price)
      );
      alert('Show seats row added successfully!');
      closeModal();
      fetchShowSeats(selectedShowId);
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleGenerateShowSeats = async () => {
    if (!window.confirm('Generate seats from theater layout? This only works if the show has no seats yet.')) return;
    try {
      const result = await adminService.generateShowSeats(parseInt(selectedShowId));
      alert(result.message);
      fetchShowSeats(selectedShowId);
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleDeleteShowSeat = async (seatId) => {
    if (!window.confirm('Delete this show seat?')) return;
    try {
      await adminService.deleteShowSeat(parseInt(selectedShowId), seatId);
      fetchShowSeats(selectedShowId);
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleToggleShowSeatAvailability = async (seatId) => {
    try {
      await adminService.toggleShowSeatAvailability(parseInt(selectedShowId), seatId);
      fetchShowSeats(selectedShowId);
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleDeleteUser = async (userId) => {
    if (!window.confirm('Are you sure you want to permanently delete this user?')) return;
    try {
      await adminService.deleteUser(userId);
      fetchUsers();
      alert('User deleted successfully!');
    } catch (error) {
      alert('Failed to delete: ' + (error.response?.data?.error || error.message));
    }
  };

  if (loading) {
    return (
      <div className="container" style={{padding: '40px', minHeight: '400px', textAlign: 'center'}}>
        <div className="spinner" style={{margin: '100px auto'}}></div>
        <p>Loading dashboard...</p>
      </div>
    );
  }

  return (
    <div className="admin-dashboard">
      <div className="container">
        <div className="dashboard-header">
          <h1>Admin Dashboard</h1>
          <p>Manage your SeatPakki platform</p>
        </div>

        <div className="dashboard-tabs">
          <button 
            className={activeTab === 'overview' ? 'tab-active' : ''} 
            onClick={() => setActiveTab('overview')}
          >
            <FaChartLine /> Overview
          </button>
          <button 
            className={activeTab === 'users' ? 'tab-active' : ''} 
            onClick={() => setActiveTab('users')}
          >
            <FaUsers /> Users
          </button>
          <button 
            className={activeTab === 'cities' ? 'tab-active' : ''} 
            onClick={() => setActiveTab('cities')}
          >
            <FaCity /> Cities
          </button>
          <button 
            className={activeTab === 'theaters' ? 'tab-active' : ''} 
            onClick={() => setActiveTab('theaters')}
          >
            <FaTheaterMasks /> Theaters
          </button>
          <button 
            className={activeTab === 'movies' ? 'tab-active' : ''} 
            onClick={() => setActiveTab('movies')}
          >
            <FaFilm /> Movies
          </button>
          <button 
            className={activeTab === 'shows' ? 'tab-active' : ''} 
            onClick={() => setActiveTab('shows')}
          >
            <FaTicketAlt /> Shows
          </button>
          <button 
            className={activeTab === 'seats' ? 'tab-active' : ''} 
            onClick={() => setActiveTab('seats')}
          >
            <FaChair /> Seats
          </button>
        </div>

        <div className="dashboard-content">
          {activeTab === 'overview' && (
            <div className="overview-section">
              <div className="stats-grid">
                <div className="stat-card">
                  <div className="stat-icon" style={{background: '#C6282820', color: '#C62828'}}>
                    <FaTicketAlt />
                  </div>
                  <div className="stat-info">
                    <h3>{dashboardData?.totalBookings || 0}</h3>
                    <p>Total Bookings</p>
                  </div>
                </div>
                <div className="stat-card">
                  <div className="stat-icon" style={{background: '#FBC02D20', color: '#FBC02D'}}>
                    <FaRupeeSign />
                  </div>
                  <div className="stat-info">
                    <h3>₹{dashboardData?.totalRevenue?.toLocaleString() || 0}</h3>
                    <p>Total Revenue</p>
                  </div>
                </div>
                <div className="stat-card">
                  <div className="stat-icon" style={{background: '#12121220', color: '#121212'}}>
                    <FaUsers />
                  </div>
                  <div className="stat-info">
                    <h3>{dashboardData?.totalUsers || 0}</h3>
                    <p>Total Users</p>
                  </div>
                </div>
                <div className="stat-card">
                  <div className="stat-icon" style={{background: '#FBC02D20', color: '#FBC02D'}}>
                    <FaFilm />
                  </div>
                  <div className="stat-info">
                    <h3>{dashboardData?.activeMovies || 0}</h3>
                    <p>Active Movies</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'users' && (
            <div className="crud-section">
              <div className="section-header">
                <h2>User Management</h2>
              </div>
              <div className="data-table">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Name</th>
                      <th>Email</th>
                      <th>Wallet</th>
                      <th>Role</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.length === 0 ? (
                      <tr><td colSpan="7" style={{textAlign: 'center'}}>No users found</td></tr>
                    ) : (
                      users.map(user => (
                        <tr key={user.id}>
                          <td>{user.id}</td>
                          <td>{user.name}</td>
                          <td>{user.emailId}</td>
                          <td>₹{user.walletBalance?.toFixed(2) || '0.00'}</td>
                          <td><span className={`badge badge-${user.role?.toLowerCase()}`}>{user.role || 'USER'}</span></td>
                          <td><span className={`badge ${user.isActive ? 'badge-active' : 'badge-inactive'}`}>{user.isActive ? 'Active' : 'Inactive'}</span></td>
                          <td className="action-buttons">
                            <button className="btn-icon btn-primary" onClick={() => openModal('adjustWallet', user)} title="Adjust Wallet">
                              <FaWallet />
                            </button>
                            <button className={`btn-icon ${user.isActive ? 'btn-warning' : 'btn-success'}`} onClick={() => handleToggleUserStatus(user.id, user.isActive)}>
                              {user.isActive ? <FaUserMinus /> : <FaPlus />}
                            </button>
                            {user.role !== 'ADMIN' && (
                              <button className="btn-icon btn-delete" onClick={() => handleDeleteUser(user.id)}>
                                <FaTrash />
                              </button>
                            )}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === 'cities' && (
            <div className="crud-section">
              <div className="section-header">
                <h2>City Management</h2>
                <button className="btn btn-primary" onClick={() => openModal('addCity')}>
                  <FaPlus /> Add City
                </button>
              </div>
              <div className="data-table">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Name</th>
                      <th>State</th>
                      <th>Country</th>
                      <th>Theaters</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {cities.length === 0 ? (
                      <tr><td colSpan="6" style={{textAlign: 'center'}}>No cities found</td></tr>
                    ) : (
                      cities.map(city => (
                        <tr key={city.id}>
                          <td>{city.id}</td>
                          <td>{city.name}</td>
                          <td>{city.state || 'N/A'}</td>
                          <td>{city.country || 'N/A'}</td>
                          <td>{city.theaters?.length || 0}</td>
                          <td className="action-buttons">
                            <button className="btn-icon btn-edit" onClick={() => openModal('addCity', city)}>
                              <FaEdit />
                            </button>
                            <button className="btn-icon btn-delete" onClick={() => handleDeleteCity(city.id)}>
                              <FaTrash />
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === 'theaters' && (
            <div className="crud-section">
              <div className="section-header">
                <h2>Theater Management</h2>
                <button className="btn btn-primary" onClick={() => {fetchCities(); openModal('addTheater');}}>
                  <FaPlus /> Add Theater
                </button>
              </div>
              <div className="data-table">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Name</th>
                      <th>City</th>
                      <th>Address</th>
                      <th>Shows</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {theaters.length === 0 ? (
                      <tr><td colSpan="6" style={{textAlign: 'center'}}>No theaters found</td></tr>
                    ) : (
                      theaters.map(theater => (
                        <tr key={theater.id}>
                          <td>{theater.id}</td>
                          <td>{theater.name}</td>
                          <td>{theater.city?.name || 'N/A'}</td>
                          <td>{theater.address || 'N/A'}</td>
                          <td>{theater.showList?.length || 0}</td>
                          <td className="action-buttons">
                            <button className="btn-icon btn-edit" onClick={() => {fetchCities(); openModal('addTheater', theater);}}>
                              <FaEdit />
                            </button>
                            <button className="btn-icon btn-delete" onClick={() => handleDeleteTheater(theater.id)}>
                              <FaTrash />
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === 'movies' && (
            <div className="crud-section">
              <div className="section-header">
                <h2>Movie Management</h2>
                <button className="btn btn-primary" onClick={() => openModal('addMovie')}>
                  <FaPlus /> Add Movie
                </button>
              </div>
              <div className="data-table">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Poster</th>
                      <th>Name</th>
                      <th>Genre</th>
                      <th>Language</th>
                      <th>Duration</th>
                      <th>Rating</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {movies.length === 0 ? (
                      <tr><td colSpan="8" style={{textAlign: 'center'}}>No movies found</td></tr>
                    ) : (
                      movies.map(movie => (
                        <tr key={movie.id}>
                          <td>{movie.id}</td>
                          <td>
                            {movie.posterUrl && (
                              <img src={movie.posterUrl} alt={movie.movieName} style={{width: '40px', height: '60px', objectFit: 'cover'}} />
                            )}
                          </td>
                          <td>{movie.movieName}</td>
                          <td>{movie.genre || 'N/A'}</td>
                          <td>{movie.language || 'N/A'}</td>
                          <td>{movie.duration || 'N/A'} min</td>
                          <td>{movie.rating || 'N/A'}</td>
                          <td className="action-buttons">
                            <button className="btn-icon btn-edit" onClick={() => openModal('addMovie', movie)}>
                              <FaEdit />
                            </button>
                            <button className="btn-icon btn-delete" onClick={() => handleDeleteMovie(movie.id)}>
                              <FaTrash />
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === 'shows' && (
            <div className="crud-section">
              <div className="section-header">
                <h2>Show Management</h2>
                <button className="btn btn-primary" onClick={() => {fetchMovies(); fetchTheaters(); openModal('addShow');}}>
                  <FaPlus /> Add Show
                </button>
              </div>
              <div className="data-table">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Movie</th>
                      <th>Theater</th>
                      <th>Date</th>
                      <th>Time</th>
                      <th>Bookings</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {shows.length === 0 ? (
                      <tr><td colSpan="7" style={{textAlign: 'center'}}>No shows found</td></tr>
                    ) : (
                      shows.map(show => (
                        <tr key={show.id}>
                          <td>{show.id}</td>
                          <td>{show.movie?.movieName || 'N/A'}</td>
                          <td>{show.theater?.name || 'N/A'}</td>
                          <td>{show.date || 'N/A'}</td>
                          <td>{show.time || 'N/A'}</td>
                          <td>{show.ticketList?.length || 0}</td>
                          <td className="action-buttons">
                            <button className="btn-icon btn-edit" onClick={() => {fetchMovies(); fetchTheaters(); openModal('addShow', show);}}>
                              <FaEdit />
                            </button>
                            <button className="btn-icon btn-delete" onClick={() => handleDeleteShow(show.id)}>
                              <FaTrash />
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === 'seats' && (
            <div className="crud-section">
              <div className="section-header">
                <h2>Seat Management</h2>
              </div>

              {/* Theater Seats Section */}
              <div style={{marginBottom: '30px', padding: '20px', background: '#1a1a2e', borderRadius: '12px'}}>
                <h3 style={{color: '#FBC02D', marginBottom: '15px'}}>Theater Seats</h3>
                <div style={{display: 'flex', gap: '10px', alignItems: 'center', marginBottom: '15px', flexWrap: 'wrap'}}>
                  <select 
                    value={selectedTheaterId} 
                    onChange={(e) => { setSelectedTheaterId(e.target.value); fetchTheaterSeats(e.target.value); }}
                    style={{padding: '8px 12px', borderRadius: '6px', background: '#16213e', color: '#fff', border: '1px solid #333'}}
                  >
                    <option value="">Select Theater</option>
                    {theaters.map(t => <option key={t.id} value={t.id}>{t.name}</option>)}
                  </select>
                  {selectedTheaterId && (
                    <button className="btn btn-primary" onClick={() => openModal('addTheaterSeatsRow')}>
                      <FaPlus /> Add Row
                    </button>
                  )}
                </div>
                {selectedTheaterId && (
                  <div>
                    {seatLoading ? <p>Loading seats...</p> : (
                      theaterSeats.length === 0 ? (
                        <p style={{color: '#aaa'}}>No seats configured for this theater. Add seats using "Add Row".</p>
                      ) : (
                        <div>
                          <p style={{color: '#aaa', marginBottom: '10px'}}>Total: {theaterSeats.length} seats</p>
                          <div style={{display: 'flex', flexWrap: 'wrap', gap: '6px'}}>
                            {theaterSeats.map(seat => (
                              <div key={seat.id} style={{
                                padding: '6px 10px', borderRadius: '6px', fontSize: '12px',
                                background: seat.seatType === 'GOLD' ? '#FFD700' : 
                                           seat.seatType === 'PREMIUM' ? '#C62828' : 
                                           seat.seatType === 'COUPLE' ? '#E91E63' : 
                                           seat.seatType === 'SILVER' ? '#9E9E9E' : '#4CAF50',
                                color: seat.seatType === 'GOLD' ? '#000' : '#fff',
                                display: 'flex', alignItems: 'center', gap: '4px', cursor: 'pointer'
                              }}
                              title={`${seat.seatNo} (${seat.seatType}) - Click to delete`}
                              >
                                {seat.seatNo}
                                <FaTrash size={10} style={{cursor: 'pointer'}} onClick={() => handleDeleteTheaterSeat(seat.id)} />
                              </div>
                            ))}
                          </div>
                          <div style={{display: 'flex', gap: '12px', marginTop: '10px', flexWrap: 'wrap'}}>
                            <span style={{fontSize: '11px', color: '#aaa'}}>
                              <span style={{display: 'inline-block', width: '10px', height: '10px', borderRadius: '2px', background: '#FFD700', marginRight: '4px'}}></span>GOLD
                            </span>
                            <span style={{fontSize: '11px', color: '#aaa'}}>
                              <span style={{display: 'inline-block', width: '10px', height: '10px', borderRadius: '2px', background: '#9E9E9E', marginRight: '4px'}}></span>SILVER
                            </span>
                            <span style={{fontSize: '11px', color: '#aaa'}}>
                              <span style={{display: 'inline-block', width: '10px', height: '10px', borderRadius: '2px', background: '#C62828', marginRight: '4px'}}></span>PREMIUM
                            </span>
                            <span style={{fontSize: '11px', color: '#aaa'}}>
                              <span style={{display: 'inline-block', width: '10px', height: '10px', borderRadius: '2px', background: '#E91E63', marginRight: '4px'}}></span>COUPLE
                            </span>
                            <span style={{fontSize: '11px', color: '#aaa'}}>
                              <span style={{display: 'inline-block', width: '10px', height: '10px', borderRadius: '2px', background: '#4CAF50', marginRight: '4px'}}></span>CLASSIC
                            </span>
                          </div>
                        </div>
                      )
                    )}
                  </div>
                )}
              </div>

              {/* Show Seats Section */}
              <div style={{padding: '20px', background: '#1a1a2e', borderRadius: '12px'}}>
                <h3 style={{color: '#FBC02D', marginBottom: '15px'}}>Show Seats</h3>
                <div style={{display: 'flex', gap: '10px', alignItems: 'center', marginBottom: '15px', flexWrap: 'wrap'}}>
                  <select 
                    value={selectedShowId} 
                    onChange={(e) => { setSelectedShowId(e.target.value); fetchShowSeats(e.target.value); }}
                    style={{padding: '8px 12px', borderRadius: '6px', background: '#16213e', color: '#fff', border: '1px solid #333', maxWidth: '400px'}}
                  >
                    <option value="">Select Show</option>
                    {shows.map(s => (
                      <option key={s.id} value={s.id}>
                        #{s.id} - {s.movie?.movieName || 'Unknown'} @ {s.theater?.name || 'Unknown'} ({s.date} {s.time})
                      </option>
                    ))}
                  </select>
                  {selectedShowId && (
                    <>
                      <button className="btn btn-primary" onClick={() => openModal('addShowSeat')}>
                        <FaPlus /> Add Seat
                      </button>
                      <button className="btn btn-primary" onClick={() => openModal('addShowSeatsRow')}>
                        <FaPlus /> Add Row
                      </button>
                      <button className="btn btn-primary" onClick={handleGenerateShowSeats} title="Generate seats from theater layout">
                        <FaCogs /> Auto-Generate
                      </button>
                    </>
                  )}
                </div>
                {selectedShowId && (
                  <div>
                    {seatLoading ? <p>Loading seats...</p> : (
                      showSeats.length === 0 ? (
                        <p style={{color: '#aaa'}}>No seats for this show. Use "Add Row" or "Auto-Generate" from theater layout.</p>
                      ) : (
                        <div>
                          <p style={{color: '#aaa', marginBottom: '10px'}}>
                            Total: {showSeats.length} seats | 
                            Available: {showSeats.filter(s => s.isAvailable).length} | 
                            Booked: {showSeats.filter(s => !s.isAvailable).length}
                          </p>
                          <div className="data-table">
                            <table>
                              <thead>
                                <tr>
                                  <th>Seat No</th>
                                  <th>Type</th>
                                  <th>Price</th>
                                  <th>Status</th>
                                  <th>Actions</th>
                                </tr>
                              </thead>
                              <tbody>
                                {showSeats.map(seat => (
                                  <tr key={seat.id}>
                                    <td>{seat.seatNo}</td>
                                    <td><span className={`badge badge-${seat.seatType?.toLowerCase()}`}>{seat.seatType}</span></td>
                                    <td>₹{seat.price}</td>
                                    <td>
                                      <span className={`badge ${seat.isAvailable ? 'badge-active' : 'badge-inactive'}`}>
                                        {seat.isAvailable ? 'Available' : 'Booked'}
                                      </span>
                                    </td>
                                    <td className="action-buttons">
                                      <button 
                                        className={`btn-icon ${seat.isAvailable ? 'btn-warning' : 'btn-success'}`} 
                                        onClick={() => handleToggleShowSeatAvailability(seat.id)}
                                        title={seat.isAvailable ? 'Mark Unavailable' : 'Mark Available'}
                                      >
                                        {seat.isAvailable ? <FaToggleOn /> : <FaToggleOff />}
                                      </button>
                                      <button className="btn-icon btn-delete" onClick={() => handleDeleteShowSeat(seat.id)} title="Delete seat">
                                        <FaTrash />
                                      </button>
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        </div>
                      )
                    )}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>
                {modalType === 'addCity' && (currentItem ? 'Edit City' : 'Add City')}
                {modalType === 'addTheater' && (currentItem ? 'Edit Theater' : 'Add Theater')}
                {modalType === 'addMovie' && (currentItem ? 'Edit Movie' : 'Add Movie')}
                {modalType === 'addShow' && (currentItem ? 'Edit Show' : 'Add Show')}
                {modalType === 'adjustWallet' && 'Adjust User Wallet'}
                {modalType === 'addTheaterSeatsRow' && 'Add Theater Seats Row'}
                {modalType === 'addShowSeat' && 'Add Show Seat'}
                {modalType === 'addShowSeatsRow' && 'Add Show Seats Row'}
              </h2>
              <button className="close-btn" onClick={closeModal}>&times;</button>
            </div>

            <div className="modal-body">
              {modalType === 'addCity' && (
                <form onSubmit={handleSubmitCity}>
                  <div className="form-group">
                    <label>City Name*</label>
                    <input type="text" name="name" value={formData.name || ''} onChange={handleInputChange} required />
                  </div>
                  <div className="form-group">
                    <label>State</label>
                    <input type="text" name="state" value={formData.state || ''} onChange={handleInputChange} />
                  </div>
                  <div className="form-group">
                    <label>Country</label>
                    <input type="text" name="country" value={formData.country || ''} onChange={handleInputChange} />
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={closeModal}>Cancel</button>
                    <button type="submit" className="btn btn-primary">{currentItem ? 'Update' : 'Add'} City</button>
                  </div>
                </form>
              )}

              {modalType === 'addTheater' && (
                <form onSubmit={handleSubmitTheater}>
                  <div className="form-group">
                    <label>Theater Name*</label>
                    <input type="text" name="name" value={formData.name || ''} onChange={handleInputChange} required />
                  </div>
                  <div className="form-group">
                    <label>Address*</label>
                    <input type="text" name="address" value={formData.address || ''} onChange={handleInputChange} required />
                  </div>
                  <div className="form-group">
                    <label>City*</label>
                    <select name="cityId" value={formData.cityId || formData.city?.id || ''} onChange={handleInputChange} required>
                      <option value="">Select City</option>
                      {cities.map(city => (
                        <option key={city.id} value={city.id}>{city.name}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={closeModal}>Cancel</button>
                    <button type="submit" className="btn btn-primary">{currentItem ? 'Update' : 'Add'} Theater</button>
                  </div>
                </form>
              )}

              {modalType === 'addMovie' && (
                <form onSubmit={handleSubmitMovie}>
                  <div className="form-group">
                    <label>Movie Name*</label>
                    <input type="text" name="movieName" value={formData.movieName || ''} onChange={handleInputChange} required />
                  </div>
                  <div className="form-row">
                    <div className="form-group">
                      <label>Duration (min)*</label>
                      <input type="number" name="duration" value={formData.duration || ''} onChange={handleInputChange} required />
                    </div>
                    <div className="form-group">
                      <label>Rating</label>
                      <input type="number" step="0.1" name="rating" value={formData.rating || ''} onChange={handleInputChange} />
                    </div>
                  </div>
                  <div className="form-row">
                    <div className="form-group">
                      <label>Genre</label>
                      <select name="genre" value={formData.genre || ''} onChange={handleInputChange}>
                        <option value="">Select Genre</option>
                        <option value="ACTION">Action</option>
                        <option value="DRAMA">Drama</option>
                        <option value="COMEDY">Comedy</option>
                        <option value="HORROR">Horror</option>
                        <option value="THRILLER">Thriller</option>
                        <option value="ROMANCE">Romance</option>
                        <option value="SCI_FI">Sci-Fi</option>
                      </select>
                    </div>
                    <div className="form-group">
                      <label>Language</label>
                      <select name="language" value={formData.language || ''} onChange={handleInputChange}>
                        <option value="">Select Language</option>
                        <option value="ENGLISH">English</option>
                        <option value="HINDI">Hindi</option>
                        <option value="TELUGU">Telugu</option>
                        <option value="TAMIL">Tamil</option>
                        <option value="KANNADA">Kannada</option>
                      </select>
                    </div>
                  </div>
                  <div className="form-group">
                    <label>Release Date</label>
                    <input type="date" name="releaseDate" value={formData.releaseDate || ''} onChange={handleInputChange} />
                  </div>
                  <div className="form-group">
                    <label>Director</label>
                    <input type="text" name="director" value={formData.director || ''} onChange={handleInputChange} />
                  </div>
                  <div className="form-group">
                    <label>Cast (comma separated)</label>
                    <input type="text" name="cast" value={formData.cast || ''} onChange={handleInputChange} />
                  </div>
                  <div className="form-group">
                    <label>Description</label>
                    <textarea name="description" value={formData.description || ''} onChange={handleInputChange} rows="3"></textarea>
                  </div>
                  <div className="form-group">
                    <label>Upload Poster</label>
                    <input type="file" accept="image/*" onChange={handleFileChange} />
                    {formData.posterUrl && !posterFile && (
                      <div style={{marginTop: '10px'}}>
                        <img src={formData.posterUrl} alt="Current poster" style={{width: '100px', height: '150px', objectFit: 'cover'}} />
                      </div>
                    )}
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={closeModal}>Cancel</button>
                    <button type="submit" className="btn btn-primary">{currentItem ? 'Update' : 'Add'} Movie</button>
                  </div>
                </form>
              )}

              {modalType === 'addShow' && (
                <form onSubmit={handleSubmitShow}>
                  <div className="form-group">
                    <label>Movie*</label>
                    <select name="movieId" value={formData.movieId || formData.movie?.id || ''} onChange={handleInputChange} required>
                      <option value="">Select Movie</option>
                      {movies.map(movie => (
                        <option key={movie.id} value={movie.id}>{movie.movieName}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Theater*</label>
                    <select name="theaterId" value={formData.theaterId || formData.theater?.id || ''} onChange={handleInputChange} required>
                      <option value="">Select Theater</option>
                      {theaters.map(theater => (
                        <option key={theater.id} value={theater.id}>{theater.name}</option>
                      ))}
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Date*</label>
                    <input type="date" name="showDate" value={formData.showDate || formData.date || ''} onChange={handleInputChange} required />
                  </div>
                  <div className="form-group">
                    <label>Time*</label>
                    <input type="time" name="showStartTime" value={formData.showStartTime || formData.time || ''} onChange={handleInputChange} required />
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={closeModal}>Cancel</button>
                    <button type="submit" className="btn btn-primary">{currentItem ? 'Update' : 'Add'} Show</button>
                  </div>
                </form>
              )}

              {modalType === 'adjustWallet' && (
                <form onSubmit={handleAdjustWallet}>
                  <div className="form-group">
                    <label>User</label>
                    <input type="text" value={`${currentItem?.name} (${currentItem?.emailId})`} disabled />
                  </div>
                  <div className="form-group">
                    <label>Current Balance</label>
                    <input type="text" value={`₹${currentItem?.walletBalance?.toFixed(2) || '0.00'}`} disabled />
                  </div>
                  <div className="form-group">
                    <label>Amount* (use negative for deduction)</label>
                    <input type="number" step="0.01" name="amount" value={formData.amount || ''} onChange={handleInputChange} required placeholder="e.g., 500 or -200" />
                  </div>
                  <div className="form-group">
                    <label>Reason</label>
                    <input type="text" name="reason" value={formData.reason || ''} onChange={handleInputChange} placeholder="Admin adjustment" />
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={closeModal}>Cancel</button>
                    <button type="submit" className="btn btn-primary">Adjust Wallet</button>
                  </div>
                </form>
              )}

              {modalType === 'addTheaterSeatsRow' && (
                <form onSubmit={handleAddTheaterSeatsRow}>
                  <div className="form-group">
                    <label>Row Prefix* (e.g., A, B, C)</label>
                    <input type="text" name="rowPrefix" value={formData.rowPrefix || ''} onChange={handleInputChange} required placeholder="A" maxLength={3} />
                  </div>
                  <div className="form-group">
                    <label>Seat Type*</label>
                    <select name="seatType" value={formData.seatType || ''} onChange={handleInputChange} required>
                      <option value="">Select Type</option>
                      <option value="GOLD">Gold</option>
                      <option value="SILVER">Silver</option>
                      <option value="PREMIUM">Premium</option>
                      <option value="COUPLE">Couple</option>
                      <option value="CLASSIC">Classic</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Number of Seats*</label>
                    <input type="number" name="count" value={formData.count || ''} onChange={handleInputChange} required min="1" max="50" placeholder="20" />
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={closeModal}>Cancel</button>
                    <button type="submit" className="btn btn-primary">Add Seats</button>
                  </div>
                </form>
              )}

              {modalType === 'addShowSeat' && (
                <form onSubmit={handleAddShowSeat}>
                  <div className="form-group">
                    <label>Seat Number* (e.g., A1)</label>
                    <input type="text" name="seatNo" value={formData.seatNo || ''} onChange={handleInputChange} required placeholder="A1" />
                  </div>
                  <div className="form-group">
                    <label>Seat Type*</label>
                    <select name="seatType" value={formData.seatType || ''} onChange={handleInputChange} required>
                      <option value="">Select Type</option>
                      <option value="GOLD">Gold (₹250)</option>
                      <option value="SILVER">Silver (₹150)</option>
                      <option value="PREMIUM">Premium (₹400)</option>
                      <option value="COUPLE">Couple (₹600)</option>
                      <option value="CLASSIC">Classic (₹150)</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Price (₹)*</label>
                    <input type="number" name="price" value={formData.price || ''} onChange={handleInputChange} required min="1" placeholder="250" />
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={closeModal}>Cancel</button>
                    <button type="submit" className="btn btn-primary">Add Seat</button>
                  </div>
                </form>
              )}

              {modalType === 'addShowSeatsRow' && (
                <form onSubmit={handleAddShowSeatsRow}>
                  <div className="form-group">
                    <label>Row Prefix* (e.g., A, B, C)</label>
                    <input type="text" name="rowPrefix" value={formData.rowPrefix || ''} onChange={handleInputChange} required placeholder="A" maxLength={3} />
                  </div>
                  <div className="form-group">
                    <label>Seat Type*</label>
                    <select name="seatType" value={formData.seatType || ''} onChange={handleInputChange} required>
                      <option value="">Select Type</option>
                      <option value="GOLD">Gold</option>
                      <option value="SILVER">Silver</option>
                      <option value="PREMIUM">Premium</option>
                      <option value="COUPLE">Couple</option>
                      <option value="CLASSIC">Classic</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>Number of Seats*</label>
                    <input type="number" name="count" value={formData.count || ''} onChange={handleInputChange} required min="1" max="50" placeholder="20" />
                  </div>
                  <div className="form-group">
                    <label>Price per Seat (₹)*</label>
                    <input type="number" name="price" value={formData.price || ''} onChange={handleInputChange} required min="1" placeholder="250" />
                  </div>
                  <div className="form-actions">
                    <button type="button" className="btn btn-secondary" onClick={closeModal}>Cancel</button>
                    <button type="submit" className="btn btn-primary">Add Seats Row</button>
                  </div>
                </form>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDashboard;
