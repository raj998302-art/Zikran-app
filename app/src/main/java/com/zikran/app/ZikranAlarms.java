package com.zikran.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;

/**
 * Schedules exact salat alarms via AlarmManager so they fire even when the app is closed.
 * Times are re-scheduled daily by PrayerAlarmReceiver; re-registered on boot by BootReceiver.
 */
public final class ZikranAlarms {
    public static final String CHANNEL_ID = "salat_alarms";
    private static final String PREFS = "zikran_alarms";
    private static final String KEY_DATA = "data";
    private static final String KEY_ENABLED = "enabled";

    private ZikranAlarms() {}

    /** Persist alarm config and (re)schedule everything. */
    public static void saveAndSchedule(Context ctx, JSONArray alarms, boolean enabled) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_DATA, alarms == null ? "[]" : alarms.toString())
                  .putBoolean(KEY_ENABLED, enabled)
                  .apply();
        scheduleAll(ctx);
    }

    /** Read saved config and schedule all enabled alarms. */
    public static void scheduleAll(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean enabled = sp.getBoolean(KEY_ENABLED, false);
        String data = sp.getString(KEY_DATA, "[]");
        try {
            JSONArray arr = new JSONArray(data);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String name = o.optString("name", "");
                String time = o.optString("time", "");
                boolean on = o.optBoolean("enabled", false);
                scheduleOne(ctx, name, time, enabled && on);
            }
        } catch (Exception ignored) {}
    }

    /** Schedule (or cancel) a single named alarm for its next occurrence. */
    private static void scheduleOne(Context ctx, String name, String hhmm, boolean enabled) {
        if (name == null || name.isEmpty()) return;
        PendingIntent pi = pending(ctx, name);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        if (!enabled || hhmm == null || !hhmm.matches("\\d{1,2}:\\d{2}")) {
            try { am.cancel(pi); } catch (Exception ignored) {}
            return;
        }

        String[] parts = hhmm.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1); // next occurrence
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        } catch (SecurityException ignored) {
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi); } catch (Exception e) {}
        }
    }

    private static PendingIntent pending(Context ctx, String name) {
        Intent intent = new Intent(ctx, PrayerAlarmReceiver.class);
        intent.setAction("com.zikran.app.PRAYER_ALARM");
        intent.putExtra("name", name);
        int requestCode = name.hashCode();
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, requestCode, intent, flags);
    }
}
