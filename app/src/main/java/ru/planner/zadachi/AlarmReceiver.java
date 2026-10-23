package ru.planner.zadachi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fired by AlarmManager at 12:00 / 23:00; shows the notification and schedules the next one. */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        int type = intent.getIntExtra("type", 0);
        if (type == Notifier.REQ_NOON) {
            Notifier.showNoon(c);
        } else if (type == Notifier.REQ_NIGHT) {
            Notifier.showNight(c);
        }
        Notifier.scheduleAll(c);
    }
}
