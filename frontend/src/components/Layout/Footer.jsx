import React from 'react';
import { Link } from 'react-router-dom';
import { FaFacebook, FaTwitter, FaInstagram, FaYoutube } from 'react-icons/fa';
import './Footer.scss';

const Footer = () => {
  return (
    <footer className="footer">
      <div className="footer-container">
        <div className="footer-content">
          <div className="footer-section">
            <h4>SeatPakki</h4>
            <p>India's largest online movie and events ticketing platform</p>
            <div className="social-links">
              <a href="https://facebook.com" target="_blank" rel="noopener noreferrer">
                <FaFacebook />
              </a>
              <a href="https://twitter.com" target="_blank" rel="noopener noreferrer">
                <FaTwitter />
              </a>
              <a href="https://instagram.com" target="_blank" rel="noopener noreferrer">
                <FaInstagram />
              </a>
              <a href="https://youtube.com" target="_blank" rel="noopener noreferrer">
                <FaYoutube />
              </a>
            </div>
          </div>

          <div className="footer-section">
            <h5>Movies Now Showing</h5>
            <ul>
              <li><Link to="/">Jawan</Link></li>
              <li><Link to="/">Pathaan</Link></li>
              <li><Link to="/">RRR</Link></li>
              <li><Link to="/">KGF Chapter 2</Link></li>
            </ul>
          </div>

          <div className="footer-section">
            <h5>Cities</h5>
            <ul>
              <li><Link to="/">Mumbai</Link></li>
              <li><Link to="/">Delhi</Link></li>
              <li><Link to="/">Bangalore</Link></li>
              <li><Link to="/">Chennai</Link></li>
              <li><Link to="/">Kolkata</Link></li>
            </ul>
          </div>

          <div className="footer-section">
            <h5>Help & Support</h5>
            <ul>
              <li><Link to="/">About Us</Link></li>
              <li><Link to="/">Contact Us</Link></li>
              <li><Link to="/">FAQs</Link></li>
              <li><Link to="/">Privacy Policy</Link></li>
              <li><Link to="/">Terms & Conditions</Link></li>
            </ul>
          </div>
        </div>

        <div className="footer-bottom">
          <p>&copy; 2026 SeatPakki. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
