import React from 'react';
import './SeatPakkiPopup.scss';

const SeatPakkiPopup = ({
  isOpen,
  title = 'SeatPakki',
  message,
  confirmText = 'OK',
  cancelText = 'Cancel',
  showCancel = false,
  onConfirm,
  onCancel,
  inputValue,
  onInputChange,
  inputPlaceholder = 'Enter value',
  loading = false
}) => {
  if (!isOpen) return null;

  const hasInput = typeof onInputChange === 'function';

  return (
    <div className="seatpakki-popup-overlay" role="dialog" aria-modal="true">
      <div className="seatpakki-popup-card">
        <div className="seatpakki-popup-brand">SeatPakki</div>
        <h3>{title}</h3>
        {message && <p>{message}</p>}

        {hasInput && (
          <input
            type="text"
            value={inputValue || ''}
            onChange={(e) => onInputChange(e.target.value)}
            placeholder={inputPlaceholder}
            className="seatpakki-popup-input"
            disabled={loading}
          />
        )}

        <div className="seatpakki-popup-actions">
          {showCancel && (
            <button type="button" className="btn-cancel" onClick={onCancel} disabled={loading}>
              {cancelText}
            </button>
          )}
          <button type="button" className="btn-confirm" onClick={onConfirm} disabled={loading}>
            {loading ? 'Please wait...' : confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};

export default SeatPakkiPopup;
