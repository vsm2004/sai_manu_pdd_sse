package com.example.staygeniefrontend;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class CheckoutNotificationReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "checkout_channel";//to be done

    @Override
    public void onReceive(Context context, Intent intent) {
        String hotelName = intent.getStringExtra("hotel_name");

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, cannot post notification
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Checkout Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Alerts when your stay checkout time is due");
            channel.enableLights(true);
            channel.setLightColor(Color.MAGENTA);
            nm.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, FinalFeedbackActivity.class);
        openIntent.putExtra("hotel_name", hotelName);
        PendingIntent pi = PendingIntent.getActivity(context, hotelName != null ? hotelName.hashCode() : 0,
                openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Checkout complete")
                .setContentText("Tap to share feedback for " + (hotelName != null ? hotelName : "your stay"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}


