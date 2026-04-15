import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { FaCar, FaUtensils, FaArrowLeft, FaCheckCircle, FaRupeeSign, FaTag } from 'react-icons/fa';
import { MdLocalParking } from 'react-icons/md';
import { buildApiUrl } from '../../config/apiBaseUrl';
import './Checkout.scss';

const Checkout = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const bookingData = location.state;

  // Parking state
  const [parkingEnabled, setParkingEnabled] = useState(false);
  const [parkingDetails, setParkingDetails] = useState({
    vehicleType: '2W',
    vehicleNumber: '',
    durationHours: 3
  });
  const [parkingPrice, setParkingPrice] = useState(0);

  // Food state
  const [foodEnabled, setFoodEnabled] = useState(false);
  const [foodItems, setFoodItems] = useState([]);
  const [selectedFoodItems, setSelectedFoodItems] = useState([]);
  const [foodTotal, setFoodTotal] = useState(0);

  // Discount state
  const [parkingDiscount, setParkingDiscount] = useState(0);
  const [loading, setLoading] = useState(false);

  // Fetch food items
  useEffect(() => {
    fetchFoodItems();
  }, []);

  // Calculate parking price when details change
  useEffect(() => {
    if (parkingEnabled) {
      calculateParkingPrice();
    } else {
      setParkingPrice(0);
      setParkingDiscount(0);
    }
  }, [parkingEnabled, parkingDetails]);

  // Calculate food total and discount
  useEffect(() => {
    const total = selectedFoodItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    setFoodTotal(total);

    // Apply parking discount if parking is enabled
    if (parkingEnabled && total > 0) {
      const discount = Math.min(total * 0.10, 100); // 10% discount, max ₹100
      setParkingDiscount(discount);
    } else {
      setParkingDiscount(0);
    }
  }, [selectedFoodItems, parkingEnabled]);

  const fetchFoodItems = async () => {
    try {
      // Fetch food menu from backend using theaterId
      const theaterId = bookingData?.theaterId;
      if (!theaterId) {
        console.warn('No theaterId available, cannot fetch food menu');
        setFoodItems([]);
        return;
      }
      const response = await fetch(buildApiUrl(`/api/food/menu/${theaterId}`));
      if (response.ok) {
        const result = await response.json();
        // Backend wraps in ApiResponse: { success: true, data: [...] }
        const rawItems = result.data || result;
        // Normalize field names: backend uses itemName/imageUrl, frontend expects name/image
        const items = (Array.isArray(rawItems) ? rawItems : []).map(item => ({
          ...item,
          name: item.name || item.itemName || 'Unknown Item',
          image: item.image || item.imageUrl || '🍿'
        }));
        setFoodItems(items);
      } else {
        console.warn('Food menu not available for this theater');
        setFoodItems([]);
      }
    } catch (error) {
      console.error('Error fetching food items:', error);
      setFoodItems([]);
    }
  };

  const calculateParkingPrice = async () => {
    try {
      const theaterId = bookingData?.theaterId;
      const response = await fetch(
        buildApiUrl(`/api/parking/pricing?vehicleType=${parkingDetails.vehicleType}&hours=${parkingDetails.durationHours}${theaterId ? `&theaterId=${theaterId}` : ''}`)
      );
      
      if (response.ok) {
        const result = await response.json();
        // Backend returns: { success: true, data: { price: 200, hourlyRate: 50 } }
        const price = result.data?.price || result.price || 0;
        setParkingPrice(price);
        console.log('Parking price calculated:', price);
      } else {
        console.warn('Parking pricing not available from backend');
        setParkingPrice(0);
      }
    } catch (error) {
      console.error('Error calculating parking price:', error);
      setParkingPrice(0);
    }
  };

  const handleParkingToggle = () => {
    setParkingEnabled(!parkingEnabled);
  };

  const handleFoodToggle = () => {
    setFoodEnabled(!foodEnabled);
    if (!foodEnabled) {
      setSelectedFoodItems([]);
    }
  };

  const handleFoodItemAdd = (item) => {
    const existing = selectedFoodItems.find(f => f.id === item.id);
    if (existing) {
      setSelectedFoodItems(prev => 
        prev.map(f => f.id === item.id ? { ...f, quantity: f.quantity + 1 } : f)
      );
    } else {
      setSelectedFoodItems(prev => [...prev, { ...item, quantity: 1 }]);
    }
  };

  const handleFoodItemRemove = (itemId) => {
    const existing = selectedFoodItems.find(f => f.id === itemId);
    if (existing.quantity > 1) {
      setSelectedFoodItems(prev => 
        prev.map(f => f.id === itemId ? { ...f, quantity: f.quantity - 1 } : f)
      );
    } else {
      setSelectedFoodItems(prev => prev.filter(f => f.id !== itemId));
    }
  };

  const calculateFinalTotal = () => {
    const ticketAmount = bookingData.totalAmount;
    const finalFoodTotal = Math.max(0, foodTotal - parkingDiscount);
    return ticketAmount + parkingPrice + finalFoodTotal;
  };

  const handleProceedToPayment = () => {
    const checkoutData = {
      ...bookingData,
      parking: parkingEnabled ? {
        ...parkingDetails,
        amount: parkingPrice
      } : null,
      food: foodEnabled ? {
        items: selectedFoodItems,
        subtotal: foodTotal,
        discount: parkingDiscount,
        total: foodTotal - parkingDiscount
      } : null,
      finalTotal: calculateFinalTotal()
    };

    navigate('/booking/payment', { state: checkoutData });
  };

  if (!bookingData) {
    return (
      <div className="checkout-page">
        <div className="container">
          <div className="error-message">
            <h3>No booking details found</h3>
            <p>Please select seats to continue.</p>
            <button onClick={() => navigate('/')}>Go to Home</button>
          </div>
        </div>
      </div>
    );
  }

  const finalTotal = calculateFinalTotal();

  return (
    <div className="checkout-page">
      <div className="container">
        {/* Header */}
        <div className="page-header">
          <button className="back-btn" onClick={() => navigate(-1)}>
            <FaArrowLeft /> Back
          </button>
          <h2>Checkout & Add-ons</h2>
        </div>

        <div className="checkout-content">
          {/* Left: Add-ons */}
          <div className="addons-section">
            {/* Booking Summary Card */}
            <div className="summary-card">
              <h3>Booking Summary</h3>
              <div className="summary-details">
                <p><strong>{bookingData.movieName}</strong></p>
                <p>{bookingData.theaterName}</p>
                <p>{bookingData.showDate} • {bookingData.showTime}</p>
                <p>Seats: {bookingData.selectedSeats.join(', ')}</p>
                <div className="price-row">
                  <span>Ticket Amount:</span>
                  <span><FaRupeeSign />{bookingData.totalAmount}</span>
                </div>
              </div>
            </div>

            {/* Parking Add-on */}
            <div className={`addon-card ${parkingEnabled ? 'active' : ''}`}>
              <div className="addon-header">
                <div className="addon-title">
                  <MdLocalParking className="icon" />
                  <div>
                    <h3>Advance Parking Slot</h3>
                    <p className="subtitle">Skip the parking hassle</p>
                  </div>
                </div>
                <label className="toggle-switch">
                  <input 
                    type="checkbox" 
                    checked={parkingEnabled} 
                    onChange={handleParkingToggle}
                  />
                  <span className="slider"></span>
                </label>
              </div>

              {parkingEnabled && (
                <div className="addon-content">
                  <div className="form-group">
                    <label>Vehicle Type</label>
                    <div className="vehicle-types">
                      {['2W', '3W', '4W'].map(type => (
                        <button
                          key={type}
                          className={`vehicle-btn ${parkingDetails.vehicleType === type ? 'selected' : ''}`}
                          onClick={() => setParkingDetails({ ...parkingDetails, vehicleType: type })}
                        >
                          {type === '2W' && '🏍️'} 
                          {type === '3W' && '🛺'} 
                          {type === '4W' && '🚗'}
                          <span>{type}</span>
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="form-group">
                    <label>Vehicle Number (Optional)</label>
                    <input
                      type="text"
                      placeholder="KA01AB1234"
                      value={parkingDetails.vehicleNumber}
                      onChange={(e) => setParkingDetails({ ...parkingDetails, vehicleNumber: e.target.value.toUpperCase() })}
                      className="input-field"
                    />
                  </div>

                  <div className="form-group">
                    <label>Parking Duration</label>
                    <select
                      value={parkingDetails.durationHours}
                      onChange={(e) => setParkingDetails({ ...parkingDetails, durationHours: parseInt(e.target.value) })}
                      className="select-field"
                    >
                      {[1, 2, 3, 4, 5, 6].map(hour => (
                        <option key={hour} value={hour}>{hour} hour{hour > 1 ? 's' : ''}</option>
                      ))}
                    </select>
                  </div>

                  <div className="price-display">
                    <span>Parking Fee:</span>
                    <span className="amount"><FaRupeeSign />{parkingPrice}</span>
                  </div>
                </div>
              )}
            </div>

            {/* Food Add-on */}
            <div className={`addon-card ${foodEnabled ? 'active' : ''}`}>
              <div className="addon-header">
                <div className="addon-title">
                  <FaUtensils className="icon" />
                  <div>
                    <h3>Food & Beverages</h3>
                    <p className="subtitle">Pre-order your favorite snacks</p>
                    {parkingEnabled && foodEnabled && (
                      <p className="offer-badge">
                        <FaTag /> 10% OFF with parking
                      </p>
                    )}
                  </div>
                </div>
                <label className="toggle-switch">
                  <input 
                    type="checkbox" 
                    checked={foodEnabled} 
                    onChange={handleFoodToggle}
                  />
                  <span className="slider"></span>
                </label>
              </div>

              {foodEnabled && (
                <div className="addon-content">
                  <div className="food-items-grid">
                    {foodItems.map(item => (
                      <div key={item.id} className="food-item">
                        <div className="food-item-info">
                          <span className="food-emoji">{item.image}</span>
                          <div>
                            <p className="food-name">{item.name}</p>
                            <p className="food-price"><FaRupeeSign />{item.price}</p>
                          </div>
                        </div>
                        <div className="food-item-actions">
                          {selectedFoodItems.find(f => f.id === item.id) ? (
                            <div className="quantity-controls">
                              <button onClick={() => handleFoodItemRemove(item.id)}>-</button>
                              <span>{selectedFoodItems.find(f => f.id === item.id).quantity}</span>
                              <button onClick={() => handleFoodItemAdd(item)}>+</button>
                            </div>
                          ) : (
                            <button 
                              className="add-btn"
                              onClick={() => handleFoodItemAdd(item)}
                            >
                              Add
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>

                  {selectedFoodItems.length > 0 && (
                    <div className="food-summary">
                      <div className="price-row">
                        <span>Food Subtotal:</span>
                        <span><FaRupeeSign />{foodTotal}</span>
                      </div>
                      {parkingDiscount > 0 && (
                        <div className="price-row discount">
                          <span>Parking Discount (10%):</span>
                          <span>- <FaRupeeSign />{parkingDiscount}</span>
                        </div>
                      )}
                      <div className="price-row total">
                        <span>Food Total:</span>
                        <span><FaRupeeSign />{foodTotal - parkingDiscount}</span>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Right: Price Summary */}
          <div className="price-summary-section">
            <div className="summary-card sticky">
              <h3>Payment Summary</h3>
              
              <div className="price-breakdown">
                <div className="price-row">
                  <span>Ticket Price</span>
                  <span><FaRupeeSign />{bookingData.totalAmount}</span>
                </div>

                {parkingEnabled && (
                  <div className="price-row">
                    <span>Parking Fee</span>
                    <span><FaRupeeSign />{parkingPrice}</span>
                  </div>
                )}

                {foodEnabled && selectedFoodItems.length > 0 && (
                  <>
                    <div className="price-row">
                      <span>Food & Beverages</span>
                      <span><FaRupeeSign />{foodTotal}</span>
                    </div>
                    {parkingDiscount > 0 && (
                      <div className="price-row discount">
                        <span>Parking Offer</span>
                        <span>- <FaRupeeSign />{parkingDiscount}</span>
                      </div>
                    )}
                  </>
                )}

                <div className="divider"></div>

                <div className="price-row total">
                  <span>Total Amount</span>
                  <span><FaRupeeSign />{finalTotal}</span>
                </div>
              </div>

              <button 
                className="proceed-btn"
                onClick={handleProceedToPayment}
                disabled={loading}
              >
                <FaCheckCircle /> Proceed to Payment
              </button>

              <p className="secure-note">
                🔒 Your payment is secure and encrypted
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Checkout;
