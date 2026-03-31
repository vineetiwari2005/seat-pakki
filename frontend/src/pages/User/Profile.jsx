import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { FaUser, FaEnvelope, FaPhone, FaWallet, FaUserShield, FaTicketAlt, FaEdit, FaGamepad, FaHistory, FaClock } from 'react-icons/fa';
import { Link } from 'react-router-dom';
import EditProfileModal from '../../components/User/EditProfileModal';
import './Profile.scss';

const Profile = () => {
  const { user, updateUser } = useAuth();
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [walletBalance, setWalletBalance] = useState(0);
  const [loadingBalance, setLoadingBalance] = useState(true);
  const [temporaryCredit, setTemporaryCredit] = useState({ availableAmount: 0, expiresAt: null });

  // Fetch wallet balance on component mount
  useEffect(() => {
    if (user?.id) {
      fetchWalletBalance();
    }
  }, [user]);

  const fetchWalletBalance = async () => {
    try {
      setLoadingBalance(true);
      
      // Fetch CONSOLIDATED wallet balance (main + temporary merged)
      const response = await fetch(`/api/wallet/balance/${user.id}`);
      if (response.ok) {
        const result = await response.json();
        const walletData = result.data || {};
        
        // Display consolidated balance (main + temp merged)
        const consolidatedBalance = walletData.consolidatedBalance || 0;
        const mainBalance = walletData.balance || 0;
        const tempBalance = walletData.temporaryBalance || 0;
        
        setWalletBalance(consolidatedBalance);
        
        // Show breakdown of components
        setTemporaryCredit({
          availableAmount: Number(tempBalance),
          mainAmount: Number(mainBalance),
          expiresAt: null
        });
        
        console.log('✅ Consolidated wallet loaded:', {
          main: mainBalance,
          temporary: tempBalance,
          total: consolidatedBalance
        });
      }
    } catch (err) {
      console.error('Error fetching wallet balance:', err);
      setWalletBalance(0);
      setTemporaryCredit({ availableAmount: 0, mainAmount: 0, expiresAt: null });
    } finally {
      setLoadingBalance(false);
    }
  };

  const handleUpdateUser = (updatedUser) => {
    updateUser(updatedUser);
  };

  if (!user) {
    return (
      <div className="profile-page">
        <div className="container">
          <div className="error-message">
            <h3>Please login to view your profile</h3>
            <Link to="/login" className="btn btn-primary">Login</Link>
          </div>
        </div>
      </div>
    );
  }

  const getRoleColor = (role) => {
    switch(role?.toUpperCase()) {
      case 'ADMIN': return '#C62828';
      case 'USER': return '#121212';
      default: return '#FBC02D';
    }
  };

  return (
    <div className="profile-page">
      <div className="profile-banner">
        <div className="banner-overlay"></div>
        <div className="container">
          <div className="profile-header">
            <div className="profile-avatar">
              <div className="avatar-circle">
                <FaUser />
              </div>
              <div className="avatar-badge" style={{ background: getRoleColor(user.role) }}>
                <FaUserShield />
              </div>
            </div>
            <div className="profile-title">
              <h1>{user.name}</h1>
              <p className="user-email">{user.email}</p>
              <span className="role-badge" style={{ background: getRoleColor(user.role) }}>
                {user.role || 'USER'}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="container">
        <div className="profile-content">
          <div className="info-section">
            <div className="section-header">
              <h2>Personal Information</h2>
              <button className="btn-edit" onClick={() => setIsEditModalOpen(true)}>
                <FaEdit /> Edit Profile
              </button>
            </div>

            <div className="info-grid">
              <div className="info-card">
                <div className="info-icon" style={{ background: '#12121220', color: '#121212' }}>
                  <FaUser />
                </div>
                <div className="info-details">
                  <label>Full Name</label>
                  <p>{user.name || 'Not provided'}</p>
                </div>
              </div>

              <div className="info-card">
                <div className="info-icon" style={{ background: '#C6282820', color: '#C62828' }}>
                  <FaEnvelope />
                </div>
                <div className="info-details">
                  <label>Email Address</label>
                  <p>{user.email}</p>
                </div>
              </div>

              <div className="info-card">
                <div className="info-icon" style={{ background: '#FBC02D20', color: '#FBC02D' }}>
                  <FaPhone />
                </div>
                <div className="info-details">
                  <label>Mobile Number</label>
                  <p>{user.mobileNumber || 'Not provided'}</p>
                </div>
              </div>

              <div className="info-card">
                <div className="info-icon" style={{ background: '#FBC02D20', color: '#FBC02D' }}>
                  <FaUserShield />
                </div>
                <div className="info-details">
                  <label>Account Role</label>
                  <p style={{ textTransform: 'uppercase', fontWeight: '600', color: getRoleColor(user.role) }}>
                    {user.role || 'USER'}
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="wallet-section">
            <div className="wallet-card">
              <div className="wallet-header">
                <div className="wallet-icon">
                  <FaWallet />
                </div>
                <div>
                  <h3>Wallet Balance</h3>
                  <p className="wallet-subtitle">Merged wallet balance (Main + Temporary)</p>
                </div>
              </div>
              {/* MERGED WALLET DISPLAY - SINGLE VALUE */}
              <div className="wallet-balance">
                <span className="currency">₹</span>
                <span className="amount">
                  {loadingBalance ? '...' : walletBalance.toFixed(2)}
                </span>
              </div>
              
              {/* COMPONENT BREAKDOWN - Shows what's included */}
              <div className="wallet-components">
                <div className="component-row">
                  <span className="component-label">
                    <span className="badge permanent">Permanent</span>
                    Main Wallet
                  </span>
                  <strong className="component-amount">₹{loadingBalance ? '...' : (temporaryCredit.mainAmount || 0).toFixed(2)}</strong>
                </div>
                <div className="component-row">
                  <span className="component-label">
                    <span className="badge temporary">Temporary</span>
                    Spin/Change Rewards (expires in 10-15 days)
                  </span>
                  <strong className="component-amount">₹{loadingBalance ? '...' : temporaryCredit.availableAmount.toFixed(2)}</strong>
                </div>
              </div>
              
            </div>
          </div>

          <div className="quick-actions">
            <h2>Quick Actions</h2>
            <div className="actions-grid">
              <Link to="/my-bookings" className="action-card">
                <div className="action-icon" style={{ background: '#C6282820', color: '#C62828' }}>
                  <FaTicketAlt />
                </div>
                <div className="action-details">
                  <h4>My Bookings</h4>
                  <p>View all your bookings</p>
                </div>
              </Link>

              <div className="action-card" onClick={() => setIsEditModalOpen(true)} style={{ cursor: 'pointer' }}>
                <div className="action-icon" style={{ background: '#12121220', color: '#121212' }}>
                  <FaEdit />
                </div>
                <div className="action-details">
                  <h4>Edit Profile</h4>
                  <p>Update your information</p>
                </div>
              </div>

              <div className="action-card">
                <div className="action-icon" style={{ background: '#FBC02D20', color: '#FBC02D' }}>
                  <FaWallet />
                </div>
                <div className="action-details">
                  <h4>Wallet</h4>
                  <p>Manage your wallet</p>
                </div>
              </div>

              <Link to="/wallet-history" className="action-card">
                <div className="action-icon" style={{ background: '#667eea20', color: '#667eea' }}>
                  <FaHistory />
                </div>
                <div className="action-details">
                  <h4>Main Wallet History</h4>
                  <p>View permanent funds</p>
                </div>
              </Link>

              <Link to="/temporary-wallet-history" className="action-card">
                <div className="action-icon" style={{ background: '#ff950020', color: '#ff9500' }}>
                  <FaClock />
                </div>
                <div className="action-details">
                  <h4>Temporary Wallet</h4>
                  <p>View expiring funds</p>
                </div>
              </Link>

              <Link to="/games-rewards" className="action-card">
                <div className="action-icon" style={{ background: '#C6282820', color: '#C62828' }}>
                  <FaGamepad />
                </div>
                <div className="action-details">
                  <h4>Games & Rewards</h4>
                  <p>Spin the wheel and win cashback</p>
                </div>
              </Link>
            </div>
          </div>
        </div>
      </div>

      <EditProfileModal
        user={user}
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        onUpdate={handleUpdateUser}
      />
    </div>
  );
};

export default Profile;
