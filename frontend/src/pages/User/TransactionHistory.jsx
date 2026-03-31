import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { paymentService, walletService } from '../../services';
import { 
  FaWallet, 
  FaArrowUp, 
  FaArrowDown, 
  FaCheckCircle, 
  FaTimesCircle,
  FaFilter,
  FaSync,
  FaTicketAlt,
  FaMoneyBillWave
} from 'react-icons/fa';
import './TransactionHistory.scss';

const TransactionHistory = () => {
  const { user } = useAuth();
  const [transactions, setTransactions] = useState([]);
  const [filteredTransactions, setFilteredTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedFilter, setSelectedFilter] = useState('ALL');

  useEffect(() => {
    if (user?.id) {
      fetchTransactions();
    }
  }, [user]);

  useEffect(() => {
    applyFilter();
  }, [selectedFilter, transactions]);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Fetch BOTH payment transactions AND wallet transactions
      const [paymentResponse, walletResponse] = await Promise.all([
        paymentService.getPaymentHistory(user.id),
        walletService.getTransactions(user.id)
      ]);
      
      // Process payment transactions
      const paymentList = Array.isArray(paymentResponse) ? paymentResponse : [];
      const paymentTransactions = paymentList.map(payment => ({
        ...payment,
        type: 'PAYMENT',
        displayType: 'Payment',
        transactionDate: payment.createdAt
      }));
      
      // Process wallet transactions
      const walletData = walletResponse.data || walletResponse;
      const walletList = Array.isArray(walletData) ? walletData : [];
      
      // Filter out:
      // 1. Wallet DEBIT transactions (already shown in payment records)
      // 2. Initial wallet balance setup (not a real transaction)
      const walletTransactions = walletList
        .filter(wallet => {
          // Exclude DEBIT transactions (shown as payments)
          if (wallet.transactionType === 'DEBIT') return false;
          // Exclude initial wallet setup
          if (wallet.description?.toLowerCase().includes('initial wallet')) return false;
          return true;
        })
        .map(wallet => ({
          ...wallet,
          type: 'WALLET',
          displayType: wallet.transactionType,
          transactionDate: wallet.createdAt,
          amount: wallet.amount,
          status: wallet.status || 'SUCCESS'
        }));
      
      // Merge both lists
      const allTransactions = [...paymentTransactions, ...walletTransactions];
      
      // Sort by latest time (createdAt descending)
      const sortedTransactions = allTransactions.sort((a, b) => 
        new Date(b.transactionDate) - new Date(a.transactionDate)
      );
      
      setTransactions(sortedTransactions);
      setFilteredTransactions(sortedTransactions);
      
      console.log('💳 Fetched transactions - Payments:', paymentTransactions.length, 'Wallet:', walletTransactions.length);
    } catch (err) {
      console.error('Error fetching transaction history:', err);
      setError(err.message || 'Failed to fetch transaction history');
    } finally {
      setLoading(false);
    }
  };

  const applyFilter = () => {
    if (selectedFilter === 'ALL') {
      setFilteredTransactions(transactions);
    } else if (selectedFilter === 'PAYMENT' || selectedFilter === 'WALLET') {
      // Filter by transaction type
      const filtered = transactions.filter(t => t.type === selectedFilter);
      setFilteredTransactions(filtered);
    } else {
      // Filter by status
      const filtered = transactions.filter(t => t.status === selectedFilter);
      setFilteredTransactions(filtered);
    }
  };

  const handleFilterChange = (filter) => {
    setSelectedFilter(filter);
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', { 
      day: '2-digit', 
      month: 'short', 
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatAmount = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2
    }).format(amount);
  };

  const getTransactionIcon = (transaction) => {
    // Handle wallet transactions
    if (transaction.type === 'WALLET') {
      switch (transaction.transactionType) {
        case 'CREDIT':
        case 'REFUND':
          return <FaArrowDown className="icon-credit" />;
        case 'DEBIT':
          return <FaArrowUp className="icon-debit" />;
        default:
          return <FaWallet className="icon-wallet" />;
      }
    }
    
    // Handle payment transactions
    const paymentMethod = transaction.paymentMethod;
    switch (paymentMethod) {
      case 'WALLET':
        return <FaWallet className="icon-wallet" />;
      case 'WALLET_CARD_SPLIT':
        return (
          <span className="icon-split">
            <FaWallet className="icon-wallet" />
            <FaMoneyBillWave className="icon-card" />
          </span>
        );
      case 'STRIPE':
      case 'CREDIT_CARD':
      case 'DEBIT_CARD':
        return <FaMoneyBillWave className="icon-card" />;
      case 'UPI':
        return <FaArrowUp className="icon-upi" />;
      default:
        return <FaTicketAlt className="icon-default" />;
    }
  };

  const getTransactionColor = (status) => {
    switch (status) {
      case 'SUCCESS':
        return 'transaction-success';
      case 'FAILED':
        return 'transaction-failed';
      case 'PENDING':
        return 'transaction-pending';
      default:
        return '';
    }
  };

  const getStatusIcon = (status) => {
    return status === 'SUCCESS' ? 
      <FaCheckCircle className="status-success" /> : 
      <FaTimesCircle className="status-failed" />;
  };

  if (loading) {
    return (
      <div className="transaction-history-page">
        <div className="container">
          <div className="loading-container">
            <div className="spinner"></div>
            <p>Loading transaction history...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="transaction-history-page">
        <div className="container">
          <div className="error-message">
            <h3>Error Loading Payment History</h3>
            <p>{error}</p>
            <button onClick={fetchTransactions} className="btn btn-primary">
              <FaSync /> Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="transaction-history-page">
      <div className="container">
        <div className="page-header">
          <div>
            <h1><FaMoneyBillWave /> Payment History</h1>
            <p className="subtitle">View all your payment transactions</p>
          </div>
        </div>

        <div className="filter-section">
          <FaFilter className="filter-icon" />
          <div className="filter-buttons">
            <button 
              className={`filter-btn ${selectedFilter === 'ALL' ? 'active' : ''}`}
              onClick={() => handleFilterChange('ALL')}
            >
              All
            </button>
            <button 
              className={`filter-btn ${selectedFilter === 'PAYMENT' ? 'active' : ''}`}
              onClick={() => handleFilterChange('PAYMENT')}
            >
              Payments
            </button>
            <button 
              className={`filter-btn ${selectedFilter === 'WALLET' ? 'active' : ''}`}
              onClick={() => handleFilterChange('WALLET')}
            >
              Wallet
            </button>
            <button 
              className={`filter-btn ${selectedFilter === 'SUCCESS' ? 'active' : ''}`}
              onClick={() => handleFilterChange('SUCCESS')}
            >
              Success
            </button>
            <button 
              className={`filter-btn ${selectedFilter === 'FAILED' ? 'active' : ''}`}
              onClick={() => handleFilterChange('FAILED')}
            >
              Failed
            </button>
          </div>
        </div>

        {filteredTransactions.length === 0 ? (
          <div className="no-transactions">
            <FaTicketAlt className="empty-icon" />
            <h3>No Payment Transactions Found</h3>
            <p>
              {selectedFilter === 'ALL' 
                ? "You haven't made any payments yet." 
                : `No ${selectedFilter.toLowerCase()} payments found.`}
            </p>
          </div>
        ) : (
          <div className="transactions-list">
            {filteredTransactions.map((transaction) => (
              <div 
                key={transaction.id} 
                className={`transaction-card ${getTransactionColor(transaction.status)}`}
              >
                <div className="transaction-icon">
                  {getTransactionIcon(transaction)}
                </div>
                
                <div className="transaction-details">
                  <div className="transaction-header">
                    <h3 className="transaction-description">
                      {transaction.type === 'WALLET' 
                        ? transaction.description || transaction.displayType
                        : transaction.movieName 
                          ? `${transaction.movieName} - ${transaction.theaterName}`
                          : 'Booking Payment'}
                    </h3>
                    <span className={`transaction-amount ${
                      transaction.type === 'WALLET' && transaction.transactionType === 'DEBIT' ? 'debit' : ''
                    }`}>
                      {transaction.type === 'WALLET' && transaction.transactionType === 'DEBIT' ? '- ' : ''}
                      {formatAmount(transaction.amount)}
                    </span>
                  </div>
                  
                  <div className="transaction-meta">
                    <div className="meta-row">
                      <span className="meta-label">Type:</span>
                      <span className="transaction-type-badge">
                        {transaction.type === 'PAYMENT' ? '💳 Payment' : '💵 Wallet'}
                      </span>
                    </div>

                    {(transaction.bookedSeats || transaction.seats) && (
                      <div className="meta-row">
                        <span className="meta-label">Seats:</span>
                        <span className="meta-value">{transaction.bookedSeats || transaction.seats}</span>
                      </div>
                    )}

                    {transaction.showDate && transaction.showTime && (
                      <div className="meta-row">
                        <span className="meta-label">Show:</span>
                        <span className="meta-value">{transaction.showDate} at {transaction.showTime}</span>
                      </div>
                    )}
                    
                    {transaction.type === 'PAYMENT' && transaction.paymentMethod && (
                      <div className="meta-row">
                        <span className="meta-label">Payment Method:</span>
                        <span className="transaction-type-badge">
                          {transaction.paymentMethod?.includes('+WALLET') || transaction.paymentMethod === 'WALLET_CARD_SPLIT'
                            ? '💳 + 💵 Split Payment' 
                            : transaction.paymentMethod}
                        </span>
                      </div>
                    )}

                    {/* Show split payment breakdown */}
                    {transaction.type === 'PAYMENT' && (transaction.paymentMethod === 'WALLET_CARD_SPLIT' || transaction.paymentMethod?.includes('+WALLET')) && (
                      <>
                        {transaction.walletAmount > 0 && (
                          <div className="meta-row">
                            <span className="meta-label">💵 Wallet Amount:</span>
                            <span className="meta-value wallet-amount">
                              {formatAmount(transaction.walletAmount)}
                            </span>
                          </div>
                        )}
                        {transaction.cardAmount > 0 && (
                          <div className="meta-row">
                            <span className="meta-label">💳 Card Amount:</span>
                            <span className="meta-value card-amount">
                              {formatAmount(transaction.cardAmount)}
                            </span>
                          </div>
                        )}
                      </>
                    )}

                    {transaction.type === 'WALLET' && transaction.transactionType && (
                      <div className="meta-row">
                        <span className="meta-label">Transaction Type:</span>
                        <span className={`transaction-type-badge ${transaction.transactionType.toLowerCase()}`}>
                          {transaction.transactionType}
                        </span>
                      </div>
                    )}
                    
                    <div className="meta-row">
                      <span className="meta-label">Status:</span>
                      <span className="transaction-status">
                        {getStatusIcon(transaction.status)}
                        {transaction.status}
                      </span>
                    </div>
                    
                    <div className="meta-row">
                      <span className="meta-label">Date:</span>
                      <span className="meta-value">{formatDate(transaction.createdAt || transaction.transactionDate)}</span>
                    </div>

                    {transaction.type === 'PAYMENT' && transaction.baseAmount && (
                      <div className="meta-row">
                        <span className="meta-label">Base Amount:</span>
                        <span className="meta-value">{formatAmount(transaction.baseAmount)}</span>
                      </div>
                    )}

                    {transaction.type === 'PAYMENT' && transaction.convenienceFee > 0 && (
                      <div className="meta-row">
                        <span className="meta-label">Convenience Fee:</span>
                        <span className="meta-value">{formatAmount(transaction.convenienceFee)}</span>
                      </div>
                    )}

                    {transaction.type === 'PAYMENT' && transaction.tax > 0 && (
                      <div className="meta-row">
                        <span className="meta-label">Tax:</span>
                        <span className="meta-value">{formatAmount(transaction.tax)}</span>
                      </div>
                    )}

                    {transaction.type === 'WALLET' && transaction.balanceAfter !== undefined && (
                      <div className="meta-row">
                        <span className="meta-label">Balance After:</span>
                        <span className="meta-value balance-after">
                          {formatAmount(transaction.balanceAfter)}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default TransactionHistory;
