package com.example.staygeniefrontend;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BusinessHotelsCatalogue extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BusinessHotelAdapter adapter;
    private List<BusinessHotel> allHotels;
    private List<BusinessHotel> filteredHotels;
    
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String[] BACKEND_URLS = Config.PHP_BASE_URLS;
    private static final String BUSINESS_ROOMS_URL = BACKEND_URLS[0] + "/businesstrips.php";//url is changed to business trips
    private static final String[] FLASK_SEARCH_URLS = {
        Config.FLASK_BASE_URLS[0] + "/search",
        Config.FLASK_BASE_URLS[1] + "/search",
        Config.FLASK_BASE_URLS[2] + "/search"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_business_hotels_catalogue);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recycler_view_hotels);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize empty lists
        allHotels = new ArrayList<>();
        filteredHotels = new ArrayList<>();

        adapter = new BusinessHotelAdapter(this, filteredHotels, hotel -> {
            Intent intent = new Intent(BusinessHotelsCatalogue.this, BookingTheStayArea.class);
            intent.putExtra("hotel_name", hotel.getName());
            intent.putExtra("price", hotel.getPrice());
            intent.putExtra("place_type", "business");
            intent.putExtra("place_id", hotel.getId()); // Assuming BusinessHotel class has getId() method
            intent.putExtra("location", hotel.getLocation()); // Add location data
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        String destination = getIntent().getStringExtra("destination");
        prefetchFromFlask("business", destination);
        // PRIORITY: Load Flask cache first (destination-specific data)
        loadBusinessFromFlaskCacheIfAvailable();
        if (allHotels.isEmpty()) loadBusinessRoomsFromAPI();

        // AI Chat & Voicebox buttons for Business catalogue
        Button chatBtn = findViewById(R.id.btn_ai_genie_chat_business);
        if (chatBtn != null) {
            chatBtn.setOnClickListener(v -> {
                Intent i = new Intent(BusinessHotelsCatalogue.this, AiGenieChatBot.class);
                i.putExtra("source", "business");
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        Button voiceBtn = findViewById(R.id.btn_ai_genie_voicebox_business);
        if (voiceBtn != null) {
            voiceBtn.setOnClickListener(v -> {
                Intent i = new Intent(BusinessHotelsCatalogue.this, AIGenieVoiceBox.class);
                i.putExtra("source", "business");
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }
    }

    private void prefetchFromFlask(String stayType, String destination) {
        try {
            org.json.JSONObject payload = new org.json.JSONObject();
            payload.put("destination", destination != null ? destination : "");
            payload.put("stay_type", stayType);
            payload.put("check_in", "2024-12-01");
            payload.put("check_out", "2024-12-03");
            okhttp3.RequestBody body = okhttp3.RequestBody.create(payload.toString(), okhttp3.MediaType.get("application/json; charset=utf-8"));
            okhttp3.Request req = new okhttp3.Request.Builder().url(FLASK_SEARCH_URLS[0]).post(body).build();
            httpClient.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {}
                @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    if (!response.isSuccessful()) return;
                    String res = response.body() != null ? response.body().string() : "";
                    getSharedPreferences("StayGenieCache", MODE_PRIVATE)
                            .edit()
                            .putString("accommodations_" + stayType, res)
                            .apply();
                    runOnUiThread(() -> loadBusinessFromFlaskCacheIfAvailable());
                }
            });
        } catch (Exception ignored) {}
    }

    private void loadBusinessFromFlaskCacheIfAvailable() {
        try {
            String cached = getSharedPreferences("StayGenieCache", MODE_PRIVATE)
                    .getString("accommodations_business", null);
            android.util.Log.d("BusinessHotelsCatalogue", "Flask cache for business: " + (cached != null ? "found" : "null"));
            if (cached == null || cached.trim().isEmpty()) {
                android.util.Log.d("BusinessHotelsCatalogue", "No Flask cache found for business");
                return;
            }

            JSONObject res = new JSONObject(cached);
            android.util.Log.d("BusinessHotelsCatalogue", "Flask cache status: " + res.optBoolean("status", false));
            if (!res.optBoolean("status", false)) {
                android.util.Log.d("BusinessHotelsCatalogue", "Flask cache status is false");
                return;
            }
            JSONArray results = res.optJSONArray("results");
            android.util.Log.d("BusinessHotelsCatalogue", "Flask cache results count: " + (results != null ? results.length() : 0));
            if (results == null || results.length() == 0) {
                android.util.Log.d("BusinessHotelsCatalogue", "No results in Flask cache");
                return;
            }

            List<BusinessHotel> apiHotels = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.optJSONObject(i);
                if (item == null) continue;
                BusinessHotel hotel = new BusinessHotel(
                        "business_flask_" + i,
                        item.optString("name", "Unknown Business Hotel"),
                        item.optString("price", "$0"),
                        item.optString("location", ""),
                        item.optDouble("rating", 4.0),
                        item.optString("amenities", ""),
                        false,
                        false,
                        R.drawable.hotel_room
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

    private void loadBusinessRoomsFromAPI() {
        String destination = getIntent().getStringExtra("destination");
        String url = BUSINESS_ROOMS_URL;
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
                        JSONArray businessStaysArray = jsonResponse.optJSONArray("data");
                        List<BusinessHotel> apiHotels = new ArrayList<>();
                        
                        if (businessStaysArray != null) {
                            for (int i = 0; i < businessStaysArray.length(); i++) {
                                JSONObject businessStayJson = businessStaysArray.optJSONObject(i);
                                if (businessStayJson != null) {
                                    BusinessHotel hotel = new BusinessHotel(
                                        businessStayJson.optString("id", "business_" + i),
                                        businessStayJson.optString("name", "Unknown Business Hotel"),
                                        businessStayJson.optString("price", "$0"),
                                        businessStayJson.optString("location", ""),
                                        businessStayJson.optDouble("rating", 4.0),
                                        businessStayJson.optString("amenities", ""),
                                        businessStayJson.optBoolean("meeting_rooms", false),
                                        businessStayJson.optBoolean("airport_shuttle", false),
                                        R.drawable.hotel_room // Default image, you can map this based on business stay data
                                    );
                                    apiHotels.add(hotel);
                                }
                            }
                        }
                        
                        runOnUiThread(() -> {
                            if (apiHotels.isEmpty()) {
                               // Toast.makeText(BusinessHotelsCatalogue.this, "No results from API. Using offline data.", Toast.LENGTH_SHORT).show();
                                loadFallbackHotels();
                            } else {
                                allHotels.clear();
                                allHotels.addAll(apiHotels);
                                filteredHotels.clear();
                                filteredHotels.addAll(allHotels);
                                adapter.notifyDataSetChanged();
                                String message = jsonResponse.optString("message", "Loaded business hotels");
                                Toast.makeText(BusinessHotelsCatalogue.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            String message = jsonResponse.optString("message", "Failed to load business hotels");
                            Toast.makeText(BusinessHotelsCatalogue.this, "API Error: " + message, Toast.LENGTH_SHORT).show();
                            loadFallbackHotels();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(BusinessHotelsCatalogue.this, "Network error. Using offline data.", Toast.LENGTH_SHORT).show();
                        loadFallbackHotels();
                    });
                }
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> {
                    Toast.makeText(BusinessHotelsCatalogue.this, "Error loading business rooms. Using offline data.", Toast.LENGTH_SHORT).show();
                    loadFallbackHotels();
                });
            }
        }).start();
    }

    private void loadFallbackHotels() {
        // Fallback to static data if API fails
        allHotels.clear();
        allHotels.add(new BusinessHotel("business_1", "Executive Suites", "$279", "Financial District", 4.6,
                "WiFi, Business Center, Gym, Spa", true, false, R.drawable.hotel_room));
        allHotels.add(new BusinessHotel("business_2", "Business Center Hotel", "$189", "Business District", 4.3,
                "WiFi, Meeting Rooms, Gym", true, false, R.drawable.hotel_one));
        allHotels.add(new BusinessHotel("business_3", "Airport Express Inn", "$149", "Airport Zone", 4.1,
                "WiFi, Shuttle, Meeting Rooms", true, true, R.drawable.hotel_two));
        allHotels.add(new BusinessHotel("business_4", "City Conference Hotel", "$209", "Downtown", 4.4,
                "WiFi, Conference Hall, Gym", true, false, R.drawable.hotel_motel_interior));
        
        filteredHotels.clear();
        filteredHotels.addAll(allHotels);
        adapter.notifyDataSetChanged();
    }

    private void filterHotels(String userPreferences) {
        if (userPreferences == null || userPreferences.trim().isEmpty()) {
            filteredHotels.clear();
            filteredHotels.addAll(allHotels);
            adapter.notifyDataSetChanged();
            return;
        }

        filteredHotels.clear();
        String preferences = userPreferences.toLowerCase();
        for (BusinessHotel hotel : allHotels) {
            boolean matches = false;

            // Basic text matching
            if (hotel.getName().toLowerCase().contains(preferences) ||
                    hotel.getLocation().toLowerCase().contains(preferences) ||
                    hotel.getAmenities().toLowerCase().contains(preferences)) {
                matches = true;
            }

            // Enhanced keyword matching
            if (preferences.contains("business")) matches = true;
            if (preferences.contains("meeting") && hotel.getAmenities().toLowerCase().contains("meeting")) matches = true;
            if (preferences.contains("conference") && hotel.getAmenities().toLowerCase().contains("conference")) matches = true;
            if (preferences.contains("shuttle") && (hotel.getAmenities().toLowerCase().contains("shuttle") || hotel.hasAirportShuttle())) matches = true;
            if (preferences.contains("gym") && hotel.getAmenities().toLowerCase().contains("gym")) matches = true;
            if (preferences.contains("wifi") && hotel.getAmenities().toLowerCase().contains("wifi")) matches = true;
            if (preferences.contains("spa") && hotel.getAmenities().toLowerCase().contains("spa")) matches = true;
            if (preferences.contains("pool") && hotel.getAmenities().toLowerCase().contains("pool")) matches = true;
            if (preferences.contains("restaurant") && hotel.getAmenities().toLowerCase().contains("restaurant")) matches = true;
            
            // Location-based filtering
            if (preferences.contains("downtown") && hotel.getLocation().toLowerCase().contains("downtown")) matches = true;
            if (preferences.contains("airport") && hotel.getLocation().toLowerCase().contains("airport")) matches = true;
            if (preferences.contains("financial") && hotel.getLocation().toLowerCase().contains("financial")) matches = true;
            if (preferences.contains("business district") && hotel.getLocation().toLowerCase().contains("business")) matches = true;
            if (preferences.contains("city center") && hotel.getLocation().toLowerCase().contains("center")) matches = true;
            
            // Price-based filtering
            if (preferences.contains("budget") && hotel.getPrice().toLowerCase().contains("149")) matches = true;
            if (preferences.contains("luxury") && hotel.getPrice().toLowerCase().contains("279")) matches = true;
            if (preferences.contains("expensive") && hotel.getPrice().toLowerCase().contains("279")) matches = true;
            if (preferences.contains("cheap") && hotel.getPrice().toLowerCase().contains("149")) matches = true;

            if (matches) filteredHotels.add(hotel);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check Flask cache in case new data was prefetched
        loadBusinessFromFlaskCacheIfAvailable();
        
        String prefs = getIntent().getStringExtra("userPreferences");
        if (prefs != null) {
            Toast.makeText(this, "Personalized business results for: " + prefs, Toast.LENGTH_LONG).show();
            filterHotels(prefs);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String prefs = intent.getStringExtra("userPreferences");
        if (prefs != null) {
            Toast.makeText(this, "Personalized business results for: " + prefs, Toast.LENGTH_LONG).show();
            filterHotels(prefs);
        }
    }
}