package com.example.staygeniefrontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.Gravity;
import android.database.Cursor;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CancelBookingActivity extends AppCompatActivity {
    
    private TextView bookingIdView, hotelNameView, totalAmountView, paymentMethodView, placeTypeView, transactionIdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cancel_booking);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        bookingIdView = findViewById(R.id.tv_booking_id);
        hotelNameView = findViewById(R.id.tv_hotel_name);
        totalAmountView = findViewById(R.id.tv_total_amount);
        paymentMethodView = findViewById(R.id.tv_payment_method);
        placeTypeView = findViewById(R.id.tv_place_type);
        transactionIdView = findViewById(R.id.tv_transaction_id);

        // Add null checks for critical UI elements
        if (bookingIdView == null || hotelNameView == null || totalAmountView == null ||
            paymentMethodView == null || placeTypeView == null || transactionIdView == null) {
            Toast.makeText(this, "Error: UI elements not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get booking details from intent
        Intent intent = getIntent();
        String bookingId = intent.getStringExtra("booking_id");
        String hotelName = intent.getStringExtra("hotel_name");
        String totalAmount = intent.getStringExtra("total_amount");
        String paymentMethod = intent.getStringExtra("payment_method");
        String placeType = intent.getStringExtra("place_type");
        String transactionId = intent.getStringExtra("transaction_id");

        // Display booking details
        if (bookingId != null) bookingIdView.setText("Booking ID: " + bookingId);
        if (hotelName != null) hotelNameView.setText("Hotel: " + hotelName);
        if (totalAmount != null) totalAmountView.setText("Amount: $" + totalAmount);
        if (paymentMethod != null) paymentMethodView.setText("Payment: " + paymentMethod);
        if (placeType != null) placeTypeView.setText("Type: " + placeType.toUpperCase());
        if (transactionId != null) transactionIdView.setText("Transaction: " + transactionId);

        Toast.makeText(this, "Booking cancelled successfully! Refund will be processed.", Toast.LENGTH_LONG).show();

        // Show latest booking history at the bottom
        showLatestHistoryToast();
        
        // Navigate back to TourTypesCatalogue after a short delay
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Intent tourTypesIntent = new Intent(CancelBookingActivity.this, TourTypesCatalogue.class);
            startActivity(tourTypesIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish(); // Close this activity to prevent back navigation issues
        }, 3000);
    }

    private void showLatestHistoryToast() {
        BookingDatabase db = new BookingDatabase(this);
        Cursor cursor = null;
        try {
            cursor = db.getAllBookings();
            if (cursor != null && cursor.moveToFirst()) {
                int hotelIdx = cursor.getColumnIndex("hotel_name");
                int statusIdx = cursor.getColumnIndex("status");
                int amountIdx = cursor.getColumnIndex("amount");
                int bookingIdIdx = cursor.getColumnIndex("booking_id");

                String latestHotel = hotelIdx >= 0 ? cursor.getString(hotelIdx) : "";
                String latestStatus = statusIdx >= 0 ? cursor.getString(statusIdx) : "";
                String latestAmount = amountIdx >= 0 ? cursor.getString(amountIdx) : "";
                String latestBookingId = bookingIdIdx >= 0 ? cursor.getString(bookingIdIdx) : "";

                String historyMsg = "History: " + latestHotel + " | " + latestStatus +
                        (latestBookingId.isEmpty() ? "" : (" | ID: " + latestBookingId));

                Toast t = Toast.makeText(this, historyMsg, Toast.LENGTH_LONG);
                t.setGravity(Gravity.BOTTOM, 0, 120);
                t.show();
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}
