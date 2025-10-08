package com.example.staygeniefrontend;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingHistoryAdapter extends RecyclerView.Adapter<BookingHistoryAdapter.BookingViewHolder> {

    private List<BookingHistoryItem> bookingList;
    private Context context;
    private OnBookingClickListener listener;

    public interface OnBookingClickListener {
        void onRebookClick(BookingHistoryItem booking);
        void onViewDetailsClick(BookingHistoryItem booking);
    }

    public BookingHistoryAdapter(List<BookingHistoryItem> bookingList, Context context, OnBookingClickListener listener) {
        this.bookingList = bookingList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking_history, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BookingHistoryItem booking = bookingList.get(position);
        
        holder.bookingIdTxt.setText("Booking ID: " + booking.getBookingId());
        holder.hotelNameTxt.setText(booking.getHotelName());
        holder.placeTypeTxt.setText(booking.getPlaceType().toUpperCase());
        holder.amountTxt.setText("$" + booking.getAmount());
        holder.paymentMethodTxt.setText(booking.getPaymentMethod());
        holder.bookingDateTxt.setText(booking.getFormattedDate());
        
        // Set status text and color
        holder.statusTxt.setText(booking.getStatus());
        if ("FINALIZED".equals(booking.getStatus())) {
            holder.statusTxt.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            holder.statusTxt.setBackgroundResource(R.drawable.status_finalized_background);
        } else if ("CANCELLED".equals(booking.getStatus())) {
            holder.statusTxt.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            holder.statusTxt.setBackgroundResource(R.drawable.status_cancelled_background);
        }

        // Set click listeners
        holder.rebookBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRebookClick(booking);
            }
        });

        holder.viewDetailsBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetailsClick(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public void updateBookings(List<BookingHistoryItem> newBookings) {
        this.bookingList = newBookings;
        notifyDataSetChanged();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView bookingIdTxt, hotelNameTxt, placeTypeTxt, amountTxt, paymentMethodTxt, bookingDateTxt, statusTxt;
        Button rebookBtn, viewDetailsBtn;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            bookingIdTxt = itemView.findViewById(R.id.bookingIdTxt);
            hotelNameTxt = itemView.findViewById(R.id.hotelNameTxt);
            placeTypeTxt = itemView.findViewById(R.id.placeTypeTxt);
            amountTxt = itemView.findViewById(R.id.amountTxt);
            paymentMethodTxt = itemView.findViewById(R.id.paymentMethodTxt);
            bookingDateTxt = itemView.findViewById(R.id.bookingDateTxt);
            statusTxt = itemView.findViewById(R.id.statusTxt);
            rebookBtn = itemView.findViewById(R.id.rebookBtn);
            viewDetailsBtn = itemView.findViewById(R.id.viewDetailsBtn);
        }
    }
}
