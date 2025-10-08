package com.example.staygeniefrontend;

public final class Config {

    private Config() {}

    // Centralized base URLs - prioritize local development
    // PHP backend (Apache/XAMPP) - try local first, then ngrok
    public static final String[] PHP_BASE_URLS = {
            "http://192.168.1.5/hotel_management/",     // Your computer's IP
            "http://10.0.2.2/hotel_management/",       // Android emulator
            "https://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management/"  // Ngrok fallback
    };

    // Flask backend - try local first, then ngrok
    public static final String[] FLASK_BASE_URLS = {
            "http://192.168.1.5:5000",                  // Your computer's IP
            "http://10.0.2.2:5000",                     // Android emulator
            "https://nondilatable-petrina-pedigreed.ngrok-free.dev:5000"  // Ngrok fallback
    };

    // Primary URLs (first in array)
    public static final String PHP_BASE_URL = PHP_BASE_URLS[0];
    public static final String FLASK_BASE_URL = FLASK_BASE_URLS[0];
}
