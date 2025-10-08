package com.example.staygeniefrontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ChooseFromExistingAccounts extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_from_existing_accounts);

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Account 1 → LocationsDataInput
        LinearLayout account1 = findViewById(R.id.account_1);
        TextView account1Name = account1.findViewById(R.id.tv_account1_name);
        TextView account1Email = account1.findViewById(R.id.tv_account1_email);
        account1.setOnClickListener(v -> {
            // Persist chosen account into AccountPrefs so downstream screens see it
            SharedPreferences legacy = getSharedPreferences("staygenie_prefs", MODE_PRIVATE);
            String username = legacy.getString("saved_username", null);
            String email = legacy.getString("saved_email", null);
            if (email != null) {
                SharedPreferences.Editor e = getSharedPreferences("AccountPrefs", MODE_PRIVATE).edit();
                if (username != null) e.putString("saved_username", username);
                e.putString("saved_email", email);
                e.putBoolean("is_logged_in", true);
                e.apply();
                android.util.Log.d("ChooseFromExistingAccounts", "Saved email to AccountPrefs: " + email);
            } else {
                android.util.Log.w("ChooseFromExistingAccounts", "No email found in staygenie_prefs");
            }
            Intent intent = new Intent(ChooseFromExistingAccounts.this, LocationsDataInput.class);
            startActivity(intent);
        });

        // Account 2 → LocationsDataInput
        LinearLayout account2 = findViewById(R.id.account_2);
        account2.setOnClickListener(v -> {
            SharedPreferences legacy = getSharedPreferences("staygenie_prefs", MODE_PRIVATE);
            String username = legacy.getString("saved_username", null);
            String email = legacy.getString("saved_email", null);
            if (email != null) {
                SharedPreferences.Editor e = getSharedPreferences("AccountPrefs", MODE_PRIVATE).edit();
                if (username != null) e.putString("saved_username", username);
                e.putString("saved_email", email);
                e.putBoolean("is_logged_in", true);
                e.apply();
                android.util.Log.d("ChooseFromExistingAccounts", "Saved email to AccountPrefs: " + email);
            } else {
                android.util.Log.w("ChooseFromExistingAccounts", "No email found in staygenie_prefs");
            }
            Intent intent = new Intent(ChooseFromExistingAccounts.this, LocationsDataInput.class);
            startActivity(intent);
        });

        // Add another account → CreateAccount
        View addAccount = findViewById(R.id.tv_add_account);
        addAccount.setOnClickListener(v -> {
            Intent intent = new Intent(ChooseFromExistingAccounts.this, CreateAccount.class);
            startActivity(intent);
        });

        // Populate saved account if present
        SharedPreferences prefs = getSharedPreferences("staygenie_prefs", MODE_PRIVATE);
        String savedUsername = prefs.getString("saved_username", null);
        String savedEmail = prefs.getString("saved_email", null);
        if (savedUsername != null && savedEmail != null) {
            if (account1Name != null) account1Name.setText(savedUsername);
            if (account1Email != null) account1Email.setText(savedEmail);
        }
    }
}
