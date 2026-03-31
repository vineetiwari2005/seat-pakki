import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { FaGift, FaClock, FaCheckCircle, FaTimesCircle, FaSync } from 'react-icons/fa';
import './TemporaryWalletHistory.scss';

const TemporaryWalletHistory = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [tempWalletBalance, setTempWalletBalance] = useState(0);
  const [transactions, setTransactions] = useState([]);
  const [filteredTransactions, setFilteredTransactions] = useState([]);
  const [selectedFilter, setSelectedFilter] = useState('ALL');
  const [error, setError] = useState(null);

  useEffect(() => {
    if (user?.id) {
      fetchTemporaryWalletData();
    } else {
      navigate('/login');
    }
  }, [user, navigate]);

  useEffect(() => {
    applyFilter();
  }, [selectedFilter, transactions]);

  const fetchTemporaryWalletData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch balance
      const balanceResponse = await fetch(`/api/wallet/balance/${user.id}`);
      if (balanceResponse.ok) {
        const balanceData = await balanceResponse.json();
        setTempWalletBalance(balanceData.data?.temporaryBalance || 0);
      }

      // Fetch temporary wallet transactions
      const txnResponse = await fetch(`/api/wallet/temporary-transactions/${user.id}`);
      if (txnResponse.ok) {
        const txnData = await txnResponse.json();
        const txnList = Array.isArray(txnData) ? txnData : txnData.data || [];
        setTransactions(txnList);
      }
    } catch (err) {
      console.error('Error fetching temporary wallet data:', err);
      setError('Failed to load temporary wallet history');
    } finally {
      setLoading(false);
    }
  };

  const applyFilter = () => {
    if (selectedFilter === 'ALL') {
      setFilteredTransactions(transactions);
    } else if (selectedFilter === 'ACTIVE') {
      setFilteredTransactions(transactions.filter(t => !t.isExpired && !t.isUsed));
    } else if (selectedFilter === 'USED') {
      setFilteredTransactions(transactions.filter(t => t.isUsed));
    } else if (selectedFilter === 'EXPIRED') {
      setFilteredTransactions(transactions.filter(t => t.isExpired));
    }
  };

  const getDaysRemaining = (expiresAt) => {
    const now = new Date();
    const expiry = new Date(expiresAt);
    const diffTime = expiry - now;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 0;
  };

  const getSourceLabel = (sourceType) => {
    const labels = {
      GAME_REWARD: '🎮 Game Reward',
      TICKET_CANCELLATION: '🎫 Booking Refund',
      TICKET_CHANGE_REFUND: '💱 Date Change Refund',
      TEMP_WALLET_PAYMENT_DEBIT: '💸 Temporary Wallet Payment Deduction',
      TEMP_WALLET_SPIN_DEBIT: '🎡 Extra Spin Purchase (Temporary Wallet)',
      TICKET_CHANGE: '🔄 Ticket Change',
      REFUND: '💰 Refund',
      BONUS: '🎁 Bonus'
    };
    return labels[sourceType] || sourceType || 'Unknown Source';
  };

  const getExpiryColor = (daysRemaining) => {
    if (daysRemaining > 7) return 'high';
    if (daysRemaining > 2) return 'medium';
    return 'low';
  };

  if (!user) {
    return (
      <div className="temporary-wallet-history-page">
        <div className="container">
          <div className="error-message">
            <h3>Please login to view temporary wallet history</h3>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="temporary-wallet-history-page">
        <div className="container">
          <div className="loading-container">
            <div className="spinner"></div>
            <p>Loading temporary wallet history...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="temporary-wallet-history-page">
        <div className="container">
          <div className="error-container">
            <h3>Error</h3>
            <p>{error}</p>
            <button onClick={fetchTemporaryWalletData} className="btn btn-primary">
              <FaSync /> Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="temporary-wallet-history-page">
      <div className="container">
        {/* Header */}
        <div className="page-header">
          <div className="header-content">
            <h1><FaGift /> Temporary Wallet History</h1>
            <p className="subtitle">Rewards & bonuses - 10-15 day expiry</p>
          </div>
        </div>

        {/* Balance Card */}
        <div className="balance-card temporary">
          <div className="balance-icon">🎁</div>
          <div className="balance-info">
            <p className="label">Active Balance</p>
            <p className="amount">₹{tempWalletBalance.toFixed(2)}</p>
            <p className="note">Expires in 10-15 days • Auto-deleted after expiry</p>
          </div>
        </div>

        {/* Filter Section */}
        <div className="filter-section">
          <div className="filter-buttons">
            <button 
              className={`filter-btn ${selectedFilter === 'ALL' ? 'active' : ''}`}
              onClick={() => setSelectedFilter('ALL')}
            >
              All ({transactions.length})
            </button>
            <button 
              className={`filter-btn ${selectedFilter === 'ACTIVE' ? 'active' : ''}`}
              onClick={() => setSelectedFilter('ACTIVE')}
            >
              Active
            </button>
            <button 
              className={`filter-btn ${selectedFilter === 'USED' ? 'active' : ''}`}
              onClick={() => setSelectedFilter('USED')}
            >
              Used
            </button>
            <button 
              className={`filter-btn ${selectedFilter === 'EXPIRED' ? 'active' : ''}`}
              onClick={() => setSelectedFilter('EXPIRED')}
            >
              Expired
            </button>
          </div>
        </div>

        {/* Transactions List */}
        <div className="transactions-section">
          {filteredTransactions.length === 0 ? (
            <div className="empty-state">
              <p>No temporary wallet transactions found</p>
            </div>
          ) : (
            <div className="transaction-list">
              {filteredTransactions.map((txn) => {
                const hasExpiry = Boolean(txn.expiresAt);
                const daysRemaining = hasExpiry ? getDaysRemaining(txn.expiresAt) : 0;
                const expiryColor = getExpiryColor(daysRemaining);
                const txnSource = txn.sourceType || txn.source;
                const amount = Number(txn.amount || 0);
                const isDebit = amount < 0;
                
                return (
                  <div className={`transaction-item status-${txn.isExpired ? 'expired' : txn.isUsed ? 'used' : 'active'}`} key={txn.id}>
                    <div className="txn-header">
                      <div className="txn-icon">
                        <FaGift />
                      </div>
                      <div className="txn-details">
                        <p className="txn-source">
                          {getSourceLabel(txnSource)}
                        </p>
                        <p className="txn-type">
                          {isDebit ? 'Temporary wallet debit' : 'Temporary wallet credit'}
                        </p>
                      </div>

                      <div className="txn-amount">
                        <p className="amount">{isDebit ? '-' : '+'}₹{Math.abs(amount).toFixed(2)}</p>
                        <p className="currency">{isDebit ? 'Debited amount' : 'Credit amount'}</p>
                      </div>

                      <span className={`status-badge ${txn.isExpired ? 'expired' : txn.isUsed ? 'used' : 'active'}`}>
                        {txn.isExpired ? <FaTimesCircle /> : <FaCheckCircle />}
                        {txn.isExpired ? 'Expired' : txn.isUsed ? 'Used' : 'Active'}
                      </span>
                    </div>

                    <div className="txn-body">
                      <div className="detail-item">
                        <p className="detail-label">Created At</p>
                        <p className="detail-value">{new Date(txn.createdAt).toLocaleString('en-IN')}</p>
                      </div>

                      {txn.paymentTransactionId && (
                        <div className="detail-item">
                          <p className="detail-label">Payment Ref</p>
                          <p className="detail-value">{txn.paymentTransactionId}</p>
                        </div>
                      )}

                      {hasExpiry && (
                        <div className="detail-item">
                          <p className="detail-label">Expires At</p>
                          <p className="detail-value">{new Date(txn.expiresAt).toLocaleString('en-IN')}</p>
                        </div>
                      )}

                      {hasExpiry && !txn.isExpired && !txn.isUsed && (
                        <div className="detail-item">
                          <p className="detail-label">Time Remaining</p>
                          <p className={`expiry-countdown ${expiryColor}`}>
                            <FaClock />
                            {daysRemaining} days left
                          </p>
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Info Section */}
        <div className="info-section">
          <div className="info-box">
            <h3>How Temporary Wallet Works</h3>
            <ul>
              <li>Game rewards valid for <strong>10 days</strong></li>
              <li>Ticket change/refund valid for <strong>15 days</strong></li>
              <li>Automatic expiry after deadline - no manual action needed</li>
              <li>Can be used immediately in wallet payment</li>
              <li>Pay from fastest-expiring rewards first (greedy deduction)</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TemporaryWalletHistory;
