package com.beeflirt.beetalk.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.beeflirt.beetalk.R;
import com.beeflirt.beetalk.activities.ChatActivity;
import com.beeflirt.beetalk.MainActivity;
import com.beeflirt.beetalk.api.ApiClient;
import com.beeflirt.beetalk.api.ApiService;
import com.beeflirt.beetalk.utils.SharedPrefManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";

    // Separate channels per notification type
    private static final String CHANNEL_MATCH   = "beetalk_matches";
    private static final String CHANNEL_MESSAGE = "beetalk_messages";
    private static final String CHANNEL_SOCIAL  = "beetalk_social";

    private static final int NOTIF_ID_MATCH   = 1001;
    private static final int NOTIF_ID_MESSAGE = 1002;
    private static final int NOTIF_ID_SUPER   = 1003;
    private static final int NOTIF_ID_DEFAULT = 1000;

    // Brand color
    private static final int BRAND_COLOR = 0xFFF2BA3D;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        String type    = remoteMessage.getData().get("type");
        String matchId = remoteMessage.getData().get("match_id");
        String action  = remoteMessage.getData().get("action");

        String title = null;
        String body  = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body  = remoteMessage.getNotification().getBody();
        }

        if (title == null) {
            title = remoteMessage.getData().get("title");
            body  = remoteMessage.getData().get("body");
        }

        // Broadcast for instant chat refresh
        if ("message".equals(type)) {
            Intent refreshIntent = new Intent("com.beeflirt.beetalk.NEW_MESSAGE");
            if (matchId != null) {
                refreshIntent.putExtra("match_id", matchId);
            }
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                    .getInstance(this).sendBroadcast(refreshIntent);
        }

        if (title != null && body != null) {
            createNotificationChannels();
            showNotification(title, body, type, matchId);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        sendTokenToServer(token);
    }

    private void sendTokenToServer(String token) {
        if (!SharedPrefManager.getInstance(this).isLoggedIn()) return;

        ApiService apiService = ApiClient.getApiService(this);
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("device_token", token);
        requestBody.addProperty("device_type", "android");

        apiService.registerDeviceToken(requestBody).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, response.isSuccessful() ? "Token registered" : "Token register failed: " + response.code());
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.e(TAG, "Token register error: " + t.getMessage());
            }
        });
    }

    private void showNotification(String title, String body, String type, String matchId) {
        Intent intent;
        int notificationId;
        String channelId;

        // Route based on type
        if ("message".equals(type)) {
            intent = new Intent(this, ChatActivity.class);
            if (matchId != null) {
                try { intent.putExtra("match_id", Integer.parseInt(matchId)); } catch (Exception ignored) {}
            }
            notificationId = NOTIF_ID_MESSAGE;
            channelId = CHANNEL_MESSAGE;
        } else if ("match".equals(type)) {
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_tab", "matches");
            notificationId = NOTIF_ID_MATCH;
            channelId = CHANNEL_MATCH;
        } else if ("super_like".equals(type)) {
            intent = new Intent(this, MainActivity.class);
            intent.putExtra("open_tab", "swipe");
            notificationId = NOTIF_ID_SUPER;
            channelId = CHANNEL_SOCIAL;
        } else {
            intent = new Intent(this, MainActivity.class);
            notificationId = NOTIF_ID_DEFAULT;
            channelId = CHANNEL_SOCIAL;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Build notification per type
        NotificationCompat.Builder builder;

        switch (type != null ? type : "") {
            case "match":
                builder = buildMatchNotification(channelId, title, body, pendingIntent);
                break;
            case "message":
                builder = buildMessageNotification(channelId, title, body, pendingIntent);
                break;
            case "super_like":
                builder = buildSuperLikeNotification(channelId, title, body, pendingIntent);
                break;
            default:
                builder = buildDefaultNotification(channelId, title, body, pendingIntent);
                break;
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(notificationId, builder.build());
        }
    }

    /**
     * Match notification - celebratory style
     */
    private NotificationCompat.Builder buildMatchNotification(String channelId, String title, String body, PendingIntent pi) {
        return new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.baseline_favorite_24)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body)
                        .setSummaryText("New Match"))
                .setColor(BRAND_COLOR)
                .setColorized(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 200, 100, 200})
                .setLights(BRAND_COLOR, 500, 2000)
                .setContentIntent(pi);
    }

    /**
     * Message notification - messaging style
     */
    private NotificationCompat.Builder buildMessageNotification(String channelId, String title, String body, PendingIntent pi) {
        return new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.baseline_chat_bubble_24)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body))
                .setColor(BRAND_COLOR)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 150})
                .setLights(BRAND_COLOR, 300, 1000)
                .setContentIntent(pi);
    }

    /**
     * Super Like notification
     */
    private NotificationCompat.Builder buildSuperLikeNotification(String channelId, String title, String body, PendingIntent pi) {
        return new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.baseline_star_24)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body)
                        .setSummaryText("Super Like"))
                .setColor(0xFF2196F3) // Blue for super like
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 100, 50, 100, 50, 100})
                .setLights(0xFF2196F3, 500, 2000)
                .setContentIntent(pi);
    }

    /**
     * Default notification
     */
    private NotificationCompat.Builder buildDefaultNotification(String channelId, String title, String body, PendingIntent pi) {
        return new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setColor(BRAND_COLOR)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pi);
    }

    /**
     * Create separate notification channels per type (Android O+)
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        // Matches channel - high importance
        NotificationChannel matchChannel = new NotificationChannel(
                CHANNEL_MATCH, "Matches", NotificationManager.IMPORTANCE_HIGH);
        matchChannel.setDescription("New match notifications");
        matchChannel.enableVibration(true);
        matchChannel.setVibrationPattern(new long[]{0, 200, 100, 200});
        matchChannel.enableLights(true);
        matchChannel.setLightColor(BRAND_COLOR);
        matchChannel.setShowBadge(true);
        nm.createNotificationChannel(matchChannel);

        // Messages channel - high importance
        NotificationChannel msgChannel = new NotificationChannel(
                CHANNEL_MESSAGE, "Messages", NotificationManager.IMPORTANCE_HIGH);
        msgChannel.setDescription("New message notifications");
        msgChannel.enableVibration(true);
        msgChannel.setVibrationPattern(new long[]{0, 150});
        msgChannel.enableLights(true);
        msgChannel.setLightColor(BRAND_COLOR);
        msgChannel.setShowBadge(true);
        nm.createNotificationChannel(msgChannel);

        // Social channel (super likes, etc) - default importance
        NotificationChannel socialChannel = new NotificationChannel(
                CHANNEL_SOCIAL, "Social", NotificationManager.IMPORTANCE_DEFAULT);
        socialChannel.setDescription("Super likes and other social notifications");
        socialChannel.enableVibration(true);
        socialChannel.enableLights(true);
        socialChannel.setLightColor(0xFF2196F3);
        socialChannel.setShowBadge(true);
        nm.createNotificationChannel(socialChannel);
    }
}
