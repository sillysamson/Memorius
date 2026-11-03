package com.memorius.app;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

public class Notifier {
    static final String CH = "reminders";

    static PendingIntent pi(Context c) {
        return PendingIntent.getBroadcast(c, 1, new Intent(c, ReminderReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void channel(Context c) {
        NotificationChannel ch = new NotificationChannel(CH, c.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT);
        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(ch);
    }

    static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi(c));
        SharedPreferences p = S.p(c);
        if (p.getInt("freq", 1) == 0) return;
        int min = p.getInt("remMin", 8 * 60);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, min / 60);
        cal.set(Calendar.MINUTE, min % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);
        long t = cal.getTimeInMillis();
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms())
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi(c));
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi(c));
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi(c));
        }
    }

    static void fire(Context base) {
        Context c = S.wrap(base);
        int f = S.p(c).getInt("freq", 1);
        if (f != 0) {
            boolean ok = Build.VERSION.SDK_INT < 33
                    || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            if (ok) {
                channel(c);
                NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
                for (Task t : Store.load(c)) {
                    if (t.done) continue;
                    long d = ChronoUnit.DAYS.between(LocalDate.now(), t.due);
                    boolean send = (f == 1 && d == 0) || (f == 2 && (d == 0 || d == 1)) || (f == 3 && d <= 7);
                    if (!send) continue;
                    PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, MainActivity.class),
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                    Notification n = new Notification.Builder(c, CH)
                            .setSmallIcon(R.drawable.ic_notif)
                            .setContentTitle(t.subject)
                            .setContentText(S.kindLabel(c, t) + " · " + S.label(c, t.due))
                            .setAutoCancel(true)
                            .setContentIntent(open)
                            .build();
                    nm.notify((int) (t.id % 100000), n);
                }
            }
        }
        schedule(c);
    }
}
