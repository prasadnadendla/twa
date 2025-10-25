package in.truelet.twa;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class LauncherActivity extends com.google.androidbrowserhelper.trusted.LauncherActivity {

    private static final String BASE_URL = "https://truelet.in"; // Base URL for your TWA

    @Override
    protected Map<String, Uri> getProtocolHandlers() {
        Map<String, Uri> registry = new HashMap<>();
        registry.put("web+truelet", Uri.parse("https://truelet.in/?url=%s"));
        return registry;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set orientation for Oreo and above to avoid crashes
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    @Override
    protected Uri getLaunchingUrl() {
        // Get the original launch URL
        Uri uri = super.getLaunchingUrl();
        Uri.Builder uriBuilder = uri.buildUpon();

        // Retrieve the FCM token from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("FCM_PREFS", MODE_PRIVATE);
        String token = prefs.getString("fcm_token", null);
        if (token != null) {
            uriBuilder.appendQueryParameter("fcm_token", token);
        }

        // Add device details
        uriBuilder.appendQueryParameter("device_model", Build.MODEL);
        uriBuilder.appendQueryParameter("manufacturer", Build.MANUFACTURER);
        uriBuilder.appendQueryParameter("os_version", Build.VERSION.RELEASE);

        // Handle deep-link URL from notification (if present)
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("url")) {
            String deepLinkUrl = intent.getStringExtra("url");
            if (deepLinkUrl != null && !deepLinkUrl.isEmpty()) {
                try {
                    // Handle relative URLs (e.g., "/leads")
                    String targetUrl;
                    if (deepLinkUrl.startsWith("/")) {
                        // Prepend base URL for relative paths
                        targetUrl = BASE_URL + deepLinkUrl;
                    } else if (isValidUrl(deepLinkUrl)) {
                        // Use full URL if valid
                        targetUrl = deepLinkUrl;
                    } else {
                        // Fallback to default URL if invalid
                        targetUrl = BASE_URL;
                    }
                    uriBuilder = Uri.parse(targetUrl).buildUpon();
                    // Re-append query parameters
                    if (token != null) {
                        uriBuilder.appendQueryParameter("fcm_token", token);
                    }
                    uriBuilder.appendQueryParameter("device_model", Build.MODEL);
                    uriBuilder.appendQueryParameter("manufacturer", Build.MANUFACTURER);
                    uriBuilder.appendQueryParameter("os_version", Build.VERSION.RELEASE);
                } catch (Exception e) {
                    // Log error and fallback to default URL
                    uriBuilder = Uri.parse(BASE_URL).buildUpon();
                }
            }
        }

        return uriBuilder.build();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the Activity's Intent
    }

    private boolean isValidUrl(String url) {
        if (url == null) return false;
        try {
            Uri uri = Uri.parse(url);
            // Check if the URL has a valid scheme and host
            return uri.getScheme() != null && 
                   (uri.getScheme().equals("http") || uri.getScheme().equals("https")) &&
                   uri.getHost() != null && 
                   uri.getHost().endsWith("truelet.in");
        } catch (Exception e) {
            return false;
        }
    }
}