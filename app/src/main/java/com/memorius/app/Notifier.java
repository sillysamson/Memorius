package com.memorius.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

final class Notifier {
    static final String CH = "reminders";

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(CH, "Reminders", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        return PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // One alarm per day at the reminder time. Re-armed after every fire, after boot and after settings change.
    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = alarmIntent(c);
        am.cancel(pi);
        Prefs p = Store.prefs;
        if ("off".equals(p.freq)) return;
        int h = 8, m = 0;
        try {
            String[] t = p.time.split(":");
            h = Integer.parseInt(t[0]);
            m = Integer.parseInt(t[1]);
        } catch (Exception ignored) {
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, h);
        cal.set(Calendar.MINUTE, m);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
    }

    static void fire(Context c) {
        Store.ensure(c);
        Str.lang = Store.prefs.lang;
        Prefs p = Store.prefs;
        if (!"off".equals(p.freq)) {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar cal = Calendar.getInstance();
            String today = f.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 1);
            String tomorrow = f.format(cal.getTime());
            StringBuilder sb = new StringBuilder();
            for (Card k : Store.cards) {
                if (k.done) continue;
                String d = k.due.length() >= 10 ? k.due.substring(0, 10) : "";
                boolean hit = p.freq.equals("daily")
                        || (p.freq.equals("due") && d.equals(today))
                        || (p.freq.equals("d1") && d.equals(tomorrow));
                if (hit) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(k.subject);
                }
            }
            if (sb.length() > 0) show(c, sb.toString());
        }
        schedule(c);
    }

    private static void show(Context c, String body) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (!nm.areNotificationsEnabled()) return;
        ensureChannel(c);
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 2, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(Str.t("remTitle"))
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true);
        nm.notify(1, b.build());
    }
}
