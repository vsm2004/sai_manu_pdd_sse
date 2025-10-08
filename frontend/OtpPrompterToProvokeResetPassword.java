package com.example.staygeniefrontend;

import android.os.Bundle;
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

public class OtpPrompterToProvokeResetPassword extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient();
    // TODO: Set to your deployed PHP backend URL
    private static final String BACKEND_URL = "http://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management";
    private static final String SEND_OTP_URL = BACKEND_URL + "/reset_password_request_via_otp.php";//modification

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp_prompter_to_provoke_reset_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etEmail = findViewById(R.id.et_email);
        String prefillEmail = getIntent().getStringExtra("email");
        if (prefillEmail != null && etEmail != null) etEmail.setText(prefillEmail);

        Button sendOtp = findViewById(R.id.btn_send_otp);
        sendOtp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                etEmail.setError("Enter email");
                return;
            }
            
            // Send OTP to backend
            sendOtpToEmail(email);
        });
    }

    private void sendOtpToEmail(String email) {
        RequestBody formBody = new FormBody.Builder()
                .add("email", email)
                .build();

        Request request = new Request.Builder()
                .url(SEND_OTP_URL)
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
                            Toast.makeText(OtpPrompterToProvokeResetPassword.this, message, Toast.LENGTH_LONG).show();
                            
                            // Navigate to VerifyOTP with email
                            android.content.Intent i = new android.content.Intent(OtpPrompterToProvokeResetPassword.this, VerifyOTP.class);
                            i.putExtra("email", email);
                            startActivity(i);
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            finish();
                        } else {
                            Toast.makeText(OtpPrompterToProvokeResetPassword.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(OtpPrompterToProvokeResetPassword.this, "Failed to send OTP. Please try again.", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(OtpPrompterToProvokeResetPassword.this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}