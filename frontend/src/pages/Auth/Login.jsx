import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import { theaterService } from '../../services';
import './Auth.scss';

const Login = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);
  
  // Theatre Admin selection state
  const [showTheatreSelect, setShowTheatreSelect] = useState(false);
  const [cities, setCities] = useState([]);
  const [theatres, setTheatres] = useState([]);
  const [selectedCity, setSelectedCity] = useState('');
  const [selectedTheatre, setSelectedTheatre] = useState('');

  const from = location.state?.from?.pathname || '/';

  // When city changes, just reset the theatre selection (filtering is done client-side)
  useEffect(() => {
    if (!selectedCity) {
      setSelectedTheatre('');
    }
  }, [selectedCity]);

  // Fetch all theaters to extract cities when theatre select opens
  useEffect(() => {
    if (!showTheatreSelect) return;
    const fetchCities = async () => {
      try {
        const data = await theaterService.getAllTheaters();
        const allTheaters = Array.isArray(data) ? data : [];
        // Extract unique cities (city is an object {id, name})
        const citySet = new Set();
        allTheaters.forEach(t => {
          const cityName = t.cityName || (t.city && t.city.name);
          if (cityName) citySet.add(cityName);
        });
        setCities([...citySet].sort());
        // Store all theatres for reference
        setTheatres(allTheaters);
      } catch (err) {
        console.error('Failed to load cities/theaters:', err);
      }
    };
    fetchCities();
  }, [showTheatreSelect]);

  const handleAdminLogin = async () => {
    setLoading(true);
    const adminCreds = { email: 'admin@gmail.com', password: 'password123' };
    setFormData(adminCreds);
    try {
      const result = await login(adminCreds);
      if (result.success) {
        toast.success('Logged in as Main Admin!');
        navigate('/admin/dashboard');
      } else {
        toast.error(result.error);
      }
    } catch (error) {
      toast.error('Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleTheatreAdminSelect = () => {
    setShowTheatreSelect(true);
  };

  const handleTheatreAdminLogin = async () => {
    setLoading(true);
    const tAdminCreds = { email: 'tadmin@gmail.com', password: 'password123' };
    setFormData(tAdminCreds);
    try {
      const result = await login(tAdminCreds);
      if (result.success) {
        // Store selected theatre ID so all /owner API calls use it
        if (selectedTheatre) {
          localStorage.setItem('selectedTheatreId', selectedTheatre);
        } else {
          localStorage.removeItem('selectedTheatreId');
        }
        const theatreName = selectedTheatre 
          ? theatres.find(t => String(t.id) === String(selectedTheatre))?.name || selectedTheatre 
          : '';
        toast.success(`Logged in as Theatre Admin${theatreName ? ` — ${theatreName}` : ''}!`);
        navigate('/theater-owner');
      } else {
        toast.error(result.error);
      }
    } catch (error) {
      toast.error('Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const result = await login(formData);
      
      if (result.success) {
        toast.success('Login successful!');
        
        // Redirect based on user role
        if (result.user.role === 'ADMIN') {
          navigate('/admin');
        } else if (result.user.role === 'THEATER_OWNER') {
          navigate('/theater-owner');
        } else {
          navigate(from);
        }
      } else {
        toast.error(result.error);
      }
    } catch (error) {
      toast.error('An error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-header">
            <h2>Welcome Back!</h2>
            <p>Login to book your favorite movies</p>
          </div>

          {/* Theatre Admin City/Theatre Selection Panel */}
          {showTheatreSelect && (
            <div className="theatre-select-panel">
              <h3 className="theatre-select-title">🎭 Theatre Admin Login</h3>
              <p className="theatre-select-desc">Select your city and theatre to continue</p>
              
              <div className="theatre-select-form">
                <div className="form-group">
                  <label htmlFor="city">City</label>
                  <select
                    id="city"
                    value={selectedCity}
                    onChange={(e) => {
                      setSelectedCity(e.target.value);
                      setSelectedTheatre('');
                    }}
                  >
                    <option value="">Select City...</option>
                    {cities.map(city => (
                      <option key={city} value={city}>{city}</option>
                    ))}
                  </select>
                </div>
                
                <div className="form-group">
                  <label htmlFor="theatre">Theatre</label>
                  <select
                    id="theatre"
                    value={selectedTheatre}
                    onChange={(e) => setSelectedTheatre(e.target.value)}
                    disabled={!selectedCity}
                  >
                    <option value="">
                      {!selectedCity ? 'Select a city first...' : 'Select Theatre...'}
                    </option>
                    {theatres
                      .filter(t => (t.cityName || (t.city && t.city.name)) === selectedCity)
                      .map(t => (
                        <option key={t.id} value={t.id}>
                          {t.name || t.theatreName} — {t.address}
                        </option>
                      ))}
                  </select>
                </div>

                <div className="theatre-select-actions">
                  <button
                    type="button"
                    className="btn btn-primary btn-lg"
                    onClick={handleTheatreAdminLogin}
                    disabled={loading}
                  >
                    {loading ? 'Logging in...' : 'Login as Theatre Admin'}
                  </button>
                  <button
                    type="button"
                    className="btn btn-text"
                    onClick={() => setShowTheatreSelect(false)}
                  >
                    ← Back to Login
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Normal Login Form */}
          {!showTheatreSelect && (
            <>
              <form onSubmit={handleSubmit} className="auth-form">
                <div className="form-group">
                  <label htmlFor="email">Email Address</label>
                  <input
                    type="email"
                    id="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    required
                    placeholder="Enter your email"
                    autoComplete="email"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="password">Password</label>
                  <input
                    type="password"
                    id="password"
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    required
                    placeholder="Enter your password"
                    autoComplete="current-password"
                  />
                </div>

                <button type="submit" className="btn btn-primary btn-lg" disabled={loading}>
                  {loading ? 'Logging in...' : 'Login'}
                </button>

                <div className="divider">
                  <span>OR</span>
                </div>

                <button 
                  type="button" 
                  className="btn btn-outline btn-lg" 
                  onClick={handleAdminLogin}
                  disabled={loading}
                >
                  Login as Main Admin
                </button>

                <button 
                  type="button" 
                  className="btn btn-outline btn-lg" 
                  onClick={handleTheatreAdminSelect}
                  disabled={loading}
                  style={{marginTop: '10px', borderColor: '#FBC02D', color: '#FBC02D'}}
                >
                  Login as Theatre Admin
                </button>
              </form>

              <div className="auth-footer">
                <p>
                  Don't have an account?{' '}
                  <Link to="/signup" className="auth-link">
                    Sign up now
                  </Link>
                </p>
              </div>
            </>
          )}
        </div>

        <div className="auth-illustration">
          <img 
            src="https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800" 
            alt="Movie Theater"
          />
          <div className="illustration-overlay">
            <h3>Experience Cinema Like Never Before</h3>
            <p>Book tickets for the latest movies, events, and shows</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Login;
