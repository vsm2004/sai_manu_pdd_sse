package com.example.staygeniefrontend;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FinalFeedbackActivity extends AppCompatActivity {

    private final OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    // TODO: Set to your deployed PHP backend URL
    private static final String[] BACKEND_URLS = {
        "https://nondilatable-petrina-pedigreed.ngrok-free.dev/hotel_management/", // Ngrok tunnel URL
        "http://192.168.137.246/hotel_management", // Your computer's actual IP
        "http://192.168.1.3/hotel_management"     // Alternative IP
    };
    private static final String FEEDBACK_URL = BACKEND_URLS[0] + "feedback.php";

    private String selectedEmoji = "😊";
    private String hotelName;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get data from intent
        hotelName = getIntent().getStringExtra("hotel_name");
        if (hotelName == null) hotelName = "Unknown Hotel";
        
        // Get user ID from SharedPreferences
        userId = getUserId();

        // Create layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        // Hotel name
        TextView hotelTitle = new TextView(this);
        hotelTitle.setText("Feedback for: " + hotelName);
        hotelTitle.setTextSize(20);
        hotelTitle.setPadding(0, 0, 0, 32);
        layout.addView(hotelTitle);

        // Emoji selection
        TextView emojiLabel = new TextView(this);
        emojiLabel.setText("How was your experience?");
        emojiLabel.setTextSize(16);
        emojiLabel.setPadding(0, 0, 0, 16);
        layout.addView(emojiLabel);

        LinearLayout emojiLayout = new LinearLayout(this);
        emojiLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        String[] emojis = {"🤩", "😊", "🙂", "😐", "😡"};
        for (String emoji : emojis) {
            Button emojiBtn = new Button(this);
            emojiBtn.setText(emoji);
            emojiBtn.setTextSize(24);
            emojiBtn.setPadding(16, 16, 16, 16);
            emojiBtn.setOnClickListener(v -> selectedEmoji = emoji);
            emojiLayout.addView(emojiBtn);
        }
        layout.addView(emojiLayout);

        // Star rating
        TextView starLabel = new TextView(this);
        starLabel.setText("Star Rating:");
        starLabel.setTextSize(16);
        starLabel.setPadding(0, 32, 0, 16);
        layout.addView(starLabel);

        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setNumStars(5);
        ratingBar.setRating(3);
        ratingBar.setStepSize(1);
        layout.addView(ratingBar);

        // Text feedback
        TextView textLabel = new TextView(this);
        textLabel.setText("Tell us more:");
        textLabel.setTextSize(16);
        textLabel.setPadding(0, 32, 0, 16);
        layout.addView(textLabel);

        EditText feedbackText = new EditText(this);
        feedbackText.setHint("Share your experience...");
        feedbackText.setMinLines(3);
        feedbackText.setMaxLines(5);
        layout.addView(feedbackText);

        // Submit button
        Button submitBtn = new Button(this);
        submitBtn.setText("Submit Feedback");
        submitBtn.setPadding(0, 32, 0, 0);
        submitBtn.setOnClickListener(v -> {
            String text = feedbackText.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Please enter your feedback", Toast.LENGTH_SHORT).show();
                return;
            }
            
            submitFeedback(selectedEmoji, (int)ratingBar.getRating(), text);
        });
        layout.addView(submitBtn);

        setContentView(layout);
    }

    private int getUserId() {
        SharedPreferences prefs = getSharedPreferences("AccountPrefs", MODE_PRIVATE);
        String userIdStr = prefs.getString("saved_user_id", "0");
        try {
            return Integer.parseInt(userIdStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void submitFeedback(String emoji, int starRating, String textFeedback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("user_id", userId);
            payload.put("emoji", emoji);
            payload.put("star_rating", starRating);
            payload.put("text_feedback", textFeedback);
            payload.put("hotel_name", hotelName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(FEEDBACK_URL)
                .post(body)
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
                            Toast.makeText(FinalFeedbackActivity.this, message, Toast.LENGTH_LONG).show();
                            
                            // Show analysis if available
                            if (jsonResponse.has("analysis")) {
                                showAnalysisResults(jsonResponse.optJSONObject("analysis"));
                            } else {
                                // Navigate to My Previous Bookings
                                Intent intent = new Intent(FinalFeedbackActivity.this, MyPreviousBookings.class);
                                startActivity(intent);
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            }
                        } else {
                            Toast.makeText(FinalFeedbackActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(FinalFeedbackActivity.this, "Failed to submit feedback", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(FinalFeedbackActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showAnalysisResults(JSONObject analysis) {
        String sentiment = analysis.optString("cumulative_label", "neutral");
        double score = analysis.optDouble("cumulative_score", 0.0);
            
            String analysisText = "Analysis Results:\n";
            analysisText += "Overall Sentiment: " + sentiment + "\n";
            analysisText += "Score: " + String.format("%.2f", score) + "\n";
            
            if (analysis.has("suggestions")) {
                // Handle suggestions array
            String suggestions = analysis.optString("suggestions", "");
                if (!suggestions.equals("[]") && !suggestions.equals("null")) {
                    analysisText += "\nSuggestions for improvement:\n" + suggestions;
                }
            }
            
            Toast.makeText(this, analysisText, Toast.LENGTH_LONG).show();
            
            // Navigate to My Previous Bookings after showing results
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(FinalFeedbackActivity.this, MyPreviousBookings.class);
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            }, 3000);
    }
}
