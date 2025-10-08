package com.example.staygeniefrontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BookingTheStayArea extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    // Multi-URL fallback for different environments
    private static final String[] BACKEND_URLS = Config.PHP_BASE_URLS;
    
    private String hotelName;
    private String price;
    private String placeType;
    private String placeId;
    private String location;

    private int getUserId() {
        SharedPreferences prefs = getSharedPreferences("AccountPrefs", MODE_PRIVATE);
        // Prefer explicit saved_user_id if present
        String savedUserId = prefs.getString("saved_user_id", null);
        if (savedUserId != null) {
            try {
                int uid = Integer.parseInt(savedUserId);
                if (uid > 0) return uid;
            } catch (NumberFormatException ignored) { }
        }

        // Fallback to email hash from AccountPrefs
        String email = prefs.getString("saved_email", null);
        if (email != null && !email.trim().isEmpty()) {
            return Math.abs(email.trim().toLowerCase().hashCode());
        }

        // Final fallback: legacy prefs used by older flows
        SharedPreferences legacy = getSharedPreferences("staygenie_prefs", MODE_PRIVATE);
        String legacyEmail = legacy.getString("saved_email", null);
        if (legacyEmail != null && !legacyEmail.trim().isEmpty()) {
            return Math.abs(legacyEmail.trim().toLowerCase().hashCode());
        }

        // Absolute fallback to avoid hard stop on booking flow
        return 1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking_the_stay_area);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get booking details from intent
        Intent src = getIntent();
        hotelName = src.getStringExtra("hotel_name");
        price = src.getStringExtra("price");
        placeType = src.getStringExtra("place_type");
        placeId = src.getStringExtra("place_id");
        location = src.getStringExtra("location");

        // Initialize UI with booking details
        initializeBookingDetails();

        // Confirm booking button
        Button confirmButton = findViewById(R.id.btn_confirm_booking);
        confirmButton.setOnClickListener(v -> {
            confirmBooking();
        });
    }

    private void initializeBookingDetails() {
        // Set hotel/place name
        TextView hotelNameView = findViewById(R.id.tv_hotel_name);
        if (hotelNameView != null && hotelName != null) {
            hotelNameView.setText(hotelName);
        }

        // Set price
        TextView priceView = findViewById(R.id.tv_price);
        if (priceView != null && price != null) {
            priceView.setText("$" + price + "/night");
        }

        // Set place type
        TextView placeTypeView = findViewById(R.id.tv_place_type);
        if (placeTypeView != null && placeType != null) {
            placeTypeView.setText(placeType.toUpperCase());
        }

        // Set check-in and check-out dates (default to tomorrow and day after)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        String checkInDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.getTime());
        
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        String checkOutDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.getTime());

        TextView checkInView = findViewById(R.id.tv_checkin_date);
        if (checkInView != null) {
            checkInView.setText(checkInDate);
        }

        TextView checkOutView = findViewById(R.id.tv_checkout_date);
        if (checkOutView != null) {
            checkOutView.setText(checkOutDate);
        }

        // Set check-in and check-out times
        TextView checkInTimeView = findViewById(R.id.tv_checkin_time);
        if (checkInTimeView != null) {
            checkInTimeView.setText("3:00 PM");
        }

        TextView checkOutTimeView = findViewById(R.id.tv_checkout_time);
        if (checkOutTimeView != null) {
            checkOutTimeView.setText("11:00 AM");
        }

        // Set features (you can customize this based on place type)
        TextView featuresView = findViewById(R.id.tv_features);
        if (featuresView != null) {
            String features = getFeaturesForPlaceType(placeType);
            featuresView.setText(features);
        }

        // Set rating (you can customize this based on actual data)
        TextView ratingView = findViewById(R.id.tv_rating);
        if (ratingView != null) {
            ratingView.setText("4.5 ⭐");
        }

        // Set location
        TextView locationView = findViewById(R.id.tv_location);
        if (locationView != null && location != null) {
            locationView.setText(location);
        } else if (locationView != null) {
            // Fallback to destination from SharedPreferences if location not provided
            SharedPreferences prefs = getSharedPreferences("AccountPrefs", MODE_PRIVATE);
            String destination = prefs.getString("saved_destination", "Location not specified");
            locationView.setText(destination);
        }

        // Calculate and set bill matrix
        calculateBillMatrix();
    }

    private String getFeaturesForPlaceType(String placeType) {
        if (placeType == null) return "Standard amenities";
        
        switch (placeType.toLowerCase()) {
            case "hotel":
                return "• Free WiFi\n• Air Conditioning\n• Room Service\n• Swimming Pool\n• Fitness Center";
            case "resort":
                return "• Beach Access\n• Spa Services\n• Multiple Restaurants\n• Water Sports\n• Kids Club";
            case "vacation":
                return "• Full Kitchen\n• Living Area\n• Laundry Facilities\n• Parking\n• Pet Friendly";
            case "business":
                return "• Business Center\n• Meeting Rooms\n• High-Speed WiFi\n• Concierge Service\n• Airport Shuttle";
            default:
                return "• Standard amenities included";
        }
    }

    private void calculateBillMatrix() {
        if (price == null) return;

        try {
            double basePrice = Double.parseDouble(price.replace("$", ""));
            double taxRate = 0.12; // 12% tax
            double serviceFee = 15.00; // Service fee
            double totalTax = basePrice * taxRate;
            double totalAmount = basePrice + totalTax + serviceFee;

            TextView basePriceView = findViewById(R.id.tv_base_price);
            if (basePriceView != null) {
                basePriceView.setText("$" + String.format("%.2f", basePrice));
            }

            TextView taxView = findViewById(R.id.tv_tax);
            if (taxView != null) {
                taxView.setText("$" + String.format("%.2f", totalTax));
            }

            TextView serviceFeeView = findViewById(R.id.tv_service_fee);
            if (serviceFeeView != null) {
                serviceFeeView.setText("$" + String.format("%.2f", serviceFee));
            }

            TextView totalView = findViewById(R.id.tv_total_amount);
            if (totalView != null) {
                totalView.setText("$" + String.format("%.2f", totalAmount));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void confirmBooking() {
        int userId = getUserId();

        // Validate required fields first
        if (hotelName == null || hotelName.trim().isEmpty()) {
            Toast.makeText(this, "Hotel name is missing. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (price == null || price.trim().isEmpty()) {
            Toast.makeText(this, "Price information is missing. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate user ID
        if (userId <= 0) {
            Toast.makeText(this, "User not logged in. Please login and try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Required safe defaults
        int safeUserId = userId > 0 ? userId : 1;
        String safeHotelName = hotelName.trim();

        // Create booking data as form data - extract numeric price
        String cleanPrice = price.replace("$", "").replace("/night", "").replace("per day", "").replace(",", "").trim();
        if (cleanPrice.isEmpty()) {
            Toast.makeText(this, "Invalid price format. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate price is numeric
        try {
            Double.parseDouble(cleanPrice);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price format. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Optional fields with defaults
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String safeCheckIn = df.format(cal.getTime());
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1);
        String safeCheckOut = df.format(cal.getTime());
        String safeGuests = "1";
        String safeRooms = "1";
        String safePlaceType = (placeType != null && !placeType.trim().isEmpty()) ? placeType : "hotel";
        String safeLocation = (location != null) ? location : "";

        android.util.Log.d("BookingDebug", "=== BOOKING REQUEST DEBUG ===");
        android.util.Log.d("BookingDebug", "User ID: " + safeUserId);
        android.util.Log.d("BookingDebug", "Hotel Name: " + safeHotelName);
        android.util.Log.d("BookingDebug", "Place Type: " + safePlaceType);
        android.util.Log.d("BookingDebug", "Check-in: " + safeCheckIn);
        android.util.Log.d("BookingDebug", "Check-out: " + safeCheckOut);
        android.util.Log.d("BookingDebug", "Guests: " + safeGuests);
        android.util.Log.d("BookingDebug", "Rooms: " + safeRooms);
        android.util.Log.d("BookingDebug", "Total Price: " + cleanPrice);
        android.util.Log.d("BookingDebug", "Original Price: " + price);
        android.util.Log.d("BookingDebug", "Location: " + safeLocation);
        
        RequestBody body = new FormBody.Builder()
                .add("user_id", String.valueOf(safeUserId))
                .add("hotel_name", safeHotelName)
                .add("place_type", safePlaceType)
                .add("check_in", safeCheckIn)
                .add("check_out", safeCheckOut)
                .add("guests", safeGuests)
                .add("rooms", safeRooms)
                .add("total_price", cleanPrice)
                .build();

        // Try multiple backend URLs sequentially
        new Thread(() -> {
            int attempt = 0;
            while (attempt < BACKEND_URLS.length) {
                String bookingUrl = BACKEND_URLS[attempt] + "book.php";
                android.util.Log.d("BookingTheStayArea", "Attempting booking URL: " + bookingUrl);
                Request request = new Request.Builder()
                        .url(bookingUrl)
                        .post(body)
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    android.util.Log.d("BookingTheStayArea", "Attempt " + (attempt + 1) + " - Response code: " + response.code());
                    
                    String ctype = response.header("Content-Type", "");
                    if (response.isSuccessful() && response.body() != null && ctype.contains("application/json")) {
                        String responseBody = response.body().string();
                        android.util.Log.d("BookingTheStayArea", "Response body: " + responseBody);
                        
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            boolean status = jsonResponse.optBoolean("status", false);
                            String message = jsonResponse.optString("message", "Unknown error");
                            
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (status) {
                                    Toast.makeText(BookingTheStayArea.this, "Booking successful! " + message, Toast.LENGTH_SHORT).show();
                                    
                                    // Navigate to payment options with booking details
                                    Intent intent = new Intent(BookingTheStayArea.this, RoomPaymentOptions.class);
                                    intent.putExtra("hotel_name", hotelName);
                                    intent.putExtra("price", price);
                                    intent.putExtra("place_type", placeType);
                                    intent.putExtra("booking_id", jsonResponse.optString("booking_id", ""));
                                    intent.putExtra("location", location);
                                    startActivity(intent);
                                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                } else {
                                    Toast.makeText(BookingTheStayArea.this, "Booking failed: " + message, Toast.LENGTH_LONG).show();
                                    android.util.Log.e("BookingTheStayArea", "Booking failed with message: " + message);
                                }
                            });
                            // Success - break the loop
                            return;
                        } catch (JSONException e) {
                            android.util.Log.e("BookingTheStayArea", "Failed to parse JSON response: " + e.getMessage());
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(BookingTheStayArea.this, "Invalid response from server", Toast.LENGTH_SHORT).show());
                            return;
                        }
                    } else {
                        if (!ctype.contains("application/json")) {
                            android.util.Log.e("BookingTheStayArea", "Non-JSON response: " + ctype);
                        }
                        // Try next URL
                        attempt++;
                        if (attempt >= BACKEND_URLS.length) {
                            final int code = response.code();
                            new Handler(Looper.getMainLooper()).post(() ->
                                    Toast.makeText(BookingTheStayArea.this, "All servers failed. HTTP " + code, Toast.LENGTH_SHORT).show());
                        }
                        continue;
                    }
                } catch (Exception e) {
                    android.util.Log.e("BookingTheStayArea", "Network error on " + bookingUrl + ": " + e.getMessage());
                    attempt++;
                    if (attempt >= BACKEND_URLS.length) {
                        String err = e.getMessage();
                        new Handler(Looper.getMainLooper()).post(() ->
                                Toast.makeText(BookingTheStayArea.this, "Network error: " + err, Toast.LENGTH_SHORT).show());
                    }
                }
            }
        }).start();
    }
}
