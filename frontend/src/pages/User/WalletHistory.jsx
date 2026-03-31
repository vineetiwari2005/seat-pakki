import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { FaWallet, FaArrowUp, FaArrowDown, FaCheckCircle, FaTimesCircle, FaSync } from 'react-icons/fa';
import './WalletHistory.scss';

const WalletHistory = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [mainWalletBalance, setMainWalletBalance] = useState(0);
  const [transactions, setTransactions] = useState([]);
  const [filteredTransactions, setFilteredTransactions] = useState([]);
  const [selectedFilter, setSelectedFilter] = useState('ALL');
  const [error, setError] = useState(null);

  useEffect(() => {
    if (user?.id) {
      fetchWalletData();
    } else {
      navigate('/login');
    }
  }, [user, navigate]);

  useEffect(() => {
    applyFilter();
  }, [selectedFilter, transactions]);

  const fetchWalletData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch balance
      const balanceResponse = await fetch(`/api/wallet/balance/${user.id}`);
      if (balanceResponse.ok) {
        const balanceData = await balanceResponse.json();
        setMainWalletBalance(balanceData.data?.balance || 0);
      }

      // Fetch main wallet transactions
      const txnResponse = await fetch(`/api/wallet/transactions/${user.id}`);
      if (txnResponse.ok) {
        const txnData = await txnResponse.json();
        const txnList = Array.isArray(txnData) ? txnData : txnData.data || [];
        setTransactions(txnList);
      }
    } catch (err) {
      console.error('Error fetching wallet data:', err);
      setError('Failed to load wallet history');
    } finally {
      setLoading(false);
    }
  };

  const applyFilter = () => {
    if (selectedFilter === 'ALL') {
      setFilteredTransactions(transactions);
    } else if (selectedFilter === 'CREDIT') {
      setFilteredTransactions(transactions.filter(t => t.transactionType === 'CREDIT' || t.amount > 0));
    } else if (selectedFilter === 'DEBIT') {
      setFilteredTransactions(transactions.filter(t => t.transactionType === 'DEBIT' || t.amount < 0));
    }
  };

  const getTransactionIcon = (txn) => {
    if (txn.transactionType === 'CREDIT' || txn.transactionType === 'REFUND' || txn.amount > 0) {
      return <FaArrowUp className="icon-credit" />;
    }
    return <FaArrowDown className="icon-debit" />;
  };

  const getTransactionDescription = (txn) => {
    if (txn.description) return txn.description;
    if (txn.transactionType === 'CREDIT') return 'Wallet Credit';
    if (txn.transactionType === 'DEBIT') return 'Wallet Debit';
    return 'Transaction';
  };

  const getStatusIcon = (status) => {
    return status === 'SUCCESS' ? 
      <FaCheckCircle className="status-success" /> : 
      <FaTimesCircle className="status-failed" />;
  };

  if (!user) {
    return (
      <div className="wallet-history-page">
        <div className="container">
          <div className="error-message">
            <h3>Please login to view wallet history</h3>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="wallet-history-page">
        <div className="container">
          <div className="loading-container">
            <div className="spinner"></div>
            <p>Loading wallet history...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="wallet-history-page">
        <div className="container">
          <div className="error-container">
            <h3>Error</h3>
            <p>{error}</p>
            <button onClick={fetchWalletData} className="btn btn-primary">
              <FaSync /> Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="wallet-history-page">
      <div className="container">
        {/* Header */}
        <div className="page-header">
          <div className="header-content">
            <h1><FaWallet /> Main Wallet History</h1>
            <p className="subtitle">Permanent balance - No expiry</p>
          </div>
        </div>

        {/* Balance Card */}
        <div className="balance-card">
          <div className="balance-icon">💰</div>
          <div className="balance-info">
            <p className="label">Current Balance</p>
            <p className="amount">₹{mainWalletBalance.toFixed(2)}</p>
            <p className="note">Permanent • No expiry date</p>
          </div>
        </div>

        {/* Filter Section */}
        <div className="filter-section">
          <div className="filter-buttons">
            <button 
              className={`filter-btn ${selectedFilter === 'ALL' ? 'active' : ''}`}
              onClick={() => setSelectedFilter('ALL')}
            >
              All Transactions ({transactions.length})
            </button>
            <button 
              className={`filter-btn ${selectedFilter === 'CREDIT' ? 'active' : ''}`}
              onClick={() => setSelectedFilter('CREDIT')}
            >
              Credits
            </button>
            <button 
              className={`filter-btn ${selectedFilter === 'DEBIT' ? 'active' : ''}`}
              onClick={() => setSelectedFilter('DEBIT')}
            >
              Debits
            </button>
          </div>
        </div>

        {/* Transactions List */}
        <div className="transactions-section">
          {filteredTransactions.length === 0 ? (
            <div className="empty-state">
              <p>No transactions found</p>
            </div>
          ) : (
            <div className="transaction-list">
              {filteredTransactions.map((txn) => (
                <div className="transaction-item" key={txn.id || txn.transactionId}>
                  {(() => {
                    const isCredit = txn.transactionType === 'CREDIT' || txn.transactionType === 'REFUND' || Number(txn.amount || 0) > 0;
                    const signedAmount = isCredit ? Math.abs(Number(txn.amount || 0)) : -Math.abs(Number(txn.amount || 0));

                    return (
                      <>
                  <div className="txn-icon">
                    {getTransactionIcon(txn)}
                  </div>
                  <div className="txn-details">
                    <p className="txn-description">
                      {getTransactionDescription(txn)}
                    </p>
                    <p className="txn-date">
                      {new Date(txn.createdAt || txn.transactionDate).toLocaleString('en-IN')}
                    </p>
                  </div>
                  <div className="txn-amount">
                    <p className={`amount ${signedAmount > 0 ? 'credit' : 'debit'}`}>
                      {signedAmount > 0 ? '+' : '-'}₹{Math.abs(signedAmount).toFixed(2)}
                    </p>
                  </div>
                  <div className="txn-status">
                    {getStatusIcon(txn.status || 'SUCCESS')}
                  </div>
                      </>
                    );
                  })()}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default WalletHistory;
