package com.example.staygeniefrontend;

import java.net.URLEncoder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MediaType;

public class TourTypesCatalogue extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient();
    // TODO: set to your deployed PHP/Flask endpoint for dashboard selections
    private static final String[] BACKEND_URLS_SELECTION = {
        Config.PHP_BASE_URLS[0] + "dashboard.php",
        Config.PHP_BASE_URLS[1] + "dashboard.php",
        Config.PHP_BASE_URLS[2] + "dashboard.php"
    };
    // Flask search endpoint (same host as PHP, port 5000)
    private static final String[] FLASK_SEARCH_URLS = {
        Config.FLASK_BASE_URLS[0] + "/search",
        Config.FLASK_BASE_URLS[1] + "/search",
        Config.FLASK_BASE_URLS[2] + "/search"
    };
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private int getUserId() {
        SharedPreferences prefs = getSharedPreferences("staygenie_prefs", MODE_PRIVATE);
        String email = prefs.getString("saved_email", null);
        return email != null ? Math.abs(email.hashCode()) : 0;
    }

    private void sendSelectionAsync(String selection) {
        int userId = getUserId();
        String destination = getIntent().getStringExtra("destination");

        String dashboardUrl = BACKEND_URLS_SELECTION[0];
        if (destination != null && !destination.isEmpty()) {
            try {
                dashboardUrl = dashboardUrl + "?destination=" +
                        java.net.URLEncoder.encode(destination, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        RequestBody form = new FormBody.Builder()
                .add("user_id", String.valueOf(userId))
                .add("selection", selection)
                .add("destination", destination != null ? destination : "")
                .build();

        Request req = new Request.Builder()
                .url(dashboardUrl)
                .post(form)
                .build();

        httpClient.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(TourTypesCatalogue.this,
                        "Selection saved offline", Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;
                String body = response.body() != null ? response.body().string() : "";
                try { new JSONObject(body); } catch (JSONException ignored) {}
            }
        });
    }


    // Fire-and-forget: fetch accommodations from Flask and cache them locally by stay type
    private void prefetchAccommodations(String stayType, String destination) {
        try {
            // Use GET request with query parameters as Flask API expects
            String url = FLASK_SEARCH_URLS[0] + "?destination=" + 
                URLEncoder.encode(destination != null ? destination : "", "UTF-8") +
                "&type=" + URLEncoder.encode(stayType, "UTF-8");
            Request req = new Request.Builder().url(url).get().build();

            httpClient.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) { 
                    android.util.Log.e("TourTypesCatalogue", "Flask API request failed for " + stayType + " in " + destination, e);
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        android.util.Log.e("TourTypesCatalogue", "Flask API response not successful: " + response.code());
                        return;
                    }
                    String res = response.body() != null ? response.body().string() : "";
                    android.util.Log.d("TourTypesCatalogue", "Flask response for " + stayType + " in " + destination + ": " + res);
                    // Cache raw JSON by type; catalogue screens can read/parse if desired
                    getSharedPreferences("StayGenieCache", MODE_PRIVATE)
                            .edit()
                            .putString("accommodations_" + stayType, res)
                            .apply();
                }
            });
        } catch (java.io.UnsupportedEncodingException e) {
            android.util.Log.e("TourTypesCatalogue", "Failed to encode URL parameters for " + stayType + " in " + destination, e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tour_types_catalogue);

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Cast to GridLayout
        GridLayout travelOptionsGrid = findViewById(R.id.grid_travel_options);

        LinearLayout hotelsOption = travelOptionsGrid.getChildCount() > 0 ? (LinearLayout) travelOptionsGrid.getChildAt(0) : null;
        LinearLayout resortsOption = travelOptionsGrid.getChildCount() > 1 ? (LinearLayout) travelOptionsGrid.getChildAt(1) : null;
        LinearLayout vacationsOption = travelOptionsGrid.getChildCount() > 2 ? (LinearLayout) travelOptionsGrid.getChildAt(2) : null;
        LinearLayout businessOption = travelOptionsGrid.getChildCount() > 3 ? (LinearLayout) travelOptionsGrid.getChildAt(3) : null;

        if (hotelsOption != null) {
            hotelsOption.setOnClickListener(v -> {
                sendSelectionAsync("hotel");
                String destination = getIntent().getStringExtra("destination");
                prefetchAccommodations("hotel", destination);
                Intent intent = new Intent(TourTypesCatalogue.this, HotelsCatalogue.class);
                // Pass destination from LocationsDataInput if available
                if (destination != null) {
                    intent.putExtra("destination", destination);
                }
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        if (resortsOption != null) {
            resortsOption.setOnClickListener(v -> {
                sendSelectionAsync("resort");
                String destination = getIntent().getStringExtra("destination");
                prefetchAccommodations("resort", destination);
                Intent intent = new Intent(TourTypesCatalogue.this, ResortsCatalogue.class);
                if (destination != null) {
                    intent.putExtra("destination", destination);
                }
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        if (vacationsOption != null) {
            vacationsOption.setOnClickListener(v -> {
                sendSelectionAsync("vacation");
                String destination = getIntent().getStringExtra("destination");
                prefetchAccommodations("vacation", destination);
                Intent intent = new Intent(TourTypesCatalogue.this, VacationsCatalogue.class);
                if (destination != null) {
                    intent.putExtra("destination", destination);
                }
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        if (businessOption != null) {
            businessOption.setOnClickListener(v -> {
                sendSelectionAsync("business");
                String destination = getIntent().getStringExtra("destination");
                prefetchAccommodations("business", destination);
                Intent intent = new Intent(TourTypesCatalogue.this, BusinessHotelsCatalogue.class);
                if (destination != null) {
                    intent.putExtra("destination", destination);
                }
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }

        // Setup My Previous Bookings button
        setupMyBookingsButton();
    }

    private void setupMyBookingsButton() {
        Button myBookingsBtn = findViewById(R.id.myBookingsBtn);
        if (myBookingsBtn != null) {
            myBookingsBtn.setOnClickListener(v -> {
                Intent intent = new Intent(TourTypesCatalogue.this, MyPreviousBookings.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            });
        }
    }
}
