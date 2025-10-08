package com.example.staygeniefrontend;

public class BusinessHotel {
    private final String id;
    private final String name;
    private final String price;
    private final String location;
    private final double rating;
    private final String amenities; // WiFi, Meeting Rooms, Conference Hall, Shuttle, Gym
    private final boolean meetingRooms;
    private final boolean airportShuttle;
    private final int imageResId;

    public BusinessHotel(String id,
                         String name,
                         String price,
                         String location,
                         double rating,
                         String amenities,
                         boolean meetingRooms,
                         boolean airportShuttle,
                         int imageResId) {
        this.id = id != null ? id : "business_" + System.currentTimeMillis();
        this.name = name;
        this.price = price;
        this.location = location;
        this.rating = rating;
        this.amenities = amenities;
        this.meetingRooms = meetingRooms;
        this.airportShuttle = airportShuttle;
        this.imageResId = imageResId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getLocation() { return location; }
    public double getRating() { return rating; }
    public String getAmenities() { return amenities; }
    public boolean hasMeetingRooms() { return meetingRooms; }
    public boolean hasAirportShuttle() { return airportShuttle; }
    public int getImageResId() { return imageResId; }
}


