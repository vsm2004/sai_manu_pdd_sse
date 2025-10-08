package com.example.staygeniefrontend;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingHistoryItem {
    private String bookingId;
    private String hotelName;
    private String placeType;
    private String amount;
    private String paymentMethod;
    private String status;
    private long bookingDate;
    private String transactionId;
    private String location;

    public BookingHistoryItem() {}

    public BookingHistoryItem(String bookingId, String hotelName, String placeType, String amount, 
                            String paymentMethod, String status, long bookingDate, String transactionId, String location) {
        this.bookingId = bookingId;
        this.hotelName = hotelName;
        this.placeType = placeType;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.bookingDate = bookingDate;
        this.transactionId = transactionId;
        this.location = location;
    }

    // Getters and Setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getHotelName() { return hotelName; }
    public void setHotelName(String hotelName) { this.hotelName = hotelName; }

    public String getPlaceType() { return placeType; }
    public void setPlaceType(String placeType) { this.placeType = placeType; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getBookingDate() { return bookingDate; }
    public void setBookingDate(long bookingDate) { this.bookingDate = bookingDate; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getFormattedDate() {
        if (bookingDate == 0) return "Unknown";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(bookingDate));
    }
}
