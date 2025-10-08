-- Create database
CREATE DATABASE IF NOT EXISTS manusaipdd;
USE manusaipdd;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    otp VARCHAR(6) DEFAULT NULL,
    otp_expiry TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create dashboard_selections table for tracking user preferences
CREATE TABLE IF NOT EXISTS dashboard_selections (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    selection VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create feedback table for storing user feedback with ML analysis
CREATE TABLE IF NOT EXISTS feedback (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    hotel_name VARCHAR(255),
    emoji VARCHAR(10),
    star_rating INT DEFAULT 3,
    feedback_text TEXT NOT NULL,
    sentiment_label VARCHAR(20),
    cumulative_score DECIMAL(3,2),
    suggestions JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create password reset OTPs table
CREATE TABLE IF NOT EXISTS password_reset_otps (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create hotels table
CREATE TABLE IF NOT EXISTS hotels (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    price VARCHAR(50) NOT NULL,
    rating DECIMAL(2,1) DEFAULT 4.0,
    amenities TEXT,
    description TEXT,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create resorts table
CREATE TABLE IF NOT EXISTS resorts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    price VARCHAR(50) NOT NULL,
    rating DECIMAL(2,1) DEFAULT 4.0,
    amenities TEXT,
    type VARCHAR(100),
    features TEXT,
    description TEXT,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create tour_agencies table for vacation packages
CREATE TABLE IF NOT EXISTS tour_agencies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    price VARCHAR(50) NOT NULL,
    duration VARCHAR(50) DEFAULT '5 days',
    type VARCHAR(100),
    highlights TEXT,
    description TEXT,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create business_stays table for business accommodations
CREATE TABLE IF NOT EXISTS business_stays (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    price VARCHAR(50) NOT NULL,
    rating DECIMAL(2,1) DEFAULT 4.0,
    amenities TEXT,
    meeting_rooms BOOLEAN DEFAULT FALSE,
    airport_shuttle BOOLEAN DEFAULT FALSE,
    description TEXT,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create locations table for storing user address data
CREATE TABLE IF NOT EXISTS locations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    current_location VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index for better performance
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_selections_user_id ON dashboard_selections(user_id);
CREATE INDEX idx_feedback_user_id ON feedback(user_id);
CREATE INDEX idx_feedback_sentiment ON feedback(sentiment_label);
CREATE INDEX idx_otp_email ON password_reset_otps(email);
CREATE INDEX idx_otp_expires ON password_reset_otps(expires_at);
CREATE INDEX idx_locations_user_id ON locations(user_id);
CREATE INDEX idx_locations_destination ON locations(destination);
CREATE INDEX idx_hotels_location ON hotels(location);
CREATE INDEX idx_resorts_location ON resorts(location);
CREATE INDEX idx_tour_agencies_location ON tour_agencies(location);
CREATE INDEX idx_business_stays_location ON business_stays(location);

-- Insert sample user for testing (password: test123)
INSERT INTO users (name, email, password) VALUES 
('Test User', 'test@example.com', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert sample location data for testing
INSERT INTO locations (user_id, current_location, destination) VALUES 
(1, 'Chennai, India', 'Goa, India'),
(1, 'Mumbai, India', 'Bangkok, Thailand')
ON DUPLICATE KEY UPDATE current_location = VALUES(current_location);

-- Insert sample hotels for testing
INSERT INTO hotels (name, location, price, rating, amenities, description) VALUES 
('Grand Plaza Hotel', 'Miami', '$199 per day', 4.5, 'WiFi, Pool, Gym, Spa', 'Luxury hotel in downtown Miami'),
('Ocean View Resort', 'Miami', '$249 per day', 4.7, 'WiFi, Pool, Beach Access, Restaurant', 'Beachfront resort with ocean views'),
('City Light Inn', 'Miami', '$149 per day', 4.2, 'WiFi, Parking, Restaurant', 'Budget-friendly hotel in city center'),
('Business Center Hotel', 'Miami', '$189 per day', 4.3, 'WiFi, Meeting Rooms, Gym', 'Perfect for business travelers'),
('Mountain Retreat Lodge', 'Aspen', '$299 per day', 4.8, 'WiFi, Spa, Ski Access, Restaurant', 'Luxury mountain lodge with ski access'),
('Cozy Budget Inn', 'Aspen', '$99 per day', 3.8, 'WiFi, Parking', 'Affordable accommodation near ski slopes'),
('Family Resort', 'Orlando', '$219 per day', 4.4, 'WiFi, Pool, Kids Club, Restaurant', 'Family-friendly resort near theme parks'),
('Executive Suites', 'Orlando', '$279 per day', 4.6, 'WiFi, Business Center, Gym, Spa', 'Executive accommodation for business travelers')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert sample resorts for testing
INSERT INTO resorts (name, location, price, rating, amenities, type, features, description) VALUES 
('Tropical Horizon Resort', 'Miami', '$350 per day', 4.5, 'WiFi, Pool, Spa, Restaurant, Beach Access', 'Tropical', 'Beach Access, Spa, Pool', 'Luxury tropical resort with private beach access'),
('Sunset Bay Resort', 'Miami', '$320 per day', 4.3, 'WiFi, Pool, Spa, Restaurant, Beach Access', 'Beach', 'Beach Access, Spa, Pool', 'Beachfront resort with stunning sunset views'),
('Mountain View Resort', 'Aspen', '$400 per day', 4.6, 'WiFi, Spa, Restaurant, Ski Access, Mountain View', 'Mountain', 'Ski Access, Mountain View, Spa', 'Luxury mountain resort with ski-in/ski-out access'),
('Luxury Paradise Resort', 'Maldives', '$600 per day', 4.8, 'WiFi, Pool, Spa, Restaurant, Beach Access, Private Beach', 'Luxury', 'Private Beach, Spa, Pool', 'Ultimate luxury resort with overwater villas'),
('Family Fun Resort', 'Orlando', '$280 per day', 4.2, 'WiFi, Pool, Kids Club, Restaurant, Theme Park Access', 'Family', 'Kids Club, Pool, Theme Park Access', 'Family-friendly resort near Disney World'),
('Ski Lodge Resort', 'Aspen', '$450 per day', 4.6, 'WiFi, Spa, Restaurant, Ski Access, Fireplace', 'Mountain', 'Ski Access, Spa, Fireplace', 'Cozy ski lodge with fireplace and spa services'),
('Budget Beach Resort', 'Cancun', '$180 per day', 3.9, 'WiFi, Pool, Restaurant, Beach Access', 'Budget', 'Beach Access, Pool', 'Affordable beach resort with all-inclusive packages'),
('Ocean Pearl Resort', 'North Carolina', '$400 per day', 3.8, 'WiFi, Pool, Gym, Restaurant, Ocean View', 'Beach', 'Ocean View, Pool, Gym', 'Oceanfront resort with fitness center and ocean views')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert sample tour agencies for testing
INSERT INTO tour_agencies (name, location, price, duration, type, highlights, description) VALUES 
('Bali Beach Escape', 'Bali, Indonesia', '$1299', '7 days', 'beach', 'Beach time, Ubud temples, snorkeling', 'Complete Bali experience with beach relaxation and cultural exploration'),
('Swiss Alps Adventure', 'Interlaken, Switzerland', '$1899', '6 days', 'mountain', 'Paragliding, hiking, Jungfraujoch', 'Adventure-packed Swiss Alps tour with mountain activities'),
('Kenya Safari', 'Maasai Mara, Kenya', '$2499', '5 days', 'safari', 'Game drives, Big Five, sunrise balloon', 'Wildlife safari experience in Kenya\'s famous Maasai Mara'),
('Mediterranean Cruise', 'Italy-Greece', '$2099', '8 days', 'cruise', 'Santorini, Rome, onboard shows', 'Luxury Mediterranean cruise visiting Italy and Greece'),
('Tokyo City Lights', 'Tokyo, Japan', '$1599', '5 days', 'city', 'Shibuya crossing, sushi tour, temples', 'Urban exploration of Tokyo with cultural experiences'),
('Patagonia Trek', 'El Chaltén, Argentina', '$2299', '7 days', 'adventure', 'Fitz Roy trek, glaciers, camping', 'Adventure trekking in Patagonia\'s stunning landscapes'),
('Maldives Luxury Retreat', 'Malé, Maldives', '$3299', '5 days', 'luxury', 'Overwater villa, spa, private beach', 'Ultimate luxury retreat in the Maldives'),
('Costa Rica Eco Tour', 'San José, Costa Rica', '$1799', '6 days', 'eco', 'Rainforest zipline, volcano, wildlife', 'Eco-friendly adventure tour of Costa Rica\'s natural wonders')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert sample business stays for testing
INSERT INTO business_stays (name, location, price, rating, amenities, meeting_rooms, airport_shuttle, description) VALUES 
('Executive Suites', 'Financial District', '$279 per day', 4.6, 'WiFi, Business Center, Gym, Spa', TRUE, FALSE, 'Luxury executive suites in the heart of the financial district'),
('Business Center Hotel', 'Business District', '$189 per day', 4.3, 'WiFi, Meeting Rooms, Gym', TRUE, FALSE, 'Modern business hotel with comprehensive meeting facilities'),
('Airport Express Inn', 'Airport Zone', '$149 per day', 4.1, 'WiFi, Shuttle, Meeting Rooms', TRUE, TRUE, 'Convenient airport hotel with free shuttle service'),
('City Conference Hotel', 'Downtown', '$209 per day', 4.4, 'WiFi, Conference Hall, Gym', TRUE, FALSE, 'Downtown conference hotel with state-of-the-art facilities'),
('Corporate Plaza', 'Business District', '$229 per day', 4.5, 'WiFi, Business Center, Meeting Rooms, Gym', TRUE, FALSE, 'Corporate-focused hotel with extensive business amenities'),
('Tech Hub Inn', 'Tech District', '$199 per day', 4.2, 'WiFi, Meeting Rooms, Business Center', TRUE, FALSE, 'Modern hotel catering to tech professionals'),
('Metro Business Hotel', 'Downtown', '$179 per day', 4.0, 'WiFi, Meeting Rooms, Gym', TRUE, FALSE, 'Affordable business hotel in downtown location'),
('Executive Airport Hotel', 'Airport Zone', '$249 per day', 4.7, 'WiFi, Business Center, Shuttle, Spa', TRUE, TRUE, 'Premium airport hotel with executive services')
ON DUPLICATE KEY UPDATE name = VALUES(name);
