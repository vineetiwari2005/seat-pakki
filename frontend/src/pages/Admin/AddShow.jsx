import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminService } from '../../services';
import { FaTicketAlt, FaArrowLeft, FaSave } from 'react-icons/fa';
import './AdminForms.scss';

const AddShow = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [movies, setMovies] = useState([]);
  const [theaters, setTheaters] = useState([]);
  const [formData, setFormData] = useState({
    movieId: '',
    theaterId: '',
    showDate: '',
    showTime: ''
  });

  useEffect(() => {
    fetchMoviesAndTheaters();
  }, []);

  const fetchMoviesAndTheaters = async () => {
    try {
      const [moviesData, theatersData] = await Promise.all([
        adminService.getAllMovies(),
        adminService.getAllTheaters()
      ]);
      setMovies(Array.isArray(moviesData) ? moviesData : []);
      setTheaters(Array.isArray(theatersData) ? theatersData : []);
    } catch (error) {
      console.error('Error fetching data:', error);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const showData = {
        movieId: parseInt(formData.movieId),
        theaterId: parseInt(formData.theaterId),
        showDate: formData.showDate,
        showStartTime: formData.showTime
      };

      await adminService.addShow(showData);
      alert('Show added successfully!');
      navigate('/admin/dashboard');
    } catch (error) {
      console.error('Error adding show:', error);
      alert('Failed to add show: ' + (error.response?.data?.error || error.response?.data?.message || error.message));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="admin-form-page">
      <div className="container">
        <div className="form-header">
          <button className="back-btn" onClick={() => navigate('/admin/dashboard')}>
            <FaArrowLeft /> Back to Dashboard
          </button>
          <h1><FaTicketAlt /> Add New Show</h1>
        </div>

        <form className="admin-form" onSubmit={handleSubmit}>
          <div className="form-grid">
            <div className="form-group full-width">
              <label>Select Movie *</label>
              <select
                name="movieId"
                value={formData.movieId}
                onChange={handleChange}
                required
              >
                <option value="">Choose a movie</option>
                {movies.map(movie => (
                  <option key={movie.id} value={movie.id}>
                    {movie.movieName} ({movie.language})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group full-width">
              <label>Select Theater *</label>
              <select
                name="theaterId"
                value={formData.theaterId}
                onChange={handleChange}
                required
              >
                <option value="">Choose a theater</option>
                {theaters.map(theater => (
                  <option key={theater.id} value={theater.id}>
                    {theater.name} - {theater.city}
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Show Date *</label>
              <input
                type="date"
                name="showDate"
                value={formData.showDate}
                onChange={handleChange}
                required
                min={new Date().toISOString().split('T')[0]}
              />
            </div>

            <div className="form-group">
              <label>Show Time *</label>
              <input
                type="time"
                name="showTime"
                value={formData.showTime}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="form-actions">
            <button type="button" className="btn-cancel" onClick={() => navigate('/admin/dashboard')}>
              Cancel
            </button>
            <button type="submit" className="btn-submit" disabled={loading}>
              <FaSave /> {loading ? 'Saving...' : 'Add Show'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddShow;
