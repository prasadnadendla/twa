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
            String channelId = CHANNEL_ID;
            CharSequence name = "Default Channel";
            String description = "Channel for default notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
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
        Intent intent = new Intent(this, LauncherActivity.class); // Replace with your launcher activity name
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            // Extract data from payload
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String url = remoteMessage.getData().get("url"); // Optional deep link
            String notificationTag = remoteMessage.getData().get("unique_id");

            // Use a default tag if none provided
            if (notificationTag == null || notificationTag.isEmpty()) {
                notificationTag = "default_tag";
            }

            // Group key for stacking notifications (e.g., messages from person X)
            String groupKey = "group_" + notificationTag;

            // Build intent for individual notification
            Intent intent = new Intent(this, LauncherActivity.class);
            if (url != null) {
                intent.putExtra("url", url);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Unique requestCode for PendingIntent
            int requestCode = (int) System.currentTimeMillis();
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Create notification channel
            createNotificationChannel();

            // Build individual notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_icon) // Replace with your icon
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setGroup(groupKey); // Assign to group

            // Unique ID for individual notification
            int notificationId = (notificationTag.hashCode() + requestCode) & 0x7FFFFFFF; // Ensure positive ID
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(notificationId, builder.build());

            // Build summary notification
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle()
                    .setBigContentTitle(title + " (New Messages)")
                    .setSummaryText(title)
                    .addLine(body); // Add the latest message

            NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_icon)
                    .setContentTitle(title)
                    .setContentText("You have new messages from " + title)
                    .setStyle(inboxStyle)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent); // Use same PendingIntent for consistency

            // Notify with summary using fixed ID for the group
            manager.notify(notificationTag.hashCode(), summaryBuilder.build());
        }
    }

}