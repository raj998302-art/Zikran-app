package com.zikran.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-registers all enabled salat alarms after device reboot. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            ZikranAlarms.scheduleAll(context);
        }
    }
}
