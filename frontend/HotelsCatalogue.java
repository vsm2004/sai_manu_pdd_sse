package com.example.staygeniefrontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HotelsCatalogue extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HotelAdapter adapter;
    private List<Hotel> allHotels; // Original list of all hotels
    private List<Hotel> filteredHotels; // Filtered list for display

    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String[] FLASK_SEARCH_URLS = {
        Config.FLASK_BASE_URLS[0] + "/search",
        Config.FLASK_BASE_URLS[1] + "/search",
        Config.FLASK_BASE_URLS[2] + "/search"
    };
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    // TODO: Set to your deployed PHP backend URL
    private static final String[] BACKEND_URLS = Config.PHP_BASE_URLS;
    private static final String HOTELS_URL = BACKEND_URLS[0] + "/hotels.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotels_catalogue);

        recyclerView = findViewById(R.id.recycler_view_hotels);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize empty lists first
        allHotels = new ArrayList<>();
        filteredHotels = new ArrayList<>();

        adapter = new HotelAdapter(filteredHotels, hotel -> {
            // Handle hotel click - navigate to common booking flow
            Intent intent = new Intent(HotelsCatalogue.this, BookingTheStayArea.class);
            intent.putExtra("hotel_name", hotel.getName());
            intent.putExtra("price", hotel.getPrice());
            intent.putExtra("place_type", "hotel");
            intent.putExtra("place_id", hotel.getId()); // Assuming Hotel class has getId() method
            intent.putExtra("location", hotel.getLocation()); // Add location data
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // Trigger Flask generation for destination and type, then read cache
        String destination = getIntent().getStringExtra("destination");
        prefetchFromFlask("hotel", destination);
        // PRIORITY: Load Flask cache first (destination-specific data)
        loadHotelsFromFlaskCacheIfAvailable();
        // FALLBACK: Load hotels from PHP only if Flask cache is empty
        if (allHotels.isEmpty()) loadHotelsFromAPI();

        // If opened with personalized preferences, apply immediately
        String initialPrefs = getIntent().getStringExtra("userPreferences");
        if (initialPrefs != null) {
            filterHotels(initialPrefs);
        }

        // AI Chat button should call AiGenieChatBot; when that activity returns with extras,
        // getIntent().getStringExtra("userPreferences") can be read to append AI suggestions.
        Button chatBtn = findViewById(R.id.btn_ai_genie_chat);
        chatBtn.setOnClickListener(v -> {
            Intent chatIntent = new Intent(HotelsCatalogue.this, AiGenieChatBot.class);
            chatIntent.putExtra("source", "hotels");
            startActivity(chatIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        // AI Voicebox button should call AIGenieVoiceBox
        Button voiceBtn = findViewById(R.id.btn_ai_genie_voicebox);
        voiceBtn.setOnClickListener(v -> {
            Intent voiceIntent = new Intent(HotelsCatalogue.this, AIGenieVoiceBox.class);
            voiceIntent.putExtra("source", "hotels");
            startActivity(voiceIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }

    private void prefetchFromFlask(String stayType, String destination) {
        try {
            // Use GET request with query parameters as Flask API expects
            String url = FLASK_SEARCH_URLS[0] + "?destination=" + 
                java.net.URLEncoder.encode(destination != null ? destination : "", "UTF-8") + 
                "&type=" + java.net.URLEncoder.encode(stayType, "UTF-8");
            Request req = new Request.Builder().url(url).get().build();
            httpClient.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    android.util.Log.e("HotelsCatalogue", "Flask API request failed: " + e.getMessage());
                }
                @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    android.util.Log.d("HotelsCatalogue", "Flask API response code: " + response.code());
                    if (!response.isSuccessful()) {
                        android.util.Log.e("HotelsCatalogue", "Flask API response not successful: " + response.code());
                        return;
                    }
                    String res = response.body() != null ? response.body().string() : "";
                    android.util.Log.d("HotelsCatalogue", "Flask API response: " + res);
                    getSharedPreferences("StayGenieCache", MODE_PRIVATE)
                            .edit()
                            .putString("accommodations_" + stayType, res)
                            .apply();
                    runOnUiThread(() -> loadHotelsFromFlaskCacheIfAvailable());
                }
            });
        } catch (Exception ignored) {}
    }

    private void loadHotelsFromFlaskCacheIfAvailable() {
        try {
            String cached = getSharedPreferences("StayGenieCache", MODE_PRIVATE)
                    .getString("accommodations_hotel", null);
            android.util.Log.d("HotelsCatalogue", "Flask cache for hotels: " + (cached != null ? "found" : "null"));
            if (cached == null || cached.trim().isEmpty()) {
                android.util.Log.d("HotelsCatalogue", "No Flask cache found for hotels");
                return;
            }

            JSONObject res = new JSONObject(cached);
            android.util.Log.d("HotelsCatalogue", "Flask cache response: " + res.toString());
            if (!res.optBoolean("status", false)) {
                android.util.Log.d("HotelsCatalogue", "Flask cache status is false");
                return;
            }
            JSONArray results = res.optJSONArray("results");
            android.util.Log.d("HotelsCatalogue", "Flask cache results count: " + (results != null ? results.length() : 0));
            if (results == null || results.length() == 0) {
                android.util.Log.d("HotelsCatalogue", "No results in Flask cache");
                return;
            }

            List<Hotel> apiHotels = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.optJSONObject(i);
                if (item == null) continue;
                String name = item.optString("name", "Unknown Hotel");
                String price = item.optString("price", "$0");
                String location = item.optString("location", "");
                double rating = 0.0;
                try { rating = item.optDouble("rating", 0.0); } catch (Exception ignored) {}
                String type = item.optString("type", "Hotel");
                String amenities;
                if (item.has("amenities") && item.opt("amenities") instanceof JSONArray) {
                    JSONArray am = item.optJSONArray("amenities");
                    StringBuilder sb = new StringBuilder();
                    if (am != null) {
                        for (int j = 0; j < am.length(); j++) {
                            if (j > 0) sb.append(", ");
                            sb.append(am.optString(j));
                        }
                    }
                    amenities = sb.toString();
                } else {
                    amenities = item.optString("amenities", "");
                }

                Hotel hotel = new Hotel(
                        "hotel_flask_" + i,
                        name,
                        price,
                        R.drawable.hotel_one,
                        location,
                        rating,
                        amenities,
                        type
                );
                apiHotels.add(hotel);
            }

            runOnUiThread(() -> {
                allHotels.clear();
                allHotels.addAll(apiHotels);
                filteredHotels.clear();
                filteredHotels.addAll(allHotels);
                if (adapter != null) adapter.notifyDataSetChanged();
            });
        } catch (Exception ignored) { }
    }

    private void loadHotelsFromAPI() {
        // Get destination from intent or use default
        String destination = getIntent().getStringExtra("destination");
        
        // Build URL with destination parameter
        String url = HOTELS_URL;
        if (destination != null && !destination.isEmpty()) {
            url += "?destination=" + destination;
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    if (jsonResponse.optBoolean("status", false)) {
                        JSONArray hotelsData = jsonResponse.optJSONArray("data");
                        List<Hotel> apiHotels = new ArrayList<>();
                        
                        if (hotelsData != null) {
                            for (int i = 0; i < hotelsData.length(); i++) {
                                JSONObject hotelJson = hotelsData.optJSONObject(i);
                                if (hotelJson != null) {
                                    Hotel hotel = new Hotel(
                                        hotelJson.optString("id", "hotel_" + i),
                                        hotelJson.optString("name", "Unknown Hotel"),
                                        hotelJson.optString("price", "$0"),
                                        R.drawable.hotel_one, // Default image - you can map from hotel data
                                        hotelJson.optString("location", ""),
                                        hotelJson.optDouble("rating", 0.0),
                                        hotelJson.optString("amenities", ""),
                                        "Hotel" // Type is always hotel for this API
                                    );
                                    apiHotels.add(hotel);
                                }
                            }
                        }
                        
                        // Update UI on main thread
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (apiHotels.isEmpty()) {
                              //  Toast.makeText(HotelsCatalogue.this, "No results from API. Using offline data.", Toast.LENGTH_SHORT).show();
                                loadFallbackHotels();
                            } else {
                                allHotels.clear();
                                allHotels.addAll(apiHotels);
                                filteredHotels.clear();
                                filteredHotels.addAll(allHotels);
                                adapter.notifyDataSetChanged();
                                String message = jsonResponse.optString("message", "Loaded hotels");
                                Toast.makeText(HotelsCatalogue.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // API returned error, use fallback data
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(HotelsCatalogue.this, "API error. Using offline data.", Toast.LENGTH_SHORT).show();
                            loadFallbackHotels();
                        });
                    }
                } else {
                    // HTTP error, use fallback data
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(HotelsCatalogue.this, "Network error. Using offline data.", Toast.LENGTH_SHORT).show();
                        loadFallbackHotels();
                    });
                }
            } catch (Exception e) {
                // Network error, use fallback data
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(HotelsCatalogue.this, "Network error. Using offline data.", Toast.LENGTH_SHORT).show();
                    loadFallbackHotels();
                });
            }
        }).start();
    }

    private void loadFallbackHotels() {
        // Fallback to static data if API fails
        allHotels.clear();
        allHotels.add(new Hotel("hotel_1", "Grand Plaza Hotel", "$199", R.drawable.hotel_one,
                "Downtown Center", 4.5, "WiFi, Pool, Gym, Spa", "Luxury"));
        allHotels.add(new Hotel("hotel_2", "Ocean View Resort", "$249", R.drawable.hotel_two,
                "Miami Beach", 4.7, "WiFi, Pool, Beach Access, Restaurant", "Resort"));
        allHotels.add(new Hotel("hotel_3", "City Light Inn", "$149", R.drawable.hotel_motel_interior,
                "North Carolina", 4.2, "WiFi, Parking, Restaurant", "Budget"));
        allHotels.add(new Hotel("hotel_4", "Business Center Hotel", "$189", R.drawable.hotel_room,
                "Business District", 4.3, "WiFi, Meeting Rooms, Gym", "Business"));
        allHotels.add(new Hotel("hotel_5", "Mountain Retreat Lodge", "$299", R.drawable.hotel_one,
                "Aspen, CO", 4.8, "WiFi, Spa, Ski Access, Restaurant", "Luxury"));
        allHotels.add(new Hotel("hotel_6", "Cozy Budget Inn", "$99", R.drawable.hotel_two,
                "City Center", 3.8, "WiFi, Parking", "Budget"));
        allHotels.add(new Hotel("hotel_7", "Family Resort", "$219", R.drawable.hotel_motel_interior,
                "Orlando, FL", 4.4, "WiFi, Pool, Kids Club, Restaurant", "Family"));
        allHotels.add(new Hotel("hotel_8", "Executive Suites", "$279", R.drawable.hotel_room,
                "Financial District", 4.6, "WiFi, Business Center, Gym, Spa", "Business"));
        
        filteredHotels.clear();
        filteredHotels.addAll(allHotels);
        adapter.notifyDataSetChanged();
    }

    private void filterHotels(String userPreferences) {
        if (userPreferences == null || userPreferences.trim().isEmpty()) {
            filteredHotels = new ArrayList<>(allHotels);
            adapter.notifyDataSetChanged();
            return;
        }

        filteredHotels.clear();
        String preferences = userPreferences.toLowerCase();

        for (Hotel hotel : allHotels) {
            boolean matches = false;

            // Check if hotel matches user preferences
            if (hotel.getName().toLowerCase().contains(preferences) ||
                    hotel.getLocation().toLowerCase().contains(preferences) ||
                    hotel.getAmenities().toLowerCase().contains(preferences) ||
                    hotel.getType().toLowerCase().contains(preferences)) {
                matches = true;
            }

            // Special keyword matching for common preferences
            if (preferences.contains("luxury") && hotel.getType().equalsIgnoreCase("Luxury")) {
                matches = true;
            }
            if (preferences.contains("budget") && hotel.getType().equalsIgnoreCase("Budget")) {
                matches = true;
            }
            if (preferences.contains("business") && hotel.getType().equalsIgnoreCase("Business")) {
                matches = true;
            }
            if (preferences.contains("resort") && hotel.getType().equalsIgnoreCase("Resort")) {
                matches = true;
            }
            if (preferences.contains("family") && hotel.getType().equalsIgnoreCase("Family")) {
                matches = true;
            }
            if (preferences.contains("pool") && hotel.getAmenities().toLowerCase().contains("pool")) {
                matches = true;
            }
            if (preferences.contains("spa") && hotel.getAmenities().toLowerCase().contains("spa")) {
                matches = true;
            }
            if (preferences.contains("gym") && hotel.getAmenities().toLowerCase().contains("gym")) {
                matches = true;
            }
            if (preferences.contains("miami") && hotel.getLocation().toLowerCase().contains("miami")) {
                matches = true;
            }
            if (preferences.contains("orlando") && hotel.getLocation().toLowerCase().contains("orlando")) {
                matches = true;
            }
            if (preferences.contains("aspen") && hotel.getLocation().toLowerCase().contains("aspen")) {
                matches = true;
            }

            if (matches) {
                filteredHotels.add(hotel);
            }
        }

        // Update adapter with filtered results
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check Flask cache in case new data was prefetched
        loadHotelsFromFlaskCacheIfAvailable();
        
        String prefs = getIntent().getStringExtra("userPreferences");
        if (prefs != null) {
            Toast.makeText(this, "Personalized hotel results for: " + prefs, Toast.LENGTH_LONG).show();
            filterHotels(prefs);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String prefs = intent.getStringExtra("userPreferences");
        if (prefs != null) {
            Toast.makeText(this, "Personalized results for: " + prefs, Toast.LENGTH_LONG).show();
            filterHotels(prefs);
        }
    }
}
