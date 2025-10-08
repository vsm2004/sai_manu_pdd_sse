package com.example.staygeniefrontend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_page);

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // No login form elements needed

        // Sign Up button → CreateAccount activity
        Button signupButton = findViewById(R.id.btn_signup);
        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, CreateAccount.class);
            startActivity(intent);
        });

        // Google Sign-In → ChooseFromExistingAccounts activity
        LinearLayout googleSignInLayout = findViewById(R.id.google_signin);
        googleSignInLayout.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, ChooseFromExistingAccounts.class);
            startActivity(intent);
        });
    }

}
