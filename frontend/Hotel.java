package com.example.staygeniefrontend;

public class Hotel {
    private final String id;
    private final String name;
    private final String price;
    private final int imageResId;
    private final String location;
    private final double rating;
    private final String amenities;
    private final String type; // luxury, budget, business, resort, etc.

    public Hotel(String name, String price) {
        this(null, name, price, 0, "", 0.0, "", "");
    }

    public Hotel(String name, String price, int imageResId) {
        this(null, name, price, imageResId, "", 0.0, "", "");
    }

    public Hotel(String id, String name, String price, int imageResId, String location, double rating, String amenities, String type) {
        this.id = id != null ? id : "hotel_" + System.currentTimeMillis();
        this.name = name;
        this.price = price;
        this.imageResId = imageResId;
        this.location = location;
        this.rating = rating;
        this.amenities = amenities;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getLocation() {
        return location;
    }

    public double getRating() {
        return rating;
    }

    public String getAmenities() {
        return amenities;
    }

    public String getType() {
        return type;
    }
}
