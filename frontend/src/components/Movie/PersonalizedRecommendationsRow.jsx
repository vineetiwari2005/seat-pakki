import React from 'react';
import MovieCard from './MovieCard';
import './PersonalizedRecommendationsRow.scss';

const PersonalizedRecommendationsRow = ({ movies = [], loading = false, title = 'Recommended For You' }) => {
  if (loading) {
    return (
      <section className="recommendation-row">
        <div className="recommendation-row__header">
          <h2>{title}</h2>
        </div>
        <div className="recommendation-row__loading">Loading personalized recommendations...</div>
      </section>
    );
  }

  if (!Array.isArray(movies) || movies.length === 0) {
    return null;
  }

  return (
    <section className="recommendation-row">
      <div className="recommendation-row__header">
        <h2>{title}</h2>
      </div>

      <div className="recommendation-row__strip" role="list" aria-label={title}>
        {movies.map((movie) => (
          <div className="recommendation-row__item" key={movie.id} role="listitem">
            <MovieCard movie={movie} />
          </div>
        ))}
      </div>
    </section>
  );
};

export default PersonalizedRecommendationsRow;
