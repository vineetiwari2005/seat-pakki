import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { FaStar, FaClock, FaMapMarkerAlt, FaTheaterMasks, FaChair, FaCheckCircle, FaExclamationCircle, FaTimesCircle } from 'react-icons/fa';
import { useApp } from '../../context/AppContext';
import { movieService, showService } from '../../services';
import { createMoviePlaceholder } from '../../utils/imageUtils';
import './ShowSelection.scss';

const ShowSelection = () => {
  const { movieName } = useParams();
  const navigate = useNavigate();
  const { selectedCity } = useApp();
  const [movie, setMovie] = useState(null);
  const [selectedDate, setSelectedDate] = useState(null);
  const [allShows, setAllShows] = useState([]); // All shows from DB for this movie
  const [theatersWithShows, setTheatersWithShows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    time: 'all',
    language: 'all',
    screen: 'all'
  });

  // Build date list: 7 continuous days from today + only specific dates with shows beyond that
  const dates = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // Always show first 7 continuous days
    const result = [];
    for (let i = 0; i < 7; i++) {
      const d = new Date(today);
      d.setDate(d.getDate() + i);
      result.push(d);
    }

    // Collect show dates that fall AFTER the 7-day window
    // Only include dates that have shows in the user's selected city
    const sevenDayEnd = new Date(today);
    sevenDayEnd.setDate(sevenDayEnd.getDate() + 7);
    sevenDayEnd.setHours(0, 0, 0, 0);

    const extraDatesSet = new Set();
    allShows.forEach(show => {
      const showDate = show.date || show.showDate;
      if (showDate) {
        const d = new Date(showDate + 'T00:00:00');
        if (d >= sevenDayEnd) {
          // Only add this date if the show's city matches the user's selected city
          const showCity = show.theaterCity || show.theater?.city || show.city || '';
          const cityMatch = !selectedCity || selectedCity === 'All Cities' || 
            showCity.toLowerCase() === selectedCity.toLowerCase() ||
            showCity.toLowerCase().includes(selectedCity.toLowerCase()) ||
            selectedCity.toLowerCase().includes(showCity.toLowerCase());
          if (cityMatch) {
            extraDatesSet.add(showDate);
          }
        }
      }
    });

    // Sort and add only the show dates beyond 7 days
    const extraDates = Array.from(extraDatesSet)
      .sort()
      .map(ds => new Date(ds + 'T00:00:00'));
    result.push(...extraDates);

    return result;
  }, [allShows, selectedCity]);


  // Set default date to today on mount
  useEffect(() => {
    const today = new Date();
    const localDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
    setSelectedDate(localDate);
  }, []);

  // Fetch movie and ALL its shows once (depends only on movieName, not on selectedDate)
  useEffect(() => {
    const fetchMovieAndShows = async () => {
      setLoading(true);
      try {
        const decodedMovieName = decodeURIComponent(movieName);
        
        const backendMovies = await movieService.getNowShowing();
        const foundMovie = backendMovies.find(m => 
          (m.movieName && m.movieName === decodedMovieName) || 
          (m.name && m.name === decodedMovieName)
        );
        
        if (foundMovie) {
          setMovie(foundMovie);
          try {
            console.log('Fetching shows from backend for movie:', foundMovie.id);
            const shows = await showService.getShowsByMovie(foundMovie.id);
            console.log('Backend shows received:', shows);
            setAllShows(shows || []);
          } catch (error) {
            console.error('Error fetching shows from backend:', error);
            setAllShows([]);
          }
        } else {
          console.log('Movie not found in backend');
          setMovie(null);
          setAllShows([]);
        }
      } catch (error) {
        console.error('Error fetching data:', error);
        setMovie(null);
        setAllShows([]);
      } finally {
        setLoading(false);
      }
    };

    fetchMovieAndShows();
  }, [movieName]);

  // Filter and organize shows whenever date, city, or allShows change
  useEffect(() => {
    if (!selectedDate || !movie) {
      setTheatersWithShows([]);
      return;
    }

    const theaterMap = new Map();
    
    // Filter shows by selected date AND selected city
    const filteredShows = allShows.filter(show => {
      const showDate = show.date || show.showDate;
      const dateMatch = showDate === selectedDate;
      
      const showCity = show.theaterCity || show.theater?.city || show.city || '';
      const cityMatch = !selectedCity || selectedCity === 'All Cities' || 
        showCity.toLowerCase() === selectedCity.toLowerCase() ||
        showCity.toLowerCase().includes(selectedCity.toLowerCase()) ||
        selectedCity.toLowerCase().includes(showCity.toLowerCase());
      
      return dateMatch && cityMatch;
    });

    console.log(`Filtered shows for date ${selectedDate}, city ${selectedCity}:`, filteredShows.length, 'out of', allShows.length);
    
    filteredShows.forEach(show => {
      const theaterId = show.theaterId;
      const theaterName = show.theaterName || 'Unknown Theater';
      const theaterAddress = show.theaterAddress || '';
      const theaterCity = show.theaterCity || selectedCity;
      
      if (!theaterMap.has(theaterId)) {
        theaterMap.set(theaterId, {
          id: theaterId,
          name: theaterName,
          address: theaterAddress,
          city: theaterCity,
          shows: []
        });
      }
      
      const normalizedShow = {
        id: show.id,
        showTime: (show.time || show.showTime || '').substring(0, 5),
        showDate: show.date || show.showDate,
        availableSeats: show.availableSeats || show.totalSeats || 100,
        screenType: show.screenType || 'Standard'
      };
      
      theaterMap.get(theaterId).shows.push(normalizedShow);
    });

    // Sort shows by time within each theater
    theaterMap.forEach(theater => {
      theater.shows.sort((a, b) => a.showTime.localeCompare(b.showTime));
    });

    setTheatersWithShows(Array.from(theaterMap.values()));
  }, [selectedDate, selectedCity, allShows, movie]);

  const formatDate = (date) => {
    const days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
    const months = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
    
    return {
      day: days[date.getDay()],
      date: date.getDate(),
      month: months[date.getMonth()]
    };
  };

  const handleShowSelect = (show, theater) => {
    navigate(`/booking/${show.id}/seats`, {
      state: {
        movieName: movie.movieName || movie.name,
        theaterName: theater.name,
        showDate: selectedDate,
        showTime: show.showTime,
        showId: show.id,
        theaterId: theater.id,
        movieId: movie.id
      }
    });
  };

  const getSeatsAvailability = (availableSeats) => {
    if (availableSeats === 0) {
      return { status: 'sold-out', text: 'Sold Out', icon: FaTimesCircle };
    } else if (availableSeats < 20) {
      return { status: 'filling-fast', text: `${availableSeats} seats`, icon: FaExclamationCircle };
    } else {
      return { status: 'available', text: `${availableSeats} seats`, icon: FaCheckCircle };
    }
  };

  const filterShows = (shows) => {
    return shows.filter(show => {
      if (filters.time !== 'all') {
        const hour = parseInt(show.showTime.split(':')[0]);
        if (filters.time === 'morning' && (hour < 6 || hour >= 12)) return false;
        if (filters.time === 'afternoon' && (hour < 12 || hour >= 18)) return false;
        if (filters.time === 'evening' && (hour < 18 || hour >= 23)) return false;
        if (filters.time === 'night' && hour >= 6 && hour < 23) return false;
      }
      if (filters.language !== 'all' && show.language !== filters.language) return false;
      if (filters.screen !== 'all' && show.screenType !== filters.screen) return false;
      return true;
    });
  };

  if (loading) {
    return (
      <div className="show-selection-page">
        <div className="container">
          <div className="loading-container">
            <div className="spinner"></div>
          </div>
        </div>
      </div>
    );
  }

  if (!movie) {
    return (
      <div className="show-selection-page">
        <div className="container">
          <div className="no-shows">
            <FaTheaterMasks />
            <h3>Movie not found</h3>
            <p>The movie you're looking for doesn't exist.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="show-selection-page">
      <div className="container">
        {/* Movie Header */}
        <div className="page-header">
          <div className="movie-summary">
            <img 
              src={movie.posterUrl && 
                   movie.posterUrl !== 'null' && 
                   movie.posterUrl !== 'undefined' && 
                   !movie.posterUrl.includes('null') && 
                   !movie.posterUrl.includes('undefined') &&
                   movie.posterUrl.startsWith('http') 
                   ? movie.posterUrl 
                   : createMoviePlaceholder(movie.movieName || movie.name, 100, 140)}
              alt={movie.movieName || movie.name}
              className="movie-poster-small"
              onError={(e) => {
                e.target.src = createMoviePlaceholder(movie.movieName || movie.name, 100, 140);
              }}
            />
            <div className="movie-info">
              <h1>{movie.movieName || movie.name}</h1>
              <div className="movie-meta">
                <span><FaStar /> {movie.rating}/10</span>
                <span><FaClock /> {movie.duration} min</span>
                <span>{Array.isArray(movie.genre) ? movie.genre.join(', ') : movie.genre}</span>
                <span>{movie.language}</span>
              </div>
            </div>
            
            {/* Date Selector */}
            <div className="date-selector">
              {dates.map((date, index) => {
                const formatted = formatDate(date);
                const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
                // Detect gap between consecutive dates for visual separator
                const prevDate = index > 0 ? dates[index - 1] : null;
                const hasGap = prevDate && (date - prevDate) > 2 * 24 * 60 * 60 * 1000;
                return (
                  <React.Fragment key={index}>
                    {hasGap && <div className="date-gap"></div>}
                    <button
                      className={`date-btn ${selectedDate === dateStr ? 'active' : ''}`}
                      onClick={() => setSelectedDate(dateStr)}
                    >
                      <div className="day">{formatted.day}</div>
                      <div className="date">{formatted.date}</div>
                      <div className="month">{formatted.month}</div>

                    </button>
                  </React.Fragment>
                );
              })}
            </div>
          </div>
        </div>

        {/* Filters */}
        <div className="filters">
          <div className="filter-group">
            <label>Time:</label>
            <select value={filters.time} onChange={(e) => setFilters({...filters, time: e.target.value})}>
              <option value="all">All Showtimes</option>
              <option value="morning">Morning (6AM - 12PM)</option>
              <option value="afternoon">Afternoon (12PM - 6PM)</option>
              <option value="evening">Evening (6PM - 11PM)</option>
              <option value="night">Night (11PM - 6AM)</option>
            </select>
          </div>
          <div className="filter-group">
            <label>Language:</label>
            <select value={filters.language} onChange={(e) => setFilters({...filters, language: e.target.value})}>
              <option value="all">All Languages</option>
              <option value="HINDI">Hindi</option>
              <option value="ENGLISH">English</option>
              <option value="TAMIL">Tamil</option>
              <option value="TELUGU">Telugu</option>
              <option value="KANNADA">Kannada</option>
            </select>
          </div>
          <div className="filter-group">
            <label>Screen:</label>
            <select value={filters.screen} onChange={(e) => setFilters({...filters, screen: e.target.value})}>
              <option value="all">All Screens</option>
              <option value="2D">2D</option>
              <option value="3D">3D</option>
              <option value="IMAX">IMAX</option>
              <option value="4DX">4DX</option>
            </select>
          </div>
        </div>

        {/* Theaters List */}
        <div className="theaters-list">
          {theatersWithShows.length === 0 ? (
            <div className="no-shows">
              <FaTheaterMasks />
              <h3>No shows available</h3>
              <p>There are no shows for this movie in {selectedCity} on this date.</p>
              <p>Try selecting a different date or city.</p>
            </div>
          ) : (
            theatersWithShows.map(theater => {
              const filteredShows = filterShows(theater.shows);
              
              if (filteredShows.length === 0) return null;
              
              return (
                <div key={theater.id} className="theater-card">
                  <div className="theater-header">
                    <div className="theater-info">
                      <h3>
                        <FaTheaterMasks />
                        {theater.name}
                      </h3>
                      <div className="theater-address">
                        <FaMapMarkerAlt />
                        {theater.address}
                      </div>
                    </div>
                    <div className="theater-features">
                      <span className="feature-badge">M-Ticket</span>
                      <span className="feature-badge">Food & Beverage</span>
                    </div>
                  </div>
                  
                  <div className="showtimes">
                    {filteredShows.map(show => {
                      const availability = getSeatsAvailability(show.availableSeats);
                      const Icon = availability.icon;
                      
                      return (
                        <button
                          key={show.id}
                          className={`showtime-btn ${show.availableSeats === 0 ? 'disabled' : ''}`}
                          onClick={() => show.availableSeats > 0 && handleShowSelect(show, theater)}
                          disabled={show.availableSeats === 0}
                        >
                          <div className="time">{show.showTime}</div>
                          <div className={`seats-info ${availability.status}`}>
                            <Icon />
                            {availability.text}
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};

export default ShowSelection;
