package com.example.staygeniefrontend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HotelAdapter extends RecyclerView.Adapter<HotelAdapter.HotelViewHolder> {

    public interface OnHotelClickListener {
        void onHotelClick(Hotel hotel);
    }

    private final List<Hotel> hotels;
    private final OnHotelClickListener listener;

    public HotelAdapter(List<Hotel> hotels, OnHotelClickListener listener) {
        this.hotels = hotels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HotelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.hotel_item, parent, false);
        return new HotelViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HotelViewHolder holder, int position) {
        Hotel hotel = hotels.get(position);
        holder.name.setText(hotel.getName());
        holder.price.setText(hotel.getPrice());

        if (hotel.getImageResId() != 0) {
            holder.image.setImageResource(hotel.getImageResId());
        } else {
            holder.image.setImageResource(R.drawable.hotel_one); // fallback
        }

        holder.bookBtn.setOnClickListener(v -> {
            if (listener != null) listener.onHotelClick(hotel);
        });
    }

    @Override
    public int getItemCount() {
        return hotels.size();
    }

    static class HotelViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView price;
        final Button bookBtn;

        HotelViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.hotel_image);
            name = itemView.findViewById(R.id.hotel_name);
            price = itemView.findViewById(R.id.hotel_price);
            bookBtn = itemView.findViewById(R.id.book_btn);
        }
    }
}
