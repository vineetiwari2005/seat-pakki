-- ============================================
-- BookMyShow Database Initialization Script
-- ============================================
-- Run this ONCE after starting backend
-- ============================================

USE bookmyshow;

-- Initialize Refund Rules (DB-driven, NO HARDCODED VALUES)
INSERT INTO refund_rules (hours_threshold, refund_percentage, description, is_active, priority, created_at, updated_at)
VALUES 
    (1, 100, 'Full refund within 1 hour of booking', true, 1, NOW(), NOW()),
    (6, 75, '75% refund within 6 hours of booking', true, 2, NOW(), NOW()),
    (12, 50, '50% refund within 12 hours of booking', true, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    refund_percentage = VALUES(refund_percentage),
    updated_at = NOW();

-- Initialize Static Pages (DB-driven, NO HARDCODED CONTENT)
INSERT INTO static_pages (page_key, title, content, display_order, is_active, created_at, updated_at)
VALUES 
    ('about_us', 'About BookMyShow', '<h2>About BookMyShow</h2><p>India''s leading entertainment platform for booking movie tickets, events, and shows.</p>', 1, true, NOW(), NOW()),
    ('faq', 'FAQ', '<h2>Frequently Asked Questions</h2><p><strong>Q: How to cancel?</strong><br>A: Go to My Bookings and click Cancel. Refund: 100% within 1hr, 75% within 6hrs, 50% within 12hrs.</p>', 2, true, NOW(), NOW()),
    ('privacy_policy', 'Privacy Policy', '<h2>Privacy Policy</h2><p>We protect your data. Last updated: Jan 2026.</p>', 3, true, NOW(), NOW()),
    ('terms_conditions', 'Terms & Conditions', '<h2>Terms & Conditions</h2><p>By using BookMyShow, you agree to our terms.</p>', 4, true, NOW(), NOW()),
    ('contact_us', 'Contact Us', '<h2>Contact Us</h2><p>Email: support@bookmyshow.com<br>Phone: 1800-123-4567</p>', 5, true, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    content = VALUES(content),
    updated_at = NOW();

SELECT 'Database initialized successfully!' AS Status;
