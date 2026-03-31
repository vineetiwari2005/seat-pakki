import React, { useEffect } from 'react';
import './SpinRewardPopup.scss';

const SpinRewardPopup = ({ amount, transactionId, onClose }) => {
  useEffect(() => {
    // Auto-close after 6 seconds
    const timer = setTimeout(onClose, 6000);
    return () => clearTimeout(timer);
  }, [onClose]);

  const getRewardMessage = () => {
    if (amount === 0) {
      return {
        title: '😔 Tough Luck!',
        message: 'No reward this time. Spin again tomorrow!',
        emoji: '💔',
        color: 'default'
      };
    } else if (amount === 400) {
      return {
        title: '🎉 JACKPOT!!!',
        message: `You won ₹${amount}! That's 1 in 10,000!`,
        emoji: '🎊',
        color: 'jackpot'
      };
    } else if (amount >= 20) {
      return {
        title: '🌟 Amazing!',
        message: `You won ₹${amount}!`,
        emoji: '⭐',
        color: 'special'
      };
    } else {
      return {
        title: '🎉 Congratulations!',
        message: `₹${amount} has been added to your Temporary Wallet!`,
        emoji: '🎁',
        color: 'normal'
      };
    }
  };

  const reward = getRewardMessage();

  return (
    <div className="reward-popup-overlay" onClick={onClose}>
      <div className={`reward-popup ${reward.color}`} onClick={e => e.stopPropagation()}>
        <div className="popup-emoji">{reward.emoji}</div>
        <h2 className="popup-title">{reward.title}</h2>
        <p className="popup-amount">₹{amount}</p>
        <p className="popup-message">{reward.message}</p>
        
        {amount > 0 && (
          <div className="popup-details">
            <p className="expiry-text">⏰ Expires in 10 days</p>
            {transactionId && (
              <p className="transaction-id">ID: {transactionId}</p>
            )}
          </div>
        )}

        <button className="popup-close-btn" onClick={onClose}>
          ✓ Got it!
        </button>
      </div>
    </div>
  );
};

export default SpinRewardPopup;
