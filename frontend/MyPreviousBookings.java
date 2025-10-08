package com.example.staygeniefrontend;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MyPreviousBookings extends AppCompatActivity implements BookingHistoryAdapter.OnBookingClickListener {

    private RecyclerView bookingsRecyclerView;
    private BookingHistoryAdapter adapter;
    private List<BookingHistoryItem> allBookings;
    private List<BookingHistoryItem> filteredBookings;
    private LinearLayout emptyState;
    private TextView totalBookingsTxt, finalizedBookingsTxt;
    private Button filterAllBtn, filterFinalizedBtn, filterCancelledBtn;
    private ImageView refreshBtn;

    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_previous_bookings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupClickListeners();
        loadBookingHistory();
    }

    private void initializeViews() {
        bookingsRecyclerView = findViewById(R.id.bookingsRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        totalBookingsTxt = findViewById(R.id.totalBookingsTxt);
        finalizedBookingsTxt = findViewById(R.id.finalizedBookingsTxt);
        filterAllBtn = findViewById(R.id.filterAllBtn);
        filterFinalizedBtn = findViewById(R.id.filterFinalizedBtn);
        filterCancelledBtn = findViewById(R.id.filterCancelledBtn);
        refreshBtn = findViewById(R.id.refreshBtn);

        // Setup RecyclerView
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        allBookings = new ArrayList<>();
        filteredBookings = new ArrayList<>();
        adapter = new BookingHistoryAdapter(filteredBookings, this, this);
        bookingsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        // Back button
        ImageView backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        // Refresh button
        refreshBtn.setOnClickListener(v -> {
            loadBookingHistory();
            Toast.makeText(this, "Bookings refreshed", Toast.LENGTH_SHORT).show();
        });

        // Filter buttons
        filterAllBtn.setOnClickListener(v -> setFilter("ALL"));
        filterFinalizedBtn.setOnClickListener(v -> setFilter("FINALIZED"));
        filterCancelledBtn.setOnClickListener(v -> setFilter("CANCELLED"));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateFilterButtons();
        applyFilter();
    }

    private void updateFilterButtons() {
        // Reset all buttons
        filterAllBtn.setBackgroundResource(R.drawable.filter_button);
        filterFinalizedBtn.setBackgroundResource(R.drawable.filter_button);
        filterCancelledBtn.setBackgroundResource(R.drawable.filter_button);

        // Set selected button
        switch (currentFilter) {
            case "ALL":
                filterAllBtn.setBackgroundResource(R.drawable.filter_button_selected);
                break;
            case "FINALIZED":
                filterFinalizedBtn.setBackgroundResource(R.drawable.filter_button_selected);
                break;
            case "CANCELLED":
                filterCancelledBtn.setBackgroundResource(R.drawable.filter_button_selected);
                break;
        }
    }

    private void applyFilter() {
        filteredBookings.clear();
        
        if ("ALL".equals(currentFilter)) {
            filteredBookings.addAll(allBookings);
        } else {
            for (BookingHistoryItem booking : allBookings) {
                if (currentFilter.equals(booking.getStatus())) {
                    filteredBookings.add(booking);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void loadBookingHistory() {
        allBookings.clear();
        BookingDatabase db = new BookingDatabase(this);
        Cursor cursor = null;
        
        try {
            cursor = db.getAllBookings();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    BookingHistoryItem booking = new BookingHistoryItem();
                    
                    int bookingIdIdx = cursor.getColumnIndex("booking_id");
                    int hotelIdx = cursor.getColumnIndex("hotel_name");
                    int statusIdx = cursor.getColumnIndex("status");
                    int amountIdx = cursor.getColumnIndex("amount");
                    int dateIdx = cursor.getColumnIndex("booking_date");
                    int detailsIdx = cursor.getColumnIndex("details");
                    
                    if (bookingIdIdx >= 0) booking.setBookingId(cursor.getString(bookingIdIdx));
                    if (hotelIdx >= 0) booking.setHotelName(cursor.getString(hotelIdx));
                    if (statusIdx >= 0) booking.setStatus(cursor.getString(statusIdx));
                    if (amountIdx >= 0) booking.setAmount(cursor.getString(amountIdx));
                    if (dateIdx >= 0) booking.setBookingDate(cursor.getLong(dateIdx));
                    
                    // Parse details for additional info
                    if (detailsIdx >= 0) {
                        String details = cursor.getString(detailsIdx);
                        parseBookingDetails(booking, details);
                    }
                    
                    allBookings.add(booking);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading booking history", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
        
        updateStats();
        applyFilter();
    }

    private void parseBookingDetails(BookingHistoryItem booking, String details) {
        if (details == null) return;
        
        // Parse details string to extract payment method, place type, etc.
        String[] parts = details.split("\\|");
        for (String part : parts) {
            if (part.contains("Payment:")) {
                booking.setPaymentMethod(part.split(":")[1].trim());
            } else if (part.contains("Booking:")) {
                booking.setPlaceType(part.split(":")[1].trim());
            }
        }
    }

    private void updateStats() {
        int total = allBookings.size();
        int finalized = 0;
        
        for (BookingHistoryItem booking : allBookings) {
            if ("FINALIZED".equals(booking.getStatus())) {
                finalized++;
            }
        }
        
        totalBookingsTxt.setText(String.valueOf(total));
        finalizedBookingsTxt.setText(String.valueOf(finalized));
    }

    private void updateEmptyState() {
        if (filteredBookings.isEmpty()) {
            emptyState.setVisibility(android.view.View.VISIBLE);
            bookingsRecyclerView.setVisibility(android.view.View.GONE);
        } else {
            emptyState.setVisibility(android.view.View.GONE);
            bookingsRecyclerView.setVisibility(android.view.View.VISIBLE);
        }
    }

    @Override
    public void onRebookClick(BookingHistoryItem booking) {
        // Navigate to booking flow with pre-filled data
        Intent intent = new Intent(this, BookingTheStayArea.class);
        intent.putExtra("hotel_name", booking.getHotelName());
        intent.putExtra("price", booking.getAmount());
        intent.putExtra("place_type", booking.getPlaceType().toLowerCase());
        intent.putExtra("location", booking.getLocation());
        intent.putExtra("place_id", "rebook_" + booking.getBookingId());
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public void onViewDetailsClick(BookingHistoryItem booking) {
        // Show booking details in a dialog or new activity
        String details = "Booking ID: " + booking.getBookingId() + "\n" +
                        "Hotel: " + booking.getHotelName() + "\n" +
                        "Type: " + booking.getPlaceType() + "\n" +
                        "Amount: $" + booking.getAmount() + "\n" +
                        "Payment: " + booking.getPaymentMethod() + "\n" +
                        "Status: " + booking.getStatus() + "\n" +
                        "Date: " + booking.getFormattedDate();
        
        Toast.makeText(this, details, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to this activity
        loadBookingHistory();
    }
}
