import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaStar, FaClock } from 'react-icons/fa';
import { createMoviePlaceholder } from '../../utils/imageUtils';
import './MovieCard.scss';

const MovieCard = ({ movie }) => {
  const [imageSrc, setImageSrc] = useState('');
  const [imageError, setImageError] = useState(false);

  useEffect(() => {
    // Check if posterUrl is valid
    if (movie.posterUrl && 
        movie.posterUrl !== 'null' && 
        movie.posterUrl !== 'undefined' && 
        !movie.posterUrl.includes('null') && 
        !movie.posterUrl.includes('undefined') &&
        movie.posterUrl.startsWith('http')) {
      setImageSrc(movie.posterUrl);
      setImageError(false);
    } else {
      // Use canvas-generated placeholder
      setImageSrc(createMoviePlaceholder(movie.movieName || 'Movie', 300, 450));
      setImageError(true);
    }
  }, [movie.posterUrl, movie.movieName]);

  const handleImageError = (e) => {
    if (!imageError) {
      setImageError(true);
      setImageSrc(createMoviePlaceholder(movie.movieName || 'Movie', 300, 450));
    }
  };

  return (
    <Link to={`/movie/${encodeURIComponent(movie.movieName)}`} className="movie-card">
      <div className="movie-poster">
        <img 
          src={imageSrc} 
          alt={movie.movieName} 
          loading="lazy"
          onError={handleImageError}
        />
        <div className="movie-overlay">
          <button className="btn btn-primary btn-sm">Book Now</button>
        </div>
        {movie.nowShowing && (
          <div className="now-showing-badge">Now Showing</div>
        )}
      </div>
      
      <div className="movie-info">
        <h3 className="movie-title">{movie.movieName}</h3>
        
        <div className="movie-meta">
          <span className="rating">
            <FaStar /> {movie.rating}
          </span>
          <span className="duration">
            <FaClock /> {movie.duration} min
          </span>
        </div>
        
        <div className="movie-genres">
          <span className="genre-badge">{movie.genre}</span>
        </div>
        
        <div className="movie-language">{movie.language}</div>
      </div>
    </Link>
  );
};

export default MovieCard;
