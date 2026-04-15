import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { loadStripe } from '@stripe/stripe-js';
import { Elements, CardElement, useStripe, useElements } from '@stripe/react-stripe-js';
import { FaCreditCard, FaLock, FaCheckCircle, FaWallet } from 'react-icons/fa';
import { walletService, paymentService, otpService } from '../../services';
import SeatPakkiPopup from '../../components/Common/SeatPakkiPopup';
import { buildApiUrl } from '../../config/apiBaseUrl';
import './Payment.scss';
const FALLBACK_STRIPE_KEY =
  (import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || '').trim() ||
  'pk_test_51SmK7UBMTQ3xf1vt2TERduPEkRF4ySl3VHQwMbW9CXBYmzUePlAaUZtol1xEB1lBknfAMCZoO9jOQQnw4Kq3ULWv002QEjCK39';

// Initialize Stripe (publishable key will be fetched from backend)
let stripePromise = null;

const PaymentForm = ({
  bookingDetails,
  pricing,
  walletAmount = 0,
  useSplitPayment = false,
  payableTotal = 0,
  useTemporaryWallet = true,
  requestOtpCode
}) => {
  const stripe = useStripe();
  const elements = useElements();
  const navigate = useNavigate();
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);
  const isSplitPayment = useSplitPayment && walletAmount > 0;

  const resolveUserId = () => {
    const fromBooking = bookingDetails?.userId;
    if (fromBooking) return Number(fromBooking);

    const storedUserRaw = localStorage.getItem('user');
    if (storedUserRaw) {
      try {
        const parsed = JSON.parse(storedUserRaw);
        const parsedId = parsed?.id || parsed?.userId;
        if (parsedId) return Number(parsedId);
      } catch (error) {
      }
    }

    return null;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!stripe || !elements) {
      return;
    }

    setProcessing(true);
    setError(null);

    try {
      // Calculate base ticket amount (without fees/tax) + food + parking
      const ticketBaseAmount = bookingDetails.baseAmount || 0;
      const foodAmount = bookingDetails.food ? (bookingDetails.food.total || 0) : 0;
      const parkingAmount = bookingDetails.parking ? (bookingDetails.parking.amount || 0) : 0;
      const totalBaseAmount = ticketBaseAmount + foodAmount + parkingAmount;
      
      const userId = resolveUserId();
      if (!userId) {
        throw new Error('Unable to identify logged-in user for OTP verification. Please login again.');
      }
      
      let response, paymentData;
      
      if (isSplitPayment) {
        // Step 1: Create split payment intent (wallet + card)
        response = await fetch(buildApiUrl('/api/payment/create-split-payment-intent'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            sessionId: bookingDetails.sessionId,
            userId: userId,
            baseAmount: totalBaseAmount,
            walletAmount: walletAmount,
            useTemporaryWallet,
            promoCode: bookingDetails.promoCode || null
          })
        });
      } else {
        // Step 1: Create regular Stripe payment intent (card only)
        response = await fetch(buildApiUrl('/api/payment/create-stripe-intent'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            sessionId: bookingDetails.sessionId,
            userId: userId,
            baseAmount: totalBaseAmount,
            useTemporaryWallet,
            promoCode: bookingDetails.promoCode || null
          })
        });
      }

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || 'Failed to create payment');
      }

      paymentData = await response.json();
      const { clientSecret, paymentIntentId, transactionId, error: apiError } = paymentData;

      if (apiError) {
        throw new Error(apiError);
      }

      // Step 2: Confirm payment with Stripe
      const cardAmount = isSplitPayment
        ? (paymentData.cardAmount ?? Math.max(payableTotal - walletAmount, 0))
        : (paymentData.cardAmount ?? paymentData.payableAmount ?? payableTotal);
      const { error: stripeError, paymentIntent } = await stripe.confirmCardPayment(
        clientSecret,
        {
          payment_method: {
            card: elements.getElement(CardElement),
            billing_details: {
              name: bookingDetails.userName || 'Guest',
              email: bookingDetails.userEmail || 'user@example.com'
            }
          }
        }
      );

      if (stripeError) {
        throw new Error(stripeError.message);
      }

      if (paymentIntent.status === 'succeeded') {
        const otpSendResponse = await otpService.sendOtp({
          userId: userId,
          purpose: 'PAYMENT',
          referenceId: transactionId
        });

        const otpPromptMessage = otpSendResponse?.maskedMobile
          ? `Enter OTP sent to your ${otpSendResponse?.channel === 'EMAIL' ? 'registered email' : 'registered mobile'} (${otpSendResponse.maskedMobile})`
          : 'Enter OTP sent to your registered mobile number';
        const otpCode = await requestOtpCode(otpPromptMessage);
        if (!otpCode || !otpCode.trim()) {
          throw new Error('OTP is required to complete payment');
        }

        // Step 3: Confirm in our backend
        const confirmEndpoint = isSplitPayment
          ? buildApiUrl('/api/payment/confirm-split-payment')
          : buildApiUrl('/api/payment/confirm-stripe');
          
        const confirmResponse = await fetch(confirmEndpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            paymentIntentId: paymentIntent.id,
            transactionId: transactionId,
            otpCode: otpCode.trim(),
            otpRequestId: otpSendResponse.otpRequestId
          })
        });

        if (!confirmResponse.ok) {
          const errorData = await confirmResponse.json();
          throw new Error(errorData.error || 'Failed to confirm payment');
        }

        const confirmData = await confirmResponse.json();

        setSuccess(true);
        setTimeout(() => {
          navigate(`/booking/confirmation/${transactionId}`, {
            state: { 
              ...bookingDetails, 
              paymentIntentId: paymentIntent.id,
              transactionId: transactionId,
              paymentMethod: isSplitPayment ? 'WALLET_CARD_SPLIT' : 'STRIPE',
              walletAmount: isSplitPayment ? walletAmount : 0,
              cardAmount: cardAmount
            }
          });
        }, 2000);
      }
    } catch (err) {
      const errorMessage = err?.response?.data?.error || err?.message || 'Payment failed';
      setError(errorMessage);
      setProcessing(false);
    }
  };

  const cardElementOptions = {
    style: {
      base: {
        fontSize: '16px',
        color: '#424770',
        '::placeholder': {
          color: '#aab7c4',
        },
      },
      invalid: {
        color: '#9e2146',
      },
    },
  };

  if (success) {
    return (
      <div className="payment-success">
        <FaCheckCircle size={60} color="#FBC02D" />
        <h2>Payment Successful!</h2>
        <p>Redirecting to confirmation...</p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="payment-form">
      <div className="card-element-wrapper">
        <label>
          <FaCreditCard /> Card Details
        </label>
        <CardElement options={cardElementOptions} />
      </div>

      {error && <div className="payment-error">{error}</div>}

      <div className="test-card-info">
        <p><strong>Test Card:</strong> 4242 4242 4242 4242</p>
        <p>Use any future expiry date and any 3-digit CVC</p>
      </div>

      <button 
        type="submit" 
        className="pay-button"
        disabled={!stripe || processing}
      >
        <FaLock /> {processing ? 'Processing...' : isSplitPayment
          ? `Pay ₹${Math.max(payableTotal - walletAmount, 0).toFixed(2)} by Card` 
          : `Pay ₹${payableTotal.toFixed(2)}`}
      </button>
    </form>
  );
};

const Payment = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [stripeKey, setStripeKey] = useState(null);
  const [paymentMethod, setPaymentMethod] = useState('WALLET'); // Default to wallet
  const [walletBalance, setWalletBalance] = useState(0);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState(null);
  const [walletAmount, setWalletAmount] = useState(0);
  const [useSplitPayment, setUseSplitPayment] = useState(false);
  const [temporaryCreditAmount, setTemporaryCreditAmount] = useState(0);
  const [temporaryCreditExpiry, setTemporaryCreditExpiry] = useState(null);
  const [useTemporaryWallet, setUseTemporaryWallet] = useState(true);
  const [otpPopup, setOtpPopup] = useState({
    isOpen: false,
    message: '',
    value: '',
    resolve: null
  });
  const bookingDetails = location.state;

  // Recalculate fees and tax based on complete amount (tickets + food + parking)
  // Exception: EXTRA_SPIN transactions have no fees or tax
  const getRecalculatedPricing = () => {
    if (!bookingDetails) return { baseAmount: 0, convenienceFee: 0, tax: 0, total: 0 };
    
    const ticketBaseAmount = parseFloat(bookingDetails.baseAmount) || 0;
    const foodAmount = bookingDetails.food ? parseFloat(bookingDetails.food.total || 0) : 0;
    const parkingAmount = bookingDetails.parking ? parseFloat(bookingDetails.parking.amount || 0) : 0;
    
    const totalBaseAmount = ticketBaseAmount + foodAmount + parkingAmount;
    
    // NO FEES for EXTRA_SPIN transactions (spin wheel purchases)
    if (bookingDetails.transactionType === 'EXTRA_SPIN') {
      return {
        baseAmount: totalBaseAmount,
        convenienceFee: 0,
        tax: 0,
        total: totalBaseAmount
      };
    }
    
    // Regular fees for movies and other transactions
    const convenienceFee = Math.max(totalBaseAmount * 0.025, 20); // 2.5% or min ₹20
    const tax = (totalBaseAmount + convenienceFee) * 0.18; // 18% GST
    const total = totalBaseAmount + convenienceFee + tax;
    
    return {
      baseAmount: totalBaseAmount,
      convenienceFee: convenienceFee,
      tax: tax,
      total: total
    };
  };

  const pricing = getRecalculatedPricing();
  const appliedTemporaryCredit = useTemporaryWallet ? temporaryCreditAmount : 0;
  const payableTotal = Math.max(0, pricing.total - appliedTemporaryCredit);
  const isZeroPayable = payableTotal <= 0.0001;

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

  useEffect(() => {
    // Fetch Stripe publishable key and wallet balance
    const fetchData = async () => {
      try {
        let publishableKey = '';

        // Fetch Stripe key from backend
        const stripeResponse = await fetch(buildApiUrl('/api/payment/stripe-config'));
        if (stripeResponse.ok) {
          const stripeData = await stripeResponse.json();
          publishableKey = (stripeData.publishableKey || '').trim();
        }

        // Fallback key for local development resilience
        if (!publishableKey) {
          publishableKey = FALLBACK_STRIPE_KEY;
        }

        if (publishableKey) {
          setStripeKey(publishableKey);
          stripePromise = loadStripe(publishableKey);
        } else {
          setStripeKey(null);
          stripePromise = null;
        }

        // Fetch wallet balance if user is logged in
        if (user?.id) {
          const walletResponse = await walletService.getBalance(user.id);
          const balance = walletResponse.data?.balance || walletResponse.balance || 0;
          setWalletBalance(balance);

          const tempCreditResponse = await walletService.getTemporaryCredit(user.id);
          const tempCreditData = tempCreditResponse.data || tempCreditResponse;
          setTemporaryCreditAmount(Number(tempCreditData?.availableAmount || 0));
          setTemporaryCreditExpiry(tempCreditData?.expiresAt || null);
          setUseTemporaryWallet(Number(tempCreditData?.availableAmount || 0) > 0);
        }
      } catch (error) {
        console.error('Failed to load payment config:', error);
        if (FALLBACK_STRIPE_KEY) {
          setStripeKey(FALLBACK_STRIPE_KEY);
          stripePromise = loadStripe(FALLBACK_STRIPE_KEY);
        }
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [user]);

  useEffect(() => {
    if (isZeroPayable) {
      setPaymentMethod('WALLET');
      setUseSplitPayment(false);
      setWalletAmount(0);
    }
  }, [isZeroPayable]);

  const handleWalletPayment = async () => {
    if (walletBalance < payableTotal) {
      setError('Insufficient wallet balance. Please add money to your wallet or choose another payment method.');
      return;
    }

    setProcessing(true);
    setError(null);

    try {
      // Calculate base ticket amount (without fees/tax) + food + parking
      const ticketBaseAmount = bookingDetails.baseAmount || 0;
      const foodAmount = bookingDetails.food ? (bookingDetails.food.total || 0) : 0;
      const parkingAmount = bookingDetails.parking ? (bookingDetails.parking.amount || 0) : 0;
      const totalBaseAmount = ticketBaseAmount + foodAmount + parkingAmount;
      
      // Initiate payment with WALLET method
      const paymentData = {
        sessionId: bookingDetails.sessionId,
        userId: user.id,
        baseAmount: totalBaseAmount,
        paymentMethod: 'WALLET',
        useTemporaryWallet,
        promoCode: bookingDetails.promoCode || null
      };

      const initiateResponse = await paymentService.initiatePayment(paymentData);
      const transactionId = initiateResponse.transactionId;

      let processPayload = {};
      if (!isZeroPayable) {
        const otpSendResponse = await otpService.sendOtp({
          userId: user.id,
          purpose: 'PAYMENT',
          referenceId: transactionId
        });

        const otpPromptMessage = otpSendResponse?.maskedMobile
          ? `Enter OTP sent to ${otpSendResponse.maskedMobile}`
          : 'Enter OTP sent to your registered mobile number';
        const otpCode = await requestOtpCode(otpPromptMessage);
        if (!otpCode || !otpCode.trim()) {
          throw new Error('OTP is required to complete payment');
        }

        processPayload = {
          otpCode: otpCode.trim(),
          otpRequestId: otpSendResponse.otpRequestId
        };
      }

      // Process payment (deduct temp wallet / wallet / finalize booking)
      const processResponse = await paymentService.processPayment(transactionId, processPayload);

      if (processResponse.status === 'SUCCESS') {
        navigate(`/booking/confirmation/${transactionId}`, {
          state: { 
            ...bookingDetails, 
            transactionId,
            paymentMethod: 'WALLET'
          }
        });
      } else {
        throw new Error(processResponse.message || 'Payment failed');
      }
    } catch (err) {
      console.error('Wallet payment error:', err);
      setError(err.response?.data?.error || err.message || 'Payment failed. Please try again.');
      setProcessing(false);
    }
  };

  if (!bookingDetails) {
    return (
      <div className="payment-page">
        <div className="container">
          <div className="error-message">
            <h3>No booking details found</h3>
            <p>Please select seats and try again.</p>
            <button onClick={() => navigate('/')}>Go to Home</button>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="payment-page">
        <div className="container">
          <div className="loading-container">
            <div className="spinner"></div>
            <p>Loading payment gateway...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="payment-page">
      <div className="container">
        <div className="payment-container">
          <div className="payment-header">
            <h1>Complete Your Payment</h1>
            <p className="secure-badge">
              <FaLock /> Secured Payment
            </p>
          </div>

          <div className="payment-content">
            <div className="payment-summary">
              <h3>Booking Summary</h3>
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
              <div className="summary-divider"></div>
              <div className="summary-item">
                <span>Ticket Price:</span>
                <strong>₹{bookingDetails.baseAmount}</strong>
              </div>
              {bookingDetails.food && (
                <>
                  <div className="summary-item">
                    <span>Food & Beverages:</span>
                    <strong>₹{bookingDetails.food.subtotal || 0}</strong>
                  </div>
                  {bookingDetails.food.discount > 0 && (
                    <div className="summary-item" style={{color: '#FBC02D'}}>
                      <span>Food Discount:</span>
                      <strong>-₹{bookingDetails.food.discount}</strong>
                    </div>
                  )}
                </>
              )}
              {bookingDetails.parking && (
                <>
                  <div className="summary-item">
                    <span>Parking:</span>
                    <strong>₹{bookingDetails.parking.amount || 0}</strong>
                  </div>
                </>
              )}
              <div className="summary-divider"></div>
              <div className="summary-item">
                <span>Convenience Fee:</span>
                <strong>₹{pricing.convenienceFee.toFixed(2)}</strong>
              </div>
              <div className="summary-item">
                <span>GST (18%):</span>
                <strong>₹{pricing.tax.toFixed(2)}</strong>
              </div>
              <div className="summary-divider"></div>
              <div className="summary-item total">
                <span>Total Amount:</span>
                <strong>₹{pricing.total.toFixed(2)}</strong>
              </div>
              {temporaryCreditAmount > 0 && (
                <>
                  <div className="summary-item">
                    <span>Use Temporary Wallet:</span>
                    <strong>
                      <input
                        type="checkbox"
                        checked={useTemporaryWallet}
                        onChange={(e) => setUseTemporaryWallet(e.target.checked)}
                      />
                    </strong>
                  </div>
                  <div className="summary-item" style={{ color: '#2e7d32' }}>
                    <span>Temporary Wallet Credit:</span>
                    <strong>-₹{Math.min(appliedTemporaryCredit, pricing.total).toFixed(2)}</strong>
                  </div>
                  <div className="summary-item total">
                    <span>Amount to Pay:</span>
                    <strong>₹{payableTotal.toFixed(2)}</strong>
                  </div>
                  {temporaryCreditExpiry && (
                    <small style={{ color: '#666' }}>
                      Temporary credit valid till: {new Date(temporaryCreditExpiry).toLocaleString('en-IN')}
                    </small>
                  )}
                </>
              )}
            </div>

            <div className="payment-form-container">
              {/* Payment Method Selection */}
              <div className="payment-method-selection">
                <h3>Select Payment Method</h3>
                <div className="payment-methods">
                  <div 
                    className={`payment-method-option ${paymentMethod === 'WALLET' ? 'active' : ''}`}
                    onClick={() => { setPaymentMethod('WALLET'); setUseSplitPayment(false); }}
                  >
                    <div className="method-header">
                      <FaWallet className="method-icon" />
                      <span className="method-name">Wallet Only</span>
                    </div>
                    <div className="wallet-balance">
                      Balance: ₹{walletBalance.toFixed(2)}
                    </div>
                    {walletBalance < payableTotal && (
                      <div className="insufficient-balance">
                        Insufficient balance (Shortfall: ₹{(payableTotal - walletBalance).toFixed(2)})
                      </div>
                    )}
                  </div>
                  {!isZeroPayable && (
                    <div 
                      className={`payment-method-option ${paymentMethod === 'CARD' ? 'active' : ''}`}
                      onClick={() => { setPaymentMethod('CARD'); setUseSplitPayment(false); }}
                    >
                      <div className="method-header">
                        <FaCreditCard className="method-icon" />
                        <span className="method-name">Card Only</span>
                      </div>
                      <div className="method-description">Pay securely with Stripe</div>
                    </div>
                  )}
                  {!isZeroPayable && walletBalance > 0 && (
                    <div 
                      className={`payment-method-option ${paymentMethod === 'SPLIT' ? 'active' : ''}`}
                      onClick={() => { 
                        setPaymentMethod('SPLIT'); 
                        setUseSplitPayment(true); 
                        setWalletAmount(Math.min(walletBalance, payableTotal)); 
                      }}
                    >
                      <div className="method-header">
                        <FaWallet className="method-icon" />
                        <FaCreditCard className="method-icon" style={{marginLeft: '5px'}} />
                        <span className="method-name">Wallet + Card</span>
                      </div>
                      <div className="method-description">
                        {walletBalance >= payableTotal 
                          ? 'Use wallet for part of payment' 
                          : 'Pay partially from wallet, rest by card'}
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {error && (
                <div className="payment-error">
                  {error}
                </div>
              )}

              {/* Wallet Payment Section */}
              {paymentMethod === 'WALLET' && (
                <div className="wallet-payment-section">
                  <button 
                    onClick={handleWalletPayment}
                    disabled={processing || walletBalance < payableTotal}
                    className="wallet-pay-button"
                  >
                    {processing ? 'Processing...' : isZeroPayable
                      ? 'Complete Booking'
                      : `Pay ₹${payableTotal.toFixed(2)} from Wallet`}
                  </button>
                  {walletBalance < payableTotal && (
                    <p className="wallet-note">
                      Insufficient balance. Try Wallet + Card option.
                    </p>
                  )}
                </div>
              )}

              {/* Split Payment Section */}
              {paymentMethod === 'SPLIT' && (
                <div className="split-payment-section">
                  <div className="split-input-group">
                    <label>Amount to pay from Wallet (Max: ₹{Math.min(walletBalance, payableTotal).toFixed(2)})</label>
                    <input 
                      type="number"
                      value={walletAmount}
                      onChange={(e) => {
                        const val = parseFloat(e.target.value) || 0;
                        setWalletAmount(Math.min(Math.max(0, val), Math.min(walletBalance, payableTotal)));
                      }}
                      min="0"
                      max={Math.min(walletBalance, payableTotal)}
                      step="0.01"
                      className="wallet-amount-input"
                    />
                  </div>
                  <div className="split-summary">
                    <div className="split-item">
                      <span>From Wallet:</span>
                      <strong>₹{walletAmount.toFixed(2)}</strong>
                    </div>
                    <div className="split-item">
                      <span>From Card:</span>
                      <strong>₹{Math.max(payableTotal - walletAmount, 0).toFixed(2)}</strong>
                    </div>
                    <div className="split-divider"></div>
                    <div className="split-item total">
                      <span>Amount to Pay:</span>
                      <strong>₹{payableTotal.toFixed(2)}</strong>
                    </div>
                  </div>
                  {stripeKey && stripePromise ? (
                    <Elements stripe={stripePromise}>
                      <PaymentForm 
                        bookingDetails={{...bookingDetails, useSplitPayment: true}} 
                        pricing={pricing} 
                        walletAmount={walletAmount}
                        useSplitPayment={true}
                        payableTotal={payableTotal}
                        useTemporaryWallet={useTemporaryWallet}
                        requestOtpCode={requestOtpCode}
                      />
                    </Elements>
                  ) : (
                    <div className="error-message">
                      <p>Payment gateway unavailable. Please try again later.</p>
                    </div>
                  )}
                </div>
              )}

              {/* Card Payment Section */}
              {paymentMethod === 'CARD' && (
                <>
                  {stripeKey && stripePromise ? (
                    <Elements stripe={stripePromise}>
                      <PaymentForm 
                        bookingDetails={{...bookingDetails, useSplitPayment: false}} 
                        pricing={pricing} 
                        walletAmount={0}
                        useSplitPayment={false}
                        payableTotal={payableTotal}
                        useTemporaryWallet={useTemporaryWallet}
                        requestOtpCode={requestOtpCode}
                      />
                    </Elements>
                  ) : (
                    <div className="error-message">
                      <p>Payment gateway unavailable. Please try again later.</p>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
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

export default Payment;

