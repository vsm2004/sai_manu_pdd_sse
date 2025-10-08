package com.example.staygeniefrontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.provider.Settings;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;
import java.io.IOException;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;

public class LocationsDataInput extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    // Use Config URLs with fallback logic
    private static final String[] BACKEND_URLS = Config.PHP_BASE_URLS;

    private int getUserId() {
        SharedPreferences account = getSharedPreferences("AccountPrefs", MODE_PRIVATE);
        String savedUserId = account.getString("saved_user_id", null);
        if (savedUserId != null) {
            try {
                int uid = Integer.parseInt(savedUserId);
                if (uid > 0) return uid;
            } catch (NumberFormatException ignored) {}
        }

        String email = account.getString("saved_email", null);
        if (email != null && !email.trim().isEmpty()) {
            return Math.abs(email.trim().toLowerCase().hashCode());
        }

        SharedPreferences legacy = getSharedPreferences("staygenie_prefs", MODE_PRIVATE);
        String legacyEmail = legacy.getString("saved_email", null);
        if (legacyEmail != null && !legacyEmail.trim().isEmpty()) {
            return Math.abs(legacyEmail.trim().toLowerCase().hashCode());
        }

        return 0; // fallback
    }

    private String getOrCreateEmail() {
        SharedPreferences account = getSharedPreferences("AccountPrefs", MODE_PRIVATE);
        String savedEmail = account.getString("saved_email", null);
        if (savedEmail != null && !savedEmail.trim().isEmpty()) {
            return savedEmail.trim();
        }

        SharedPreferences legacy = getSharedPreferences("staygenie_prefs", MODE_PRIVATE);
        String legacyEmail = legacy.getString("saved_email", null);
        if (legacyEmail != null && !legacyEmail.trim().isEmpty()) {
            return legacyEmail.trim();
        }

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String guestEmail = (androidId != null && !androidId.isEmpty())
                ? ("guest-" + androidId + "@guest.local")
                : ("guest-" + System.currentTimeMillis() + "@guest.local");

        SharedPreferences.Editor editor = account.edit();
        editor.putString("saved_email", guestEmail);
        editor.apply();
        return guestEmail;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_locations_data_input);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etCountry = findViewById(R.id.et_country);
        EditText etDestination = findViewById(R.id.et_destination);
        Button btnContinue = findViewById(R.id.btn_continue);
        View logout = findViewById(R.id.logout_container);

        btnContinue.setOnClickListener(v -> {
            String country = etCountry.getText().toString().trim();
            String destination = etDestination.getText().toString().trim();

            if (country.isEmpty() || destination.isEmpty()) {
                etCountry.setError(country.isEmpty() ? "Enter current location" : null);
                etDestination.setError(destination.isEmpty() ? "Enter destination" : null);
                return;
            }

            saveLocationDataToBackend(country, destination);
        });

        logout.setOnClickListener(v -> {
            Intent loginIntent = new Intent(LocationsDataInput.this, LoginPage.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
    }

    private void saveLocationDataToBackend(String currentLocation, String destination) {
        trySaveWithUrl(currentLocation, destination, 0);
    }

    private void trySaveWithUrl(String currentLocation, String destination, int attemptCount) {
        if (attemptCount >= BACKEND_URLS.length) {
            runOnUiThread(() -> Toast.makeText(this,
                    "Cannot connect to server. Please check:\n1. XAMPP is running\n2. Your computer's IP address\n3. Firewall settings",
                    Toast.LENGTH_LONG).show());
            return;
        }

        int userId = getUserId();
        String savedEmail = getOrCreateEmail();

        String url = BACKEND_URLS[attemptCount] + "enteraddress.php";

        // Ensure we always have at least one identifier available now (user_id or generated email)

        // Send user_id only if it's valid (> 0), otherwise send email
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("current_location", currentLocation)
                .add("destination", destination);
        
        if (userId > 0) {
            formBuilder.add("user_id", String.valueOf(userId));
            android.util.Log.d("LocationsDataInput", "Sending with user_id: " + userId);
        } else {
            formBuilder.add("email", savedEmail != null ? savedEmail : "");
            android.util.Log.d("LocationsDataInput", "Sending with email: " + savedEmail);
        }
        
        RequestBody formBody = formBuilder.build();

        android.util.Log.d("LocationsDataInput", "Attempt " + (attemptCount + 1) + "/" + BACKEND_URLS.length);
        android.util.Log.d("LocationsDataInput", "Calling URL: " + url);
        android.util.Log.d("LocationsDataInput", "User ID: " + userId + ", Email: " + savedEmail);
        android.util.Log.d("LocationsDataInput", "Current Location: " + currentLocation + ", Destination: " + destination);
        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                trySaveWithUrl(currentLocation, destination, attemptCount + 1);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String ctype = response.header("Content-Type", "");
                String body = response.body() != null ? response.body().string() : "";
                try {
                    if (!ctype.contains("application/json")) {
                        android.util.Log.e("LocationsDataInput", "Non-JSON response: " + ctype + ", body: " + body);
                        trySaveWithUrl(currentLocation, destination, attemptCount + 1);
                        return;
                    }
                    JSONObject json = new JSONObject(body);
                    boolean status = json.optBoolean("status", false);
                    String message = json.optString("message", "Unknown error");

                    runOnUiThread(() -> {
                        if (status) {
                            SharedPreferences.Editor editor = getSharedPreferences("AccountPrefs", MODE_PRIVATE).edit();
                            editor.putString("saved_destination", destination);
                            editor.putString("saved_current_location", currentLocation);
                            editor.apply();

                            Intent intent = new Intent(LocationsDataInput.this, TourTypesCatalogue.class);
                            intent.putExtra("source", currentLocation);
                            intent.putExtra("destination", destination);
                            startActivity(intent);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        } else {
                            Toast.makeText(LocationsDataInput.this, "Error: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    trySaveWithUrl(currentLocation, destination, attemptCount + 1);
                }
            }
        });
    }
}
