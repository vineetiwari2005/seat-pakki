import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { gameRewardsService } from '../../services';
import { Link } from 'react-router-dom';
import './GameRewards.scss';

const TOTAL_TIME = 120;
const ICONS = ['🎬', '🍿', '🎟️', '⭐', '🎭', '🎶', '🎥', '🕹️'];

const createDeck = () => {
  const cards = [...ICONS, ...ICONS]
    .map((icon, idx) => ({ id: idx, icon, flipped: false, matched: false }))
    .sort(() => Math.random() - 0.5)
    .map((card, idx) => ({ ...card, id: idx }));
  return cards;
};

const GameRewards = () => {
  const { user } = useAuth();

  const [status, setStatus] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const [cards, setCards] = useState(createDeck);
  const [selectedCards, setSelectedCards] = useState([]);
  const [moves, setMoves] = useState(0);
  const [timeLeft, setTimeLeft] = useState(TOTAL_TIME);
  const [gameStarted, setGameStarted] = useState(false);
  const [gameEnded, setGameEnded] = useState(false);
  const [result, setResult] = useState(null);

  const matchedPairs = useMemo(() => cards.filter((card) => card.matched).length / 2, [cards]);

  const score = useMemo(() => {
    const timeBonus = Math.max(0, timeLeft);
    const pairScore = matchedPairs * 10;
    const movePenalty = Math.max(0, moves - matchedPairs * 2);
    return Math.max(0, pairScore + timeBonus - movePenalty * 2);
  }, [matchedPairs, timeLeft, moves]);

  const fetchData = async () => {
    if (!user?.id) return;
    try {
      setLoading(true);
      setErrorMessage('');
      const [statusRes, historyRes] = await Promise.all([
        gameRewardsService.getStatus(user.id),
        gameRewardsService.getHistory(user.id)
      ]);
      setStatus(statusRes.data || statusRes);
      setHistory(historyRes.data || historyRes || []);
    } catch (error) {
      console.error('Failed to load game rewards data:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [user?.id]);

  useEffect(() => {
    if (!gameStarted || gameEnded) return;
    const interval = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          setGameEnded(true);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [gameStarted, gameEnded]);

  useEffect(() => {
    if (matchedPairs === ICONS.length && gameStarted) {
      setGameEnded(true);
    }
  }, [matchedPairs, gameStarted]);

  useEffect(() => {
    if (selectedCards.length !== 2) return;

    const [first, second] = selectedCards;
    setMoves((prev) => prev + 1);

    if (cards[first].icon === cards[second].icon) {
      setCards((prev) =>
        prev.map((card, idx) =>
          idx === first || idx === second ? { ...card, matched: true } : card
        )
      );
      setSelectedCards([]);
      return;
    }

    const timer = setTimeout(() => {
      setCards((prev) =>
        prev.map((card, idx) =>
          idx === first || idx === second ? { ...card, flipped: false } : card
        )
      );
      setSelectedCards([]);
    }, 700);

    return () => clearTimeout(timer);
  }, [selectedCards, cards]);

  const handleCardClick = (index) => {
    if (gameEnded || selectedCards.length === 2) return;
    const target = cards[index];
    if (target.flipped || target.matched) return;

    if (!gameStarted) {
      setGameStarted(true);
    }

    setCards((prev) => prev.map((card, idx) => (idx === index ? { ...card, flipped: true } : card)));
    setSelectedCards((prev) => [...prev, index]);
  };

  const resetGame = () => {
    setErrorMessage('');
    setCards(createDeck());
    setSelectedCards([]);
    setMoves(0);
    setTimeLeft(TOTAL_TIME);
    setGameStarted(false);
    setGameEnded(false);
    setResult(null);
  };

  const submitResult = async () => {
    if (!user?.id || !gameEnded || submitting) return;

    try {
      setSubmitting(true);
      setErrorMessage('');
      const payload = {
        score,
        moves,
        timeTakenSeconds: TOTAL_TIME - timeLeft
      };
      const response = await gameRewardsService.submitPlay(user.id, payload);
      setResult(response.data || response);
      await fetchData();
    } catch (error) {
      setErrorMessage(error?.response?.data?.message || error?.response?.data?.error || 'Could not submit score.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!user) {
    return (
      <div className="game-rewards-page">
        <div className="game-card">
          <h2>Please login to play</h2>
          <Link to="/login" className="btn-link">Go to Login</Link>
        </div>
      </div>
    );
  }

  const canPlay = status?.canPlayToday;

  return (
    <div className="game-rewards-page">
      <div className="game-header-card">
        <h1>Games & Rewards</h1>
        <p>Play the memory challenge once per day and win cashback in temporary wallet (valid for 10 days).</p>
      </div>

      <div className="stats-grid">
        <div className="stat-box"><span>Today Status</span><strong>{loading ? '...' : canPlay ? 'Can Play' : 'Already Played'}</strong></div>
        <div className="stat-box"><span>Month High Score</span><strong>{loading ? '...' : status?.monthlyHighestScore ?? 0}</strong></div>
        <div className="stat-box"><span>Month Avg Score</span><strong>{loading ? '...' : Number(status?.monthlyAverageScore || 0).toFixed(2)}</strong></div>
        <div className="stat-box"><span>Temp Wallet</span><strong>₹{loading ? '...' : Number(status?.temporaryWalletAvailable || 0).toFixed(2)}</strong></div>
      </div>

      {!canPlay && !loading && (
        <div className="info-banner">
          Today’s game is already played. Come back tomorrow for another chance.
        </div>
      )}

      {errorMessage && (
        <div className="error-banner">{errorMessage}</div>
      )}

      <div className="game-card">
        <div className="game-top">
          <div>⏱️ Time Left: <strong>{timeLeft}s</strong></div>
          <div>🎯 Score: <strong>{score}</strong></div>
          <div>🔁 Moves: <strong>{moves}</strong></div>
        </div>

        <div className="board">
          {cards.map((card, index) => (
            <button
              key={card.id}
              className={`tile ${card.flipped || card.matched ? 'open' : ''} ${card.matched ? 'matched' : ''}`}
              onClick={() => handleCardClick(index)}
              disabled={!canPlay || loading || gameEnded}
            >
              {card.flipped || card.matched ? card.icon : '❓'}
            </button>
          ))}
        </div>

        <div className="actions-row">
          <button className="btn-light" onClick={resetGame}>Reset Board</button>
          <button
            className="btn-primary"
            disabled={!canPlay || loading || !gameEnded || submitting || !!result}
            onClick={submitResult}
          >
            {submitting ? 'Submitting...' : 'Claim Reward'}
          </button>
        </div>

        {result && (
          <div className="result-box">
            <h3>Result Submitted</h3>
            <p>Reward: <strong>₹{Number(result.rewardAmount || 0).toFixed(2)}</strong></p>
            <p>{result.rewardMessage}</p>
            {result.rewardExpiresAt && (
              <p>Valid till: {new Date(result.rewardExpiresAt).toLocaleString('en-IN')}</p>
            )}
          </div>
        )}
      </div>

      <div className="history-card">
        <h2>Recent Plays</h2>
        {history.length === 0 ? (
          <p>No plays yet.</p>
        ) : (
          <div className="history-list">
            {history.map((item, idx) => (
              <div className="history-item" key={`${item.playedAt}-${idx}`}>
                <span>{item.playedDate}</span>
                <strong>Score: {item.score}</strong>
                <span>{item.playedAt ? new Date(item.playedAt).toLocaleString('en-IN') : '-'}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default GameRewards;
