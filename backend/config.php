<?php
// -------------------------
// Database configuration
// -------------------------
$host = 'localhost';
$username = 'root';
$password = '';
$database = 'manusaipdd';

// Create DB connection
$conn = new mysqli($host, $username, $password, $database);

// Check connection
if ($conn->connect_error) {
    die(json_encode([
        "status" => false,
        "message" => "Database connection failed: " . $conn->connect_error
    ]));
}
$conn->set_charset("utf8");

// -------------------------
// Flask API Configuration
// -------------------------
// Detect environment
if (defined('ANDROID_EMULATOR')) {
    // Android emulator
    $FLASK_API_BASE = 'http://10.0.2.2:5000';
} elseif (defined('REAL_DEVICE')) {
    // Real device: replace with your ngrok URL
    $FLASK_API_BASE = 'https://your-ngrok-url.ngrok-free.app';
} else {
    // Default: local PC
    $FLASK_API_BASE = 'http://127.0.0.1:5000';
}

// Optional: make it global
$GLOBALS['FLASK_API_BASE'] = $FLASK_API_BASE;
?>
