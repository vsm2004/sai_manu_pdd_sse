package com.example.staygeniefrontend;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiGenieChatBot extends AppCompatActivity {

    private EditText userInput;
    private ImageView submitBtn;
    private TextView suggestionText;
    private final OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    // TODO: replace with your deployed endpoint (Flask or PHP)
    private static final String[] BACKEND_URLS = {
        "https://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management/ai-genie_chatbox.php", // Ngrok tunnel URL
        "http://192.168.137.246/hotel_management/ai-genie_chatbox.php", // Your computer's actual IP
        "http://192.168.1.3/hotel_management/ai-genie_chatbox.php"     // Alternative IP
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_genie_chat_bot);

        userInput = findViewById(R.id.user_input);
        submitBtn = findViewById(R.id.submit_button);
        suggestionText = findViewById(R.id.suggestion_text);

        // Check if we're coming from vacations, resorts, business or hotels
        String source = getIntent().getStringExtra("source");
        boolean isFromResorts = "resorts".equals(source);
        boolean isFromVacations = "vacations".equals(source);
        boolean isFromBusiness = "business".equals(source);

        // Set placeholder text and suggestions based on source
        if (isFromVacations) {
            userInput.setHint("e.g., 'Bali beach tour 7 days' or 'Swiss Alps adventure 6 days'");
            if (suggestionText != null) {
                suggestionText.setText("💡 Try: beach, mountain, city, adventure, safari, cruise, luxury, eco, Bali, Maldives, Kenya");
            }
        } else if (isFromBusiness) {
            userInput.setHint("e.g., 'downtown business hotel with meeting rooms' or 'airport business stay with shuttle'");
            if (suggestionText != null) {
                suggestionText.setText("💡 Try: business, meeting, conference, shuttle, gym, WiFi, downtown, airport, financial district");
            }
        } else if (isFromResorts) {
            userInput.setHint("e.g., 'beach resort with spa in Miami' or 'mountain resort with ski access'");
            if (suggestionText != null) {
                suggestionText.setText("💡 Try: beach, mountain, luxury, budget, family, pool, spa, ski, Miami, Orlando, Aspen, Maldives");
            }
        } else {
            userInput.setHint("e.g., 'luxury hotel with spa in Miami' or 'budget family resort with pool'");
            if (suggestionText != null) {
                suggestionText.setText("💡 Try: luxury, budget, business, family, pool, spa, gym, Miami, Orlando, Aspen");
            }
        }

        submitBtn.setOnClickListener(v -> {
            String text = userInput.getText().toString().trim();

            if(!text.isEmpty()){
                // Call backend for routing and filters; fall back to local processing on failure
                callBackend(text, new BackendCallback() {
                    @Override public void onSuccess(boolean status, String reply, String redirect, String filtersJoined) {
                        Intent intent;
                        if (redirect != null) {
                            if (redirect.contains("vacation")) {
                                intent = new Intent(AiGenieChatBot.this, VacationsCatalogue.class);
                            } else if (redirect.contains("resort")) {
                                intent = new Intent(AiGenieChatBot.this, ResortsCatalogue.class);
                            } else if (redirect.contains("business")) {
                                intent = new Intent(AiGenieChatBot.this, BusinessHotelsCatalogue.class);
                            } else {
                                intent = new Intent(AiGenieChatBot.this, HotelsCatalogue.class);
                            }
                        } else {
                            if (isFromVacations) intent = new Intent(AiGenieChatBot.this, VacationsCatalogue.class);
                            else if (isFromResorts) intent = new Intent(AiGenieChatBot.this, ResortsCatalogue.class);
                            else if (isFromBusiness) intent = new Intent(AiGenieChatBot.this, BusinessHotelsCatalogue.class);
                            else intent = new Intent(AiGenieChatBot.this, HotelsCatalogue.class);
                        }
                        if (filtersJoined != null && !filtersJoined.isEmpty()) {
                            intent.putExtra("userPreferences", filtersJoined.toLowerCase());
                        } else {
                            intent.putExtra("userPreferences", processUserInput(text, isFromResorts));
                        }
                        intent.putExtra("source", source);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                    }
                    @Override public void onError(String error) {
                        Toast.makeText(AiGenieChatBot.this, "Using local search (network issue)", Toast.LENGTH_SHORT).show();
                        Intent intent;
                        if (isFromVacations) intent = new Intent(AiGenieChatBot.this, VacationsCatalogue.class);
                        else if (isFromResorts) intent = new Intent(AiGenieChatBot.this, ResortsCatalogue.class);
                        else if (isFromBusiness) intent = new Intent(AiGenieChatBot.this, BusinessHotelsCatalogue.class);
                        else intent = new Intent(AiGenieChatBot.this, HotelsCatalogue.class);
                        intent.putExtra("userPreferences", processUserInput(text, isFromResorts));
                        intent.putExtra("source", source);
                        startActivity(intent);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();
                    }
                });
            }
        });
    }

    private interface BackendCallback {
        void onSuccess(boolean status, String reply, String redirect, String filtersJoined);
        void onError(String error);
    }

    private void callBackend(String message, BackendCallback cb) {
        JSONObject payload = new JSONObject();
        try { payload.put("message", message); } catch (JSONException ignored) {}

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(BACKEND_URLS[0])
                .post(body)
                .build();

        new Thread(() -> {
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                String resBody = response.body() != null ? response.body().string() : "{}";
                JSONObject res = new JSONObject(resBody);
                boolean status = res.optBoolean("status", false);
                String reply = res.optString("reply", "");
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
                new Handler(Looper.getMainLooper()).post(() -> cb.onSuccess(status, reply, redirect, finalFiltersJoined));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> cb.onError(e.getMessage()));
            }
        }).start();
    }

    private String processUserInput(String input, boolean isFromResorts) {
        // Simple processing to extract key preferences
        String processed = input.toLowerCase();

        // Add some intelligent keyword mapping
        if (processed.contains("cheap") || processed.contains("affordable")) {
            processed += " budget";
        }
        if (processed.contains("expensive") || processed.contains("high-end") || processed.contains("premium")) {
            processed += " luxury";
        }
        if (processed.contains("kids") || processed.contains("children") || processed.contains("family")) {
            processed += " family";
        }

        // Resort/Vacation-specific mappings
        if (isFromResorts || "vacations".equals(getIntent().getStringExtra("source"))) {
            if (processed.contains("relax") || processed.contains("vacation") || processed.contains("beach")) {
                processed += " beach";
            }
            if (processed.contains("mountain") || processed.contains("ski") || processed.contains("snow")) {
                processed += " mountain";
            }
            if (processed.contains("city")) {
                processed += " city";
            }
            if (processed.contains("adventure") || processed.contains("hike") || processed.contains("trek")) {
                processed += " adventure";
            }
            if (processed.contains("safari")) {
                processed += " safari";
            }
            if (processed.contains("cruise") || processed.contains("ship")) {
                processed += " cruise";
            }
            if (processed.contains("tropical") || processed.contains("paradise")) {
                processed += " tropical";
            }
        } else {
            // Hotel-specific mappings
            if (processed.contains("work") || processed.contains("business") || processed.contains("meeting")) {
                processed += " business";
            }
            if (processed.contains("relax") || processed.contains("vacation") || processed.contains("beach")) {
                processed += " resort";
            }
        }

        return processed;
    }
}
