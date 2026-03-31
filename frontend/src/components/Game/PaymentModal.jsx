  import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import './PaymentModal.scss';

const PaymentModal = ({ userId, onSuccess, onClose, paymentOption }) => {
  const EXTRA_SPIN_PRICE = 10;
  const { user } = useAuth();
  
  const [selectedPayment, setSelectedPayment] = useState(paymentOption || null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState(null);
  const [mainWalletBalance, setMainWalletBalance] = useState(0);
  const [tempWalletBalance, setTempWalletBalance] = useState(0);

  useEffect(() => {
    fetchWalletBalances();
  }, [user?.id]);

  const fetchWalletBalances = async () => {
    try {
      // Fetch SEPARATE main and temporary wallet balances
      const endpoint = `/api/wallet/balance/${user?.id || userId}`;
      const response = await fetch(endpoint);
      
      if (response.ok) {
        const result = await response.json();
        const walletData = result.data || {};
        
        // Keep them SEPARATE
        const main = walletData.balance || 0;
        const temp = walletData.temporaryBalance || 0;
        
        setMainWalletBalance(main);
        setTempWalletBalance(temp);
        
        console.log('✅ Wallet balances fetched (SEPARATE):', {
          main: main,
          temporary: temp
        });
      }
    } catch (err) {
      console.error('Error fetching wallet balances:', err);
      setMainWalletBalance(0);
      setTempWalletBalance(0);
    }
  };

  // Handle Main Wallet Payment
  const handleMainWalletPayment = async () => {
    if (mainWalletBalance < EXTRA_SPIN_PRICE) {
      setError(`Insufficient Main Wallet balance. You have ₹${mainWalletBalance.toFixed(2)}`);
      return;
    }

    setIsProcessing(true);
    setError(null);

    try {
      const response = await fetch('/api/game/purchase-extra-spin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: user?.id || userId,
          amount: EXTRA_SPIN_PRICE,
          paymentMethod: 'MAIN_WALLET',
          timestamp: new Date().toISOString()
        }),
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error || 'Payment failed');
      }

      const data = await response.json();
      console.log('✅ Main wallet payment successful:', data);
      setIsProcessing(false);
      onSuccess();
    } catch (err) {
      console.error('Main wallet payment error:', err);
      setError(err.message || 'Main wallet payment failed. Please try again.');
      setIsProcessing(false);
    }
  };

  // Handle Temporary Wallet Payment (Greedy)
  const handleTempWalletPayment = async () => {
    if (tempWalletBalance < EXTRA_SPIN_PRICE) {
      setError(`Insufficient Temporary Wallet balance. You have ₹${tempWalletBalance.toFixed(2)}`);
      return;
    }

    setIsProcessing(true);
    setError(null);

    try {
      const response = await fetch('/api/game/purchase-extra-spin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: user?.id || userId,
          amount: EXTRA_SPIN_PRICE,
          paymentMethod: 'TEMPORARY_WALLET',
          timestamp: new Date().toISOString()
        }),
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error || 'Payment failed');
      }

      const data = await response.json();
      console.log('✅ Temporary wallet payment successful (greedy deduction):', data);
      setIsProcessing(false);
      onSuccess();
    } catch (err) {
      console.error('Temporary wallet payment error:', err);
      setError(err.message || 'Temporary wallet payment failed. Please try again.');
      setIsProcessing(false);
    }
  };

  const handleCardPayment = async () => {
    setIsProcessing(true);
    setError(null);

    try {
      const response = await fetch('/api/game/purchase-extra-spin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          userId: user?.id || userId,
          amount: EXTRA_SPIN_PRICE,
          paymentMethod: 'CARD',
          timestamp: new Date().toISOString(),
          cardLast4: '4242'
        }),
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error || 'Card payment failed');
      }

      await response.json();
      setIsProcessing(false);
      onSuccess();
    } catch (err) {
      setError(err.message || 'Card payment failed. Please try again.');
      setIsProcessing(false);
    }
  };

  return (
    <div className="payment-modal-overlay" onClick={onClose}>
      <div className="payment-modal" onClick={e => e.stopPropagation()}>
        <button className="close-btn" onClick={onClose}>✕</button>

        <div className="modal-header">
          <h2>💳 Buy Extra Spin</h2>
          <p>Keep spinning and winning!</p>
        </div>

        {error && <div className="modal-error">{error}</div>}

        <div className="payment-info">
          <div className="amount-box">
            <span className="label">Cost per spin:</span>
            <span className="amount">₹{EXTRA_SPIN_PRICE}</span>
          </div>
        </div>

        {/* SEPARATE Wallet Balances */}
        <div className="wallet-balances-separate">
          <div className="balance-card main-wallet">
            <span className="icon">🏦</span>
            <div className="balance-info">
              <p className="label">Main Wallet</p>
              <p className="amount">₹{mainWalletBalance.toFixed(2)}</p>
              <p className="note">No expiry • Permanent</p>
            </div>
          </div>

          <div className="balance-card temp-wallet">
            <span className="icon">⏰</span>
            <div className="balance-info">
              <p className="label">Temporary Wallet</p>
              <p className="amount">₹{tempWalletBalance.toFixed(2)}</p>
              <p className="note">Expires in 10-15 days</p>
            </div>
          </div>
        </div>

        {/* THREE Payment Options */}
        <div className="payment-methods">
          <h3>Select Payment Method</h3>

          {/* Option 1: Main Wallet */}
          <label className="payment-option">
            <input
              type="radio"
              name="payment-method"
              value="main-wallet"
              checked={selectedPayment === 'main-wallet'}
              onChange={(e) => setSelectedPayment(e.target.value)}
              disabled={isProcessing || mainWalletBalance < EXTRA_SPIN_PRICE}
            />
            <div className="option-content">
              <span className="option-icon">💳</span>
              <div>
                <p className="option-title">Main Wallet</p>
                <p className="option-desc">₹{mainWalletBalance.toFixed(2)} available</p>
              </div>
            </div>
          </label>

          {/* Option 2: Temporary Wallet */}
          <label className="payment-option">
            <input
              type="radio"
              name="payment-method"
              value="temp-wallet"
              checked={selectedPayment === 'temp-wallet'}
              onChange={(e) => setSelectedPayment(e.target.value)}
              disabled={isProcessing || tempWalletBalance < EXTRA_SPIN_PRICE}
            />
            <div className="option-content">
              <span className="option-icon">🎁</span>
              <div>
                <p className="option-title">Temporary Wallet (Rewards)</p>
                <p className="option-desc">₹{tempWalletBalance.toFixed(2)} available • Expires soon</p>
              </div>
            </div>
          </label>

          {/* Option 3: Credit/Debit Card */}
          <label className="payment-option">
            <input
              type="radio"
              name="payment-method"
              value="card"
              checked={selectedPayment === 'card'}
              onChange={(e) => setSelectedPayment(e.target.value)}
              disabled={isProcessing}
            />
            <div className="option-content">
              <span className="option-icon">💰</span>
              <div>
                <p className="option-title">Credit/Debit Card</p>
                <p className="option-desc">Visa, Mastercard, Rupay</p>
              </div>
            </div>
          </label>
        </div>

        <div className="modal-actions">
          <button
            className="cancel-btn"
            onClick={onClose}
            disabled={isProcessing}
          >
            Cancel
          </button>

          {selectedPayment === 'main-wallet' ? (
            <button
              className="pay-btn"
              onClick={handleMainWalletPayment}
              disabled={isProcessing || mainWalletBalance < EXTRA_SPIN_PRICE}
            >
              {isProcessing ? (
                <>
                  <span className="spinner"></span>
                  Processing...
                </>
              ) : (
                `Pay ₹${EXTRA_SPIN_PRICE} from Main Wallet`
              )}
            </button>
          ) : selectedPayment === 'temp-wallet' ? (
            <button
              className="pay-btn"
              onClick={handleTempWalletPayment}
              disabled={isProcessing || tempWalletBalance < EXTRA_SPIN_PRICE}
            >
              {isProcessing ? (
                <>
                  <span className="spinner"></span>
                  Processing...
                </>
              ) : (
                `Pay ₹${EXTRA_SPIN_PRICE} from Temporary Wallet`
              )}
            </button>
          ) : selectedPayment === 'card' ? (
            <button
              className="pay-btn"
              onClick={handleCardPayment}
              disabled={isProcessing}
            >
              {isProcessing ? (
                <>
                  <span className="spinner"></span>
                  Processing...
                </>
              ) : (
                `Pay ₹${EXTRA_SPIN_PRICE} via Card`
              )}
            </button>
          ) : (
            <button
              className="pay-btn"
              disabled={true}
            >
              Select a payment method
            </button>
          )}
        </div>

        <p className="security-note">🔒 No GST or convenience fees for spin purchases • Encrypted & Secure</p>
      </div>
    </div>
  );
};

export default PaymentModal;
