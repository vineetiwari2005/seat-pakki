import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminService } from '../../services';
import api from '../../services/api';
import { FaFilm, FaArrowLeft, FaSave, FaUpload, FaSpinner } from 'react-icons/fa';
import './AdminForms.scss';

const AddMovie = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [formData, setFormData] = useState({
    movieName: '',
    duration: '',
    language: '',
    genre: '',
    releaseDate: '',
    posterUrl: '',
    description: '',
    rating: 0,
    cast: '',
    director: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleImageUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      alert('Please upload an image file');
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      alert('Image size should be less than 5MB');
      return;
    }

    setUploading(true);
    
    try {
      // Upload to backend poster endpoint
      const uploadData = new FormData();
      uploadData.append('file', file);

      const response = await api.post('/admin/movies/upload-poster', uploadData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      if (response.data?.url) {
        setFormData(prev => ({
          ...prev,
          posterUrl: response.data.url
        }));
        alert('Image uploaded successfully!');
      } else {
        throw new Error('Upload failed - no URL returned');
      }
    } catch (error) {
      console.error('Upload error:', error);
      // Fallback: convert to base64 data URL for local preview
      const reader = new FileReader();
      reader.onload = (ev) => {
        setFormData(prev => ({ ...prev, posterUrl: ev.target.result }));
        alert('Image loaded locally (upload to server failed, using local preview).');
      };
      reader.readAsDataURL(file);
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      // Convert duration to integer, rating to double
      const movieData = {
        ...formData,
        duration: parseInt(formData.duration),
        rating: parseFloat(formData.rating) || 0
      };

      await adminService.addMovie(movieData);
      alert('Movie added successfully!');
      navigate('/admin/dashboard');
    } catch (error) {
      alert('Failed to add movie: ' + (error.response?.data?.error || error.message));
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
          <h1><FaFilm /> Add New Movie</h1>
        </div>

        <form className="admin-form" onSubmit={handleSubmit}>
          <div className="form-grid">
            <div className="form-group">
              <label>Movie Name *</label>
              <input
                type="text"
                name="movieName"
                value={formData.movieName}
                onChange={handleChange}
                required
                placeholder="Enter movie name"
              />
            </div>

            <div className="form-group">
              <label>Duration (minutes) *</label>
              <input
                type="number"
                name="duration"
                value={formData.duration}
                onChange={handleChange}
                required
                min="1"
                placeholder="e.g., 150"
              />
            </div>

            <div className="form-group">
              <label>Language *</label>
              <select
                name="language"
                value={formData.language}
                onChange={handleChange}
                required
              >
                <option value="">Select Language</option>
                <option value="HINDI">Hindi</option>
                <option value="ENGLISH">English</option>
                <option value="TELUGU">Telugu</option>
                <option value="TAMIL">Tamil</option>
                <option value="MARATHI">Marathi</option>
                <option value="PUNJAB">Punjabi</option>
                <option value="KANNADA">Kannada</option>
              </select>
            </div>

            <div className="form-group">
              <label>Genre *</label>
              <select
                name="genre"
                value={formData.genre}
                onChange={handleChange}
                required
              >
                <option value="">Select Genre</option>
                <option value="ACTION">Action</option>
                <option value="COMEDY">Comedy</option>
                <option value="DRAMA">Drama</option>
                <option value="THRILLER">Thriller</option>
                <option value="ROMANTIC">Romantic</option>
                <option value="HISTORICAL">Historical</option>
                <option value="ANIMATION">Animation</option>
                <option value="SPORTS">Sports</option>
                <option value="SOCIAL">Social</option>
                <option value="WAR">War</option>
              </select>
            </div>

            <div className="form-group">
              <label>Release Date *</label>
              <input
                type="date"
                name="releaseDate"
                value={formData.releaseDate}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label>Rating (0-10)</label>
              <input
                type="number"
                name="rating"
                value={formData.rating}
                onChange={handleChange}
                min="0"
                max="10"
                step="0.1"
                placeholder="e.g., 8.5"
              />
            </div>

            <div className="form-group full-width">
              <label>Poster Image</label>
              <div className="image-upload-container">
                <div className="upload-button-group">
                  <label htmlFor="imageUpload" className={`upload-button ${uploading ? 'uploading' : ''}`}>
                    {uploading ? (
                      <>
                        <FaSpinner className="spinner" />
                        Uploading...
                      </>
                    ) : (
                      <>
                        <FaUpload />
                        Upload Image
                      </>
                    )}
                  </label>
                  <input
                    type="file"
                    id="imageUpload"
                    accept="image/*"
                    onChange={handleImageUpload}
                    disabled={uploading}
                    style={{ display: 'none' }}
                  />
                </div>
                <input
                  type="text"
                  name="posterUrl"
                  value={formData.posterUrl}
                  onChange={handleChange}
                  placeholder="Or enter image URL manually"
                />
                {formData.posterUrl && (
                  <div className="image-preview">
                    <img src={formData.posterUrl} alt="Poster preview" />
                  </div>
                )}
              </div>
            </div>

            <div className="form-group full-width">
              <label>Director</label>
              <input
                type="text"
                name="director"
                value={formData.director}
                onChange={handleChange}
                placeholder="Director name"
              />
            </div>

            <div className="form-group full-width">
              <label>Cast (comma-separated)</label>
              <input
                type="text"
                name="cast"
                value={formData.cast}
                onChange={handleChange}
                placeholder="Actor 1, Actor 2, Actor 3"
              />
            </div>

            <div className="form-group full-width">
              <label>Description</label>
              <textarea
                name="description"
                value={formData.description}
                onChange={handleChange}
                rows="4"
                placeholder="Movie description..."
              />
            </div>
          </div>

          <div className="form-actions">
            <button type="button" className="btn-cancel" onClick={() => navigate('/admin/dashboard')}>
              Cancel
            </button>
            <button type="submit" className="btn-submit" disabled={loading}>
              <FaSave /> {loading ? 'Saving...' : 'Add Movie'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddMovie;
