package com.example.staygeniefrontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ResortsCatalogue extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ResortAdapter adapter;
    private List<Resort> allResorts;
    private List<Resort> filteredResorts;
    
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String[] BACKEND_URLS = Config.PHP_BASE_URLS;
    private static final String RESORTS_URL = BACKEND_URLS[0] + "/resorts.php";
    private static final String[] FLASK_SEARCH_URLS = {
        Config.FLASK_BASE_URLS[0] + "/search",
        Config.FLASK_BASE_URLS[1] + "/search",
        Config.FLASK_BASE_URLS[2] + "/search"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resorts_catalogue);

        recyclerView = findViewById(R.id.recycler_view_hotels);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize empty lists
        allResorts = new ArrayList<>();
        filteredResorts = new ArrayList<>();

        adapter = new ResortAdapter(this, filteredResorts, resort -> {
            // Navigate to common booking flow
            Intent intent = new Intent(ResortsCatalogue.this, BookingTheStayArea.class);
            intent.putExtra("hotel_name", resort.getName());
            intent.putExtra("price", resort.getPrice());
            intent.putExtra("place_type", "resort");
            intent.putExtra("place_id", resort.getId()); // Assuming Resort class has getId() method
            intent.putExtra("location", resort.getLocation()); // Add location data
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        String destination = getIntent().getStringExtra("destination");
        prefetchFromFlask("resort", destination);
        // PRIORITY: Load Flask cache first (destination-specific data)
        loadResortsFromFlaskCacheIfAvailable();
        if (allResorts.isEmpty()) loadResortsFromAPI();

        // Apply personalized filters if present at launch
        String initialPrefs = getIntent().getStringExtra("userPreferences");
        if (initialPrefs != null) {
            filterResorts(initialPrefs);
        }

        // Updated IDs for AI-Genie buttons
        Button chatBtn = findViewById(R.id.btn_ai_genie_chat_resorts);
        if (chatBtn != null) {
            chatBtn.setOnClickListener(v -> {
                Intent intent = new Intent(ResortsCatalogue.this, AiGenieChatBot.class);
                intent.putExtra("source", "resorts");
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        Button voiceBtn = findViewById(R.id.btn_ai_genie_voicebox_resorts);
        if (voiceBtn != null) {
            voiceBtn.setOnClickListener(v -> {
                Intent intent = new Intent(ResortsCatalogue.this, AIGenieVoiceBox.class);
                intent.putExtra("source", "resorts");
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }
    }

    private void prefetchFromFlask(String stayType, String destination) {
        try {
            // Use GET request with query parameters as Flask API expects
            String url = FLASK_SEARCH_URLS[0] + "?destination=" + 
                java.net.URLEncoder.encode(destination != null ? destination : "", "UTF-8") + 
                "&type=" + java.net.URLEncoder.encode(stayType, "UTF-8");
            okhttp3.Request req = new okhttp3.Request.Builder().url(url).get().build();
            httpClient.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {}
                @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    if (!response.isSuccessful()) return;
                    String res = response.body() != null ? response.body().string() : "";
                    getSharedPreferences("StayGenieCache", MODE_PRIVATE)
                            .edit()
                            .putString("accommodations_" + stayType, res)
                            .apply();
                    runOnUiThread(() -> loadResortsFromFlaskCacheIfAvailable());
                }
            });
        } catch (Exception ignored) {}
    }

    private void loadResortsFromFlaskCacheIfAvailable() {
        try {
            String cached = getSharedPreferences("StayGenieCache", MODE_PRIVATE)
                    .getString("accommodations_resort", null);
            android.util.Log.d("ResortsCatalogue", "Flask cache for resorts: " + (cached != null ? "found" : "null"));
            if (cached == null || cached.trim().isEmpty()) {
                android.util.Log.d("ResortsCatalogue", "No Flask cache found for resorts");
                return;
            }

            JSONObject res = new JSONObject(cached);
            android.util.Log.d("ResortsCatalogue", "Flask cache status: " + res.optBoolean("status", false));
            if (!res.optBoolean("status", false)) {
                android.util.Log.d("ResortsCatalogue", "Flask cache status is false");
                return;
            }
            JSONArray results = res.optJSONArray("results");
            android.util.Log.d("ResortsCatalogue", "Flask cache results count: " + (results != null ? results.length() : 0));
            if (results == null || results.length() == 0) {
                android.util.Log.d("ResortsCatalogue", "No results in Flask cache");
                return;
            }

            List<Resort> apiResorts = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.optJSONObject(i);
                if (item == null) continue;
                Resort resort = new Resort(
                        "resort_flask_" + i,
                        item.optString("name", "Unknown Resort"),
                        item.optString("location", ""),
                        item.optString("price", "$0"),
                        String.valueOf(item.optDouble("rating", 0.0)),
                        R.drawable.resorts_image_one,
                        item.optString("amenities", ""),
                        item.optString("type", ""),
                        ""
                );
                apiResorts.add(resort);
            }

            runOnUiThread(() -> {
                allResorts.clear();
                allResorts.addAll(apiResorts);
                filteredResorts.clear();
                filteredResorts.addAll(allResorts);
                if (adapter != null) adapter.notifyDataSetChanged();
            });
        } catch (Exception ignored) { }
    }

    private void loadResortsFromAPI() {
        String destination = getIntent().getStringExtra("destination");
        String url = RESORTS_URL;
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
                        JSONArray resortsArray = jsonResponse.optJSONArray("data");
                        List<Resort> apiResorts = new ArrayList<>();
                        
                        if (resortsArray != null) {
                            for (int i = 0; i < resortsArray.length(); i++) {
                                JSONObject resortJson = resortsArray.optJSONObject(i);
                                if (resortJson != null) {
                                    Resort resort = new Resort(
                                        resortJson.optString("id", "resort_" + i),
                                        resortJson.optString("name", "Unknown Resort"),
                                        resortJson.optString("location", ""),
                                        resortJson.optString("price", "$0"),
                                        resortJson.optString("rating", "0"),
                                        R.drawable.resorts_image_one, // Default image, you can map this based on resort data
                                        resortJson.optString("amenities", ""),
                                        resortJson.optString("type", ""),
                                        resortJson.optString("features", "")
                                    );
                                    apiResorts.add(resort);
                                }
                            }
                        }
                        
                        runOnUiThread(() -> {
                            if (apiResorts.isEmpty()) {
                               // Toast.makeText(ResortsCatalogue.this, "No results from API. Using offline data.", Toast.LENGTH_SHORT).show();
                                loadFallbackResorts();
                            } else {
                                allResorts.clear();
                                allResorts.addAll(apiResorts);
                                filteredResorts.clear();
                                filteredResorts.addAll(allResorts);
                                adapter.notifyDataSetChanged();
                                String message = jsonResponse.optString("message", "Loaded resorts");
                                Toast.makeText(ResortsCatalogue.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            String message = jsonResponse.optString("message", "Failed to load resorts");
                            Toast.makeText(ResortsCatalogue.this, "API Error: " + message, Toast.LENGTH_SHORT).show();
                            loadFallbackResorts();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(ResortsCatalogue.this, "Network error. Using offline data.", Toast.LENGTH_SHORT).show();
                        loadFallbackResorts();
                    });
                }
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> {
                    Toast.makeText(ResortsCatalogue.this, "Error loading resorts. Using offline data.", Toast.LENGTH_SHORT).show();
                    loadFallbackResorts();
                });
            }
        }).start();
    }

    private void loadFallbackResorts() {
        // Fallback to static data if API fails
        allResorts.clear();
        allResorts.add(new Resort("resort_1", "Tropical Horizon", "Buenos Aires", "$200", "4.5", R.drawable.resorts_image_one,
                "WiFi, Pool, Spa, Restaurant, Beach Access", "Tropical", "Beach Access, Spa, Pool"));
        allResorts.add(new Resort("resort_2", "Ocean Pearl Resort", "North Carolina", "$400", "3.8", R.drawable.resorts_four,
                "WiFi, Pool, Gym, Restaurant, Ocean View", "Beach", "Ocean View, Pool, Gym"));
        allResorts.add(new Resort("resort_3", "Sunset Bay Resort", "Miami", "$350", "4.3", R.drawable.resorts_two,
                "WiFi, Pool, Spa, Restaurant, Beach Access", "Beach", "Beach Access, Spa, Pool"));
        allResorts.add(new Resort("resort_4", "Mountain View Resort", "Colorado", "$300", "4.0", R.drawable.resorts_three,
                "WiFi, Spa, Restaurant, Ski Access, Mountain View", "Mountain", "Ski Access, Mountain View, Spa"));
        allResorts.add(new Resort("resort_5", "Luxury Paradise Resort", "Maldives", "$600", "4.8", R.drawable.resorts_image_one,
                "WiFi, Pool, Spa, Restaurant, Beach Access, Private Beach", "Luxury", "Private Beach, Spa, Pool"));
        allResorts.add(new Resort("resort_6", "Family Fun Resort", "Orlando", "$280", "4.2", R.drawable.resorts_four,
                "WiFi, Pool, Kids Club, Restaurant, Theme Park Access", "Family", "Kids Club, Pool, Theme Park Access"));
        allResorts.add(new Resort("resort_7", "Ski Lodge Resort", "Aspen", "$450", "4.6", R.drawable.resorts_two,
                "WiFi, Spa, Restaurant, Ski Access, Fireplace", "Mountain", "Ski Access, Spa, Fireplace"));
        allResorts.add(new Resort("resort_8", "Budget Beach Resort", "Cancun", "$180", "3.9", R.drawable.resorts_three,
                "WiFi, Pool, Restaurant, Beach Access", "Budget", "Beach Access, Pool"));
        
        filteredResorts.clear();
        filteredResorts.addAll(allResorts);
        adapter.notifyDataSetChanged();
    }

    private void filterResorts(String userPreferences) {
        if (userPreferences == null || userPreferences.trim().isEmpty()) {
            filteredResorts.clear();
            filteredResorts.addAll(allResorts);
            adapter.notifyDataSetChanged();
            return;
        }

        filteredResorts.clear();
        String preferences = userPreferences.toLowerCase();

        for (Resort resort : allResorts) {
            boolean matches = resort.getName().toLowerCase().contains(preferences) ||
                    resort.getLocation().toLowerCase().contains(preferences) ||
                    resort.getAmenities().toLowerCase().contains(preferences) ||
                    resort.getType().toLowerCase().contains(preferences) ||
                    resort.getFeatures().toLowerCase().contains(preferences);

            // Extra keyword checks
            if (preferences.contains("beach") && (resort.getType().equalsIgnoreCase("Beach") ||
                    resort.getFeatures().toLowerCase().contains("beach"))) matches = true;
            if (preferences.contains("mountain") && resort.getType().equalsIgnoreCase("Mountain")) matches = true;
            if (preferences.contains("tropical") && resort.getType().equalsIgnoreCase("Tropical")) matches = true;
            if (preferences.contains("luxury") && resort.getType().equalsIgnoreCase("Luxury")) matches = true;
            if (preferences.contains("budget") && resort.getType().equalsIgnoreCase("Budget")) matches = true;
            if (preferences.contains("family") && resort.getType().equalsIgnoreCase("Family")) matches = true;
            if (preferences.contains("pool") && resort.getAmenities().toLowerCase().contains("pool")) matches = true;
            if (preferences.contains("spa") && resort.getAmenities().toLowerCase().contains("spa")) matches = true;
            if (preferences.contains("ski") && resort.getFeatures().toLowerCase().contains("ski")) matches = true;

            if (preferences.contains("miami") && resort.getLocation().toLowerCase().contains("miami")) matches = true;
            if (preferences.contains("orlando") && resort.getLocation().toLowerCase().contains("orlando")) matches = true;
            if (preferences.contains("aspen") && resort.getLocation().toLowerCase().contains("aspen")) matches = true;
            if (preferences.contains("maldives") && resort.getLocation().toLowerCase().contains("maldives")) matches = true;
            if (preferences.contains("cancun") && resort.getLocation().toLowerCase().contains("cancun")) matches = true;

            if (matches) filteredResorts.add(resort);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check Flask cache in case new data was prefetched
        loadResortsFromFlaskCacheIfAvailable();
        
        String prefs = getIntent().getStringExtra("userPreferences");
        if (prefs != null) {
            Toast.makeText(this, "Personalized resort results for: " + prefs, Toast.LENGTH_LONG).show();
            filterResorts(prefs);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String prefs = intent.getStringExtra("userPreferences");
        if (prefs != null) {
            Toast.makeText(this, "Personalized resort results for: " + prefs, Toast.LENGTH_LONG).show();
            filterResorts(prefs);
        }
    }
}
