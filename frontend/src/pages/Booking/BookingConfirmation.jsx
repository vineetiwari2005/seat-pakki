import React, { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { QRCodeCanvas } from 'qrcode.react';
import html2pdf from 'html2pdf.js';
import { FaCheckCircle, FaDownload, FaHome, FaCalendar, FaClock, FaMapMarkerAlt, FaTicketAlt, FaRupeeSign, FaEnvelope, FaCar, FaUtensils } from 'react-icons/fa';
import { MdEventSeat } from 'react-icons/md';
import { authService } from '../../services';
import { buildApiUrl } from '../../config/apiBaseUrl';
import './BookingConfirmation.scss';

const BookingConfirmation = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { ticketId: transactionId } = useParams();
  const bookingData = location.state;
  const user = authService.getCurrentUser();

  const [bookingId] = useState(`BMS${Date.now()}${Math.floor(Math.random() * 1000)}`);
  const [currentTime] = useState(new Date().toLocaleString());
  const [paymentData, setPaymentData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [emailSending, setEmailSending] = useState(false);
  const pdfTicketRef = useRef(null);

  const formatShowDate = (date) => {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  useEffect(() => {
    // Scroll to top on mount
    window.scrollTo(0, 0);

    // If no booking data, redirect to home
    if (!bookingData) {
      setTimeout(() => navigate('/'), 3000);
    } else {
      // Fetch payment data from backend for consistency
      fetchPaymentData();
    }
  }, [bookingData, navigate]);

  const fetchPaymentData = async () => {
    try {
      const txnId = bookingData.transactionId || transactionId;
      if (!txnId) {
        setLoading(false);
        return;
      }

      const response = await fetch(buildApiUrl(`/api/payment/status/${txnId}`));
      if (response.ok) {
        const data = await response.json();
        setPaymentData(data);
      }
    } catch (error) {
      console.error('Failed to fetch payment data:', error);
    } finally {
      setLoading(false);
    }
  };

  const generateTicketPdfBlob = async () => {
    const ticketElement = pdfTicketRef.current;
    if (!ticketElement) {
      throw new Error('Ticket view not found for PDF generation');
    }

    const worker = html2pdf()
      .set({
        margin: [6, 6, 6, 6],
        filename: `ticket_${bookingId}.pdf`,
        image: { type: 'jpeg', quality: 0.9 },
        html2canvas: {
          scale: 1.8,
          useCORS: true,
          backgroundColor: '#ffffff',
          logging: false,
          letterRendering: true,
          windowWidth: 900
        },
        jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
        pagebreak: { mode: ['avoid-all', 'css', 'legacy'] }
      })
      .from(ticketElement)
      .toPdf();

    const pdfBlob = await worker.outputPdf('blob');
    return pdfBlob;
  };

  const handleDownload = async () => {
    try {
      const pdfBlob = await generateTicketPdfBlob();
      const link = document.createElement('a');
      const url = URL.createObjectURL(pdfBlob);
      link.href = url;
      link.download = `ticket_${bookingId}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to download PDF:', error);
      alert('Unable to download ticket PDF. Please try again.');
    }
  };

  const handleEmail = async () => {
    try {
      setEmailSending(true);
      const pdfBlob = await generateTicketPdfBlob();
      const recipientEmail = bookingData?.userEmail || user?.email || user?.emailId;

      if (!recipientEmail) {
        throw new Error('Recipient email not found');
      }

      const formData = new FormData();
      formData.append('email', recipientEmail);
      formData.append('transactionId', bookingData.transactionId || transactionId || '');
      formData.append('subject', `Your Ticket - ${bookingId}`);
      formData.append('bookingId', bookingId);
      formData.append('movieName', bookingData.movieName || 'N/A');
      formData.append('theaterName', bookingData.theaterName || 'N/A');
      formData.append('showDate', bookingData.showDate || 'N/A');
      formData.append('showTime', bookingData.showTime || 'N/A');
      formData.append('seats', (bookingData.selectedSeats || []).join(', '));
      formData.append('totalPaid', (totalAmount || 0).toFixed(2));
      formData.append('parkingInfo', bookingData.parking ? `${bookingData.parking.vehicleType || ''} ${bookingData.parking.vehicleNumber || ''} (${bookingData.parking.durationHours || bookingData.parking.duration || 0}h)` : '');
      formData.append('parkingAmount', bookingData.parking ? String((bookingData.parking.amount || bookingData.parking.parkingFee || 0).toFixed(2)) : '');
      formData.append('foodItems', bookingData.food && bookingData.food.items ? bookingData.food.items.map(i => `${i.quantity}x ${i.name}`).join(', ') : '');
      formData.append('foodAmount', bookingData.food ? String((bookingData.food.total || bookingData.food.subtotal || 0).toFixed(2)) : '');
      formData.append('pdfFile', pdfBlob, `ticket_${bookingId}.pdf`);

      const response = await fetch(buildApiUrl('/api/bookings/email-ticket'), {
        method: 'POST',
        body: formData
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || errorData.error || 'Failed to send ticket email');
      }

      alert('Confirmation email sent to ' + recipientEmail);
    } catch (error) {
      console.error('Failed to email PDF:', error);
      alert(error.message || 'Failed to send ticket email. Please try again.');
    } finally {
      setEmailSending(false);
    }
  };

  // Use payment data from backend if available, otherwise use booking data
  const totalAmount = paymentData?.totalAmount || bookingData.finalTotal || bookingData.totalAmount;
  const baseAmount = paymentData?.baseAmount || bookingData.baseAmount;
  const convenienceFee = paymentData?.convenienceFee || bookingData.convenienceFee || 0;
  const tax = paymentData?.tax || bookingData.tax || 0;
  const paymentMethod = paymentData?.paymentMethod || bookingData.paymentMethod || 'STRIPE';

  if (loading) {
    return (
      <div className="booking-confirmation-page">
        <div className="container">
          <div className="loading-container">
            <div className="spinner"></div>
            <p>Loading booking details...</p>
          </div>
        </div>
      </div>
    );
  }

  if (!bookingData) {
    return (
      <div className="booking-confirmation-page">
        <div className="container">
          <div className="error-state">
            <h3>No booking details found</h3>
            <p>Redirecting to home...</p>
          </div>
        </div>
      </div>
    );
  }

  // Build compact QR payload for reliable scanning
  const qrPayload = {
    bookingId: bookingId,
    movie: bookingData.movieName || 'N/A',
    theater: bookingData.theaterName || 'N/A',
    date: bookingData.showDate || 'N/A',
    time: bookingData.showTime || 'N/A',
    seats: bookingData.selectedSeats || 'N/A',
    amount: `INR ${Number(totalAmount || 0).toFixed(2)}`,
    txnId: bookingData.transactionId || transactionId || 'N/A',
  };

  // Add parking summary if present
  if (bookingData.parking) {
    qrPayload.parking = `${bookingData.parking.vehicleType || ''} - ${bookingData.parking.vehicleNumber || ''} (${bookingData.parking.durationHours || 0}h)`;
  }

  // Add food summary if present
  if (bookingData.food && bookingData.food.items) {
    qrPayload.food = bookingData.food.items.map(i => `${i.name}x${i.quantity}`).join(', ');
    qrPayload.foodTotal = `INR ${Number(bookingData.food.total || 0).toFixed(2)}`;
  }

  qrPayload.generatedAt = new Date().toISOString();
  const qrData = JSON.stringify(qrPayload);

  const renderTicketCard = (isPdf = false) => (
    <div ref={isPdf ? pdfTicketRef : null} className={`ticket-card ${isPdf ? 'pdf-ticket' : ''}`}>
      <div className="ticket-content">
        <div className="ticket-details">
          <div className="movie-header">
            <h2>{bookingData.movieName}</h2>
            <span className="rating">U/A</span>
          </div>

          <div className="detail-row">
            <FaMapMarkerAlt className="icon" />
            <div>
              <label>Theater</label>
              <p>{bookingData.theaterName}</p>
            </div>
          </div>

          <div className="detail-row">
            <FaCalendar className="icon" />
            <div>
              <label>Date</label>
              <p>{formatShowDate(bookingData.showDate)}</p>
            </div>
          </div>

          <div className="detail-row">
            <FaClock className="icon" />
            <div>
              <label>Show Time</label>
              <p>{bookingData.showTime}</p>
            </div>
          </div>

          <div className="detail-row">
            <MdEventSeat className="icon" />
            <div>
              <label>Seats ({bookingData.selectedSeats?.length || 0})</label>
              <div className="seat-numbers">
                {bookingData.selectedSeats?.map((seat, index) => (
                  <span key={index} className="seat-badge">{seat}</span>
                ))}
              </div>
            </div>
          </div>

          <div className="detail-row">
            <FaTicketAlt className="icon" />
            <div>
              <label>Ticket Type</label>
              <p>M-Ticket (Mobile Ticket)</p>
            </div>
          </div>

          {bookingData.parking && (
            <div className="detail-row parking-details">
              <FaCar className="icon" />
              <div>
                <label>Parking Details</label>
                <p><strong>Slot:</strong> {bookingData.parking.slotNumber || `P-${Math.floor(Math.random() * 500) + 1}`}</p>
                <p><strong>Vehicle:</strong> {bookingData.parking.vehicleType} - {bookingData.parking.vehicleNumber || 'Not provided'}</p>
                <p><strong>Duration:</strong> {bookingData.parking.durationHours || bookingData.parking.duration || 4} hours</p>
                <p><strong>Fee:</strong> ₹{(bookingData.parking.amount || bookingData.parking.parkingFee || 0).toFixed(2)}</p>
              </div>
            </div>
          )}

          {bookingData.food && bookingData.food.items && bookingData.food.items.length > 0 && (
            <div className="detail-row food-details">
              <FaUtensils className="icon" />
              <div>
                <label>Pre-Ordered Food</label>
                {bookingData.food.items.map((item, idx) => (
                  <p key={idx}>{item.quantity}x {item.name} - ₹{(item.price * item.quantity).toFixed(2)}</p>
                ))}
                {bookingData.food.discount > 0 && (
                  <p className="discount-text">Discount: -₹{bookingData.food.discount.toFixed(2)}</p>
                )}
                <p><strong>Food Total:</strong> ₹{(bookingData.food.total || bookingData.food.subtotal || 0).toFixed(2)}</p>
              </div>
            </div>
          )}

          <div className="price-summary">
            <div className="price-row">
              <span>Ticket Price</span>
              <span>₹{(baseAmount || 0).toFixed(2)}</span>
            </div>
            {bookingData.parking && (
              <div className="price-row">
                <span>Parking Fee</span>
                <span>₹{(bookingData.parking.amount || 0).toFixed(2)}</span>
              </div>
            )}
            {bookingData.food && (
              <>
                <div className="price-row">
                  <span>Food & Beverages</span>
                  <span>₹{(bookingData.food.subtotal || 0).toFixed(2)}</span>
                </div>
                {bookingData.food.discount > 0 && (
                  <div className="price-row discount">
                    <span>Food Discount</span>
                    <span>-₹{bookingData.food.discount.toFixed(2)}</span>
                  </div>
                )}
              </>
            )}
            <div className="price-row">
              <span>Convenience Fee</span>
              <span>₹{convenienceFee.toFixed(2)}</span>
            </div>
            <div className="price-row">
              <span>GST (18%)</span>
              <span>₹{tax.toFixed(2)}</span>
            </div>
            <div className="price-divider"></div>
            <div className="price-row total">
              <span>Total Paid</span>
              <span>₹{totalAmount.toFixed(2)}</span>
            </div>
            {paymentData?.paymentMethod === 'WALLET_CARD_SPLIT' && paymentData?.walletAmount > 0 && (
              <>
                <div className="price-divider"></div>
                <div className="price-row split-info">
                  <span>💳 Paid via Wallet</span>
                  <span>₹{paymentData.walletAmount.toFixed(2)}</span>
                </div>
                <div className="price-row split-info">
                  <span>🏦 Paid via Card</span>
                  <span>₹{paymentData.cardAmount.toFixed(2)}</span>
                </div>
              </>
            )}
            {paymentData?.paymentMethod === 'WALLET' && (
              <>
                <div className="price-divider"></div>
                <div className="price-row split-info">
                  <span>💳 Paid via Wallet</span>
                  <span>₹{paymentData.totalAmount.toFixed(2)}</span>
                </div>
              </>
            )}
          </div>

          <div className="transaction-info">
            <p><strong>Payment Method:</strong> {
              paymentData?.paymentMethod === 'WALLET_CARD_SPLIT'
                ? '💳 + 🏦 Wallet + Card'
                : (paymentData?.paymentMethod || paymentMethod)
            }</p>
            <p><strong>Booked At:</strong> {currentTime}</p>
            <p><strong>Booked By:</strong> {bookingData.userName || user?.name}</p>
          </div>
        </div>

        <div className="ticket-qr">
          <div className="qr-section">
            <h3>Show this QR at the theater</h3>
            <div className="qr-code">
              <QRCodeCanvas
                value={qrData}
                size={220}
                level="M"
                includeMargin={true}
              />
            </div>
            <p className="qr-note">Scan to verify booking</p>
            <div className="booking-ref">
              {bookingId}
            </div>
          </div>
        </div>
      </div>

      <div className="ticket-footer">
        <div className="footer-note">
          <p>⚠️ Please arrive 15 minutes before showtime</p>
          <p>📱 Carry a valid ID proof along with this ticket</p>
          <p>🎬 Outside food and beverages are not allowed</p>
        </div>
      </div>
    </div>
  );

  return (
    <div className="booking-confirmation-page">
      <div className="container">
        {/* Success Header */}
        <div className="success-header">
          <div className="success-icon">
            <FaCheckCircle />
          </div>
          <h1>Booking Confirmed!</h1>
          <p>Your tickets have been booked successfully</p>
          <div className="booking-id">
            <strong>Booking ID:</strong> {bookingId}
          </div>
        </div>

        {renderTicketCard(false)}


        {/* Action Buttons */}
        <div className="action-buttons">
          <button className="btn-download" onClick={handleDownload}>
            <FaDownload /> Download Ticket
          </button>
          <button className="btn-email" onClick={handleEmail} disabled={emailSending}>
            <FaEnvelope /> {emailSending ? 'Sending...' : 'Email Ticket'}
          </button>
          <button className="btn-home" onClick={() => navigate('/')}>
            <FaHome /> Back to Home
          </button>
        </div>

        {/* Cancellation Policy */}
        <div className="policy-section">
          <h3>Cancellation Policy</h3>
          <ul>
            <li>Tickets can be cancelled up to 20 minutes before the show start time</li>
            <li>Cancellation charges: ₹{(bookingData.convenienceFee || 20).toFixed(2)} per ticket</li>
            <li>Refund will be processed within 5-7 working days</li>
            <li>No cancellation allowed for shows that have already started</li>
          </ul>
        </div>

        <div className="pdf-capture-root" aria-hidden="true">
          {renderTicketCard(true)}
        </div>
      </div>
    </div>
  );
};

export default BookingConfirmation;
