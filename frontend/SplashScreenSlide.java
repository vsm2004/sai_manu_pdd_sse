package com.example.staygeniefrontend;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenSlide extends AppCompatActivity {
    private static final int SPLASH_DELAY = 5000; // 5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen_slide);

        // 🔹 Fade in the genie lamp
        ImageView lamp = findViewById(R.id.genieLamp);
        lamp.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        // 🔹 After delay, move to CreateAccount with fade transition
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreenSlide.this, LoginPage.class);
            startActivity(intent);

            // Apply fade in/out transition between activities
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

            finish(); // close splash so user can’t go back
        }, SPLASH_DELAY);
    }
}
