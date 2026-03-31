import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { bookingService } from '../../services';
import { FaTicketAlt, FaCalendarAlt, FaClock, FaMapMarkerAlt, FaTrash, FaCheckCircle } from 'react-icons/fa';
import { QRCodeCanvas } from 'qrcode.react';
import SeatPakkiPopup from '../../components/Common/SeatPakkiPopup';
import './MyBookings.scss';

const MyBookings = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cancellingId, setCancellingId] = useState(null);
  const [changingId, setChangingId] = useState(null);
  const [popupState, setPopupState] = useState({
    isOpen: false,
    title: 'SeatPakki',
    message: '',
    showCancel: false,
    onConfirm: null
  });

  const showInfoPopup = (title, message) => {
    setPopupState({
      isOpen: true,
      title,
      message,
      showCancel: false,
      onConfirm: () => setPopupState((prev) => ({ ...prev, isOpen: false }))
    });
  };

  const showConfirmPopup = (title, message) => {
    return new Promise((resolve) => {
      setPopupState({
        isOpen: true,
        title,
        message,
        showCancel: true,
        onConfirm: () => {
          setPopupState((prev) => ({ ...prev, isOpen: false }));
          resolve(true);
        },
        onCancel: () => {
          setPopupState((current) => ({ ...current, isOpen: false }));
          resolve(false);
        }
      });
    });
  };

  useEffect(() => {
    if (user?.id) {
      fetchBookings();
    }
  }, [user]);

  const fetchBookings = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Use bookingService which includes auth token
      const data = await bookingService.getUserBookings(user.id);
      
      // Sort by booking date (newest first)
      const sortedData = Array.isArray(data) ? 
        data.sort((a, b) => new Date(b.bookedAt) - new Date(a.bookedAt)) : [];
      
      setBookings(sortedData);
      console.log('📋 Fetched bookings:', sortedData.length);
    } catch (err) {
      console.error('Error fetching bookings:', err);
      setError(err.message || 'Failed to fetch bookings');
    } finally {
      setLoading(false);
    }
  };

  const handleCancelBooking = async (ticketId) => {
    try {
      setCancellingId(ticketId);
      
      // First, get refund estimate
      const estimate = await bookingService.getRefundEstimate(ticketId);
      
      const originalAmount = estimate.ticketAmount ?? estimate.originalAmount ?? 0;
      const refundAmount = estimate.refundAmount ?? 0;
      const refundPercentage = estimate.refundPercentage ?? 0;
      const cancellationFee = originalAmount - refundAmount;
      const policyMessage = estimate.policyMessage || '';

      if (estimate.canCancel === false) {
        showInfoPopup('Cancellation Not Allowed', `Cannot cancel this booking.\n\n${policyMessage}`);
        setCancellingId(null);
        return;
      }

      const confirmMessage = `Refund Estimate:\n\n` +
        `Original Amount: ₹${originalAmount.toFixed(2)}\n` +
        `Refund Amount: ₹${refundAmount.toFixed(2)} (${refundPercentage}%)\n` +
        `Cancellation Fee: ₹${cancellationFee.toFixed(2)}\n\n` +
        `${policyMessage}\n\n` +
        `Do you want to proceed with cancellation?`;
      
      const isConfirmed = await showConfirmPopup('Confirm Cancellation', confirmMessage);
      if (!isConfirmed) {
        setCancellingId(null);
        return;
      }

      // Proceed with cancellation
      const result = await bookingService.cancelBooking(ticketId, user.id);
      
      // Refresh bookings after successful cancellation
      await fetchBookings();
      
      showInfoPopup('Booking Cancelled', result.message || 'Booking cancelled successfully! Refund has been credited to your wallet.');
    } catch (err) {
      console.error('Error cancelling booking:', err);
      showInfoPopup('Cancellation Failed', `Failed to cancel booking: ${err.response?.data?.message || err.response?.data?.error || err.message}`);
    } finally {
      setCancellingId(null);
    }
  };

  const handleChangeDate = async (ticketId) => {
    try {
      setChangingId(ticketId);

      const confirmMessage =
        'Change Date will cancel this ticket and credit a time-based refund (after cancellation charge) to your temporary wallet for 15 days. Continue?';

      const isConfirmed = await showConfirmPopup('Change Date', confirmMessage);
      if (!isConfirmed) {
        return;
      }

      const result = await bookingService.changeBookingDate(ticketId, user.id);
      await fetchBookings();

      const tempAmount = Number(result.temporaryWalletAmount || 0);
      const refundAmount = Number(result.refundAmount || tempAmount || 0);
      const cancellationCharge = Number(result.cancellationCharge || 0);
      const refundPercentage = Number(result.refundPercentage || 0);
      const expiryText = result.temporaryWalletExpiresAt
        ? `\nValid till: ${new Date(result.temporaryWalletExpiresAt).toLocaleString('en-IN')}`
        : '';

      showInfoPopup(
        'Date Change Successful',
        (result.message || 'Date change completed.') +
        `\nRefund credited: ₹${refundAmount.toFixed(2)} (${refundPercentage}%)` +
        `\nCancellation charge: ₹${cancellationCharge.toFixed(2)}` +
        `\nTemporary wallet credited: ₹${tempAmount.toFixed(2)}${expiryText}`
      );

      navigate('/');
    } catch (err) {
      console.error('Error changing booking date:', err);
      showInfoPopup('Date Change Failed', `Failed to change date: ${err.response?.data?.message || err.response?.data?.error || err.message}`);
    } finally {
      setChangingId(null);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', { 
      day: '2-digit', 
      month: 'short', 
      year: 'numeric' 
    });
  };

  const formatTime = (timeString) => {
    if (!timeString) return '';
    // Handle different time formats
    if (timeString.includes(':')) {
      const [hours, minutes] = timeString.split(':');
      const hour = parseInt(hours);
      const ampm = hour >= 12 ? 'PM' : 'AM';
      const displayHour = hour % 12 || 12;
      return `${displayHour}:${minutes} ${ampm}`;
    }
    return timeString;
  };

  const isPastBooking = (showDate, showTime) => {
    if (!showDate) return false;
    const now = new Date();
    
    // Parse date and time properly
    const dateStr = showDate.includes('T') ? showDate.split('T')[0] : showDate;
    const timeStr = showTime || '00:00:00';
    const [hours, minutes] = timeStr.split(':');
    
    // Create datetime from date and time
    const bookingDateTime = new Date(dateStr);
    bookingDateTime.setHours(parseInt(hours, 10), parseInt(minutes, 10), 0);
    
    return bookingDateTime < now;
  };

  // Helper to extract data from both DTO and full Ticket formats
  const getBookingData = (booking) => {
    // Check if it's the DTO format (from /api/bookings/user/{userId})
    if (booking.ticketId) {
      return {
        id: booking.ticketId,
        movieName: booking.movieName,
        theaterName: booking.theaterName,
        theaterAddress: '',
        showDate: booking.showDate,
        showTime: booking.showTime,
        seats: booking.seats,
        totalPrice: booking.totalPrice,
        bookedAt: booking.bookedAt,
        isActive: booking.isActive,
        status: booking.status || 'BOOKED',
        refundAmount: booking.refundAmount,
        cancelledAt: booking.cancelledAt
      };
    }
    // Full Ticket format (from /ticket/user/{userId})
    return {
      id: booking.id,
      movieName: booking.show?.movie?.movieName || 'Unknown Movie',
      theaterName: booking.show?.theater?.name || 'Unknown Theater',
      theaterAddress: booking.show?.theater?.address || '',
      showDate: booking.show?.date,
      showTime: booking.show?.time,
      seats: booking.bookedSeats,
      totalPrice: booking.totalTicketsPrice,
      bookedAt: booking.bookedAt,
      isActive: booking.isActive,
      status: booking.status || 'BOOKED',
      refundAmount: booking.refundAmount,
      cancelledAt: booking.cancelledAt
    };
  };

  if (loading) {
    return (
      <div className="my-bookings-page">
        <div className="container">
          <div className="loading-container">
            <div className="spinner"></div>
            <p>Loading your bookings...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="my-bookings-page">
        <div className="container">
          <div className="error-message">
            <h3>Error Loading Bookings</h3>
            <p>{error}</p>
            <button onClick={fetchBookings} className="btn btn-primary">Try Again</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="my-bookings-page">
      <div className="container">
        <div className="page-header">
          <h1><FaTicketAlt /> My Bookings</h1>
          <p className="subtitle">View and manage your movie bookings</p>
        </div>

        {bookings.length === 0 ? (
          <div className="no-bookings">
            <FaTicketAlt className="empty-icon" />
            <h3>No Bookings Yet</h3>
            <p>You haven't made any bookings. Start exploring movies and book your tickets!</p>
            <a href="/" className="btn btn-primary">Browse Movies</a>
          </div>
        ) : (
          <div className="bookings-grid">
            {bookings.map((booking) => {
              const bookingData = getBookingData(booking);
              const isCancelled = bookingData.status === 'CANCELLED';
              // Use isActive from backend if available, otherwise calculate
              const isActive = isCancelled ? false : (
                bookingData.isActive !== undefined 
                  ? bookingData.isActive 
                  : !isPastBooking(bookingData.showDate, bookingData.showTime)
              );
              
              const qrData = JSON.stringify({
                bookingId: `BMS${bookingData.id}`,
                movie: bookingData.movieName || 'N/A',
                theater: bookingData.theaterName || 'N/A',
                date: bookingData.showDate || 'N/A',
                time: bookingData.showTime || 'N/A',
                seats: bookingData.seats || 'N/A',
                amount: `INR ${Number(bookingData.totalPrice || 0).toFixed(2)}`,
                status: isCancelled ? 'CANCELLED' : (isActive ? 'ACTIVE' : 'COMPLETED'),
              });

              return (
                <div key={bookingData.id} className={`booking-card ${!isActive ? 'past-booking' : ''} ${isCancelled ? 'cancelled-booking' : ''}`}>
                  <div className="booking-header">
                    <div className="booking-status">
                      {isCancelled ? (
                        <span className="status-badge cancelled">Cancelled</span>
                      ) : !isActive ? (
                        <span className="status-badge completed">Completed</span>
                      ) : (
                        <span className="status-badge active">
                          <FaCheckCircle /> Active
                        </span>
                      )}
                    </div>
                    <div className="booking-id">
                      Booking ID: <strong>BMS{bookingData.id}</strong>
                    </div>
                  </div>

                  <div className="booking-content">
                    <div className="booking-details">
                      <h3 className="movie-title">{bookingData.movieName}</h3>
                      
                      <div className="detail-row">
                        <FaMapMarkerAlt className="icon" />
                        <div>
                          <label>Theater</label>
                          <p>{bookingData.theaterName}</p>
                          {bookingData.theaterAddress && <small>{bookingData.theaterAddress}</small>}
                        </div>
                      </div>

                      <div className="detail-row">
                        <FaCalendarAlt className="icon" />
                        <div>
                          <label>Show Date & Time</label>
                          <p>{formatDate(bookingData.showDate)} • {formatTime(bookingData.showTime)}</p>
                        </div>
                      </div>

                      <div className="detail-row">
                        <FaTicketAlt className="icon" />
                        <div>
                          <label>Seats</label>
                          <div className="seat-badges">
                            {bookingData.seats?.split(',').filter(s => s.trim()).map((seat, idx) => (
                              <span key={idx} className="seat-badge">{seat.trim()}</span>
                            ))}
                          </div>
                        </div>
                      </div>

                      <div className="booking-price">
                        <span>Total Amount</span>
                        <strong>₹{Number(bookingData.totalPrice).toFixed(2)}</strong>
                      </div>

                      {isCancelled && bookingData.refundAmount != null && (
                        <div className="refund-info">
                          <span>Refund Credited</span>
                          <strong className="refund-amount">₹{Number(bookingData.refundAmount).toFixed(2)}</strong>
                          {bookingData.cancelledAt && (
                            <small>Cancelled on: {formatDate(bookingData.cancelledAt)}</small>
                          )}
                        </div>
                      )}

                      <div className="booking-footer">
                        <small>Booked on: {formatDate(bookingData.bookedAt)}</small>
                        {isActive && !isCancelled && (
                          <div className="booking-actions">
                            <button
                              className="btn-change"
                              onClick={() => handleChangeDate(bookingData.id)}
                              disabled={changingId === bookingData.id || cancellingId === bookingData.id}
                            >
                              <FaCalendarAlt /> {changingId === bookingData.id ? 'Changing...' : 'Change Date'}
                            </button>
                            <button 
                              className="btn-cancel"
                              onClick={() => handleCancelBooking(bookingData.id)}
                              disabled={cancellingId === bookingData.id || changingId === bookingData.id}
                            >
                              <FaTrash /> {cancellingId === bookingData.id ? 'Cancelling...' : 'Cancel Booking'}
                            </button>
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="booking-qr">
                      <div className="qr-code">
                        <QRCodeCanvas 
                          value={qrData}
                          size={180}
                          level="M"
                          includeMargin={true}
                        />
                      </div>
                      <p className="qr-label">Show at Theater</p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
      <SeatPakkiPopup
        isOpen={popupState.isOpen}
        title={popupState.title}
        message={popupState.message}
        showCancel={popupState.showCancel}
        onConfirm={popupState.onConfirm}
        onCancel={popupState.onCancel || (() => setPopupState((prev) => ({ ...prev, isOpen: false })))}
      />
    </div>
  );
};

export default MyBookings;
