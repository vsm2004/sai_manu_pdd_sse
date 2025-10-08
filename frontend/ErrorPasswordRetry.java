package com.example.staygeniefrontend;

import android.os.Bundle;
import android.widget.Button;
import android.content.Intent;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ErrorPasswordRetry extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_error_password_retry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get email from intent
        String email = getIntent().getStringExtra("email");
        
        // Show helpful message
        Toast.makeText(this, "Passwords didn't match. Please try again.", Toast.LENGTH_LONG).show();
        
        Button retry = findViewById(R.id.retryButton);
        retry.setOnClickListener(v -> {
            Intent i = new Intent(ErrorPasswordRetry.this, ResetPassword.class);
            if (email != null) {
                i.putExtra("email", email);
            }
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });
        
        // Add forgot password button if it exists in layout
        Button forgotPasswordBtn = findViewById(R.id.forgotPasswordButton);
        if (forgotPasswordBtn != null) {
            forgotPasswordBtn.setOnClickListener(v -> {
                if (email != null) {
                    Intent i = new Intent(ErrorPasswordRetry.this, OtpPrompterToProvokeResetPassword.class);
                    i.putExtra("email", email);
                    startActivity(i);
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    finish();
                } else {
                    Toast.makeText(this, "Email not found. Please try again.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}