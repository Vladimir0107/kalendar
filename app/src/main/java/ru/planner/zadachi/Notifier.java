package ru.planner.zadachi;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;

/** Schedules and shows the 12:00 and 23:00 daily notifications. */
public class Notifier {

    public static final String CHANNEL = "daily";
    public static final int REQ_NOON = 1200;
    public static final int REQ_NIGHT = 2300;

    private static final String[] NAMES = {
            "полупокер", "ебло", "Алкаш", "киповец", "канеки кен", "Рафик",
            "Карл Джонсон", "Щёкинец", "Азотовец", "Бомж", "Гарри", "Супер Сус",
            "Убермаргинал", "Поднебесный", "Арсен Маркарян", "Субьект", "Арнольд",
            "Гарри Лудини", "Я и бал принцесс", "вконченный"
    };

    private static final Random RND = new Random();

    public static String randomName() {
        return NAMES[RND.nextInt(NAMES.length)];
    }

    public static void createChannel(Context c) {
        NotificationChannel ch = new NotificationChannel(
                CHANNEL, "Ежедневные напоминания", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Планы на день в 12:00 и итоги в 23:00");
        c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    public static void scheduleAll(Context c) {
        schedule(c, 12, 0, REQ_NOON);
        schedule(c, 23, 0, REQ_NIGHT);
    }

    private static void schedule(Context c, int hour, int minute, int req) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        Intent i = new Intent(c, AlarmReceiver.class).putExtra("type", req);
        PendingIntent pi = PendingIntent.getBroadcast(c, req, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);
        long t = next.toInstant().toEpochMilli();

        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
        }
    }

    /** 12:00 — plans for today. */
    public static void showNoon(Context c) {
        LocalDate today = LocalDate.now();
        List<Store.Task> tasks = Store.getTasks(c, today);
        boolean work = Store.isWorkDay(c, today);
        String name = randomName();

        String title;
        StringBuilder body = new StringBuilder();
        if (work) body.append("Сегодня рабочая смена.\n");
        if (tasks.isEmpty()) {
            title = "Эй, " + name + ", на сегодня дел нет!";
            body.append("Можно отдыхать.");
        } else {
            title = "Эй, " + name + ", у тебя сегодня много дел!";
            for (Store.Task t : tasks) {
                body.append(t.done ? "✔ " : "• ").append(t.text).append('\n');
            }
        }
        post(c, REQ_NOON, title, body.toString().trim());
    }

    /** 23:00 — how many tasks were done. */
    public static void showNight(Context c) {
        List<Store.Task> tasks = Store.getTasks(c, LocalDate.now());
        int done = 0;
        for (Store.Task t : tasks) if (t.done) done++;
        String name = randomName();

        String title = "Эй, " + name + ", итоги дня";
        String body;
        if (tasks.isEmpty()) {
            body = "Сегодня дел не было.";
        } else {
            body = "Выполнено " + done + " из " + tasks.size() + " дел.";
            if (done == tasks.size()) body += " Красава!";
        }
        post(c, REQ_NIGHT, title, body);
    }

    private static void post(Context c, int id, String title, String body) {
        createChannel(c);
        Intent open = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        c.getSystemService(NotificationManager.class).notify(id, n);
    }
}
