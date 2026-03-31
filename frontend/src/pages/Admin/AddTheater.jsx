import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminService } from '../../services';
import { FaTheaterMasks, FaArrowLeft, FaSave } from 'react-icons/fa';
import './AdminForms.scss';

const AddTheater = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    address: '',
    city: '',
    noOfScreens: 1
  });

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
      const theaterData = {
        ...formData,
        noOfScreens: parseInt(formData.noOfScreens)
      };

      await adminService.addTheater(theaterData);
      alert('Theater added successfully!');
      navigate('/admin/dashboard');
    } catch (error) {
      alert('Failed to add theater: ' + (error.response?.data?.error || error.message));
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
          <h1><FaTheaterMasks /> Add New Theater</h1>
        </div>

        <form className="admin-form" onSubmit={handleSubmit}>
          <div className="form-grid">
            <div className="form-group full-width">
              <label>Theater Name *</label>
              <input
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                required
                placeholder="Enter theater name"
              />
            </div>

            <div className="form-group full-width">
              <label>Address *</label>
              <input
                type="text"
                name="address"
                value={formData.address}
                onChange={handleChange}
                required
                placeholder="Enter full address"
              />
            </div>

            <div className="form-group">
              <label>City *</label>
              <input
                type="text"
                name="city"
                value={formData.city}
                onChange={handleChange}
                required
                placeholder="e.g., Mumbai, Delhi"
              />
            </div>

            <div className="form-group">
              <label>Number of Screens *</label>
              <input
                type="number"
                name="noOfScreens"
                value={formData.noOfScreens}
                onChange={handleChange}
                required
                min="1"
                placeholder="e.g., 4"
              />
            </div>
          </div>

          <div className="form-actions">
            <button type="button" className="btn-cancel" onClick={() => navigate('/admin/dashboard')}>
              Cancel
            </button>
            <button type="submit" className="btn-submit" disabled={loading}>
              <FaSave /> {loading ? 'Saving...' : 'Add Theater'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddTheater;
