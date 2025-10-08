package com.example.staygeniefrontend;

public class Resort {
    private String id;
    private String name;
    private String location;
    private String price;
    private String rating;
    private int imageResId;
    private String amenities;
    private String type; // beach, mountain, tropical, luxury, family, etc.
    private String features; // spa, pool, beach access, ski access, etc.

    public Resort(String name, String location, String price, String rating, int imageResId) {
        this(null, name, location, price, rating, imageResId, "", "", "");
    }

    public Resort(String id, String name, String location, String price, String rating, int imageResId, String amenities, String type, String features) {
        this.id = id != null ? id : "resort_" + System.currentTimeMillis();
        this.name = name;
        this.location = location;
        this.price = price;
        this.rating = rating;
        this.imageResId = imageResId;
        this.amenities = amenities;
        this.type = type;
        this.features = features;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getPrice() { return price; }
    public String getRating() { return rating; }
    public int getImageResId() { return imageResId; }
    public String getAmenities() { return amenities; }
    public String getType() { return type; }
    public String getFeatures() { return features; }
}
