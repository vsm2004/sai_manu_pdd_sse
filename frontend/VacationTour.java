package com.example.staygeniefrontend;

public class VacationTour {
    private final String id;
    private final String name;
    private final String location;
    private final String price;
    private final String duration; // e.g., 5 days, 10 nights
    private final int imageResId;
    private final String type; // adventure, cultural, beach, mountain, city, cruise, safari
    private final String highlights; // key highlights/amenities

    public VacationTour(String id, String name, String location, String price, String duration, int imageResId, String type, String highlights) {
        this.id = id != null ? id : "vacation_" + System.currentTimeMillis();
        this.name = name;
        this.location = location;
        this.price = price;
        this.duration = duration;
        this.imageResId = imageResId;
        this.type = type;
        this.highlights = highlights;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getPrice() { return price; }
    public String getDuration() { return duration; }
    public int getImageResId() { return imageResId; }
    public String getType() { return type; }
    public String getHighlights() { return highlights; }
}


