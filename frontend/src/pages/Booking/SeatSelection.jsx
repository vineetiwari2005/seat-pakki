import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { FaCouch, FaArrowLeft, FaCheckCircle, FaClock, FaRupeeSign } from 'react-icons/fa';
import { MdEventSeat, MdChair, MdDoNotDisturb, MdLock, MdCheckCircle } from 'react-icons/md';
import { seatLockService, authService } from '../../services';
import { buildApiUrl } from '../../config/apiBaseUrl';
import './SeatSelection.scss';
import axios from 'axios';

const SeatSelection = () => {
  const { showId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const showDetails = location.state;

  const [selectedSeats, setSelectedSeats] = useState([]);
  const [lockedSeats, setLockedSeats] = useState([]);
  const [bookedSeats, setBookedSeats] = useState([]);
  const [sessionId] = useState(`SESSION_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`);
  const [timeRemaining, setTimeRemaining] = useState(900); // 15 minutes default, synced after lock
  const [timerActive, setTimerActive] = useState(false); // only after seats locked
  const [loading, setLoading] = useState(false);
  const [loadingSeats, setLoadingSeats] = useState(true);
  const [error, setError] = useState(null);
  const [seats, setSeats] = useState([]);

  const user = authService.getCurrentUser();

  // Load seats from backend
  useEffect(() => {
    const fetchSeats = async () => {
      try {
        setLoadingSeats(true);
        
        // Fetch all seats for the show
        const seatsResponse = await axios.get(buildApiUrl(`/show/${showId}/seats/availability`));
        const allSeats = seatsResponse.data || [];
        
        console.log('Fetched seats from backend:', allSeats.length, 'seats');
        
        // Fetch currently locked seats
        const lockedResponse = await axios.get(buildApiUrl(`/api/seat-locks/show/${showId}/locked-seats`));
        const currentlyLocked = lockedResponse.data || [];
        
        // Group seats by type + row letter so same-letter rows of different types stay separate
        const seatsByTypeRow = {};
        const bookedSeatNos = [];
        
        allSeats.forEach(seat => {
          const row = seat.seatNo.charAt(0); // First character is row (A, B, C, etc.)
          const key = `${seat.seatType}_${row}`; // e.g. "COUPLE_C" vs "CLASSIC_C"
          if (!seatsByTypeRow[key]) {
            seatsByTypeRow[key] = [];
          }
          seatsByTypeRow[key].push({
            id: seat.id,
            number: seat.seatNo,
            row: row,
            type: seat.seatType,
            price: seat.price,
            status: seat.isAvailable ? 'available' : 'booked'
          });
          
          if (!seat.isAvailable) {
            bookedSeatNos.push(seat.seatNo);
          }
        });
        
        // Convert to array of rows, sorted by seat type priority then row letter
        const typeOrder = { COUPLE: 0, CLASSIC: 1, GOLD: 2, PREMIUM: 3, SILVER: 4 };
        const seatRows = Object.keys(seatsByTypeRow)
          .sort((a, b) => {
            const typeA = seatsByTypeRow[a][0]?.type || 'CLASSIC';
            const typeB = seatsByTypeRow[b][0]?.type || 'CLASSIC';
            const orderA = typeOrder[typeA] ?? 5;
            const orderB = typeOrder[typeB] ?? 5;
            if (orderA !== orderB) return orderA - orderB;
            return a.localeCompare(b);
          })
          .map(key => seatsByTypeRow[key].sort((a, b) => {
            // Sort by number
            const numA = parseInt(a.number.substring(1));
            const numB = parseInt(b.number.substring(1));
            return numA - numB;
          }));
        
        console.log('Processed seat rows:', seatRows.length, 'rows');
        
        setSeats(seatRows);
        setBookedSeats(bookedSeatNos);
        setLockedSeats(currentlyLocked);
        setLoadingSeats(false);
      } catch (err) {
        console.error('Error loading seats:', err);
        setError('Failed to load seat information. Please try again.');
        setLoadingSeats(false);
      }
    };
    
    if (showId) {
      fetchSeats();
    }
  }, [showId]);

  // Timer countdown - only active after backend seat lock confirms expiry
  useEffect(() => {
    if (!timerActive) return;

    const timer = setInterval(() => {
      setTimeRemaining(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          handleTimeout();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [timerActive]);

  const handleTimeout = () => {
    alert('Seat lock expired! Your selected seats have been released. Please select again.');
    setSelectedSeats([]);
    setTimerActive(false);
    setTimeRemaining(900);
    // Refresh seat availability
    window.location.reload();
  };

  // Get the paired seat number for a couple seat (1↔2, 3↔4, 5↔6, etc.)
  const getCouplePair = (seatNumber, seatType) => {
    if (seatType !== 'COUPLE') return null;
    const row = seatNumber.charAt(0);
    const num = parseInt(seatNumber.substring(1));
    const pairNum = num % 2 === 1 ? num + 1 : num - 1; // odd→next, even→prev
    return row + pairNum;
  };

  const handleSeatClick = (seatNumber, seatType, price) => {
    // Check if seat is booked or locked by others
    if (bookedSeats.includes(seatNumber) || lockedSeats.includes(seatNumber)) {
      return;
    }

    // Handle couple seats — always select/deselect in pairs
    if (seatType === 'COUPLE') {
      const pairNumber = getCouplePair(seatNumber, seatType);
      const isSelected = selectedSeats.find(s => s.number === seatNumber);

      if (isSelected) {
        // Deselect both seats in the pair
        setSelectedSeats(prev => prev.filter(s => s.number !== seatNumber && s.number !== pairNumber));
      } else {
        // Check if pair seat is booked/locked
        if (pairNumber && (bookedSeats.includes(pairNumber) || lockedSeats.includes(pairNumber))) {
          alert(`Cannot select ${seatNumber} — its pair seat ${pairNumber} is unavailable.`);
          return;
        }
        // Check max seats limit (need room for 2)
        const alreadyHasPair = selectedSeats.find(s => s.number === pairNumber);
        const seatsToAdd = alreadyHasPair ? 1 : 2;
        if (selectedSeats.length + seatsToAdd > 10) {
          alert('You can select maximum 10 seats at a time.');
          return;
        }
        // Find pair seat's price from the seat data
        let pairPrice = price;
        for (const row of seats) {
          const found = row.find(s => s.number === pairNumber);
          if (found) { pairPrice = found.price; break; }
        }
        // Select both
        const newSeats = [...selectedSeats.filter(s => s.number !== seatNumber && s.number !== pairNumber)];
        newSeats.push({ number: seatNumber, type: seatType, price });
        if (pairNumber && !newSeats.find(s => s.number === pairNumber)) {
          newSeats.push({ number: pairNumber, type: seatType, price: pairPrice });
        }
        setSelectedSeats(newSeats);
      }
      return;
    }

    const isSelected = selectedSeats.find(s => s.number === seatNumber);
    
    if (isSelected) {
      // Deselect seat
      setSelectedSeats(prev => prev.filter(s => s.number !== seatNumber));
    } else {
      // Check max seats limit
      if (selectedSeats.length >= 10) {
        alert('You can select maximum 10 seats at a time.');
        return;
      }
      
      // Select seat
      setSelectedSeats(prev => [...prev, { number: seatNumber, type: seatType, price }]);
    }
  };

  const getSeatStatus = (seatNumber) => {
    if (bookedSeats.includes(seatNumber)) return 'booked';
    if (lockedSeats.includes(seatNumber)) return 'locked';
    if (selectedSeats.find(s => s.number === seatNumber)) return 'selected';
    return 'available';
  };

  const calculateTotal = () => {
    const baseAmount = selectedSeats.reduce((sum, seat) => sum + seat.price, 0);
    const convenienceFee = Math.max(baseAmount * 0.025, 20); // 2.5% or min ₹20
    const tax = (baseAmount + convenienceFee) * 0.18; // 18% GST
    const total = baseAmount + convenienceFee + tax;

    return {
      baseAmount: baseAmount.toFixed(2),
      convenienceFee: convenienceFee.toFixed(2),
      tax: tax.toFixed(2),
      total: total.toFixed(2)
    };
  };

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const handleProceedToPayment = async () => {
    if (selectedSeats.length === 0) {
      alert('Please select at least one seat.');
      return;
    }

    // Validate couple seats are in even pairs
    const coupleSeats = selectedSeats.filter(s => s.type === 'COUPLE');
    if (coupleSeats.length % 2 !== 0) {
      alert('Couple seats must be selected in pairs (even number). You have ' + coupleSeats.length + ' couple seat(s) selected. Please select or deselect one more couple seat to make it an even number.');
      return;
    }

    if (!user) {
      alert('Please login to continue.');
      navigate('/login', { state: { from: location.pathname } });
      return;
    }

    setLoading(true);
    setError(null);

    let backendSessionId = sessionId; // Will be updated with backend-generated sessionId
    let seatsLocked = false;

    try {
      // Lock seats via backend (REQUIRED)
      const lockData = {
        showId: parseInt(showId),
        userId: user.id,
        seatNumbers: selectedSeats.map(s => s.number)
      };

      const lockResponse = await seatLockService.lockSeats(lockData);
      // Use the sessionId returned by backend (backend generates its own UUID)
      backendSessionId = lockResponse.sessionId || lockResponse.data?.sessionId || sessionId;
      seatsLocked = true;

      // Sync timer with backend lock expiry
      const backendExpiry = lockResponse.expiryTimeSeconds || lockResponse.data?.expiryTimeSeconds;
      if (backendExpiry && backendExpiry > 0) {
        setTimeRemaining(backendExpiry);
        setTimerActive(true);
        console.log('⏱️ Timer synced with backend expiry:', backendExpiry, 'seconds');
      }

      console.log('✅ Seats locked successfully with session:', backendSessionId);

      // Proceed to checkout page (with add-ons)
      const pricing = calculateTotal();
      const bookingData = {
        showId: showId,
        sessionId: backendSessionId,
        seatsLocked: seatsLocked,
        movieName: showDetails?.movieName || 'Selected Movie',
        theaterName: showDetails?.theaterName || 'Selected Theater',
        showDate: showDetails?.showDate || new Date().toISOString().split('T')[0],
        showTime: showDetails?.showTime || '19:00',
        theaterId: showDetails?.theaterId,
        selectedSeats: selectedSeats.map(s => s.number),
        seatDetails: selectedSeats,
        lockExpirySeconds: backendExpiry || 900,
        baseAmount: parseFloat(pricing.baseAmount),
        convenienceFee: parseFloat(pricing.convenienceFee),
        tax: parseFloat(pricing.tax),
        totalAmount: parseFloat(pricing.total),
        userName: user.name,
        userEmail: user.email || user.emailId
      };

      navigate('/booking/checkout', { state: bookingData });
    } catch (err) {
      const errorMessage = err.response?.data?.error || err.response?.data?.message || err.message || 'Failed to proceed. Please try again.';
      setError(errorMessage);
      alert('Error: ' + errorMessage);
    } finally {
      setLoading(false);
    }
  };

  if (loadingSeats) {
    return (
      <div className="seat-selection-page">
        <div className="container">
          <div className="loading-message">
            <h3>Loading seats...</h3>
          </div>
        </div>
      </div>
    );
  }

  if (!showDetails) {
    return (
      <div className="seat-selection-page">
        <div className="container">
          <div className="error-message">
            <h3>No show details found</h3>
            <p>Please select a show to continue.</p>
            <button onClick={() => navigate('/')}>Go to Home</button>
          </div>
        </div>
      </div>
    );
  }

  const pricing = calculateTotal();

  return (
    <div className="seat-selection-page">
      <div className="container">
        {/* Header */}
        <div className="page-header">
          <button className="back-btn" onClick={() => navigate(-1)}>
            <FaArrowLeft /> Back
          </button>
          <div className="show-info">
            <h1>{showDetails.movieName}</h1>
            <p>{showDetails.theaterName} | {showDetails.showDate} | {showDetails.showTime}</p>
          </div>
          {selectedSeats.length > 0 && (
            <div className={`timer ${timerActive ? 'timer-locked' : ''} ${timeRemaining < 120 ? 'timer-warning' : ''}`}>
              <FaClock /> {timerActive ? formatTime(timeRemaining) : 'Select & proceed to lock'}
            </div>
          )}
        </div>

        <div className="seat-selection-content">
          {/* Seat Map */}
          <div className="seat-map-container">
            <div className="screen">
              <div className="screen-label">SCREEN THIS WAY</div>
            </div>

            <div className="seat-map">
              {seats.length === 0 ? (
                <div className="no-seats-message">
                  <p>No seats available for this show.</p>
                  <p>Please contact theater management.</p>
                </div>
              ) : (
                (() => {
                  // Group rows by seat type to add section dividers
                  let lastType = null;
                  return seats.map((row, rowIndex) => {
                    const rowType = row[0]?.type || 'STANDARD';
                    lastType = rowType;
                    return (
                      <div className="seat-row" key={rowIndex}>
                        <div className="seats">
                          {row.map((seat, seatIndex) => {
                            const status = getSeatStatus(seat.number);
                            return (
                              <button
                                key={seatIndex}
                                className={`seat ${status} ${seat.type.toLowerCase()}`}
                                onClick={() => handleSeatClick(seat.number, seat.type, seat.price)}
                                disabled={status === 'booked' || status === 'locked'}
                                title={`${seat.number} - ${seat.type} - ₹${seat.price}`}
                              >
                                <MdEventSeat />
                                <span className="seat-number">{seat.number}</span>
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    );
                  });
                })()
              )}
            </div>

            {/* Legend */}
            <div className="seat-legend">
              <div className="legend-item">
                <div className="legend-seat available"><MdEventSeat /></div>
                <span>Available</span>
              </div>
              <div className="legend-item">
                <div className="legend-seat selected"><MdEventSeat /></div>
                <span>Selected</span>
              </div>
              <div className="legend-item">
                <div className="legend-seat booked"><MdEventSeat /></div>
                <span>Booked</span>
              </div>
              <div className="legend-item">
                <div className="legend-seat locked"><MdEventSeat /></div>
                <span>Locked</span>
              </div>
            </div>

            {/* Seat Types & Pricing - Dynamic based on actual seat data */}
            <div className="seat-types">
              {(() => {
                // Build dynamic seat type info from actual data
                const typeInfo = {};
                seats.forEach(row => {
                  row.forEach(seat => {
                    const type = seat.type;
                    if (!typeInfo[type]) {
                      typeInfo[type] = { rows: new Set(), price: seat.price };
                    }
                    typeInfo[type].rows.add(seat.row);
                  });
                });
                const typeOrder = ['COUPLE', 'PREMIUM', 'GOLD', 'SILVER', 'CLASSIC'];
                const typeEmoji = { COUPLE: '💑', PREMIUM: '⭐', GOLD: '🥇', SILVER: '🥈', CLASSIC: '🎬' };
                return typeOrder
                  .filter(t => typeInfo[t])
                  .map(type => {
                    const info = typeInfo[type];
                    const rowLetters = Array.from(info.rows).sort().join('-');
                    return (
                      <div key={type} className={`type-item ${type.toLowerCase()}`}>
                        <span className="type-name">{typeEmoji[type] || ''} {type} ({rowLetters})</span>
                        <span className="type-price">₹{info.price}{type === 'COUPLE' ? ' × 2' : ''}</span>
                        {type === 'COUPLE' && <span className="type-note">Must book in pairs</span>}
                      </div>
                    );
                  });
              })()}
            </div>
          </div>

          {/* Booking Summary */}
          <div className="booking-summary">
            <h3>Booking Summary</h3>
            
            {selectedSeats.length > 0 ? (
              <>
                <div className="selected-seats-list">
                  <h4>Selected Seats ({selectedSeats.length})</h4>
                  <div className="seats-grid">
                    {selectedSeats.map(seat => (
                      <div key={seat.number} className="seat-tag">
                        {seat.number}
                        <button 
                          className="remove-seat"
                          onClick={() => handleSeatClick(seat.number, seat.type, seat.price)}
                        >
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="price-breakdown">
                  <div className="price-item">
                    <span>Ticket Price</span>
                    <span>₹{pricing.baseAmount}</span>
                  </div>
                  <div className="price-item">
                    <span>Convenience Fee</span>
                    <span>₹{pricing.convenienceFee}</span>
                  </div>
                  <div className="price-item">
                    <span>GST (18%)</span>
                    <span>₹{pricing.tax}</span>
                  </div>
                  <div className="price-divider"></div>
                  <div className="price-item total">
                    <span>Total Amount</span>
                    <span>₹{pricing.total}</span>
                  </div>
                </div>

                {error && <div className="error-alert">{error}</div>}

                <button 
                  className="proceed-btn"
                  onClick={handleProceedToPayment}
                  disabled={loading}
                >
                  {loading ? (
                    'Processing...'
                  ) : (
                    <>
                      <FaCheckCircle /> Proceed to Payment
                    </>
                  )}
                </button>

                <p className="note">
                  <FaClock /> {timerActive 
                    ? `Seats locked for ${formatTime(timeRemaining)}` 
                    : 'Seats will be locked when you proceed'}
                </p>
              </>
            ) : (
              <div className="no-selection">
                <FaCouch size={50} />
                <p>Select seats to continue</p>
                <small>Click on available seats to select</small>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SeatSelection;
