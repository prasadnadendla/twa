package in.truelet.twa;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import in.truelet.twa.LauncherActivity;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


public class FcmService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "default_channel";
    private static final String CHANNEL_NAME = "Default Notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Store token
        SharedPreferences prefs = getSharedPreferences("FCM_PREFS", MODE_PRIVATE);
        prefs.edit().putString("fcm_token", token).apply();

        // Restart launcher to pass token to web (invisible restart)
        Intent intent = new Intent(this, LauncherActivity.class);  // Replace with your launcher activity name
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            // Extract data from payload (sent from your server)
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String url = remoteMessage.getData().get("url");  // Optional deep link
            String notificationTag = remoteMessage.getData().get("unique_id");
            // Build intent to open app (with optional URL)
            Intent intent = new Intent(this, LauncherActivity.class);
            if (url != null) {
                intent.putExtra("url", url);  // Pass to handle in launcher
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int requestCode = (int) System.currentTimeMillis(); // Unique request code
            PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Use a default tag if none provided
            if (notificationTag == null || notificationTag.isEmpty()) {
                notificationTag = "default_tag";
            }
            // Build and show notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_icon)  // Replace with your icon
                   // .setLargeIcon(R.drawable.)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // if(notificationTag !=null && !notificationTag.isEmpty()){
            //     manager.notify(notificationTag,1, builder.build());  // Use a unique ID if needed
            // } else {
                manager.notify(1, builder.build());  // Use a unique ID if needed
            //}
            
        }
    }
}