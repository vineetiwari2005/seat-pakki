import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FaSearch, FaUserCircle, FaBars, FaTimes, FaMapMarkerAlt } from 'react-icons/fa';
import { useAuth } from '../../context/AuthContext';
import { useApp } from '../../context/AppContext';
import './Navbar.scss';

const Navbar = () => {
  const { isAuthenticated, user, logout, isAdmin, isTheaterOwner } = useAuth();
  const { selectedCity, cities, changeCity, searchQuery, setSearchQuery } = useApp();
  const navigate = useNavigate();
  
  console.log('🧭 Navbar rendering. User:', user?.name || 'Not logged in', 'City:', selectedCity);
  
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [cityDropdownOpen, setCityDropdownOpen] = useState(false);
  const [userDropdownOpen, setUserDropdownOpen] = useState(false);

  const handleSearch = (e) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/?search=${searchQuery}`);
      setMobileMenuOpen(false);
    }
  };

  const handleCityChange = (city) => {
    changeCity(city);
    setCityDropdownOpen(false);
  };

  const handleLogout = () => {
    logout();
    setUserDropdownOpen(false);
    setMobileMenuOpen(false);
    navigate('/');
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <div className="navbar-left">
          <Link to="/" className="navbar-logo">
            <span className="logo-text">Seat</span>
            <span className="logo-accent">Pakki</span>
          </Link>
          
          <div className={`navbar-search ${mobileMenuOpen ? 'mobile-open' : ''}`}>
            <form onSubmit={handleSearch}>
              <FaSearch className="search-icon" />
              <input
                type="text"
                placeholder="Search for Movies, Events, Plays, Sports..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </form>
          </div>
        </div>

        <div className={`navbar-right ${mobileMenuOpen ? 'mobile-open' : ''}`}>
          <div className="city-selector" onClick={() => setCityDropdownOpen(!cityDropdownOpen)}>
            <FaMapMarkerAlt />
            <span>{selectedCity}</span>
            {cityDropdownOpen && (
              <div className="city-dropdown">
                {cities.map((city) => (
                  <div
                    key={city.id}
                    className={`city-item ${city.name === selectedCity ? 'active' : ''}`}
                    onClick={() => handleCityChange(city.name)}
                  >
                    {city.name}
                  </div>
                ))}
              </div>
            )}
          </div>

          {isAuthenticated ? (
            <div className="user-menu" onClick={() => setUserDropdownOpen(!userDropdownOpen)}>
              <FaUserCircle className="user-icon" />
              <span className="user-name">{user?.name || 'User'}</span>
              {userDropdownOpen && (
                <div className="user-dropdown">
                  <Link to="/profile" onClick={() => setUserDropdownOpen(false)}>
                    My Profile
                  </Link>
                  <Link to="/my-bookings" onClick={() => setUserDropdownOpen(false)}>
                    My Bookings
                  </Link>
                  <Link to="/transactions" onClick={() => setUserDropdownOpen(false)}>
                    Transaction History
                  </Link>
                  {isAdmin() && (
                    <Link to="/admin" onClick={() => setUserDropdownOpen(false)}>
                      Admin Dashboard
                    </Link>
                  )}
                  {isTheaterOwner() && (
                    <Link to="/theater-owner" onClick={() => setUserDropdownOpen(false)}>
                      Theater Dashboard
                    </Link>
                  )}
                  <div className="dropdown-divider"></div>
                  <button onClick={handleLogout} className="logout-btn">
                    Logout
                  </button>
                </div>
              )}
            </div>
          ) : (
            <div className="auth-buttons">
              <Link to="/login" className="btn-login">
                Sign In
              </Link>
            </div>
          )}
        </div>

        <button 
          className="mobile-menu-btn"
          onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
        >
          {mobileMenuOpen ? <FaTimes /> : <FaBars />}
        </button>
      </div>
    </nav>
  );
};

export default Navbar;
