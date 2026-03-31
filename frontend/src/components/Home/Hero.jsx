import React, { useState, useEffect } from 'react';
import { movieService } from '../../services';
import { createMoviePlaceholder } from '../../utils/imageUtils';
import { useApp } from '../../context/AppContext';
import './Hero.scss';

const Hero = () => {
  const { selectedCity } = useApp();
  const [featuredMovies, setFeaturedMovies] = useState([]);
  const [currentSlide, setCurrentSlide] = useState(0);
  const [loading, setLoading] = useState(true);

  console.log('🎭 Hero component rendering for city:', selectedCity);

  useEffect(() => {
    fetchFeaturedMovies();
  }, [selectedCity]);

  const fetchFeaturedMovies = async () => {
    try {
      setLoading(true);
      let movies = [];
      
      // Fetch movies based on selected city
      if (selectedCity && selectedCity !== 'All Cities') {
        movies = await movieService.getMoviesByCity(selectedCity);
      } else {
        movies = await movieService.getNowShowing();
      }
      
      // Ensure movies is an array
      if (!Array.isArray(movies)) {
        console.warn('⚠️ Backend returned non-array response for featured movies:', movies);
        movies = [];
      }
      
      // Get top 5 highest rated movies for hero banner
      const topMovies = movies
        .filter(m => m.posterUrl) // Only movies with poster images
        .sort((a, b) => (b.rating || 0) - (a.rating || 0))
        .slice(0, 5);
      
      setFeaturedMovies(topMovies);
      console.log('🎬 Featured movies loaded:', topMovies.length, 'for city:', selectedCity);
    } catch (error) {
      console.error('Error loading featured movies:', error);
      setFeaturedMovies([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (featuredMovies.length === 0) return;
    
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % featuredMovies.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [featuredMovies.length]);

  const getBannerImageUrl = (movie) => {
    if (movie?.posterUrl && 
        movie.posterUrl !== 'null' && 
        movie.posterUrl !== 'undefined' && 
        !movie.posterUrl.includes('null') && 
        !movie.posterUrl.includes('undefined') &&
        movie.posterUrl.startsWith('http')) {
      return movie.posterUrl;
    }
    return createMoviePlaceholder(movie?.movieName || 'Movie', 1200, 600);
  };

  if (loading) {
    return (
      <div className="hero" style={{ minHeight: '400px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <p>Loading featured movies...</p>
      </div>
    );
  }

  if (featuredMovies.length === 0) {
    return null;
  }

  const movie = featuredMovies[currentSlide];
  console.log('🎬 Current hero slide:', movie?.movieName, 'posterUrl:', movie?.posterUrl);

  return (
    <div className="hero">
      <div 
        className="hero-background"
        style={{ 
          backgroundImage: `url(${getBannerImageUrl(movie)})`,
          backgroundSize: 'cover',
          backgroundPosition: 'center',
          backgroundRepeat: 'no-repeat'
        }}
      >
        <div className="hero-overlay"></div>
      </div>
      
      <div className="hero-content">
        <div className="container">
          <div className="hero-text">
            <span className="hero-badge">Now Showing</span>
            <h1 className="hero-title">{movie.movieName}</h1>
            <p className="hero-description">{movie.description}</p>
            
            <div className="hero-meta">
              <span className="meta-item">
                <strong>Rating:</strong> {movie.rating}/10
              </span>
              <span className="meta-item">
                <strong>Duration:</strong> {movie.duration} min
              </span>
              <span className="meta-item">
                <strong>Language:</strong> {movie.language}
              </span>
            </div>
          </div>
        </div>
      </div>
      
      <div className="hero-indicators">
        {featuredMovies.map((_, index) => (
          <button
            key={index}
            className={`indicator ${index === currentSlide ? 'active' : ''}`}
            onClick={() => setCurrentSlide(index)}
          />
        ))}
      </div>
    </div>
  );
};

export default Hero;
