import React, { useEffect, useState } from 'react';
import './GameRewardPopup.scss';

const GameRewardPopup = ({ score, onClose, rewardAmount, walletMessage }) => {
  const [showAnimation, setShowAnimation] = useState(false);

  useEffect(() => {
    setShowAnimation(true);
  }, []);

  const getRewardTier = (score) => {
    if (score >= 500) {
      return { tier: 'Gold', amount: rewardAmount || 500, icon: '🏆' };
    } else if (score >= 300) {
      return { tier: 'Silver', amount: rewardAmount || 300, icon: '🥈' };
    } else if (score >= 100) {
      return { tier: 'Bronze', amount: rewardAmount || 100, icon: '🥉' };
    } else {
      return { tier: 'Try Again', amount: 0, icon: '💪' };
    }
  };

  const reward = getRewardTier(score);

  return (
    <div className="reward-popup-overlay">
      <div className={`reward-popup-container ${showAnimation ? 'show' : ''}`}>
        <div className="reward-popup-content">
          <div className="reward-header">
            <h1>🎉 Congratulations! 🎉</h1>
            <p>You've completed the memory challenge!</p>
          </div>

          <div className="reward-stats">
            <div className="stat-row">
              <span className="label">Your Score:</span>
              <span className="value">{score}</span>
            </div>
            <div className="stat-row">
              <span className="label">Reward Tier:</span>
              <span className="value tier-badge">{reward.icon} {reward.tier}</span>
            </div>
          </div>

          {reward.amount > 0 && (
            <div className="reward-amount-section">
              <div className="amount-box">
                <p className="amount-label">Temporary Wallet Credit</p>
                <p className="amount-value">₹{reward.amount}</p>
              </div>
              <p className="wallet-info">
                💳 Added to your temporary wallet and valid till {walletMessage || 'end of the month'}
              </p>
            </div>
          )}

          {reward.amount === 0 && (
            <div className="no-reward-section">
              <p>Keep practicing! Score 100+ points next time to earn rewards.</p>
            </div>
          )}

          <button className="close-popup-btn" onClick={onClose}>
            ✓ Done
          </button>
        </div>
      </div>
    </div>
  );
};

export default GameRewardPopup;
