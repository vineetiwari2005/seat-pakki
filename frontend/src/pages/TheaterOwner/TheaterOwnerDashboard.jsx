import React, { useState, useEffect, useCallback } from 'react';
import { Routes, Route, Link, useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { theatreOwnerService, movieService, theaterService } from '../../services';
import { useAuth } from '../../context/AuthContext';
import ErrorBoundary from '../../components/ErrorBoundary';
import './TheaterOwnerDashboard.scss';

// =====================================================
// Sidebar Navigation
// =====================================================
const Sidebar = ({ theatre }) => {
  const location = useLocation();
  const basePath = '/theater-owner';

  const navItems = [
    { path: '', label: 'Dashboard' },
    { path: '/shows', label: 'Shows' },
    { path: '/recommendations', label: 'Recommendations' },
    { path: '/payments', label: 'Payments' },
    { path: '/analytics', label: 'Analytics' },
  ];

  return (
    <aside className="to-sidebar">
      {theatre && (
        <div className="to-sidebar__theatre-info">
          <div className="to-sidebar__theatre-avatar">
            {(theatre.name || 'T').charAt(0)}
          </div>
          <h3 className="to-sidebar__theatre-name">{theatre.name}</h3>
          <p className="to-sidebar__theatre-city">{theatre.cityName}</p>
          <p className="to-sidebar__theatre-address">{theatre.address}</p>
          <span className="to-sidebar__seat-badge">{theatre.seatCount} Seats</span>
        </div>
      )}
      <nav className="to-sidebar__nav">
        {navItems.map(item => {
          const fullPath = basePath + item.path;
          const isActive = location.pathname === fullPath ||
            (item.path !== '' && location.pathname.startsWith(fullPath));
          return (
            <Link
              key={item.path}
              to={fullPath}
              className={`to-sidebar__link ${isActive ? 'to-sidebar__link--active' : ''}`}
            >
              <span className="to-sidebar__link-dot"></span>
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </aside>
  );
};

// =====================================================
// Stats Card (clean, no emoji)
// =====================================================
const StatCard = ({ title, value, color = '#C62828', subtitle }) => (
  <div className="to-stat-card" style={{ '--card-color': color }}>
    <div className="to-stat-card__content">
      <p className="to-stat-card__title">{title}</p>
      <p className="to-stat-card__value">{value}</p>
      {subtitle && <p className="to-stat-card__subtitle">{subtitle}</p>}
    </div>
  </div>
);

// =====================================================
// Movie Poster with fallback
// =====================================================
const MoviePoster = ({ url, name, className = 'to-table__poster' }) => {
  const [imgError, setImgError] = React.useState(false);
  const isPlaceholder = url && url.includes('via.placeholder.com');

  if (!url || imgError || isPlaceholder) {
    return (
      <div className="to-table__poster-fallback" title={name || ''}>
        {(name || 'M').charAt(0)}
      </div>
    );
  }

  return (
    <img
      src={url}
      alt={name || ''}
      className={className}
      loading="lazy"
      onError={() => setImgError(true)}
    />
  );
};

// =====================================================
// Section Header
// =====================================================
const SectionHeader = ({ title, subtitle }) => (
  <div className="to-section-header">
    <h3 className="to-section-header__title">{title}</h3>
    {subtitle && <p className="to-section-header__sub">{subtitle}</p>}
  </div>
);

// =====================================================
// Dashboard Overview
// =====================================================
const DashboardOverview = ({ theatreInfo }) => {
  const [dashboard, setDashboard] = useState(null);
  const [recentShows, setRecentShows] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [dashData, showsData] = await Promise.all([
          theatreOwnerService.getDashboard(),
          theatreOwnerService.getShows()
        ]);
        setDashboard(dashData);
        const shows = Array.isArray(showsData) ? showsData : [];
        const today = new Date().toISOString().split('T')[0];
        const upcoming = shows
          .filter(s => s.date >= today)
          .sort((a, b) => a.date.localeCompare(b.date) || (a.time || '').localeCompare(b.time || ''))
          .slice(0, 8);
        setRecentShows(upcoming);
      } catch (err) {
        toast.error('Failed to load dashboard');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) return <div className="to-loading">Loading dashboard...</div>;
  if (!dashboard) return <div className="to-empty">No theatre assigned to your account.</div>;

  const theatre = dashboard.theatre || theatreInfo || {};

  return (
    <div className="to-dashboard">
      <div className="to-dashboard__header">
        <div>
          <h2 className="to-dashboard__title">Welcome, Theatre Admin</h2>
          <p className="to-dashboard__subtitle">
            Managing: <strong>{theatre.name || 'N/A'}</strong> — {theatre.cityName || ''} · {theatre.address || ''}
          </p>
        </div>
        <div className="to-dashboard__date">
          {new Date().toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
        </div>
      </div>

      <div className="to-stats-grid">
        <StatCard title="Total Shows" value={dashboard.totalShows || 0} color="#C62828" subtitle={`${dashboard.todaysShows || 0} today`} />
        <StatCard title="Theatre Seats" value={theatre.seatCount || 0} color="#1565C0" />
        <StatCard title="Total Bookings" value={dashboard.totalBookings || 0} color="#2E7D32" />
        <StatCard title="Total Revenue" value={`₹${(dashboard.totalRevenue || 0).toLocaleString()}`} color="#6A1B9A" />
        <StatCard title="Pending Recs" value={dashboard.pendingRecommendations || 0} color="#F57F17" />
        <StatCard title="Occupancy Rate" value={`${(dashboard.occupancyRate || 0).toFixed(1)}%`} color="#00838F" />
      </div>

      {recentShows.length > 0 && (
        <div className="to-card">
          <div className="to-card__header">
            <h3>Upcoming Shows</h3>
            <Link to="/theater-owner/shows" className="to-card__link">View All</Link>
          </div>
          <div className="to-table-wrapper">
            <table className="to-table">
              <thead>
                <tr>
                  <th>Movie</th>
                  <th>Date</th>
                  <th>Time</th>
                  <th>Total Seats</th>
                  <th>Booked</th>
                  <th>Available</th>
                </tr>
              </thead>
              <tbody>
                {recentShows.map((show, i) => (
                  <tr key={show.id || i}>
                    <td className="to-table__movie">
                      <MoviePoster url={show.movie?.posterUrl} name={show.movie?.movieName} />
                      <span>{show.movie?.movieName || show.movieName || 'N/A'}</span>
                    </td>
                    <td>{show.date}</td>
                    <td>{show.time}</td>
                    <td>{show.totalSeats || 0}</td>
                    <td>{show.bookedSeats || 0}</td>
                    <td>
                      <span className={`to-badge ${(show.availableSeats || show.totalSeats) > 0 ? 'to-badge--success' : 'to-badge--danger'}`}>
                        {show.availableSeats != null ? show.availableSeats : show.totalSeats || 0}
                      </span>
                    </td>
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
// Shows Management
// =====================================================
const ShowsPage = () => {
  const [shows, setShows] = useState([]);
  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({ movieId: '', date: '', time: '' });
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedMovies, setExpandedMovies] = useState(new Set());
  const [useCustomSeats, setUseCustomSeats] = useState(false);
  const SEAT_TYPES = ['CLASSIC', 'PREMIUM', 'GOLD', 'SILVER', 'COUPLE'];
  const DEFAULT_PRICES = { CLASSIC: 150, PREMIUM: 400, GOLD: 250, SILVER: 150, COUPLE: 600 };
  const DEFAULT_PREFIXES = { CLASSIC: 'C', PREMIUM: 'P', GOLD: 'G', SILVER: 'S', COUPLE: 'L' };
  const [seatConfigs, setSeatConfigs] = useState(
    SEAT_TYPES.map(type => ({ seatType: type, count: 0, rowPrefix: DEFAULT_PREFIXES[type], price: DEFAULT_PRICES[type] }))
  );

  const fetchShows = useCallback(async () => {
    try {
      const data = await theatreOwnerService.getShows();
      setShows(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error('Failed to load shows');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchMovies = useCallback(async () => {
    try {
      const data = await movieService.getNowShowing();
      setMovies(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load movies:', err);
    }
  }, []);

  useEffect(() => { fetchShows(); fetchMovies(); }, [fetchShows, fetchMovies]);

  const handleAddShow = async (e) => {
    e.preventDefault();
    try {
      const showPayload = {
        movieId: parseInt(formData.movieId),
        showDate: formData.date,
        showStartTime: formData.time + ':00',
        useCustomSeats: useCustomSeats
      };
      if (useCustomSeats) {
        showPayload.seatConfigs = seatConfigs.filter(sc => sc.count > 0).map(sc => ({
          seatType: sc.seatType,
          count: parseInt(sc.count),
          rowPrefix: sc.rowPrefix,
          price: parseInt(sc.price)
        }));
        if (showPayload.seatConfigs.length === 0) {
          toast.error('Please configure at least one seat type with count > 0');
          return;
        }
        // Validate couple seats must be even count
        const coupleConfig = showPayload.seatConfigs.find(sc => sc.seatType === 'COUPLE');
        if (coupleConfig && coupleConfig.count % 2 !== 0) {
          toast.error('Couple seats must be added in even numbers (pairs). You entered ' + coupleConfig.count + '.');
          return;
        }
      }
      await theatreOwnerService.addShow(showPayload);
      toast.success('Show added successfully!');
      setShowForm(false);
      setFormData({ movieId: '', date: '', time: '' });
      setUseCustomSeats(false);
      setSeatConfigs(SEAT_TYPES.map(type => ({ seatType: type, count: 0, rowPrefix: DEFAULT_PREFIXES[type], price: DEFAULT_PRICES[type] })));
      fetchShows();
    } catch (err) {
      toast.error(err.response?.data?.error || err.response?.data || 'Failed to add show');
    }
  };

  const handleDeleteShow = async (showId) => {
    if (!window.confirm('Delete this show? This cannot be undone.')) return;
    try {
      await theatreOwnerService.deleteShow(showId);
      toast.success('Show deleted');
      fetchShows();
    } catch (err) {
      toast.error(err.response?.data?.error || err.response?.data || 'Failed to delete show');
    }
  };

  const toggleMovie = (movieKey) => {
    setExpandedMovies(prev => {
      const next = new Set(prev);
      if (next.has(movieKey)) next.delete(movieKey);
      else next.add(movieKey);
      return next;
    });
  };

  if (loading) return <div className="to-loading">Loading shows...</div>;

  const today = new Date().toISOString().split('T')[0];

  // Filter out past shows and apply search
  let upcomingShows = shows.filter(s => s.date >= today);
  if (searchTerm) {
    const term = searchTerm.toLowerCase();
    upcomingShows = upcomingShows.filter(s =>
      (s.movie?.movieName || s.movieName || '').toLowerCase().includes(term)
    );
  }

  // Group by movie
  const movieGroups = {};
  upcomingShows.forEach(show => {
    const movieName = show.movie?.movieName || show.movieName || 'Unknown Movie';
    const movieId = show.movie?.id || show.movieId || movieName;
    const key = `${movieId}`;
    if (!movieGroups[key]) {
      movieGroups[key] = {
        movieName,
        movieId,
        posterUrl: show.movie?.posterUrl || null,
        language: show.movie?.language || '',
        genre: show.movie?.genre || '',
        duration: show.movie?.duration || null,
        rating: show.movie?.rating || null,
        shows: []
      };
    }
    movieGroups[key].shows.push(show);
  });

  // Sort shows within each movie group by date/time
  Object.values(movieGroups).forEach(group => {
    group.shows.sort((a, b) => a.date.localeCompare(b.date) || (a.time || '').localeCompare(b.time || ''));
    group.totalSeats = group.shows.reduce((sum, s) => sum + (s.totalSeats || 0), 0);
    group.totalBooked = group.shows.reduce((sum, s) => sum + (s.bookedSeats || 0), 0);
  });

  // Sort movie groups by earliest show date
  const sortedGroups = Object.entries(movieGroups).sort((a, b) => {
    const aFirst = a[1].shows[0]?.date || '';
    const bFirst = b[1].shows[0]?.date || '';
    return aFirst.localeCompare(bFirst);
  });

  const totalUpcoming = upcomingShows.length;

  return (
    <div className="to-shows">
      <div className="to-page-header">
        <h2>Shows Management</h2>
        <button
          className={`to-btn ${showForm ? 'to-btn--secondary' : 'to-btn--primary'}`}
          onClick={() => setShowForm(!showForm)}
        >
          {showForm ? 'Cancel' : '+ Add Show'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleAddShow} className="to-add-show-form">
          <div className="to-form-group">
            <label>Movie</label>
            <select value={formData.movieId} onChange={e => setFormData({ ...formData, movieId: e.target.value })} required>
              <option value="">Select Movie...</option>
              {movies.map(m => (
                <option key={m.id} value={m.id}>{m.movieName || m.name} {m.language ? `(${m.language})` : ''}</option>
              ))}
            </select>
          </div>
          <div className="to-form-group">
            <label>Date</label>
            <input type="date" value={formData.date} onChange={e => setFormData({ ...formData, date: e.target.value })} min={today} required />
          </div>
          <div className="to-form-group">
            <label>Time</label>
            <input type="time" value={formData.time} onChange={e => setFormData({ ...formData, time: e.target.value })} required />
          </div>

          <div className="to-form-group to-form-group--full">
            <label className="to-toggle-label">
              <input type="checkbox" checked={useCustomSeats} onChange={e => setUseCustomSeats(e.target.checked)} />
              <span>Configure custom seat types & pricing</span>
            </label>
            <small className="to-form-hint">
              {useCustomSeats ? 'Set the number, row prefix, and price for each seat type below.' : 'Seats will be auto-generated from the theatre layout with default pricing.'}
            </small>
          </div>

          {useCustomSeats && (
            <div className="to-seat-config-section">
              <h4 className="to-seat-config-title">Seat Configuration</h4>
              <div className="to-seat-config-table">
                <div className="to-seat-config-header">
                  <span>Seat Type</span><span>Row Prefix</span><span>Count</span><span>Price</span>
                </div>
                {seatConfigs.map((sc, idx) => (
                  <div key={sc.seatType} className="to-seat-config-row">
                    <span className="to-seat-config-type">
                      <span className="to-seat-config-dot" style={{ background: { CLASSIC: '#607D8B', PREMIUM: '#7B1FA2', GOLD: '#FBC02D', SILVER: '#9E9E9E', COUPLE: '#E91E63' }[sc.seatType] }}></span>
                      {sc.seatType}
                    </span>
                    <input type="text" value={sc.rowPrefix} maxLength={2} onChange={e => { const u = [...seatConfigs]; u[idx] = { ...u[idx], rowPrefix: e.target.value.toUpperCase() }; setSeatConfigs(u); }} className="to-seat-config-input to-seat-config-input--prefix" placeholder="A" />
                    <input type="number" value={sc.count} min={0} max={200} onChange={e => { const u = [...seatConfigs]; u[idx] = { ...u[idx], count: e.target.value }; setSeatConfigs(u); }} className="to-seat-config-input to-seat-config-input--count" placeholder="0" />
                    <input type="number" value={sc.price} min={0} onChange={e => { const u = [...seatConfigs]; u[idx] = { ...u[idx], price: e.target.value }; setSeatConfigs(u); }} className="to-seat-config-input to-seat-config-input--price" placeholder="0" />
                  </div>
                ))}
              </div>
              <div className="to-seat-config-summary">
                Total seats: <strong>{seatConfigs.reduce((sum, sc) => sum + (parseInt(sc.count) || 0), 0)}</strong>
              </div>
            </div>
          )}

          <button type="submit" className="to-btn to-btn--success">Add Show</button>
        </form>
      )}

      <div className="to-filters">
        <input type="text" placeholder="Search movie name..." value={searchTerm} onChange={e => setSearchTerm(e.target.value)} className="to-search-input" />
        <span className="to-show-count">{totalUpcoming} upcoming shows across {sortedGroups.length} movies</span>
      </div>

      {sortedGroups.length === 0 ? (
        <div className="to-empty-state to-card">
          <h3>No Upcoming Shows</h3>
          <p>{searchTerm ? 'No movies match your search.' : 'Add shows to get started!'}</p>
        </div>
      ) : (
        <div className="to-movie-groups">
          {sortedGroups.map(([key, group]) => {
            const isExpanded = expandedMovies.has(key);
            const occupancy = group.totalSeats > 0 ? ((group.totalBooked / group.totalSeats) * 100).toFixed(0) : 0;
            return (
              <div key={key} className={`to-movie-card to-card ${isExpanded ? 'to-movie-card--expanded' : ''}`}>
                <div className="to-movie-card__header" onClick={() => toggleMovie(key)}>
                  <div className="to-movie-card__info">
                    <MoviePoster url={group.posterUrl} name={group.movieName} />
                    <div className="to-movie-card__details">
                      <h3 className="to-movie-card__title">{group.movieName}</h3>
                      <div className="to-movie-card__meta">
                        {group.language && <span className="to-tag">{group.language}</span>}
                        {group.genre && <span className="to-tag">{group.genre}</span>}
                        {group.duration && <span className="to-tag">{group.duration} min</span>}
                        {group.rating && <span className="to-tag to-tag--gold">{group.rating}</span>}
                      </div>
                    </div>
                  </div>
                  <div className="to-movie-card__stats">
                    <div className="to-movie-card__stat">
                      <span className="to-movie-card__stat-val">{group.shows.length}</span>
                      <span className="to-movie-card__stat-label">Shows</span>
                    </div>
                    <div className="to-movie-card__stat">
                      <span className="to-movie-card__stat-val">{group.totalBooked}/{group.totalSeats}</span>
                      <span className="to-movie-card__stat-label">Booked</span>
                    </div>
                    <div className="to-movie-card__stat">
                      <span className="to-movie-card__stat-val">{occupancy}%</span>
                      <span className="to-movie-card__stat-label">Occupancy</span>
                    </div>
                    <span className={`to-movie-card__chevron ${isExpanded ? 'to-movie-card__chevron--open' : ''}`}>
                      {isExpanded ? '\u25B2' : '\u25BC'}
                    </span>
                  </div>
                </div>
                {isExpanded && (
                  <div className="to-movie-card__shows">
                    <div className="to-table-wrapper">
                      <table className="to-table to-table--compact">
                        <thead>
                          <tr><th>Date</th><th>Time</th><th>Total Seats</th><th>Booked</th><th>Tickets</th><th>Actions</th></tr>
                        </thead>
                        <tbody>
                          {group.shows.map(show => (
                            <tr key={show.id}>
                              <td>
                                {new Date(show.date + 'T00:00:00').toLocaleDateString('en-IN', { weekday: 'short', month: 'short', day: 'numeric' })}
                              </td>
                              <td>{show.time}</td>
                              <td>{show.totalSeats || 0}</td>
                              <td><span className={show.bookedSeats > 0 ? 'to-text-success' : ''}>{show.bookedSeats || 0}</span></td>
                              <td>{show.ticketCount || 0}</td>
                              <td>
                                <button className="to-btn to-btn--danger to-btn--sm" onClick={(e) => { e.stopPropagation(); handleDeleteShow(show.id); }} title="Delete show">Delete</button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

// =====================================================
// Recommendations Page
// =====================================================
const RecommendationsPage = () => {
  const [recommendations, setRecommendations] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchRecommendations = useCallback(async () => {
    try {
      const data = await theatreOwnerService.getRecommendations();
      setRecommendations(Array.isArray(data) ? data : []);
    } catch (err) {
      toast.error('Failed to load recommendations');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchRecommendations(); }, [fetchRecommendations]);

  const handleAccept = async (id) => {
    try {
      await theatreOwnerService.acceptRecommendation(id, 'Accepted - will schedule shows');
      toast.success('Recommendation accepted!');
      fetchRecommendations();
    } catch (err) {
      toast.error(err.response?.data?.error || err.response?.data || 'Failed to accept');
    }
  };

  const handleReject = async (id) => {
    const reason = prompt('Reason for rejection (optional):') || 'Not suitable for our audience';
    try {
      await theatreOwnerService.rejectRecommendation(id, reason);
      toast.success('Recommendation rejected');
      fetchRecommendations();
    } catch (err) {
      toast.error(err.response?.data?.error || err.response?.data || 'Failed to reject');
    }
  };

  if (loading) return <div className="to-loading">Loading recommendations...</div>;

  const pending = recommendations.filter(r => r.status === 'PENDING');
  const accepted = recommendations.filter(r => r.status === 'ACCEPTED');
  const rejected = recommendations.filter(r => r.status === 'REJECTED');

  const statusConfig = {
    PENDING: { color: '#F57F17', bg: '#F57F1715', label: 'Pending' },
    ACCEPTED: { color: '#2E7D32', bg: '#2E7D3215', label: 'Accepted' },
    REJECTED: { color: '#C62828', bg: '#C6282815', label: 'Rejected' }
  };

  const renderRecCard = (rec) => {
    const cfg = statusConfig[rec.status] || statusConfig.PENDING;
    return (
      <div key={rec.id} className="to-rec-card" style={{ '--rec-color': cfg.color }}>
        <div className="to-rec-card__body">
          <div className="to-rec-card__movie-info">
            <MoviePoster url={rec.movie?.posterUrl} name={rec.movie?.movieName} className="to-rec-card__poster" />
            <div>
              <h3 className="to-rec-card__movie-name">{rec.movie?.movieName || 'Movie'}</h3>
              <div className="to-rec-card__meta">
                {rec.movie?.genre && <span className="to-tag">{rec.movie.genre}</span>}
                {rec.movie?.language && <span className="to-tag">{rec.movie.language}</span>}
                {rec.movie?.duration && <span className="to-tag">{rec.movie.duration} min</span>}
                {rec.movie?.rating && <span className="to-tag to-tag--gold">{rec.movie.rating}</span>}
              </div>
              {rec.adminMessage && <p className="to-rec-card__message"><strong>Admin says:</strong> {rec.adminMessage}</p>}
              {rec.theatreAdminResponse && <p className="to-rec-card__response"><em>Your response: {rec.theatreAdminResponse}</em></p>}
            </div>
          </div>
          <div className="to-rec-card__actions">
            <span className="to-status-badge" style={{ background: cfg.bg, color: cfg.color }}>{cfg.label}</span>
            {rec.status === 'PENDING' && (
              <div className="to-rec-card__buttons">
                <button className="to-btn to-btn--success to-btn--sm" onClick={() => handleAccept(rec.id)}>Accept</button>
                <button className="to-btn to-btn--danger to-btn--sm" onClick={() => handleReject(rec.id)}>Reject</button>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="to-recommendations">
      <div className="to-page-header">
        <h2>Movie Recommendations from Main Admin</h2>
        <div className="to-rec-summary">
          <span className="to-rec-count to-rec-count--pending">{pending.length} pending</span>
          <span className="to-rec-count to-rec-count--accepted">{accepted.length} accepted</span>
          <span className="to-rec-count to-rec-count--rejected">{rejected.length} rejected</span>
        </div>
      </div>

      {recommendations.length === 0 ? (
        <div className="to-empty-state to-card">
          <h3>No Recommendations Yet</h3>
          <p>The Main Admin hasn't sent any movie recommendations to your theatre yet.</p>
        </div>
      ) : (
        <div className="to-rec-list">
          {pending.length > 0 && (
            <div className="to-rec-section">
              <h3 className="to-rec-section__title">Pending ({pending.length})</h3>
              {pending.map(renderRecCard)}
            </div>
          )}
          {accepted.length > 0 && (
            <div className="to-rec-section">
              <h3 className="to-rec-section__title">Accepted ({accepted.length})</h3>
              {accepted.map(renderRecCard)}
            </div>
          )}
          {rejected.length > 0 && (
            <div className="to-rec-section">
              <h3 className="to-rec-section__title">Rejected ({rejected.length})</h3>
              {rejected.map(renderRecCard)}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

// =====================================================
// Payment Analytics Page (replaces Seats)
// =====================================================
const PaymentAnalyticsPage = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await theatreOwnerService.getPaymentAnalytics();
        setData(result);
      } catch (err) {
        toast.error('Failed to load payment analytics');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) return <div className="to-loading">Loading payment analytics...</div>;
  if (!data) return <div className="to-empty">No payment data available yet.</div>;

  const s = (val) => (typeof val === 'object' && val !== null) ? 0 : (val ?? 0);
  const methodBreakdown = Array.isArray(data.paymentMethodBreakdown) ? data.paymentMethodBreakdown : [];
  const showPayments = Array.isArray(data.showPayments) ? data.showPayments : [];
  const dailyTrend = Array.isArray(data.dailyPaymentTrend) ? data.dailyPaymentTrend : [];
  const hourlyPayments = Array.isArray(data.hourlyPayments) ? data.hourlyPayments : [];
  const statusDist = Array.isArray(data.paymentStatusDistribution) ? data.paymentStatusDistribution : [];
  const promoAnalytics = data.promoAnalytics || {};
  const promoBreakdown = Array.isArray(promoAnalytics.promoBreakdown) ? promoAnalytics.promoBreakdown : [];
  const refundAnalytics = data.refundAnalytics || {};

  const methodColors = {
    CREDIT_CARD: '#1565C0', DEBIT_CARD: '#0277BD', UPI: '#6A1B9A',
    NET_BANKING: '#00695C', WALLET: '#E65100', STRIPE: '#635BFF',
    CASH: '#33691E', WALLET_CARD_SPLIT: '#AD1457', UNKNOWN: '#999'
  };
  const methodLabels = {
    CREDIT_CARD: 'Credit Card', DEBIT_CARD: 'Debit Card', UPI: 'UPI',
    NET_BANKING: 'Net Banking', WALLET: 'Wallet', STRIPE: 'Stripe',
    CASH: 'Cash', WALLET_CARD_SPLIT: 'Wallet + Card', UNKNOWN: 'Unknown'
  };
  const statusColors = {
    SUCCESS: '#2E7D32', FAILED: '#C62828', PENDING: '#F57F17',
    REFUNDED: '#6A1B9A', CANCELLED: '#455A64', PROCESSING: '#0277BD'
  };

  const maxDailyRev = Math.max(...dailyTrend.map(d => d.revenue || 0), 1);
  const maxHourlyRev = Math.max(...hourlyPayments.map(h => h.revenue || 0), 1);

  return (
    <div className="to-analytics">
      <div className="to-page-header"><h2>Payment Analytics</h2></div>

      {/* KPI Cards */}
      <div className="to-kpi-grid">
        <div className="to-kpi-card to-kpi-card--revenue">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Total Revenue</span></div>
          <div className="to-kpi-card__value">{'\u20B9'}{s(data.totalRevenue).toLocaleString()}</div>
          <div className="to-kpi-card__sub">Base: {'\u20B9'}{s(data.totalBaseAmount).toLocaleString()}</div>
        </div>
        <div className="to-kpi-card to-kpi-card--bookings">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Transactions</span><span className="to-kpi-card__badge">{s(data.successfulTransactions)} successful</span></div>
          <div className="to-kpi-card__value">{s(data.totalTransactions)}</div>
        </div>
        <div className="to-kpi-card to-kpi-card--shows">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Avg Transaction</span></div>
          <div className="to-kpi-card__value">{'\u20B9'}{s(data.avgTransactionValue).toLocaleString()}</div>
        </div>
        <div className="to-kpi-card to-kpi-card--occupancy">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Wallet Payments</span></div>
          <div className="to-kpi-card__value">{'\u20B9'}{s(data.totalWalletAmount).toLocaleString()}</div>
        </div>
        <div className="to-kpi-card to-kpi-card--upcoming">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Card Payments</span></div>
          <div className="to-kpi-card__value">{'\u20B9'}{s(data.totalCardAmount).toLocaleString()}</div>
        </div>
        <div className="to-kpi-card to-kpi-card--cancel">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Refunds</span><span className="to-kpi-card__badge">{s(refundAnalytics.totalRefundCount)} refunded</span></div>
          <div className="to-kpi-card__value">{'\u20B9'}{s(data.totalRefunds).toLocaleString()}</div>
        </div>
      </div>

      {/* Revenue Breakdown - Fees, Tax, Discounts */}
      <div className="to-card to-card--elevated">
        <SectionHeader title="Revenue Breakdown" subtitle={`Total: \u20B9${s(data.totalRevenue).toLocaleString()}`} />
        <div className="to-metric-row">
          <div className="to-metric"><span className="to-metric__val">{'\u20B9'}{s(data.totalBaseAmount).toLocaleString()}</span><span className="to-metric__label">Base Amount</span></div>
          <div className="to-metric to-metric--warning"><span className="to-metric__val">{'\u20B9'}{s(data.totalConvenienceFee).toLocaleString()}</span><span className="to-metric__label">Convenience Fee</span></div>
          <div className="to-metric to-metric--info"><span className="to-metric__val">{'\u20B9'}{s(data.totalTax).toLocaleString()}</span><span className="to-metric__label">Tax Collected</span></div>
          <div className="to-metric to-metric--success"><span className="to-metric__val">{'\u20B9'}{s(data.totalDiscounts).toLocaleString()}</span><span className="to-metric__label">Discounts Given</span></div>
          <div className="to-metric to-metric--danger"><span className="to-metric__val">{'\u20B9'}{s(data.totalRefunds).toLocaleString()}</span><span className="to-metric__label">Refunds Issued</span></div>
        </div>
      </div>

      {/* Payment Method Breakdown */}
      {methodBreakdown.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Payment Method Distribution" />
          <div className="to-table-wrapper">
            <table className="to-table to-table--striped">
              <thead><tr><th>Payment Method</th><th>Transactions</th><th>Revenue</th><th>Share</th></tr></thead>
              <tbody>
                {methodBreakdown.map((item, i) => (
                  <tr key={item.method || i}>
                    <td>
                      <span className="to-seat-type-cell">
                        <span className="to-dot" style={{ background: methodColors[item.method] || '#999' }}></span>
                        <strong>{methodLabels[item.method] || item.method}</strong>
                      </span>
                    </td>
                    <td>{s(item.count)}</td>
                    <td className="to-text-bold">{'\u20B9'}{s(item.revenue).toLocaleString()}</td>
                    <td>
                      <div className="to-progress-bar">
                        <div className="to-progress-bar__fill" style={{ width: `${s(item.percentage)}%`, background: methodColors[item.method] || '#999' }}></div>
                        <span className="to-progress-bar__text">{s(item.percentage)}%</span>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Payment Status Distribution */}
      {statusDist.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Transaction Status Overview" />
          <div className="to-metric-row">
            {statusDist.map((item, i) => {
              const color = statusColors[item.status] || '#999';
              return (
                <div key={item.status || i} className="to-metric" style={{ borderBottom: `3px solid ${color}` }}>
                  <span className="to-metric__val" style={{ color }}>{s(item.count)}</span>
                  <span className="to-metric__label">{item.status}</span>
                  <span className="to-metric__sub">{s(item.percentage)}%</span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Show-wise Payment Breakdown */}
      {showPayments.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Show-wise Payment Breakdown" subtitle={`${showPayments.length} shows with payments`} />
          <div className="to-table-wrapper">
            <table className="to-table to-table--striped">
              <thead><tr><th>Movie</th><th>Date</th><th>Time</th><th>Txns</th><th>Revenue</th><th>Wallet</th><th>Card</th><th>Methods</th></tr></thead>
              <tbody>
                {showPayments.map((sp, i) => {
                  const methods = typeof sp.methodBreakdown === 'object' && sp.methodBreakdown ? sp.methodBreakdown : {};
                  return (
                    <tr key={sp.showId || i}>
                      <td><strong>{sp.movieName}</strong></td>
                      <td>{sp.showDate ? new Date(sp.showDate + 'T00:00:00').toLocaleDateString('en-IN', { month: 'short', day: 'numeric' }) : 'N/A'}</td>
                      <td>{sp.showTime || 'N/A'}</td>
                      <td>{s(sp.transactionCount)}</td>
                      <td className="to-text-bold">{'\u20B9'}{s(sp.revenue).toLocaleString()}</td>
                      <td>{'\u20B9'}{s(sp.walletAmount).toLocaleString()}</td>
                      <td>{'\u20B9'}{s(sp.cardAmount).toLocaleString()}</td>
                      <td>
                        <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
                          {Object.entries(methods).map(([m, cnt]) => (
                            <span key={m} className="to-tag" style={{ background: (methodColors[m] || '#999') + '18', color: methodColors[m] || '#999', fontSize: '10px' }}>
                              {methodLabels[m] || m}: {String(cnt)}
                            </span>
                          ))}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Daily Payment Trend */}
      {dailyTrend.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Daily Payment Trend" subtitle="Last 14 days" />
          <div className="to-chart-bars">
            {dailyTrend.map((dt, i) => {
              const h = ((dt.revenue || 0) / maxDailyRev) * 100;
              return (
                <div key={i} className="to-chart-bar-col">
                  <div className="to-chart-bar-amount">{'\u20B9'}{(dt.revenue || 0).toLocaleString()}</div>
                  <div className="to-chart-bar-track"><div className="to-chart-bar to-chart-bar--blue" style={{ height: `${Math.max(h, 3)}%` }}></div></div>
                  <div className="to-chart-bar-label">{new Date(dt.date + 'T00:00:00').toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric' })}</div>
                  <div className="to-chart-bar-sub">{dt.transactions || 0} txns</div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Hourly Payment Distribution */}
      {hourlyPayments.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Peak Booking Hours" subtitle="When do customers pay?" />
          <div className="to-chart-bars">
            {hourlyPayments.map((hp, i) => {
              const h = ((hp.revenue || 0) / maxHourlyRev) * 100;
              return (
                <div key={i} className="to-chart-bar-col">
                  <div className="to-chart-bar-amount">{'\u20B9'}{(hp.revenue || 0).toLocaleString()}</div>
                  <div className="to-chart-bar-track"><div className="to-chart-bar to-chart-bar--red" style={{ height: `${Math.max(h, 3)}%` }}></div></div>
                  <div className="to-chart-bar-label">{hp.label}</div>
                  <div className="to-chart-bar-sub">{hp.transactions || 0} txns</div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Promo Code Analytics */}
      {promoBreakdown.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Promo Code Usage" subtitle={`${s(promoAnalytics.totalPromoUsed)} redemptions · \u20B9${s(promoAnalytics.totalDiscount).toLocaleString()} total discount`} />
          <div className="to-table-wrapper">
            <table className="to-table to-table--striped">
              <thead><tr><th>Promo Code</th><th>Times Used</th><th>Total Discount</th></tr></thead>
              <tbody>
                {promoBreakdown.map((promo, i) => (
                  <tr key={promo.code || i}>
                    <td><span className="to-tag to-tag--gold">{promo.code}</span></td>
                    <td>{s(promo.usageCount)}</td>
                    <td className="to-text-bold">{'\u20B9'}{s(promo.totalDiscount).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Refund Analytics */}
      {s(refundAnalytics.totalRefundCount) > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Refund Analytics" />
          <div className="to-metric-row">
            <div className="to-metric to-metric--danger"><span className="to-metric__val">{s(refundAnalytics.totalRefundCount)}</span><span className="to-metric__label">Total Refunds</span></div>
            <div className="to-metric to-metric--warning"><span className="to-metric__val">{'\u20B9'}{s(refundAnalytics.totalRefundAmount).toLocaleString()}</span><span className="to-metric__label">Total Refund Amount</span></div>
            <div className="to-metric to-metric--info"><span className="to-metric__val">{'\u20B9'}{s(refundAnalytics.avgRefundAmount).toLocaleString()}</span><span className="to-metric__label">Avg Refund</span></div>
          </div>
        </div>
      )}
    </div>
  );
};

// =====================================================
// Analytics Page (Professional, Clean)
// =====================================================
const AnalyticsPage = () => {
  const [analytics, setAnalytics] = useState(null);
  const [seatRevenue, setSeatRevenue] = useState(null);
  const [recStats, setRecStats] = useState(null);
  const [timeSlots, setTimeSlots] = useState(null);
  const [cancellations, setCancellations] = useState(null);
  const [weeklyRevenue, setWeeklyRevenue] = useState(null);
  const [genreData, setGenreData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAnalytics = async () => {
      try {
        const [baseData, seatRev, recSt, timeSlot, cancel, weekly, genre] = await Promise.all([
          theatreOwnerService.getAnalytics(),
          theatreOwnerService.getSeatTypeRevenue().catch(() => null),
          theatreOwnerService.getRecommendationStats().catch(() => null),
          theatreOwnerService.getTimeSlotAnalytics().catch(() => null),
          theatreOwnerService.getCancellationStats().catch(() => null),
          theatreOwnerService.getWeeklyRevenueTrend().catch(() => null),
          theatreOwnerService.getGenrePerformance().catch(() => null)
        ]);
        setAnalytics(baseData);
        setSeatRevenue(seatRev);
        setRecStats(recSt);
        setTimeSlots(timeSlot);
        setCancellations(cancel);
        setWeeklyRevenue(weekly);
        setGenreData(genre);
      } catch (err) {
        toast.error('Failed to load analytics');
      } finally {
        setLoading(false);
      }
    };
    fetchAnalytics();
  }, []);

  if (loading) return <div className="to-loading">Loading analytics...</div>;
  if (!analytics || typeof analytics !== 'object') return <div className="to-empty">No analytics available.</div>;

  // Safe accessor: ensures only primitives are rendered
  const safe = (val, fallback = 0) => (typeof val === 'object' && val !== null) ? fallback : (val ?? fallback);

  const moviePerformance = Array.isArray(analytics.moviePerformance) ? analytics.moviePerformance : [];
  const dailyTrend = Array.isArray(analytics.dailyTrend) ? analytics.dailyTrend : [];
  const sortedMovies = [...moviePerformance].sort((a, b) => (b.revenue || 0) - (a.revenue || 0));
  const maxRevenue = sortedMovies.length > 0 ? Math.max(...sortedMovies.map(m => m.revenue || 0)) : 1;
  const seatTypeColors = { CLASSIC: '#607D8B', PREMIUM: '#7B1FA2', GOLD: '#FBC02D', SILVER: '#9E9E9E', COUPLE: '#E91E63' };

  // Normalize data safely
  const normalizedTimeSlots = Array.isArray(timeSlots) ? timeSlots : (timeSlots ? Object.entries(timeSlots).map(([k, v]) => ({ timeSlot: k, ...(typeof v === 'object' && v ? v : { showCount: v }) })) : []);
  const normalizedGenreData = Array.isArray(genreData) ? genreData : (genreData ? Object.entries(genreData).map(([genre, d]) => ({ genre, ...(typeof d === 'object' && d ? d : { showCount: d }) })) : []);
  const normalizedWeekly = Array.isArray(weeklyRevenue) ? weeklyRevenue : [];

  return (
    <div className="to-analytics">
      <div className="to-page-header"><h2>Theatre Analytics</h2></div>

      {/* KPI Cards */}
      <div className="to-kpi-grid">
        <div className="to-kpi-card to-kpi-card--shows">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Total Shows</span><span className="to-kpi-card__badge">{safe(analytics.todaysShows)} today</span></div>
          <div className="to-kpi-card__value">{safe(analytics.totalShows)}</div>
        </div>
        <div className="to-kpi-card to-kpi-card--bookings">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Total Bookings</span></div>
          <div className="to-kpi-card__value">{safe(analytics.totalBookings)}</div>
        </div>
        <div className="to-kpi-card to-kpi-card--revenue">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Total Revenue</span><span className="to-kpi-card__badge">₹{safe(analytics.todaysRevenue).toLocaleString()} today</span></div>
          <div className="to-kpi-card__value">₹{safe(analytics.totalRevenue).toLocaleString()}</div>
        </div>
        <div className="to-kpi-card to-kpi-card--occupancy">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Occupancy Rate</span><span className="to-kpi-card__badge">{safe(analytics.bookedSeats)} / {safe(analytics.totalSeats)}</span></div>
          <div className="to-kpi-card__value">{Number(safe(analytics.occupancyRate)).toFixed(1)}%</div>
          <div className="to-kpi-card__bar"><div className="to-kpi-card__bar-fill" style={{ width: `${Math.min(safe(analytics.occupancyRate), 100)}%` }}></div></div>
        </div>
        <div className="to-kpi-card to-kpi-card--upcoming">
          <div className="to-kpi-card__top"><span className="to-kpi-card__label">Upcoming Shows</span><span className="to-kpi-card__badge">next 7 days</span></div>
          <div className="to-kpi-card__value">{safe(analytics.upcomingShows)}</div>
        </div>
        {cancellations && (
          <div className="to-kpi-card to-kpi-card--cancel">
            <div className="to-kpi-card__top"><span className="to-kpi-card__label">Cancellation Rate</span><span className="to-kpi-card__badge">{cancellations.cancelledTickets || 0} cancelled</span></div>
            <div className="to-kpi-card__value">{(cancellations.cancellationRate || 0).toFixed(1)}%</div>
          </div>
        )}
      </div>

      {/* Seat Type Revenue */}
      {seatRevenue && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Seat Type Revenue Breakdown" subtitle={seatRevenue.theatreName ? `${seatRevenue.theatreName} · Total: ₹${(seatRevenue.totalRevenue || 0).toLocaleString()}` : null} />
          <div className="to-table-wrapper">
            <table className="to-table to-table--striped">
              <thead><tr><th>Seat Type</th><th>Total Seats</th><th>Booked</th><th>Revenue</th><th>Occupancy</th><th>Revenue Share</th></tr></thead>
              <tbody>
                {(seatRevenue.seatTypeBreakdown || []).map((item, i) => (
                  <tr key={item.seatType || i}>
                    <td><span className="to-seat-type-cell"><span className="to-dot" style={{ background: seatTypeColors[item.seatType] || '#999' }}></span><strong>{item.seatType}</strong></span></td>
                    <td>{item.totalSeats || 0}</td>
                    <td>{item.bookedSeats || 0}</td>
                    <td className="to-text-bold">₹{(item.revenue || 0).toLocaleString()}</td>
                    <td>{(item.occupancyRate || 0).toFixed(1)}%</td>
                    <td><div className="to-progress-bar"><div className="to-progress-bar__fill" style={{ width: `${item.revenueShare || 0}%`, background: seatTypeColors[item.seatType] || '#999' }}></div><span className="to-progress-bar__text">{(item.revenueShare || 0).toFixed(0)}%</span></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Recommendation Stats */}
      {recStats && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Recommendation Performance" />
          <div className="to-metric-row">
            <div className="to-metric"><span className="to-metric__val">{recStats.totalRecommendations || recStats.total || 0}</span><span className="to-metric__label">Total Received</span></div>
            <div className="to-metric to-metric--success"><span className="to-metric__val">{recStats.accepted || 0}</span><span className="to-metric__label">Accepted</span></div>
            <div className="to-metric to-metric--danger"><span className="to-metric__val">{recStats.rejected || 0}</span><span className="to-metric__label">Rejected</span></div>
            <div className="to-metric to-metric--warning"><span className="to-metric__val">{recStats.pending || 0}</span><span className="to-metric__label">Pending</span></div>
            <div className="to-metric to-metric--purple"><span className="to-metric__val">{(recStats.acceptanceRate || 0).toFixed(1)}%</span><span className="to-metric__label">Acceptance Rate</span></div>
            {recStats.conversionRate != null && (
              <div className="to-metric to-metric--info"><span className="to-metric__val">{(recStats.conversionRate || 0).toFixed(1)}%</span><span className="to-metric__label">Conversion Rate</span><span className="to-metric__sub">{recStats.acceptedWithShows || 0} shows</span></div>
            )}
          </div>
        </div>
      )}

      {/* Time Slot Analytics */}
      {normalizedTimeSlots.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Time Slot Performance" />
          <div className="to-timeslot-grid">
            {normalizedTimeSlots.map((slot, idx) => {
              const name = slot.timeSlot || `Slot ${idx + 1}`;
              const colors = { 'Morning': '#FF9800', 'Afternoon': '#F44336', 'Evening': '#7B1FA2', 'Night': '#1565C0' };
              const matchKey = Object.keys(colors).find(k => name.includes(k)) || 'Evening';
              return (
                <div key={idx} className="to-timeslot-card" style={{ '--ts-accent': colors[matchKey] }}>
                  <div className="to-timeslot-card__header">{name}</div>
                  <div className="to-timeslot-card__stats">
                    <div className="to-timeslot-stat"><span className="to-timeslot-stat__val">{slot.showCount || 0}</span><span className="to-timeslot-stat__label">Shows</span></div>
                    <div className="to-timeslot-stat"><span className="to-timeslot-stat__val">{slot.totalBookings || 0}</span><span className="to-timeslot-stat__label">Bookings</span></div>
                    <div className="to-timeslot-stat"><span className="to-timeslot-stat__val">₹{(slot.revenue || 0).toLocaleString()}</span><span className="to-timeslot-stat__label">Revenue</span></div>
                  </div>
                  {slot.occupancyRate != null && (
                    <div className="to-timeslot-card__occ">
                      <div className="to-timeslot-card__occ-bar"><div style={{ width: `${Math.min(slot.occupancyRate || 0, 100)}%` }}></div></div>
                      <span>{(slot.occupancyRate || 0).toFixed(1)}% occupancy</span>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Weekly Revenue Trend */}
      {normalizedWeekly.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Weekly Revenue Trend" subtitle="Last 4 weeks" />
          <div className="to-chart-bars">
            {normalizedWeekly.map((wk, i) => {
              const maxWkRev = Math.max(...normalizedWeekly.map(w => w.revenue || 0), 1);
              const height = ((wk.revenue || 0) / maxWkRev) * 100;
              return (
                <div key={i} className="to-chart-bar-col">
                  <div className="to-chart-bar-amount">₹{(wk.revenue || 0).toLocaleString()}</div>
                  <div className="to-chart-bar-track"><div className="to-chart-bar to-chart-bar--blue" style={{ height: `${Math.max(height, 3)}%` }}></div></div>
                  <div className="to-chart-bar-label">{wk.label || wk.week || `Week ${i + 1}`}</div>
                  <div className="to-chart-bar-sub">{wk.bookings || 0} bookings</div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Genre Performance */}
      {normalizedGenreData.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Genre Performance" />
          <div className="to-table-wrapper">
            <table className="to-table to-table--striped">
              <thead><tr><th>Genre</th><th>Shows</th><th>Bookings</th><th>Revenue</th><th>Occupancy</th><th>Share</th></tr></thead>
              <tbody>
                {(() => {
                  const maxGRev = Math.max(...normalizedGenreData.map(e => e.revenue || 0), 1);
                  return [...normalizedGenreData].sort((a, b) => (b.revenue || 0) - (a.revenue || 0)).map((g, i) => (
                    <tr key={g.genre || i}>
                      <td><strong>{g.genre}</strong></td>
                      <td>{g.showCount || g.shows || 0}</td>
                      <td>{g.bookings || 0}</td>
                      <td className="to-text-bold">₹{(g.revenue || 0).toLocaleString()}</td>
                      <td>{(g.occupancyRate || 0).toFixed(1)}%</td>
                      <td><div className="to-progress-bar"><div className="to-progress-bar__fill to-progress-bar__fill--genre" style={{ width: `${maxGRev > 0 ? ((g.revenue || 0) / maxGRev) * 100 : 0}%` }}></div></div></td>
                    </tr>
                  ));
                })()}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Movie Performance */}
      {sortedMovies.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Movie Performance" />
          <div className="to-table-wrapper">
            <table className="to-table to-table--striped">
              <thead><tr><th>#</th><th>Movie</th><th>Shows</th><th>Bookings</th><th>Revenue</th><th>Revenue Share</th></tr></thead>
              <tbody>
                {sortedMovies.map((mp, i) => (
                  <tr key={mp.movieId || i}>
                    <td>{i + 1}</td>
                    <td><strong>{mp.movieName || 'N/A'}</strong></td>
                    <td>{mp.showCount || 0}</td>
                    <td>{mp.bookings || 0}</td>
                    <td className="to-text-bold">₹{(mp.revenue || 0).toLocaleString()}</td>
                    <td><div className="to-progress-bar"><div className="to-progress-bar__fill" style={{ width: `${maxRevenue > 0 ? ((mp.revenue || 0) / maxRevenue) * 100 : 0}%` }}></div></div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Daily Booking Trends */}
      {dailyTrend.length > 0 && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Daily Booking Trends" subtitle="Last 7 days" />
          <div className="to-chart-bars">
            {dailyTrend.map((dt, i) => {
              const maxB = Math.max(...dailyTrend.map(d => d.bookings || 0), 1);
              const h = ((dt.bookings || 0) / maxB) * 100;
              return (
                <div key={i} className="to-chart-bar-col">
                  <div className="to-chart-bar-amount">{dt.bookings || 0}</div>
                  <div className="to-chart-bar-track"><div className="to-chart-bar to-chart-bar--red" style={{ height: `${Math.max(h, 3)}%` }}></div></div>
                  <div className="to-chart-bar-label">{new Date(dt.date + 'T00:00:00').toLocaleDateString('en-IN', { weekday: 'short', month: 'short', day: 'numeric' })}</div>
                  <div className="to-chart-bar-sub">₹{(dt.revenue || 0).toLocaleString()}</div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Cancellation Statistics */}
      {cancellations && (
        <div className="to-card to-card--elevated">
          <SectionHeader title="Cancellation Statistics" />
          <div className="to-metric-row">
            <div className="to-metric"><span className="to-metric__val">{cancellations.totalTickets || 0}</span><span className="to-metric__label">Total Tickets</span></div>
            <div className="to-metric to-metric--success"><span className="to-metric__val">{cancellations.activeTickets || 0}</span><span className="to-metric__label">Active</span></div>
            <div className="to-metric to-metric--danger"><span className="to-metric__val">{cancellations.cancelledTickets || 0}</span><span className="to-metric__label">Cancelled</span></div>
            <div className="to-metric to-metric--warning"><span className="to-metric__val">₹{(cancellations.totalRefunds || cancellations.refundedAmount || 0).toLocaleString()}</span><span className="to-metric__label">Total Refunds</span></div>
            <div className="to-metric to-metric--danger"><span className="to-metric__val">{(cancellations.cancellationRate || 0).toFixed(1)}%</span><span className="to-metric__label">Cancel Rate</span></div>
          </div>
        </div>
      )}
    </div>
  );
};

// =====================================================
// Main Layout
// =====================================================
const TheaterOwnerDashboard = () => {
  const [theatre, setTheatre] = useState(null);

  useEffect(() => {
    const fetchTheatre = async () => {
      try {
        const data = await theatreOwnerService.getDashboard();
        if (data?.theatre) setTheatre(data.theatre);
      } catch (err) {
        console.error('Failed to load theatre info:', err);
      }
    };
    fetchTheatre();
  }, []);

  return (
    <div className="to-layout">
      <Sidebar theatre={theatre} />
      <div className="to-main">
        <Routes>
          <Route path="/" element={<DashboardOverview theatreInfo={theatre} />} />
          <Route path="/shows" element={<ShowsPage />} />
          <Route path="/recommendations" element={<RecommendationsPage />} />
          <Route path="/payments" element={<ErrorBoundary><PaymentAnalyticsPage /></ErrorBoundary>} />
          <Route path="/analytics" element={<ErrorBoundary><AnalyticsPage /></ErrorBoundary>} />
        </Routes>
      </div>
    </div>
  );
};

export default TheaterOwnerDashboard;
