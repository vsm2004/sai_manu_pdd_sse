package com.example.staygeniefrontend;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AIGenieVoiceBox extends AppCompatActivity {

    private ImageView micIcon;
    private TextView statusText;
    private TextView instructionText;

    private final OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    // TODO: set to your deployed voicebox endpoint (Flask/PHP)
    private static final String[] BACKEND_URLS_VOICE = {
        "https://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management/ai-genie_voicebox.php", // Ngrok tunnel URL
        "http://192.168.137.246/hotel_management/ai-genie_voicebox.php", // Your computer's actual IP
        "http://192.168.1.3/hotel_management/ai-genie_voicebox.php"     // Alternative IP
    };

    private final String[] hotelVoiceInputs = {
            "luxury hotel with spa",
            "budget family resort with pool",
            "business hotel with meeting rooms",
            "Miami beach resort",
            "Aspen mountain lodge",
            "Orlando family hotel",
            "downtown luxury suite",
            "cheap hotel with gym"
    };

    private final String[] resortVoiceInputs = {
            "beach resort with spa",
            "mountain resort with ski access",
            "luxury tropical resort",
            "Miami beach resort",
            "Aspen ski resort",
            "Orlando family resort",
            "Maldives luxury resort",
            "budget beach resort"
    };

    private final String[] vacationVoiceInputs = {
            "Bali beach tour 7 days",
            "Swiss Alps adventure 6 days",
            "Kenya safari 5 days",
            "Mediterranean cruise 8 days",
            "Tokyo city tour 5 days",
            "Patagonia trek 7 days",
            "Maldives luxury retreat 5 days",
            "Costa Rica eco tour 6 days"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aigenie_voice_box);

        // Handle system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        micIcon = findViewById(R.id.mic_icon);
        statusText = findViewById(R.id.status_text);         // Add this ID to XML
        instructionText = findViewById(R.id.instruction_text); // Add this ID to XML

        // Determine if we came from vacations, resorts, business or hotels
        String source = getIntent().getStringExtra("source");
        boolean isFromResorts = "resorts".equals(source);
        boolean isFromVacations = "vacations".equals(source);
        boolean isFromBusiness = "business".equals(source);

        // Set instruction text
        if (instructionText != null) {
            if (isFromVacations) {
                instructionText.setText("🎤 Tap the microphone and speak your vacation tour preferences");
            } else if (isFromResorts) {
                instructionText.setText("🎤 Tap the microphone and speak your resort preferences");
            } else if (isFromBusiness) {
                instructionText.setText("🎤 Speak your business hotel preferences (meeting rooms, downtown, airport shuttle)");
            } else {
                instructionText.setText("🎤 Tap the microphone and speak your hotel preferences");
            }
        }

        // Microphone click listener
        micIcon.setOnClickListener(v -> {
            if (statusText != null) {
                statusText.setText("🎤 Listening...");
            }

            // Simulate voice recognition delay
            new Handler().postDelayed(() -> {
                Random random = new Random();
                String[] inputArray = isFromVacations ? vacationVoiceInputs : (isFromResorts ? resortVoiceInputs : hotelVoiceInputs);
                String spokenText = inputArray[random.nextInt(inputArray.length)];

                if (statusText != null) {
                    statusText.setText("✅ Heard: \"" + spokenText + "\"");
                }

                Toast.makeText(this, "Voice input: " + spokenText, Toast.LENGTH_SHORT).show();

                // Call backend for redirect/filters; fallback to local processing on error
                callBackendVoice(spokenText, new BackendCallback() {
                    @Override public void onSuccess(String redirect, String filtersJoined) {
                        Intent intent;
                        if (redirect != null) {
                            if (redirect.contains("vacation")) intent = new Intent(AIGenieVoiceBox.this, VacationsCatalogue.class);
                            else if (redirect.contains("resort")) intent = new Intent(AIGenieVoiceBox.this, ResortsCatalogue.class);
                            else if (redirect.contains("business")) intent = new Intent(AIGenieVoiceBox.this, BusinessHotelsCatalogue.class);
                            else intent = new Intent(AIGenieVoiceBox.this, HotelsCatalogue.class);
                        } else {
                            intent = isFromVacations ? new Intent(AIGenieVoiceBox.this, VacationsCatalogue.class)
                                    : (isFromResorts ? new Intent(AIGenieVoiceBox.this, ResortsCatalogue.class)
                                    : (isFromBusiness ? new Intent(AIGenieVoiceBox.this, BusinessHotelsCatalogue.class)
                                    : new Intent(AIGenieVoiceBox.this, HotelsCatalogue.class)));
                        }
                        if (filtersJoined != null && !filtersJoined.isEmpty()) {
                            intent.putExtra("userPreferences", filtersJoined.toLowerCase());
                        } else {
                            intent.putExtra("userPreferences", processVoiceInput(spokenText, isFromResorts || isFromVacations));
                        }
                        startActivity(intent);
                        finish();
                    }
                    @Override public void onError(String error) {
                        Toast.makeText(AIGenieVoiceBox.this, "Using local search (network issue)", Toast.LENGTH_SHORT).show();
                        Intent intent = isFromVacations ? new Intent(AIGenieVoiceBox.this, VacationsCatalogue.class)
                                : (isFromResorts ? new Intent(AIGenieVoiceBox.this, ResortsCatalogue.class)
                                : (isFromBusiness ? new Intent(AIGenieVoiceBox.this, BusinessHotelsCatalogue.class)
                                : new Intent(AIGenieVoiceBox.this, HotelsCatalogue.class)));
                        intent.putExtra("userPreferences", processVoiceInput(spokenText, isFromResorts || isFromVacations));
                        startActivity(intent);
                        finish();
                    }
                });
            }, 2000);
        });
    }

    private interface BackendCallback {
        void onSuccess(String redirect, String filtersJoined);
        void onError(String error);
    }

    private void callBackendVoice(String voiceInput, BackendCallback cb) {
        JSONObject payload = new JSONObject();
        try { payload.put("voice_input", voiceInput); } catch (JSONException ignored) {}

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BACKEND_URLS_VOICE[0])
                .post(body)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                String resBody = response.body() != null ? response.body().string() : "{}";
                JSONObject res = new JSONObject(resBody);
                String redirect = res.optString("redirect", null);
                String filtersJoined = null;
                if (res.has("filters")) {
                    JSONArray arr = res.optJSONArray("filters");
                    if (arr != null && arr.length() > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < arr.length(); i++) {
                            if (i > 0) sb.append(' ');
                            sb.append(arr.optString(i));
                        }
                        filtersJoined = sb.toString();
                    }
                }
                String finalFiltersJoined = filtersJoined;
                runOnUiThread(() -> cb.onSuccess(redirect, finalFiltersJoined));
            } catch (Exception e) {
                runOnUiThread(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    private String processVoiceInput(String input, boolean isFromResorts) {
        String processed = input.toLowerCase();

        // Common mappings
        if (processed.contains("cheap") || processed.contains("affordable")) processed += " budget";
        if (processed.contains("expensive") || processed.contains("high-end") || processed.contains("premium")) processed += " luxury";
        if (processed.contains("kids") || processed.contains("children") || processed.contains("family")) processed += " family";

        if (isFromResorts) {
            if (processed.contains("relax") || processed.contains("vacation") || processed.contains("beach") || processed.contains("sea view")) processed += " beach";
            if (processed.contains("mountain") || processed.contains("ski") || processed.contains("snow")) processed += " mountain";
            if (processed.contains("tropical") || processed.contains("paradise")) processed += " tropical";
        } else {
            if (processed.contains("work") || processed.contains("business") || processed.contains("meeting")) processed += " business";
            if (processed.contains("relax") || processed.contains("vacation") || processed.contains("beach") || processed.contains("sea view")) processed += " resort";
        }

        return processed;
    }
}
