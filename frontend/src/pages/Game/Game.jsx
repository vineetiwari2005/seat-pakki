import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import DailySpinner from '../../components/Game/DailySpinner';
import { FaClock } from 'react-icons/fa';
import './Game.scss';

const Game = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [spinStatus, setSpinStatus] = useState(null);
  const [timeUntilSpin, setTimeUntilSpin] = useState(null);
  const [loadingStatus, setLoadingStatus] = useState(true);

  // Fetch spin status on component mount
  useEffect(() => {
    if (user?.id) {
      fetchSpinStatus();
    }
  }, [user?.id]);

  // Update countdown timer every second
  useEffect(() => {
    if (timeUntilSpin && timeUntilSpin > 0) {
      const interval = setInterval(() => {
        setTimeUntilSpin(prev => (prev > 0 ? prev - 1 : 0));
      }, 1000);
      return () => clearInterval(interval);
    }
  }, [timeUntilSpin]);

  const fetchSpinStatus = async () => {
    try {
      setLoadingStatus(true);
      const response = await fetch(`/api/game/spin-status/${user.id}`);
      
      if (response.ok) {
        const result = await response.json();
        
        // The API returns { hasSpunToday, extraSpinsBalance, timeUntilNextSpin }
        // We need to check if user can spin free (hasSpunToday === false)
        const canSpinFree = !result.hasSpunToday;
        
        setSpinStatus({
          canSpinFree: canSpinFree,
          extraSpinsBalance: result.extraSpinsBalance || 0,
          timeUntilNextSpin: result.timeUntilNextSpin || "00:00:00"
        });

        // Fetch seconds remaining if user has already spun today
        if (!canSpinFree) {
          fetchTimeUntilSpin();
        }
      } else {
        console.error('Error fetching spin status');
      }
    } catch (error) {
      console.error('Failed to fetch spin status:', error);
    } finally {
      setLoadingStatus(false);
    }
  };

  const fetchTimeUntilSpin = async () => {
    try {
      const response = await fetch(`/api/game/time-until-spin/${user.id}`);
      
      if (response.ok) {
        const result = await response.json();
        setTimeUntilSpin(result.secondsRemaining || 0);
      } else {
        console.error('Error fetching time until spin');
      }
    } catch (error) {
      console.error('Failed to fetch time until spin:', error);
    }
  };

  const formatTimeRemaining = (seconds) => {
    if (!seconds || seconds <= 0) return "Spin available now!";
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hours}h ${minutes}m ${secs}s`;
  };

  const handleSpinEnd = async (result) => {
    try {
      // Call backend API to log the spin result
      const response = await fetch('/api/game/spin-reward', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          userId: user?.id,
          rewardAmount: result?.amount || 0,
          timestamp: new Date().toISOString(),
        }),
      });

      if (response.ok) {
        console.log('✅ Spin result logged successfully');
      } else {
        console.error('Error logging spin result');
      }
    } catch (error) {
      console.error('Failed to log spin result:', error);
    }
  };

  return (
    <div className="game-page">
      {/* Hero Banner */}
      <div className="game-hero-banner">
        <div className="hero-content">
          <h1 className="hero-title">🎡 Try your luck !!! Spin the wheel</h1>
          <p className="hero-subtitle">Win amazing rewards every day. One free spin every 24 hours!</p>
        </div>
      </div>

      {/* Spin Status Card */}
      {!loadingStatus && spinStatus && (
        <div className={`spin-status-card ${spinStatus.canSpinFree ? 'available' : 'unavailable'}`}>
          <div className="status-icon">
            <FaClock />
          </div>
          <div className="status-content">
            {spinStatus.canSpinFree ? (
              <>
                <h3>✅ Free Spin Available!</h3>
                <p>You can spin the wheel for free right now!</p>
              </>
            ) : (
              <>
                <h3>⏱️ Next Free Spin In</h3>
                <p className="countdown">{formatTimeRemaining(timeUntilSpin)}</p>
              </>
            )}
          </div>
          {spinStatus.extraSpinsBalance > 0 && (
            <div className="extra-spins">
              <p>Extra Spins Available: <strong>{spinStatus.extraSpinsBalance}</strong></p>
            </div>
          )}
        </div>
      )}

      {/* Daily Spinner Component */}
      <DailySpinner onSpinEnd={handleSpinEnd} />
    </div>
  );
};

export default Game;
