package com.zikran.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * Fired by AlarmManager at each enabled salat time.
 * Shows a full notification (works when app is closed) and re-schedules itself for the next day.
 */
public class PrayerAlarmReceiver extends BroadcastReceiver {

    private static final String[] NAMES = {"Fajr","Dhuhr","Asr","Maghrib","Isha","PreFajr"};
    private static final String[] AR = {"الفجر","الظهر","العصر","المغرب","العشاء","قبل الفجر"};
    private static final String[] MSG = {
        "Rise for Fajr prayer — witnessed by the angels of night and day.",
        "Dhuhr time has arrived. Break for prayer.",
        "Asr prayer — the middle prayer, do not miss it.",
        "Hasten to Maghrib — time to break your fast and pray.",
        "Isha time — complete your day with prayer.",
        "30 minutes before Fajr — time for Tahajjud!"
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent == null ? null : intent.getStringExtra("name");
        if (name == null) name = "";
        int idx = index(name);

        // next-day re-schedule keeps alarms running forever without app
        ZikranAlarms.scheduleAll(context);

        String title = "Salat Time — " + (idx >= 0 ? name : name);
        String arabic = idx >= 0 ? AR[idx] : "";
        String text = (idx >= 0 ? MSG[idx] : "Time for prayer.")
            + (arabic.isEmpty() ? "" : "  " + arabic);

        Intent open = new Intent(context, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int pflags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pflags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(context, 2001, open, pflags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ZikranAlarms.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(new long[]{0, 400, 200, 400, 200, 800});

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try {
                nm.notify(idx >= 0 ? 3000 + idx : 3099, builder.build());
            } catch (SecurityException ignored) {}
        }
    }

    private static int index(String name) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return i;
        return -1;
    }
}
