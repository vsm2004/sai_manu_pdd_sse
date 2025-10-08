package com.example.staygeniefrontend;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ResortAdapter extends RecyclerView.Adapter<ResortAdapter.ResortViewHolder> {

    private Context context;
    private List<Resort> resortList;
    private OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(Resort resort);
    }

    public ResortAdapter(Context context, List<Resort> resortList, OnBookClickListener listener) {
        this.context = context;
        this.resortList = resortList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResortViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_resort_card, parent, false);
        return new ResortViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResortViewHolder holder, int position) {
        Resort resort = resortList.get(position);
        holder.name.setText(resort.getName());
        holder.location.setText(resort.getLocation());
        holder.price.setText(resort.getPrice());
        holder.image.setImageResource(resort.getImageResId());

        // ✅ Set rating correctly as float
        try {
            holder.rating.setRating(Float.parseFloat(resort.getRating()));
        } catch (NumberFormatException e) {
            holder.rating.setRating(0); // fallback if rating string is invalid
        }

        holder.bookButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookClick(resort);
            }
        });
    }

    @Override
    public int getItemCount() {
        return resortList.size();
    }

    static class ResortViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, location, price;
        RatingBar rating;
        Button bookButton;

        ResortViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.resort_image);
            name = itemView.findViewById(R.id.resort_name);
            location = itemView.findViewById(R.id.resort_location);
            price = itemView.findViewById(R.id.resort_price);
            rating = itemView.findViewById(R.id.resort_rating); // ✅ RatingBar now
            bookButton = itemView.findViewById(R.id.btn_book_resort);
        }
    }
}
