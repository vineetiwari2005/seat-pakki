import React, { useState } from 'react';
import { FaTimes, FaSave } from 'react-icons/fa';
import SeatPakkiPopup from '../Common/SeatPakkiPopup';
import './EditProfileModal.scss';

const EditProfileModal = ({ user, isOpen, onClose, onUpdate }) => {
  const [formData, setFormData] = useState({
    name: user?.name || '',
    email: user?.email || '',
    mobileNo: user?.mobileNumber || user?.mobileNo || ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [otpPopup, setOtpPopup] = useState({
    isOpen: false,
    message: '',
    value: '',
    resolve: null
  });

  const requestOtpCode = (message) => {
    return new Promise((resolve) => {
      setOtpPopup({
        isOpen: true,
        message,
        value: '',
        resolve
      });
    });
  };

  const closeOtpPopup = (code = null) => {
    if (otpPopup.resolve) {
      otpPopup.resolve(code);
    }
    setOtpPopup({
      isOpen: false,
      message: '',
      value: '',
      resolve: null
    });
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    setSuccess('');

    // Validation
    if (!formData.name.trim()) {
      setError('Name is required');
      setLoading(false);
      return;
    }

    if (!formData.email.trim()) {
      setError('Email is required');
      setLoading(false);
      return;
    }

    // Email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      setError('Invalid email format');
      setLoading(false);
      return;
    }

    const mobileRegex = /^[0-9]{10}$/;
    if (!mobileRegex.test(formData.mobileNo)) {
      setError('Mobile number must be 10 digits');
      setLoading(false);
      return;
    }

    try {
      const token = localStorage.getItem('accessToken');
      const userId = user.id;

      const existingEmail = (user?.email || '').trim();
      const existingMobile = (user?.mobileNumber || user?.mobileNo || '').trim();
      const requestedEmail = formData.email.trim();
      const requestedMobile = formData.mobileNo.trim();
      const sensitiveChanged = existingEmail !== requestedEmail || existingMobile !== requestedMobile;

      let otpCode = null;
      let otpRequestId = null;

      if (sensitiveChanged) {
        const otpResponse = await fetch(`http://localhost:8080/api/otp/send`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({
            userId,
            purpose: 'PROFILE_UPDATE',
            referenceId: String(userId)
          })
        });

        const otpData = await otpResponse.json();
        if (!otpResponse.ok) {
          throw new Error(otpData.error || 'Failed to send OTP');
        }

        otpRequestId = otpData.otpRequestId;
        const promptMessage = otpData.maskedMobile
          ? `Enter OTP sent to your ${otpData?.channel === 'EMAIL' ? 'registered email' : 'registered mobile'} (${otpData.maskedMobile})`
          : 'Enter OTP sent to your registered mobile number';

        otpCode = await requestOtpCode(promptMessage);
        if (!otpCode || !otpCode.trim()) {
          throw new Error('OTP is required to update email or mobile number');
        }
      }

      const response = await fetch(`http://localhost:8080/user/${userId}/profile`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          ...formData,
          email: requestedEmail,
          mobileNo: requestedMobile,
          otpCode: otpCode ? otpCode.trim() : null,
          otpRequestId
        })
      });

      const data = await response.json();

      if (response.ok) {
        setSuccess('Profile updated successfully!');
        setTimeout(() => {
          const updatedUser = {
            ...user,
            name: data?.user?.name || formData.name,
            email: data?.user?.emailId || formData.email,
            mobileNumber: data?.user?.mobileNo || formData.mobileNo,
            mobileNo: data?.user?.mobileNo || formData.mobileNo
          };
          onUpdate(updatedUser);
          onClose();
        }, 1500);
      } else {
        setError(data.error || 'Failed to update profile');
      }
    } catch (err) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Edit Profile</h2>
          <button className="close-btn" onClick={onClose}>
            <FaTimes />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="name">Full Name</label>
            <input
              type="text"
              id="name"
              name="name"
              value={formData.name}
              onChange={handleChange}
              placeholder="Enter your full name"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="email">Email Address</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="Enter your email"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="mobileNo">Mobile Number</label>
            <input
              type="tel"
              id="mobileNo"
              name="mobileNo"
              value={formData.mobileNo}
              onChange={handleChange}
              placeholder="Enter 10-digit mobile number"
              disabled={loading}
              maxLength={10}
            />
          </div>

          {error && (
            <div className="alert alert-error">
              {error}
            </div>
          )}

          {success && (
            <div className="alert alert-success">
              {success}
            </div>
          )}

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-outline"
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loading}
            >
              {loading ? 'Saving...' : (
                <>
                  <FaSave /> Save Changes
                </>
              )}
            </button>
          </div>
        </form>
      </div>
      <SeatPakkiPopup
        isOpen={otpPopup.isOpen}
        title="OTP Verification"
        message={otpPopup.message}
        showCancel={true}
        confirmText="Verify OTP"
        cancelText="Cancel"
        inputValue={otpPopup.value}
        onInputChange={(value) => setOtpPopup((prev) => ({ ...prev, value }))}
        inputPlaceholder="Enter OTP"
        onConfirm={() => closeOtpPopup(otpPopup.value)}
        onCancel={() => closeOtpPopup(null)}
      />
    </div>
  );
};

export default EditProfileModal;
