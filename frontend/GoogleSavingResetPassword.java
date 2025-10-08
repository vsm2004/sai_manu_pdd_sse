package com.example.staygeniefrontend;

import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoogleSavingResetPassword extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient();
    // TODO: Set to your deployed PHP backend URL
    // For emulator: use 10.0.2.2, for physical device: use your computer's IP
    private static final String[] BACKEND_URLS = {
        "https://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management/", // Ngrok tunnel URL
        "http://192.168.137.246/hotel_management",  // Your computer's actual IP
        "http://192.168.1.3/hotel_management"       // Alternative IP
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_google_saving_reset_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String username = getIntent().getStringExtra("username");
        String email = getIntent().getStringExtra("email");
        String password = getIntent().getStringExtra("password");

        TextView emailTxt = findViewById(R.id.emailTxt);
        TextView passTxt = findViewById(R.id.passTxt);
        if (emailTxt != null && email != null) emailTxt.setText(email);
        if (passTxt != null && password != null) passTxt.setText("********");

        Button neverBtn = findViewById(R.id.neverBtn);
        Button saveBtn = findViewById(R.id.saveBtn);

        neverBtn.setOnClickListener(v -> {
            // Do not save credentials; proceed to collect location before catalogue
            // But still mark as logged in for session management
            SharedPreferences prefs = getSharedPreferences("AccountPrefs", MODE_PRIVATE);
            prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .apply();
            navigateToLocationsInput();
        });

        saveBtn.setOnClickListener(v -> {
            // If this is a password reset flow, update password in backend first
            if (password != null && !password.isEmpty()) {
                updatePasswordInBackend(email, password, username);
            } else {
                // Regular save flow - just save locally
                saveCredentialsLocally(username, email, password);
                navigateToLocationsInput();
            }
        });
    }

    private void updatePasswordInBackend(String email, String password, String username) {
        updatePasswordWithUrl(email, password, username, 0);
    }

    private void updatePasswordWithUrl(String email, String password, String username, int attemptCount) {
        if (attemptCount >= BACKEND_URLS.length) {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(GoogleSavingResetPassword.this, "Cannot connect to server. Please check:\n1. XAMPP is running\n2. Your computer's IP address\n3. Firewall settings", Toast.LENGTH_LONG).show();
            });
            return;
        }
        
        String resetPasswordUrl = BACKEND_URLS[attemptCount] + "/confirm_save_password_by_typingtwice.php";
        
        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", password)
                .add("confirm_password", password)
                .build();

        Request request = new Request.Builder()
                .url(resetPasswordUrl)
                .post(formBody)
                .build();
        
        android.util.Log.d("GoogleSavingResetPassword", "Trying URL: " + resetPasswordUrl);

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    boolean success = jsonResponse.optBoolean("status", false) || "success".equals(jsonResponse.optString("status", ""));
                    String message = jsonResponse.optString("message", "Unknown error");
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (success) {
                            // Password updated successfully in backend
                            saveCredentialsLocally(username, email, password);
                            Toast.makeText(GoogleSavingResetPassword.this, "Password saved successfully!", Toast.LENGTH_SHORT).show();
                            navigateToLocationsInput();
                        } else {
                            Toast.makeText(GoogleSavingResetPassword.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    android.util.Log.w("GoogleSavingResetPassword", "Failed with URL: " + resetPasswordUrl + " (HTTP " + response.code() + ")");
                    // Try next URL
                    updatePasswordWithUrl(email, password, username, attemptCount + 1);
                }
            } catch (Exception e) {
                android.util.Log.e("GoogleSavingResetPassword", "Failed with URL: " + resetPasswordUrl + " - " + e.getMessage());
                // Try next URL on network error
                updatePasswordWithUrl(email, password, username, attemptCount + 1);
            }
        }).start();
    }

    private void saveCredentialsLocally(String username, String email, String password) {
        SharedPreferences prefs = getSharedPreferences("AccountPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("saved_username", username)
                .putString("saved_email", email)
                .putString("saved_password", password)
                .putBoolean("is_logged_in", true)
                .apply();
    }

    private void navigateToLocationsInput() {
        Intent i = new Intent(GoogleSavingResetPassword.this, LocationsDataInput.class);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}