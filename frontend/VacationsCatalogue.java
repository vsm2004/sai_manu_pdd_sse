package com.example.staygeniefrontend;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.activity.EdgeToEdge;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VacationsCatalogue extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VacationTourAdapter adapter;
    private List<VacationTour> allTours;
    private List<VacationTour> filteredTours;
    
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final String[] BACKEND_URLS = Config.PHP_BASE_URLS;
    private static final String VACATIONS_URL = BACKEND_URLS[0] + "/vacations.php";
    private static final String[] FLASK_SEARCH_URLS = {
        Config.FLASK_BASE_URLS[0] + "/search",
        Config.FLASK_BASE_URLS[1] + "/search",
        Config.FLASK_BASE_URLS[2] + "/search"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vacations_catalogue);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recycler_view_hotels);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize empty lists
        allTours = new ArrayList<>();
        filteredTours = new ArrayList<>();

        adapter = new VacationTourAdapter(this, filteredTours, tour -> {
            // Navigate to BookingTheStayArea with selected tour details
            Intent intent = new Intent(VacationsCatalogue.this, BookingTheStayArea.class);
            intent.putExtra("hotel_name", tour.getName());
            intent.putExtra("price", tour.getPrice());
            intent.putExtra("place_type", "vacation");
            intent.putExtra("place_id", tour.getId()); // Assuming VacationTour class has getId() method
            intent.putExtra("location", tour.getLocation()); // Add location data
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        String destination = getIntent().getStringExtra("destination");
        prefetchFromFlask("vacation", destination);
        // PRIORITY: Load Flask cache first (destination-specific data)
        loadVacationsFromFlaskCacheIfAvailable();
        if (allTours.isEmpty()) loadVacationsFromAPI();

        Button chatBtn = findViewById(R.id.btn_ai_genie_chat_vacations);
        if (chatBtn != null) {
            chatBtn.setOnClickListener(v -> {
                Intent intent = new Intent(VacationsCatalogue.this, AiGenieChatBot.class);
                intent.putExtra("source", "vacations");
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        Button voiceBtn = findViewById(R.id.btn_ai_genie_voicebox_vacations);
        if (voiceBtn != null) {
            voiceBtn.setOnClickListener(v -> {
                Intent intent = new Intent(VacationsCatalogue.this, AIGenieVoiceBox.class);
                intent.putExtra("source", "vacations");
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
            Request req = new Request.Builder().url(url).get().build();
            httpClient.newCall(req).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {}
                @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) throws java.io.IOException {
                    if (!response.isSuccessful()) return;
                    String res = response.body() != null ? response.body().string() : "";
                    getSharedPreferences("StayGenieCache", MODE_PRIVATE)
                            .edit()
                            .putString("accommodations_" + stayType, res)
                            .apply();
                    runOnUiThread(() -> loadVacationsFromFlaskCacheIfAvailable());
                }
            });
        } catch (Exception ignored) {}
    }

    private void loadVacationsFromFlaskCacheIfAvailable() {
        try {
            String cached = getSharedPreferences("StayGenieCache", MODE_PRIVATE)
                    .getString("accommodations_vacation", null);
            android.util.Log.d("VacationsCatalogue", "Flask cache for vacations: " + (cached != null ? "found" : "null"));
            if (cached == null || cached.trim().isEmpty()) {
                android.util.Log.d("VacationsCatalogue", "No Flask cache found for vacations");
                return;
            }

            JSONObject res = new JSONObject(cached);
            android.util.Log.d("VacationsCatalogue", "Flask cache status: " + res.optBoolean("status", false));
            if (!res.optBoolean("status", false)) {
                android.util.Log.d("VacationsCatalogue", "Flask cache status is false");
                return;
            }
            JSONArray results = res.optJSONArray("results");
            android.util.Log.d("VacationsCatalogue", "Flask cache results count: " + (results != null ? results.length() : 0));
            if (results == null || results.length() == 0) {
                android.util.Log.d("VacationsCatalogue", "No results in Flask cache");
                return;
            }

            List<VacationTour> apiTours = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.optJSONObject(i);
                if (item == null) continue;
                VacationTour tour = new VacationTour(
                        "vacation_flask_" + i,
                        item.optString("name", "Unknown Tour"),
                        item.optString("location", ""),
                        item.optString("price", "$0"),
                        item.optString("duration", "5 days"),
                        R.drawable.resorts_two,
                        item.optString("type", ""),
                        item.optString("highlights", "")
                );
                apiTours.add(tour);
            }

            runOnUiThread(() -> {
                allTours.clear();
                allTours.addAll(apiTours);
                filteredTours.clear();
                filteredTours.addAll(allTours);
                if (adapter != null) adapter.notifyDataSetChanged();
            });
        } catch (Exception ignored) { }
    }

    private void loadVacationsFromAPI() {
        String destination = getIntent().getStringExtra("destination");
        String url = VACATIONS_URL;
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
                        JSONArray agenciesArray = jsonResponse.optJSONArray("data");
                        List<VacationTour> apiTours = new ArrayList<>();
                        
                        if (agenciesArray != null) {
                            for (int i = 0; i < agenciesArray.length(); i++) {
                                JSONObject agencyJson = agenciesArray.optJSONObject(i);
                                if (agencyJson != null) {
                                    VacationTour tour = new VacationTour(
                                        agencyJson.optString("id", "vacation_" + i),
                                        agencyJson.optString("name", "Unknown Tour"),
                                        agencyJson.optString("location", ""),
                                        agencyJson.optString("price", "$0"),
                                        agencyJson.optString("duration", "5 days"),
                                        R.drawable.resorts_two, // Default image, you can map this based on agency data
                                        agencyJson.optString("type", "adventure"),
                                        agencyJson.optString("highlights", "")
                                    );
                                    apiTours.add(tour);
                                }
                            }
                        }
                        
                        runOnUiThread(() -> {
                            if (apiTours.isEmpty()) {
                             //   Toast.makeText(VacationsCatalogue.this, "No results from API. Using offline data.", Toast.LENGTH_SHORT).show();
                                loadFallbackTours();
                            } else {
                                allTours.clear();
                                allTours.addAll(apiTours);
                                filteredTours.clear();
                                filteredTours.addAll(allTours);
                                adapter.notifyDataSetChanged();
                                String message = jsonResponse.optString("message", "Loaded vacation tours");
                                Toast.makeText(VacationsCatalogue.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        runOnUiThread(() -> {
                            String message = jsonResponse.optString("message", "Failed to load vacation tours");
                            Toast.makeText(VacationsCatalogue.this, "API Error: " + message, Toast.LENGTH_SHORT).show();
                            loadFallbackTours();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(VacationsCatalogue.this, "Network error. Using offline data.", Toast.LENGTH_SHORT).show();
                        loadFallbackTours();
                    });
                }
            } catch (IOException | JSONException e) {
                runOnUiThread(() -> {
                    Toast.makeText(VacationsCatalogue.this, "Error loading vacations. Using offline data.", Toast.LENGTH_SHORT).show();
                    loadFallbackTours();
                });
            }
        }).start();
    }

    private void loadFallbackTours() {
        // Fallback to static data if API fails
        allTours.clear();
        allTours.add(new VacationTour("vacation_1", "Bali Beach Escape", "Bali, Indonesia", "$1299", "7 days", R.drawable.resorts_two, "beach", "Beach time, Ubud temples, snorkeling"));
        allTours.add(new VacationTour("vacation_2", "Swiss Alps Adventure", "Interlaken, Switzerland", "$1899", "6 days", R.drawable.resorts_three, "mountain", "Paragliding, hiking, Jungfraujoch"));
        allTours.add(new VacationTour("vacation_3", "Kenya Safari", "Maasai Mara, Kenya", "$2499", "5 days", R.drawable.resorts_image_one, "safari", "Game drives, Big Five, sunrise balloon"));
        allTours.add(new VacationTour("vacation_4", "Mediterranean Cruise", "Italy-Greece", "$2099", "8 days", R.drawable.resorts_four, "cruise", "Santorini, Rome, onboard shows"));
        allTours.add(new VacationTour("vacation_5", "Tokyo City Lights", "Tokyo, Japan", "$1599", "5 days", R.drawable.hotel_room, "city", "Shibuya crossing, sushi tour, temples"));
        allTours.add(new VacationTour("vacation_6", "Patagonia Trek", "El Chaltén, Argentina", "$2299", "7 days", R.drawable.hotel_one, "adventure", "Fitz Roy trek, glaciers, camping"));
        allTours.add(new VacationTour("vacation_7", "Maldives Luxury Retreat", "Malé, Maldives", "$3299", "5 days", R.drawable.resorts_image_one, "luxury", "Overwater villa, spa, private beach"));
        allTours.add(new VacationTour("vacation_8", "Costa Rica Eco Tour", "San José, Costa Rica", "$1799", "6 days", R.drawable.hotel_two, "eco", "Rainforest zipline, volcano, wildlife"));
        
        filteredTours.clear();
        filteredTours.addAll(allTours);
        adapter.notifyDataSetChanged();
    }

    private void filterTours(String userPreferences) {
        if (userPreferences == null || userPreferences.trim().isEmpty()) {
            filteredTours.clear();
            filteredTours.addAll(allTours);
            adapter.notifyDataSetChanged();
            return;
        }

        filteredTours.clear();
        String preferences = userPreferences.toLowerCase();

        for (VacationTour tour : allTours) {
            boolean matches = tour.getName().toLowerCase().contains(preferences) ||
                    tour.getLocation().toLowerCase().contains(preferences) ||
                    tour.getType().toLowerCase().contains(preferences) ||
                    tour.getHighlights().toLowerCase().contains(preferences) ||
                    tour.getDuration().toLowerCase().contains(preferences);

            if (preferences.contains("beach") && tour.getType().equalsIgnoreCase("beach")) matches = true;
            if (preferences.contains("mountain") && tour.getType().equalsIgnoreCase("mountain")) matches = true;
            if (preferences.contains("city") && tour.getType().equalsIgnoreCase("city")) matches = true;
            if (preferences.contains("adventure") && tour.getType().equalsIgnoreCase("adventure")) matches = true;
            if (preferences.contains("safari") && tour.getType().equalsIgnoreCase("safari")) matches = true;
            if (preferences.contains("cruise") && tour.getType().equalsIgnoreCase("cruise")) matches = true;
            if (preferences.contains("luxury") && tour.getType().equalsIgnoreCase("luxury")) matches = true;
            if (preferences.contains("eco") && tour.getType().equalsIgnoreCase("eco")) matches = true;

            if (preferences.contains("bali") && tour.getLocation().toLowerCase().contains("bali")) matches = true;
            if (preferences.contains("swiss") || preferences.contains("alps"))
                if (tour.getLocation().toLowerCase().contains("switzerland") || tour.getLocation().toLowerCase().contains("interlaken"))
                    matches = true;
            if (preferences.contains("kenya") || preferences.contains("safari"))
                if (tour.getLocation().toLowerCase().contains("kenya") || tour.getType().equalsIgnoreCase("safari"))
                    matches = true;
            if (preferences.contains("maldives"))
                if (tour.getLocation().toLowerCase().contains("maldives"))
                    matches = true;

            if (matches) filteredTours.add(tour);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check Flask cache in case new data was prefetched
        loadVacationsFromFlaskCacheIfAvailable();
        
        String prefs = getIntent().getStringExtra("userPreferences");
        if (prefs != null) {
            Toast.makeText(this, "Personalized vacation results for: " + prefs, Toast.LENGTH_LONG).show();
            filterTours(prefs);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String prefs = intent.getStringExtra("userPreferences");
        if (prefs != null) {
            Toast.makeText(this, "Personalized vacation results for: " + prefs, Toast.LENGTH_LONG).show();
            filterTours(prefs);
        }
    }
}
