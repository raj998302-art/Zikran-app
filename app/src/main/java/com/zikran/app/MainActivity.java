package com.zikran.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private WebView webView;
    private static final int LOC_PERMISSION_REQUEST = 1001;
    private static final int NOTIF_PERMISSION_REQUEST = 1002;

    private GeolocationPermissions.Callback geoCallback;
    private String geoOrigin;
    private boolean pendingNativeLocation = false;

    private LocationManager locationManager;
    private LocationListener activeListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean locationInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#08100D"));
            getWindow().setNavigationBarColor(Color.parseColor("#08100D"));
        }

        createNotificationChannel();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setDatabaseEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(false);
        }

        webView.addJavascriptInterface(new ZikranInterface(), "ZikranNative");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                if (scheme.equals("http") || scheme.equals("https")) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (Exception ignored) {}
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback callback) {
                geoOrigin = origin;
                geoCallback = callback;
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                    if (pendingNativeLocation) {
                        pendingNativeLocation = false;
                        nativeGetLocation();
                    }
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOC_PERMISSION_REQUEST
                    );
                }
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        request.grant(request.getResources());
                    }
                });
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message)
                    .setPositiveButton("OK", (d, w) -> result.confirm())
                    .setOnCancelListener(d -> result.cancel())
                    .show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(MainActivity.this)
                    .setMessage(message)
                    .setPositiveButton("Yes", (d, w) -> result.confirm())
                    .setNegativeButton("No", (d, w) -> result.cancel())
                    .setOnCancelListener(d -> result.cancel())
                    .show();
                return true;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // requested contextually from JS when user enables alarms
            }
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                ZikranAlarms.CHANNEL_ID,
                "Salat Alarms",
                NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Prayer time reminders and alarms");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 400, 200, 400, 200, 800});
            channel.setSound(
                android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI,
                new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    // ═══════════ native location ═══════════
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    }

    private Location bestLastKnown() {
        Location best = null;
        if (locationManager == null) return null;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
            try {
                if (locationManager.isProviderEnabled(provider) && hasLocationPermission()) {
                    Location l = locationManager.getLastKnownLocation(provider);
                    if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
                }
            } catch (SecurityException | IllegalArgumentException ignored) {}
        }
        return best;
    }

    private void nativeGetLocation() {
        if (!hasLocationPermission()) {
            pendingNativeLocation = true;
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOC_PERMISSION_REQUEST);
            return;
        }
        if (locationManager == null) { deliverLocation(null); return; }

        // fresh last-known fix (< 10 min old) → deliver immediately
        Location last = bestLastKnown();
        long now = System.currentTimeMillis();
        if (last != null && (now - last.getTime()) < 10 * 60 * 1000L) {
            deliverLocation(last);
            return;
        }

        if (locationInProgress) return;
        locationInProgress = true;

        activeListener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                locationInProgress = false;
                stopListening();
                deliverLocation(location);
            }
            @Override public void onProviderDisabled(String provider) { stopListeningSafely(); }
            @Override public void onProviderEnabled(String provider) {}
            @Deprecated
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        };

        boolean any = false;
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, activeListener, Looper.getMainLooper());
                any = true;
            }
        } catch (SecurityException | IllegalArgumentException ignored) {}
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, activeListener, Looper.getMainLooper());
                any = true;
            }
        } catch (SecurityException | IllegalArgumentException ignored) {}

        // timeout: 20s → fall back to last known (even stale) or null
        mainHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (locationInProgress) {
                    locationInProgress = false;
                    stopListening();
                    deliverLocation(bestLastKnown());
                }
            }
        }, 20000L);

        if (!any) {
            locationInProgress = false;
            stopListening();
            deliverLocation(last);
        }
    }

    private void stopListeningSafely() {
        mainHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (locationInProgress) {
                    locationInProgress = false;
                    stopListening();
                    deliverLocation(bestLastKnown());
                }
            }
        }, 1500L);
    }

    private void stopListening() {
        if (locationManager != null && activeListener != null) {
            try { locationManager.removeUpdates(activeListener); } catch (SecurityException ignored) {}
            activeListener = null;
        }
    }

    private void deliverLocation(Location loc) {
        String json;
        if (loc == null) {
            json = "null";
        } else {
            try {
                JSONObject o = new JSONObject();
                o.put("lat", loc.getLatitude());
                o.put("lng", loc.getLongitude());
                o.put("acc", loc.hasAccuracy() ? (double) loc.getAccuracy() : 0);
                o.put("provider", loc.getProvider() == null ? "gps" : loc.getProvider());
                json = o.toString();
            } catch (Exception e) { json = "null"; }
        }
        final String js = json;
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (webView != null) {
                    webView.evaluateJavascript(
                        "window.__nativeLocation&&window.__nativeLocation(" + js + ")", null);
                }
            }
        });
    }

    // ═══════════ permission results ═══════════
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOC_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (geoCallback != null && geoOrigin != null) {
                geoCallback.invoke(geoOrigin, granted, false);
                geoCallback = null; geoOrigin = null;
            }
            if (granted) {
                Toast.makeText(this, "Location enabled — Barakallahu feek", Toast.LENGTH_SHORT).show();
                if (pendingNativeLocation) {
                    pendingNativeLocation = false;
                    nativeGetLocation();
                }
            } else {
                Toast.makeText(this, "Location off — you can pick your city manually", Toast.LENGTH_LONG).show();
                if (pendingNativeLocation) {
                    pendingNativeLocation = false;
                    deliverLocation(null);
                }
            }
        } else if (requestCode == NOTIF_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            final String js = granted ? "true" : "false";
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (webView != null) {
                        webView.evaluateJavascript("window.__notifPermission&&window.__notifPermission(" + js + ")", null);
                    }
                }
            });
        }
    }

    // ═══════════ JavaScript bridge ═══════════
    class ZikranInterface {

        @JavascriptInterface
        public void vibrate(int ms) {
            Vibrator v = getVibrator();
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(Math.max(1, ms), VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(Math.max(1, ms));
            }
        }

        @JavascriptInterface
        public void vibratePattern(String pattern) {
            Vibrator v = getVibrator();
            if (v == null || !v.hasVibrator()) return;
            try {
                String[] parts = pattern.split(",");
                long[] p = new long[parts.length];
                for (int i = 0; i < parts.length; i++) p[i] = Math.max(0, Long.parseLong(parts[i].trim()));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(p, -1));
                } else {
                    v.vibrate(p, -1);
                }
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() ->
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void keepScreenOn(boolean on) {
            runOnUiThread(() -> {
                if (on) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            });
        }

        @JavascriptInterface
        public void getLocation() {
            nativeGetLocation();
        }

        @JavascriptInterface
        public void requestNotifPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIF_PERMISSION_REQUEST);
                }
            }
        }

        @JavascriptInterface
        public void scheduleAlarms(String json) {
            try {
                JSONObject root = new JSONObject(json);
                JSONArray arr = root.optJSONArray("alarms");
                boolean enabled = root.optBoolean("enabled", true);
                ZikranAlarms.saveAndSchedule(MainActivity.this, arr, enabled);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface
        public void checkExactAlarms() {
            boolean ok = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                ok = am != null && am.canScheduleExactAlarms();
            }
            final String js = ok ? "true" : "false";
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (webView != null) {
                        webView.evaluateJavascript("window.__exactAlarm&&window.__exactAlarm(" + js + ")", null);
                    }
                }
            });
        }
    }

    private Vibrator getVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm == null ? null : vm.getDefaultVibrator();
        }
        return (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopListening();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
