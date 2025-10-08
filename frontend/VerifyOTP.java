package com.example.staygeniefrontend;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

public class VerifyOTP extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient();
    // TODO: Set to your deployed PHP backend URL
    private static final String[] BACKEND_URLS = {
        "https://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management/", // Ngrok tunnel URL
        "http://192.168.137.246/hotel_management", // Your computer's actual IP
        "http://192.168.1.3/hotel_management"     // Alternative IP
    };
    private static final String VERIFY_OTP_URL = BACKEND_URLS[0] + "verify_submitted_otp.php";//modified

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify_otp);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText otpInput = findViewById(R.id.otpInput);
        Button verifyButton = findViewById(R.id.verifyButton);

        String expectedOtp = getIntent().getStringExtra("otp");
        String email = getIntent().getStringExtra("email");

        verifyButton.setOnClickListener(v -> {
            String entered = otpInput.getText().toString().trim();
            if (entered.isEmpty()) {
                otpInput.setError("Enter OTP");
                return;
            }
            
            // Verify OTP with backend
            verifyOtpWithBackend(email, entered);
        });
    }

    private void verifyOtpWithBackend(String email, String otp) {
        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .add("otp", otp)
                .build();

        Request request = new Request.Builder()
                .url(VERIFY_OTP_URL)
                .post(formBody)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    boolean success = jsonResponse.optBoolean("status", false);
                    String message = jsonResponse.optString("message", "Unknown error");
                    
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (success) {
                            Toast.makeText(VerifyOTP.this, message, Toast.LENGTH_SHORT).show();
                            
                            // Navigate to ResetPassword
                            Intent i = new Intent(VerifyOTP.this, ResetPassword.class);
                            i.putExtra("email", email);
                            startActivity(i);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            finish();
                        } else {
                            Toast.makeText(VerifyOTP.this, message, Toast.LENGTH_LONG).show();
                            
                            // Navigate to IncorrectOtpScreen
                            Intent i = new Intent(VerifyOTP.this, IncorrectOtpScreen.class);
                            i.putExtra("email", email);
                            startActivity(i);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            finish();
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(VerifyOTP.this, "Failed to verify OTP. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(VerifyOTP.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}