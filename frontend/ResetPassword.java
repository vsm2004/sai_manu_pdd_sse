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

public class ResetPassword extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient();
    // TODO: Set to your deployed PHP backend URL
    private static final String BACKEND_URL = "http://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management";
    private static final String RESET_PASSWORD_URL = BACKEND_URL + "/confirm_save_password_by_typingtwice.php";//modification

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText newPass = findViewById(R.id.newPassword);
        EditText confirmPass = findViewById(R.id.confirmPassword);
        Button resetBtn = findViewById(R.id.resetButton);
        String email = getIntent().getStringExtra("email");

        resetBtn.setOnClickListener(v -> {
            String p1 = newPass.getText().toString().trim();
            String p2 = confirmPass.getText().toString().trim();
            
            if (p1.isEmpty() || p2.isEmpty()) {
                if (p1.isEmpty()) newPass.setError("Enter password");
                if (p2.isEmpty()) confirmPass.setError("Confirm password");
                return;
            }
            
            if (!p1.equals(p2)) {
                Toast.makeText(ResetPassword.this, "Passwords do not match. Please try again.", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(ResetPassword.this, ErrorPasswordRetry.class);
                i.putExtra("email", email);
                startActivity(i);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
                return;
            }
            
            // Reset password with backend
            resetPasswordWithBackend(email, p1);
        });
    }

    private void resetPasswordWithBackend(String email, String newPassword) {
        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("password", newPassword)
                .add("confirm_password", newPassword)
                .build();

        Request request = new Request.Builder()
                .url(RESET_PASSWORD_URL)
                .post(formBody)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    String status = jsonResponse.optString("status", "false");
                    String message = jsonResponse.optString("message", "Unknown error");
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if ("success".equals(status)) {
                            Toast.makeText(ResetPassword.this, message, Toast.LENGTH_SHORT).show();
                            
                            // Go to Save/Never screen with the new password
                            Intent i = new Intent(ResetPassword.this, GoogleSavingResetPassword.class);
                            i.putExtra("username", "");
                            i.putExtra("email", email);
                            i.putExtra("password", newPassword);
                            startActivity(i);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            finish();
                        } else {
                            Toast.makeText(ResetPassword.this, message, Toast.LENGTH_LONG).show();
                            
                            // Navigate to ErrorPasswordRetry for retry
                            Intent i = new Intent(ResetPassword.this, ErrorPasswordRetry.class);
                            i.putExtra("email", email);
                            startActivity(i);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            finish();
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(ResetPassword.this, "Failed to reset password. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(ResetPassword.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}