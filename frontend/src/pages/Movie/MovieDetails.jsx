import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FaStar, FaClock, FaCalendar } from 'react-icons/fa';
import { useApp } from '../../context/AppContext';
import { useAuth } from '../../context/AuthContext';
import { createMoviePlaceholder } from '../../utils/imageUtils';
import api from '../../services/api';
import { showService } from '../../services';
import './MovieDetails.scss';

const MovieDetails = () => {
  const { movieName } = useParams();
  const navigate = useNavigate();
  const { selectedCity } = useApp();
  const { isAuthenticated } = useAuth();
  const [movie, setMovie] = useState(null);
  const [shows, setShows] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchMovieAndShows();
  }, [movieName, selectedCity]);

  const fetchMovieAndShows = async () => {
    try {
      setLoading(true);
      const decodedMovieName = decodeURIComponent(movieName);
      
      // Fetch movie details from database
      const movieResponse = await api.get(`/movie/${decodedMovieName}`);
      const movieData = movieResponse.data;
      setMovie(movieData);
      
      // Fetch shows for this movie from database
      if (movieData?.id) {
        const showsResponse = await showService.getShowsByMovie(movieData.id);
        const showsData = Array.isArray(showsResponse) ? showsResponse : [];
        
        // Filter shows by selected city if needed
        const filteredShows = showsData.filter(show => {
          if (selectedCity === 'All Cities') return true;
          return show.theater?.city === selectedCity || show.theaterCity === selectedCity;
        });
        
        setShows(filteredShows);
        console.log(`📽️ Loaded ${filteredShows.length} shows for ${decodedMovieName} in ${selectedCity}`);
      }
    } catch (error) {
      console.error('Error fetching movie details:', error);
      setMovie(null);
      setShows([]);
    } finally {
      setLoading(false);
    }
  };

  const handleBookNow = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/movie/${movieName}/shows` } } });
    } else {
      navigate(`/movie/${movieName}/shows`);
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="spinner"></div>
        <p>Loading movie details...</p>
      </div>
    );
  }

  if (!movie) {
    return (
      <div className="loading-container">
        <div className="error-message">
          <h2>Movie Not Found</h2>
          <p>The movie "{decodeURIComponent(movieName)}" could not be found.</p>
          <button className="btn btn-primary" onClick={() => navigate('/')}>
            Go to Home
          </button>
        </div>
      </div>
    );
  }

  const getImageUrl = (width, height) => {
    if (movie.posterUrl && 
        movie.posterUrl !== 'null' && 
        movie.posterUrl !== 'undefined' && 
        !movie.posterUrl.includes('null') && 
        !movie.posterUrl.includes('undefined') &&
        movie.posterUrl.startsWith('http')) {
      return movie.posterUrl;
    }
    return createMoviePlaceholder(movie.movieName || movie.name, width, height);
  };

  return (
    <div className="movie-details-page">
      <div className="movie-banner" style={{ backgroundImage: `url(${getImageUrl(1200, 600)})` }}>
        <div className="banner-overlay"></div>
        <div className="container">
          <div className="movie-header">
            <img 
              src={getImageUrl(300, 450)} 
              alt={movie.movieName || movie.name} 
              className="movie-poster-large"
              onError={(e) => {
                e.target.src = createMoviePlaceholder(movie.movieName || movie.name, 300, 450);
              }}
            />
            <div className="movie-info-main">
              <h1>{movie.movieName || movie.name}</h1>
              <div className="movie-meta-large">
                <span><FaStar /> {movie.rating}/10</span>
                <span><FaClock /> {movie.duration} min</span>
                <span>{Array.isArray(movie.genre) ? movie.genre.join(', ') : movie.genre}</span>
                <span>{movie.language}</span>
              </div>
              <p className="movie-description-large">{movie.description}</p>
              <div className="movie-cast">
                <strong>Cast:</strong> {Array.isArray(movie.cast) ? movie.cast.join(', ') : movie.cast}
              </div>
              <div className="movie-director">
                <strong>Director:</strong> {movie.director}
              </div>
              <button className="btn btn-primary btn-lg" onClick={handleBookNow}>
                Book Tickets
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Why Book With Us Section */}
      <div className="why-book-with-us">
        <div className="container">
          <h2 className="section-title">Why Book With SeatPakki?</h2>
          <div className="benefits-grid">
            <div className="benefit-card">
              <div className="benefit-icon">🎟️</div>
              <h3>Easy Booking</h3>
              <p>Simple and fast booking process with secure payment options</p>
            </div>
            <div className="benefit-card">
              <div className="benefit-icon">💰</div>
              <h3>Best Prices</h3>
              <p>Competitive pricing with exclusive deals and offers</p>
            </div>
            <div className="benefit-card">
              <div className="benefit-icon">🎫</div>
              <h3>Instant Confirmation</h3>
              <p>Get your tickets instantly with QR code access</p>
            </div>
            <div className="benefit-card">
              <div className="benefit-icon">🔒</div>
              <h3>Secure Payments</h3>
              <p>Multiple payment options with 100% secure transactions</p>
            </div>
            <div className="benefit-card">
              <div className="benefit-icon">🍿</div>
              <h3>Food & Parking</h3>
              <p>Pre-book your food and parking for a seamless experience</p>
            </div>
            <div className="benefit-card">
              <div className="benefit-icon">⚡</div>
              <h3>Quick Refunds</h3>
              <p>Hassle-free cancellation with instant wallet refunds</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default MovieDetails;
