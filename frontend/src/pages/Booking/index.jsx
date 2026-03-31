// Placeholder booking pages - implement seat selection, payment, confirmation
import React from 'react';

export const ShowSelection = () => (
  <div className="container" style={{padding: '40px', minHeight: '400px'}}>
    <h2>Select Show</h2>
    <p>Show times selection page - implement theater and showtime selection grid</p>
  </div>
);

export const SeatSelection = () => (
  <div className="container" style={{padding: '40px', minHeight: '400px'}}>
    <h2>Select Seats</h2>
    <p>Seat selection page - implement seat map with A-E rows, 10 seats each</p>
  </div>
);

export const Payment = () => (
  <div className="container" style={{padding: '40px', minHeight: '400px'}}>
    <h2>Payment</h2>
    <p>Payment page - implement payment form with mock gateway integration</p>
  </div>
);

export const BookingConfirmation = () => (
  <div className="container" style={{padding: '40px', minHeight: '400px'}}>
    <h2>Booking Confirmed!</h2>
    <p>Confirmation page - show ticket details, QR code, booking ID</p>
  </div>
);
