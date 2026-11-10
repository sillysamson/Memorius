package com.memorius.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Two separate things live here:
//   Notifications: channels, sounds, permission (what a notification looks and sounds like)
//   Reminders:     when and how often (frequency + exact time). They do not depend on the notification settings.
final class Notifier {
    // card-deadline channels, one per kind, plus one for general notifications
    static final String CH_HOMEWORK = "homework", CH_EXAM = "exam", CH_PREP = "prepping", CH_GENERAL = "general";
    private static final String[] BASES = {CH_HOMEWORK, CH_EXAM, CH_PREP, CH_GENERAL};

    // Android cannot change a channel's sound after it exists, so the sound revision is part of the id.
    private static String id(String base, Prefs p) {
        return base + "_" + p.soundRev;
    }

    private static Uri sound(String v) {
        if (v == null || v.isEmpty()) return Settings.System.DEFAULT_NOTIFICATION_URI;
        if (v.equals("silent")) return null;
        return Uri.parse(v);
    }

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        Store.ensure(c);
        Str.lang = Store.prefs.lang;
        Prefs p = Store.prefs;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        make(nm, id(CH_HOMEWORK, p), Str.t("chHomework"), NotificationManager.IMPORTANCE_DEFAULT, sound(p.soundDeadline));
        make(nm, id(CH_EXAM, p), Str.t("chExam"), NotificationManager.IMPORTANCE_HIGH, sound(p.soundDeadline));
        make(nm, id(CH_PREP, p), Str.t("chPrep"), NotificationManager.IMPORTANCE_DEFAULT, sound(p.soundDeadline));
        make(nm, id(CH_GENERAL, p), Str.t("notifs"), NotificationManager.IMPORTANCE_DEFAULT, sound(p.soundGeneral));
    }

    private static void make(NotificationManager nm, String id, String name, int importance, Uri sound) {
        NotificationChannel ch = new NotificationChannel(id, name, importance);
        if (sound == null) ch.setSound(null, null);
        else ch.setSound(sound, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        nm.createNotificationChannel(ch);
    }

    // after a sound changed: drop the old channels, create new ones
    static void rebuildChannels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        Prefs p = Store.prefs;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        List<NotificationChannel> all = nm.getNotificationChannels();
        for (NotificationChannel ch : all) {
            for (String b : BASES) {
                if (ch.getId().startsWith(b + "_") && !ch.getId().equals(id(b, p))) nm.deleteNotificationChannel(ch.getId());
            }
        }
        ensureChannel(c);
    }

    // ---------- reminders ----------
    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        return PendingIntent.getBroadcast(c, 1, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static boolean canExact(Context c) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am.canScheduleExactAlarms();
    }

    static int[] parseTime(String t) {
        int[] r = {8, 0, 0};
        try {
            String[] x = t.split(":");
            for (int i = 0; i < 3 && i < x.length; i++) r[i] = Integer.parseInt(x[i]);
        } catch (Exception ignored) {
        }
        return r;
    }

    // One alarm per day at the reminder time (to the second when exact alarms are allowed).
    // Re-armed after every fire, after boot and after settings change.
    static void schedule(Context c) {
        Store.ensure(c);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = alarmIntent(c);
        am.cancel(pi);
        Prefs p = Store.prefs;
        if ("off".equals(p.freq)) return;
        int[] t = parseTime(p.time);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, t[0]);
        cal.set(Calendar.MINUTE, t[1]);
        cal.set(Calendar.SECOND, t[2]);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);
        if (canExact(c)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
    }

    static void fire(Context c) {
        Store.ensure(c);
        Str.lang = Store.prefs.lang;
        Prefs p = Store.prefs;
        if (!"off".equals(p.freq)) {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar cal = Calendar.getInstance();
            String today = f.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, Math.max(1, p.daysBefore));
            String target = f.format(cal.getTime()); // the due date that is "X days away" today
            StringBuilder hw = new StringBuilder(), ex = new StringBuilder(), pr = new StringBuilder();
            for (Card k : Store.cards) {
                if (k.done) continue;
                String d = k.due.length() >= 10 ? k.due.substring(0, 10) : "";
                boolean hit = p.freq.equals("daily")
                        || (p.freq.equals("due") && d.equals(today))
                        || (p.freq.equals("dx") && d.equals(target));
                if (!hit) continue;
                StringBuilder sb = k.type.equals("exam") ? ex : k.type.equals("prepping") ? pr : hw;
                if (sb.length() > 0) sb.append(", ");
                sb.append(k.subject);
            }
            if (hw.length() > 0) show(c, 1, CH_HOMEWORK, Str.t("remTitle"), hw.toString(), p.soundDeadline);
            if (ex.length() > 0) show(c, 2, CH_EXAM, Str.t("remExam"), ex.toString(), p.soundDeadline);
            if (pr.length() > 0) show(c, 3, CH_PREP, Str.t("remPrep"), pr.toString(), p.soundDeadline);
        }
        schedule(c);
    }

    // "Test" buttons on the Notifications page (uses the saved sounds)
    static void test(Context c, boolean deadline) {
        Store.ensure(c);
        Str.lang = Store.prefs.lang;
        Prefs p = Store.prefs;
        show(c, deadline ? 91 : 92, deadline ? CH_HOMEWORK : CH_GENERAL, Str.t("test"),
                deadline ? Str.t("soundDeadline") : Str.t("soundGeneral"), deadline ? p.soundDeadline : p.soundGeneral);
    }

    private static void show(Context c, int nid, String base, String title, String body, String soundPref) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (!nm.areNotificationsEnabled()) return;
        ensureChannel(c);
        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 2, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, id(base, Store.prefs)) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT < 26) b.setSound(sound(soundPref)); // older Android: sound lives on the notification
        nm.notify(nid, b.build());
    }
}
