import React, { useState } from 'react';
import { FaFilter, FaTimes } from 'react-icons/fa';
import { useApp } from '../../context/AppContext';
import './FilterBar.scss';

const FilterBar = () => {
  const { filters, updateFilters, resetFilters } = useApp();
  const [showFilters, setShowFilters] = useState(false);

  const genres = ['ACTION', 'COMEDY', 'DRAMA', 'THRILLER', 'ROMANCE', 'HORROR', 'FANTASY', 'ADVENTURE', 'BIOGRAPHY', 'HISTORICAL', 'MYTHOLOGY'];
  const languages = ['HINDI', 'ENGLISH', 'TAMIL', 'TELUGU', 'KANNADA', 'MALAYALAM', 'BENGALI', 'MARATHI'];

  const handleFilterChange = (filterType, value) => {
    updateFilters({ [filterType]: value });
  };

  const hasActiveFilters = filters.genre || filters.language || filters.minRating > 0;

  return (
    <div className="filter-bar">
      <button 
        className="filter-toggle"
        onClick={() => setShowFilters(!showFilters)}
      >
        <FaFilter /> Filters
        {hasActiveFilters && <span className="filter-count">Active</span>}
      </button>

      <div className={`filter-panel ${showFilters ? 'open' : ''}`}>
        <div className="filter-header">
          <h3>Filters</h3>
          <button className="close-btn" onClick={() => setShowFilters(false)}>
            <FaTimes />
          </button>
        </div>

        <div className="filter-section">
          <h4>Genre</h4>
          <div className="filter-options">
            <button
              className={`filter-option ${!filters.genre ? 'active' : ''}`}
              onClick={() => handleFilterChange('genre', '')}
            >
              All
            </button>
            {genres.map(genre => (
              <button
                key={genre}
                className={`filter-option ${filters.genre === genre ? 'active' : ''}`}
                onClick={() => handleFilterChange('genre', genre)}
              >
                {genre}
              </button>
            ))}
          </div>
        </div>

        <div className="filter-section">
          <h4>Language</h4>
          <div className="filter-options">
            <button
              className={`filter-option ${!filters.language ? 'active' : ''}`}
              onClick={() => handleFilterChange('language', '')}
            >
              All
            </button>
            {languages.map(language => (
              <button
                key={language}
                className={`filter-option ${filters.language === language ? 'active' : ''}`}
                onClick={() => handleFilterChange('language', language)}
              >
                {language}
              </button>
            ))}
          </div>
        </div>

        <div className="filter-section">
          <h4>Minimum Rating</h4>
          <div className="rating-slider">
            <input
              type="range"
              min="0"
              max="10"
              step="0.5"
              value={filters.minRating}
              onChange={(e) => handleFilterChange('minRating', parseFloat(e.target.value))}
            />
            <div className="rating-value">{filters.minRating > 0 ? `${filters.minRating}+` : 'Any'}</div>
          </div>
        </div>

        <div className="filter-actions">
          <button className="btn btn-secondary" onClick={resetFilters}>
            Reset All
          </button>
          <button className="btn btn-primary" onClick={() => setShowFilters(false)}>
            Apply Filters
          </button>
        </div>
      </div>

      {showFilters && (
        <div className="filter-overlay" onClick={() => setShowFilters(false)} />
      )}
    </div>
  );
};

export default FilterBar;
