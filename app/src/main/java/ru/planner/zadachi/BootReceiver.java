package ru.planner.zadachi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-schedules alarms after reboot, app update, or time/timezone change. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Notifier.createChannel(c);
        Notifier.scheduleAll(c);
    }
}
