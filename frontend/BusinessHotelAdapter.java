package com.example.staygeniefrontend;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BusinessHotelAdapter extends RecyclerView.Adapter<BusinessHotelAdapter.VH> {

    public interface OnBusinessHotelClickListener { void onClick(BusinessHotel hotel); }

    private final Context context;
    private final List<BusinessHotel> hotels;
    private final OnBusinessHotelClickListener listener;

    public BusinessHotelAdapter(Context context, List<BusinessHotel> hotels, OnBusinessHotelClickListener listener) {
        this.context = context;
        this.hotels = hotels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.hotel_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BusinessHotel hotel = hotels.get(position);
        holder.name.setText(hotel.getName());
        holder.price.setText(hotel.getPrice());
        if (hotel.getImageResId() != 0) holder.image.setImageResource(hotel.getImageResId());

        holder.bookBtn.setOnClickListener(v -> {
            if (listener != null) listener.onClick(hotel);
        });
    }

    @Override
    public int getItemCount() { return hotels.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView price;
        final Button bookBtn;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.hotel_image);
            name = itemView.findViewById(R.id.hotel_name);
            price = itemView.findViewById(R.id.hotel_price);
            bookBtn = itemView.findViewById(R.id.book_btn);
        }
    }
}


