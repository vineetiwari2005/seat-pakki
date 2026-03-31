import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { FaCar, FaUtensils, FaLock, FaCheckCircle, FaTimes } from 'react-icons/fa';
import './PaymentWithAddons.scss';
import api from '../../services/api';

const PaymentWithAddons = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const bookingDetails = location.state;

  // Add-on states
  const [showParkingModal, setShowParkingModal] = useState(false);
  const [showFoodModal, setShowFoodModal] = useState(false);
  const [selectedAddons, setSelectedAddons] = useState([]);
  const [parkingData, setParkingData] = useState(null);
  const [foodItems, setFoodItems] = useState([]);
  const [selectedFoodItems, setSelectedFoodItems] = useState([]);
  
  // Payment state
  const [totalAmount, setTotalAmount] = useState(bookingDetails?.totalAmount || 0);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!bookingDetails) {
      navigate('/');
      return;
    }
    
    // Fetch available food items
    fetchFoodMenu();
  }, [bookingDetails]);

  const fetchFoodMenu = async () => {
    try {
      const response = await api.get(`/api/food/menu/${bookingDetails.theaterId}`);
      setFoodItems(response.data);
    } catch (err) {
      console.error('Failed to fetch food menu:', err);
    }
  };

  const handleParkingSelect = async () => {
    setShowParkingModal(true);
  };

  const handleAddParking = async (vehicleData) => {
    try {
      const response = await api.post('/api/payment/addons/select-parking', {
        sessionId: bookingDetails.sessionId,
        userId: bookingDetails.userId,
        vehicleType: vehicleData.vehicleType,
        vehicleNumber: vehicleData.vehicleNumber,
        theaterId: bookingDetails.theaterId
      });

      setParkingData(vehicleData);
      setShowParkingModal(false);
      refreshAddons();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to add parking');
    }
  };

  const handleFoodSelect = () => {
    setShowFoodModal(true);
  };

  const handleAddFood = async () => {
    if (selectedFoodItems.length === 0) {
      setError('Please select at least one food item');
      return;
    }

    try {
      const items = selectedFoodItems.map(item => ({
        foodItemId: item.id,
        quantity: item.quantity
      }));

      await api.post('/api/payment/addons/select-food', {
        sessionId: bookingDetails.sessionId,
        userId: bookingDetails.userId,
        theaterId: bookingDetails.theaterId,
        items
      });

      setShowFoodModal(false);
      refreshAddons();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to add food');
    }
  };

  const refreshAddons = async () => {
    try {
      const response = await api.get(`/api/payment/addons/${bookingDetails.sessionId}`);
      setSelectedAddons(response.data);
      
      // Calculate new total
      const addonTotal = response.data.reduce((sum, addon) => sum + addon.amount, 0);
      setTotalAmount(bookingDetails.baseAmount + addonTotal);
    } catch (err) {
      console.error('Failed to refresh add-ons:', err);
    }
  };

  const handleRemoveAddon = async (addonType) => {
    try {
      await api.delete(`/api/payment/addons/${bookingDetails.sessionId}/${addonType}`);
      
      if (addonType === 'PARKING') {
        setParkingData(null);
      } else if (addonType === 'FOOD_BEVERAGE') {
        setSelectedFoodItems([]);
      }
      
      refreshAddons();
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to remove add-on');
    }
  };

  const handlePayment = async () => {
    setProcessing(true);
    setError(null);

    try {
      // Initiate payment with add-ons
      const response = await api.post('/api/payment/with-addons/initiate', {
        sessionId: bookingDetails.sessionId,
        userId: bookingDetails.userId,
        ticketAmount: bookingDetails.baseAmount,
        paymentMethod: 'CREDIT_CARD',
        ...(parkingData && {
          parking: {
            vehicleType: parkingData.vehicleType,
            vehicleNumber: parkingData.vehicleNumber,
            theaterId: bookingDetails.theaterId
          }
        }),
        ...(selectedFoodItems.length > 0 && {
          food: {
            theaterId: bookingDetails.theaterId,
            items: selectedFoodItems.map(item => ({
              foodItemId: item.id,
              quantity: item.quantity
            }))
          }
        })
      });

      const { transactionId } = response.data;

      // Process payment
      const paymentResponse = await api.post(`/api/payment/with-addons/process/${transactionId}`);

      if (paymentResponse.data.paymentStatus === 'SUCCESS') {
        navigate(`/booking/confirmation/${transactionId}`, {
          state: {
            ...bookingDetails,
            ...paymentResponse.data,
            transactionId
          }
        });
      } else {
        setError('Payment failed. Please try again.');
      }
    } catch (err) {
      setError(err.response?.data?.error || 'Payment processing failed');
    } finally {
      setProcessing(false);
    }
  };

  const toggleFoodItem = (item) => {
    const existing = selectedFoodItems.find(i => i.id === item.id);
    if (existing) {
      setSelectedFoodItems(selectedFoodItems.filter(i => i.id !== item.id));
    } else {
      setSelectedFoodItems([...selectedFoodItems, { ...item, quantity: 1 }]);
    }
  };

  const updateFoodQuantity = (itemId, quantity) => {
    setSelectedFoodItems(selectedFoodItems.map(item =>
      item.id === itemId ? { ...item, quantity: Math.max(1, quantity) } : item
    ));
  };

  const calculateFoodTotal = () => {
    return selectedFoodItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
  };

  if (!bookingDetails) {
    return (
      <div className="payment-addons-page">
        <div className="error-container">
          <h3>No booking details found</h3>
          <button onClick={() => navigate('/')}>Go to Home</button>
        </div>
      </div>
    );
  }

  return (
    <div className="payment-addons-page">
      <div className="container">
        <h1>Complete Your Booking</h1>

        {/* Booking Summary */}
        <div className="booking-summary">
          <h3>Booking Details</h3>
          <div className="summary-grid">
            <div className="summary-item">
              <span>Movie:</span>
              <strong>{bookingDetails.movieName}</strong>
            </div>
            <div className="summary-item">
              <span>Theater:</span>
              <strong>{bookingDetails.theaterName}</strong>
            </div>
            <div className="summary-item">
              <span>Date & Time:</span>
              <strong>{bookingDetails.showDate} at {bookingDetails.showTime}</strong>
            </div>
            <div className="summary-item">
              <span>Seats:</span>
              <strong>{bookingDetails.selectedSeats?.join(', ')}</strong>
            </div>
          </div>
        </div>

        {/* Add-ons Section */}
        <div className="addons-section">
          <h3>Enhance Your Experience (Optional)</h3>
          
          <div className="addon-cards">
            {/* Parking Card */}
            <div className="addon-card">
              <div className="addon-icon">
                <FaCar size={40} />
              </div>
              <h4>Car Parking</h4>
              <p>Reserve your parking spot</p>
              {!parkingData ? (
                <button onClick={handleParkingSelect} className="addon-btn">
                  Add Parking
                </button>
              ) : (
                <div className="addon-selected">
                  <FaCheckCircle color="green" />
                  <p>{parkingData.vehicleType} - {parkingData.vehicleNumber}</p>
                  <button onClick={() => handleRemoveAddon('PARKING')} className="remove-btn">
                    <FaTimes /> Remove
                  </button>
                </div>
              )}
            </div>

            {/* Food Card */}
            <div className="addon-card">
              <div className="addon-icon">
                <FaUtensils size={40} />
              </div>
              <h4>Food & Beverages</h4>
              <p>Pre-order your snacks</p>
              {selectedFoodItems.length === 0 ? (
                <button onClick={handleFoodSelect} className="addon-btn">
                  Browse Menu
                </button>
              ) : (
                <div className="addon-selected">
                  <FaCheckCircle color="green" />
                  <p>{selectedFoodItems.length} items selected</p>
                  <button onClick={() => handleRemoveAddon('FOOD_BEVERAGE')} className="remove-btn">
                    <FaTimes /> Remove
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Price Breakdown */}
        <div className="price-breakdown">
          <h3>Price Summary</h3>
          <div className="price-item">
            <span>Tickets ({bookingDetails.selectedSeats?.length})</span>
            <span>₹{bookingDetails.baseAmount}</span>
          </div>
          {selectedAddons.map(addon => (
            <div key={addon.addonId} className="price-item">
              <span>{addon.details}</span>
              <span>₹{addon.amount}</span>
            </div>
          ))}
          <div className="price-divider"></div>
          <div className="price-item total">
            <span>Total Amount</span>
            <strong>₹{totalAmount}</strong>
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {/* Payment Button */}
        <button 
          onClick={handlePayment}
          disabled={processing}
          className="payment-btn"
        >
          <FaLock /> {processing ? 'Processing...' : `Proceed to Pay ₹${totalAmount}`}
        </button>

        {/* Skip Add-ons */}
        <p className="skip-text">
          You can skip add-ons and proceed with ticket booking only
        </p>
      </div>

      {/* Parking Modal */}
      {showParkingModal && (
        <ParkingModal 
          onClose={() => setShowParkingModal(false)}
          onSubmit={handleAddParking}
        />
      )}

      {/* Food Modal */}
      {showFoodModal && (
        <FoodModal 
          onClose={() => setShowFoodModal(false)}
          foodItems={foodItems}
          selectedItems={selectedFoodItems}
          onToggleItem={toggleFoodItem}
          onUpdateQuantity={updateFoodQuantity}
          onSubmit={handleAddFood}
          total={calculateFoodTotal()}
        />
      )}
    </div>
  );
};

// Parking Modal Component
const ParkingModal = ({ onClose, onSubmit }) => {
  const [vehicleType, setVehicleType] = useState('TWO_WHEELER');
  const [vehicleNumber, setVehicleNumber] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit({ vehicleType, vehicleNumber });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Add Parking</h3>
          <button onClick={onClose}><FaTimes /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Vehicle Type</label>
            <select value={vehicleType} onChange={e => setVehicleType(e.target.value)}>
              <option value="TWO_WHEELER">Two Wheeler (₹30/hr)</option>
              <option value="FOUR_WHEELER">Four Wheeler (₹50/hr)</option>
              <option value="EV">Electric Vehicle (₹60/hr)</option>
            </select>
          </div>
          <div className="form-group">
            <label>Vehicle Number</label>
            <input 
              type="text" 
              value={vehicleNumber}
              onChange={e => setVehicleNumber(e.target.value.toUpperCase())}
              placeholder="MH01AB1234"
              required
            />
          </div>
          <button type="submit" className="submit-btn">Add Parking</button>
        </form>
      </div>
    </div>
  );
};

// Food Modal Component
const FoodModal = ({ onClose, foodItems, selectedItems, onToggleItem, onUpdateQuantity, onSubmit, total }) => {
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content food-modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Select Food & Beverages</h3>
          <button onClick={onClose}><FaTimes /></button>
        </div>
        
        <div className="food-items">
          {foodItems.map(item => {
            const selected = selectedItems.find(i => i.id === item.id);
            return (
              <div key={item.id} className={`food-item ${selected ? 'selected' : ''}`}>
                <div className="food-info">
                  <h4>{item.name}</h4>
                  <p className="food-category">{item.category}</p>
                  <p className="food-price">₹{item.price}</p>
                </div>
                <div className="food-actions">
                  {!selected ? (
                    <button onClick={() => onToggleItem(item)} className="add-btn">Add</button>
                  ) : (
                    <div className="quantity-control">
                      <button onClick={() => onUpdateQuantity(item.id, selected.quantity - 1)}>-</button>
                      <span>{selected.quantity}</span>
                      <button onClick={() => onUpdateQuantity(item.id, selected.quantity + 1)}>+</button>
                      <button onClick={() => onToggleItem(item)} className="remove-small"><FaTimes /></button>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        <div className="modal-footer">
          <div className="food-total">
            <span>Total:</span>
            <strong>₹{total}</strong>
          </div>
          <button onClick={onSubmit} className="submit-btn" disabled={selectedItems.length === 0}>
            Add to Order
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentWithAddons;
