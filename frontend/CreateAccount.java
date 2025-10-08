package com.example.staygeniefrontend;

import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
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

public class CreateAccount extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    // TODO: Set to your deployed PHP backend URL
    // For emulator: use 10.0.2.2, for physical device: use your computer's IP
    private static final String[] BACKEND_URLS = {
        "https://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management/", // Ngrok tunnel URL
        "http://192.168.137.246/hotel_management",  // Your computer's actual IP
        "http://192.168.1.3/hotel_management"       // Alternative IP
    };
    private static int currentUrlIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etUsername = findViewById(R.id.et_username);
        EditText etEmail = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        Button btnEnter = findViewById(R.id.btn_enter);

        btnEnter.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty()) { etUsername.setError("Enter username"); return; }
            if (email.isEmpty()) { etEmail.setError("Enter email"); return; }
            if (password.isEmpty()) { etPassword.setError("Enter password"); return; }

            // Register with backend
            registerUser(username, email, password);
        });
    }

    private String getCurrentBackendUrl() {
        return BACKEND_URLS[currentUrlIndex];
    }

    private void tryNextUrl() {
        currentUrlIndex = (currentUrlIndex + 1) % BACKEND_URLS.length;
    }

    private void registerUser(String username, String email, String password) {
        registerUserWithUrl(username, email, password, 0);
    }

    private void registerUserWithUrl(String username, String email, String password, int attemptCount) {
        if (attemptCount >= BACKEND_URLS.length) {
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(CreateAccount.this, "Cannot connect to server. Please check:\n1. XAMPP is running\n2. Your computer's IP address\n3. Firewall settings", Toast.LENGTH_LONG).show();
            });
            return;
        }
        
        String registerUrl = BACKEND_URLS[attemptCount] + "/register.php";
        RequestBody formBody = new FormBody.Builder()
                .add("name", username)
                .add("email", email)
                .add("password", password)
                .build();

        Request request = new Request.Builder()
                .url(registerUrl)
                .post(formBody)
                .build();
        
        android.util.Log.d("CreateAccount", "Trying URL: " + registerUrl + " (Attempt " + (attemptCount + 1) + "/" + BACKEND_URLS.length + ")");

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    boolean success = jsonResponse.optBoolean("status", false);
                    String message = jsonResponse.optString("message", "Unknown error");
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (success) {
                            // Save user data to SharedPreferences
                            JSONObject user = jsonResponse.optJSONObject("user");
                            if (user != null) {
                                saveUserData(user);
                            }
                            
                            Toast.makeText(CreateAccount.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            
                            android.util.Log.d("CreateAccount", "Navigating to SaveNewCredentials with username: " + username + ", email: " + email);
                            
                            // Navigate to SaveNewCredentials for new account credential saving
                            Intent intent = new Intent(CreateAccount.this, SaveNewCredentials.class);
                            intent.putExtra("username", username);
                            intent.putExtra("email", email);
                            intent.putExtra("password", password);
                            startActivity(intent);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            finish();
                        } else {
                            // Handle different error cases
                            if (message.contains("already registered") || jsonResponse.has("email_exists")) {
                                // Email exists - navigate to existing accounts without login check
                                Toast.makeText(CreateAccount.this, "Email already registered. Please choose from existing accounts.", Toast.LENGTH_SHORT).show();
                                
                                Intent intent = new Intent(CreateAccount.this, ChooseFromExistingAccounts.class);
                                startActivity(intent);
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                finish();
                            } else {
                                Toast.makeText(CreateAccount.this, message, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                } else {
                    android.util.Log.w("CreateAccount", "Failed with URL: " + registerUrl + " (HTTP " + response.code() + ")");
                    // Try next URL
                    registerUserWithUrl(username, email, password, attemptCount + 1);
                }
            } catch (Exception e) {
                android.util.Log.e("CreateAccount", "Failed with URL: " + registerUrl + " - " + e.getMessage());
                // Try next URL on network error
                registerUserWithUrl(username, email, password, attemptCount + 1);
            }
        }).start();
    }


    private void saveUserData(JSONObject user) {
        SharedPreferences.Editor editor = getSharedPreferences("AccountPrefs", MODE_PRIVATE).edit();
        editor.putString("saved_user_id", user.optString("id", "0"));
        editor.putString("saved_username", user.optString("name", ""));
        editor.putString("saved_email", user.optString("email", ""));
        editor.putBoolean("is_logged_in", true);
        editor.apply();
    }
}