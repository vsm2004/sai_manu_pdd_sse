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

public class VacationTourAdapter extends RecyclerView.Adapter<VacationTourAdapter.TourViewHolder> {

    public interface OnBookClickListener { void onBookClick(VacationTour tour); }

    private final Context context;
    private final List<VacationTour> tours;
    private final OnBookClickListener listener;

    public VacationTourAdapter(Context context, List<VacationTour> tours, OnBookClickListener listener) {
        this.context = context;
        this.tours = tours;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_vacation_tour, parent, false);
        return new TourViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TourViewHolder holder, int position) {
        VacationTour tour = tours.get(position);
        holder.name.setText(tour.getName());
        holder.location.setText(tour.getLocation());
        holder.price.setText(tour.getPrice());
        holder.duration.setText(tour.getDuration());
        if (tour.getImageResId() != 0) {
            holder.image.setImageResource(tour.getImageResId());
        }

        holder.bookBtn.setOnClickListener(v -> {
            if (listener != null) listener.onBookClick(tour);
        });
    }

    @Override
    public int getItemCount() { return tours.size(); }

    static class TourViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView location;
        final TextView price;
        final TextView duration;
        final Button bookBtn;

        TourViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.tour_image);
            name = itemView.findViewById(R.id.tour_name);
            location = itemView.findViewById(R.id.tour_location);
            price = itemView.findViewById(R.id.tour_price);
            duration = itemView.findViewById(R.id.tour_duration);
            bookBtn = itemView.findViewById(R.id.btn_book_tour);
        }
    }
}



