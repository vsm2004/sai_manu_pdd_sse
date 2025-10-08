package com.example.staygeniefrontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoomPaymentOptions extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient();
    // TODO: Set to your deployed PHP backend URL
    private static final String[] BACKEND_URLS = {
        "https://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management/", // Ngrok tunnel URL
        "http://192.168.137.246/hotel_management", // Your computer's actual IP
        "http://192.168.1.3/hotel_management"     // Alternative IP
    };
    private static final String PAYMENT_URL = BACKEND_URLS[0] + "/payment.php";//modification
    
    private LinearLayout creditCardLayout, visaLayout, upiLayout;
    private String bookingId, hotelName, totalAmount, placeType;

    private int getUserId() {
        SharedPreferences prefs = getSharedPreferences("AccountPrefs", MODE_PRIVATE);
        String savedUserId = prefs.getString("saved_user_id", null);
        try {
            return savedUserId != null ? Integer.parseInt(savedUserId) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_payment_options);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get booking details from intent
        Intent src = getIntent();
        bookingId = src.getStringExtra("booking_id");
        hotelName = src.getStringExtra("hotel_name");
        totalAmount = src.getStringExtra("price");
        placeType = src.getStringExtra("place_type");

        // Find payment option layouts
        creditCardLayout = findViewById(R.id.credit_card_layout);
        visaLayout = findViewById(R.id.visa_layout);
        upiLayout = findViewById(R.id.upi_layout);

        // Set up payment option click listeners
        creditCardLayout.setOnClickListener(v -> processPayment("Credit Card"));
        visaLayout.setOnClickListener(v -> processPayment("Visa"));
        upiLayout.setOnClickListener(v -> processPayment("UPI"));
    }

    private void processPayment(String paymentMethod) {
        int userId = getUserId();
        
        if (userId == 0) {
            Toast.makeText(this, "User not logged in. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure required fields are present; generate sane fallbacks if missing
        if (bookingId == null || bookingId.trim().isEmpty()) {
            bookingId = String.valueOf(System.currentTimeMillis());
        }
        if (totalAmount == null) {
            totalAmount = "0";
        }

        // Prepare form-encoded body (PHP expects form fields)
        // Normalize amount to digits (strip currency/labels like "per day")
        String cleanAmountRaw = totalAmount.replace("$", "").replace(",", "").trim();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < cleanAmountRaw.length(); i++) {
            char c = cleanAmountRaw.charAt(i);
            if ((c >= '0' && c <= '9') || c == '.') digits.append(c);
        }
        String cleanAmount = digits.length() > 0 ? digits.toString() : "0";
        android.util.Log.d("BookingDebug", "Payment Request: user_id=" + userId + ", booking_id=" + bookingId + ", amount=" + cleanAmount);

        RequestBody body = new okhttp3.FormBody.Builder()
                .add("booking_id", bookingId)
                .add("user_id", String.valueOf(userId))
                .add("amount", cleanAmount)
                // match typical PHP param naming
                .add("payment_method", paymentMethod)
                .add("method", paymentMethod)
                // send helpful context if backend expects it
                .add("hotel_name", hotelName != null ? hotelName : "")
                .add("place_type", placeType != null ? placeType : "")
                .build();

        Request request = new Request.Builder()
                .url(PAYMENT_URL)
                .post(body)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                android.util.Log.d("RoomPaymentOptions", "Response code: " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    android.util.Log.d("RoomPaymentOptions", "Response body: " + responseBody);
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    boolean status = jsonResponse.optBoolean("status", false);
                    String message = jsonResponse.optString("message", "Unknown error");
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (status) {
                            Toast.makeText(RoomPaymentOptions.this, message, Toast.LENGTH_SHORT).show();
                            
                            // Navigate to BookingAndPaymentStatus with payment details
                            Intent intent = new Intent(RoomPaymentOptions.this, BookingAndPaymentStatus.class);
                            intent.putExtra("booking_id", bookingId);
                            intent.putExtra("hotel_name", hotelName);
                            intent.putExtra("total_amount", totalAmount);
                            intent.putExtra("place_type", placeType);
                            intent.putExtra("payment_method", paymentMethod);
                            intent.putExtra("transaction_id", jsonResponse.optString("transaction_id", ""));
                            startActivity(intent);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        } else {
                            Toast.makeText(RoomPaymentOptions.this, "Payment failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(RoomPaymentOptions.this, "Payment failed. HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("RoomPaymentOptions", "processPayment request failed", e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    String errorMsg = "Payment failed: " + e.getClass().getSimpleName();
                    if (e.getMessage() != null) errorMsg += " - " + e.getMessage();
                    Toast.makeText(RoomPaymentOptions.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
