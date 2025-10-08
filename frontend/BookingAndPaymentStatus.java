package com.example.staygeniefrontend;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BookingAndPaymentStatus extends AppCompatActivity {

    private TextView bookingIdTextView, hotelNameTextView, totalAmountTextView, paymentMethodTextView, placeTypeTextView;
    private Button finalizeRoomBtn, cancelRoomBtn;

    private String bookingId, hotelName, totalAmount, paymentMethod, placeType, transactionId;
    private long checkoutTimeMillis = System.currentTimeMillis() + 86_400_000L; // +1 day

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking_and_payment_status);

        // Edge-to-Edge Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Views
        bookingIdTextView = findViewById(R.id.booking_id_textview);
        hotelNameTextView = findViewById(R.id.hotel_name_textview);
        totalAmountTextView = findViewById(R.id.total_amount_textview);
        paymentMethodTextView = findViewById(R.id.payment_method_textview);
        placeTypeTextView = findViewById(R.id.place_type_textview);
        finalizeRoomBtn = findViewById(R.id.finalize_room_btn);
        cancelRoomBtn = findViewById(R.id.cancel_room_btn);

        // Add null checks for critical UI elements
        if (bookingIdTextView == null || hotelNameTextView == null || totalAmountTextView == null ||
            paymentMethodTextView == null || placeTypeTextView == null || 
            finalizeRoomBtn == null || cancelRoomBtn == null) {
            Toast.makeText(this, "Error: UI elements not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get Intent data
        Intent intent = getIntent();
        bookingId = intent.getStringExtra("booking_id");
        hotelName = intent.getStringExtra("hotel_name");
        totalAmount = intent.getStringExtra("total_amount");
        paymentMethod = intent.getStringExtra("payment_method");
        placeType = intent.getStringExtra("place_type");
        transactionId = intent.getStringExtra("transaction_id");

        // Display dynamic data
        if (bookingId != null) bookingIdTextView.setText("Booking ID: " + bookingId);
        if (hotelName != null) hotelNameTextView.setText(hotelName);
        if (totalAmount != null) totalAmountTextView.setText("Total Amount: $" + totalAmount);
        if (paymentMethod != null) paymentMethodTextView.setText("Payment Method: " + paymentMethod);
        if (placeType != null) placeTypeTextView.setText("Type: " + placeType.toUpperCase());

        // Finalize Room Button
        finalizeRoomBtn.setOnClickListener(v -> {
            addBookingHistory("FINALIZED");
            scheduleCheckoutNotification(); // Only for finalized bookings
            Toast.makeText(this, "Room finalized! Checkout notification set.", Toast.LENGTH_SHORT).show();
            
            // Navigate to FinalizeBookingActivity
            Intent finalizeIntent = new Intent(BookingAndPaymentStatus.this, FinalizeBookingActivity.class);
            finalizeIntent.putExtra("booking_id", bookingId);
            finalizeIntent.putExtra("hotel_name", hotelName);
            finalizeIntent.putExtra("total_amount", totalAmount);
            finalizeIntent.putExtra("payment_method", paymentMethod);
            finalizeIntent.putExtra("place_type", placeType);
            finalizeIntent.putExtra("transaction_id", transactionId);
            startActivity(finalizeIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // Cancel Room Button
        cancelRoomBtn.setOnClickListener(v -> {
            addBookingHistory("CANCELLED");
            Toast.makeText(this, "Room cancelled! History updated.", Toast.LENGTH_SHORT).show();
            
            // Navigate to CancelBookingActivity
            Intent cancelIntent = new Intent(BookingAndPaymentStatus.this, CancelBookingActivity.class);
            cancelIntent.putExtra("booking_id", bookingId);
            cancelIntent.putExtra("hotel_name", hotelName);
            cancelIntent.putExtra("total_amount", totalAmount);
            cancelIntent.putExtra("payment_method", paymentMethod);
            cancelIntent.putExtra("place_type", placeType);
            cancelIntent.putExtra("transaction_id", transactionId);
            startActivity(cancelIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    // Save booking history in SQLite
    private void addBookingHistory(String status) {
        BookingDatabase db = new BookingDatabase(this);
        String historyDetails = String.format("Booking: %s | Payment: %s | Amount: $%s | Transaction: %s", 
            placeType != null ? placeType.toUpperCase() : "Unknown", 
            paymentMethod != null ? paymentMethod : "Unknown", 
            totalAmount != null ? totalAmount : "0", 
            transactionId != null ? transactionId : "N/A");
        
        db.addBooking(hotelName, status, bookingId, historyDetails);
    }

    // Schedule checkout notification only for finalized bookings
    private void scheduleCheckoutNotification() {
        Intent broadcast = new Intent(this, CheckoutNotificationReceiver.class);
        broadcast.putExtra("hotel_name", hotelName);
        PendingIntent pi = PendingIntent.getBroadcast(this, bookingId.hashCode(), broadcast,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, checkoutTimeMillis, pi);
    }
}
