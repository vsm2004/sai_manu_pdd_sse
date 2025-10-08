package com.example.staygeniefrontend;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
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

public class IncorrectOtpScreen extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient();
    // TODO: Set to your deployed PHP backend URL
    private static final String[] BACKEND_URLS = {
        "https://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management/", // Ngrok tunnel URL
        "http://192.168.137.246/hotel_management", // Your computer's actual IP
        "http://192.168.1.3/hotel_management"     // Alternative IP
    };
    private static final String RESEND_OTP_URL = BACKEND_URLS[0] + "reask_otp_ifprevious_wrong.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_incorrect_otp_screen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button resend = findViewById(R.id.generateOtpButton);
        String email = getIntent().getStringExtra("email");
        
        if (email == null) {
            Toast.makeText(this, "Email not found. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        resend.setOnClickListener(v -> {
            // Resend OTP to backend
            resendOtpToEmail(email);
        });
    }

    private void resendOtpToEmail(String email) {
        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .build();

        Request request = new Request.Builder()
                .url(RESEND_OTP_URL)
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
                            Toast.makeText(IncorrectOtpScreen.this, message, Toast.LENGTH_LONG).show();
                            
                            // Navigate back to VerifyOTP with email
                            Intent i = new Intent(IncorrectOtpScreen.this, VerifyOTP.class);
                            i.putExtra("email", email);
                            startActivity(i);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            finish();
                        } else {
                            Toast.makeText(IncorrectOtpScreen.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(IncorrectOtpScreen.this, "Failed to resend OTP. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(IncorrectOtpScreen.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}