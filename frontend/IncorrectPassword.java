package com.example.staygeniefrontend;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class IncorrectPassword extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_incorrect_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText emailField = findViewById(R.id.email);
        String email = getIntent().getStringExtra("email");
        if (email != null && emailField != null) emailField.setText(email);

        Button typeAgain = findViewById(R.id.typeAgainBtn);
        Button forgot = findViewById(R.id.forgotPasswordBtn);

        typeAgain.setOnClickListener(v -> {
            Intent i = new Intent(IncorrectPassword.this, CreateAccount.class);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            finish();
        });

        forgot.setOnClickListener(v -> {
            Intent i = new Intent(IncorrectPassword.this, OtpPrompterToProvokeResetPassword.class);
            if (email != null) i.putExtra("email", email);
            startActivity(i);
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }
}