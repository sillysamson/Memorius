package com.memorius.app;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

public class SettingsActivity extends Activity {
    static final int[] SWATCH = {0xFF38385A, 0xFF2F4A3F, 0xFF5A3846, 0xFF3F4752, 0xFF2F4F6B, 0xFF4F3F38};
    LinearLayout box;

    @Override protected void attachBaseContext(Context b) { super.attachBaseContext(S.wrap(b)); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTheme(S.light(this) ? R.style.AppThemeLight : R.style.AppTheme);
        build();
    }

    int dp(float v) { return S.dp(this, v); }

    TextView text(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        S.round(l, S.card(this), dp(26));
        l.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        box.addView(l, lp);
        return l;
    }

    void section(String s) {
        TextView t = text(s, 13, true, S.mute(this));
        t.setPadding(dp(8), dp(18), 0, dp(8));
        box.addView(t);
    }

    View choice(LinearLayout parent, String label, boolean sel, View.OnClickListener l) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(14), 0, dp(14), 0);
        TextView t = text(label, 16, false, S.ink(this));
        r.addView(t, new LinearLayout.LayoutParams(0, dp(52), 1));
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (sel) r.addView(text("✓", 20, true, 0xFF34C759));
        r.setOnClickListener(l);
        parent.addView(r, new LinearLayout.LayoutParams(-1, -2));
        return r;
    }

    void build() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(S.bg(this));
        sv.setFitsSystemWindows(true);
        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(10), dp(18), dp(40));
        sv.addView(box);
        setContentView(sv);
        getWindow().setStatusBarColor(S.bg(this));
        getWindow().setNavigationBarColor(S.bg(this));
        if (S.light(this)) sv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("←", 28, true, S.ink(this));
        back.setPadding(0, 0, dp(16), 0);
        back.setOnClickListener(v -> finish());
        top.addView(back);
        top.addView(text(getString(R.string.settings), 30, true, S.ink(this)));
        box.addView(top);

        // language
        section(getString(R.string.language));
        LinearLayout c = card();
        String cur = S.lang(this);
        String[][] langs = {{"en-GB", "English (UK)"}, {"id", "Bahasa Indonesia"}, {"fr", "Français"}};
        for (String[] l : langs) {
            final String code = l[0];
            choice(c, l[1], cur.equals(code), v -> { S.p(this).edit().putString("lang", code).apply(); changed(); });
        }

        // appearance
        section(getString(R.string.appearance));
        c = card();
        LinearLayout lr = new LinearLayout(this);
        lr.setGravity(Gravity.CENTER_VERTICAL);
        lr.setPadding(dp(14), 0, dp(14), 0);
        TextView lt = text(getString(R.string.light_mode), 16, false, S.ink(this));
        lr.addView(lt, new LinearLayout.LayoutParams(0, dp(52), 1));
        lt.setGravity(Gravity.CENTER_VERTICAL);
        Switch sw = new Switch(this);
        sw.setChecked(S.light(this));
        sw.setOnCheckedChangeListener((b, on) -> { S.p(this).edit().putBoolean("light", on).apply(); changed(); });
        lr.addView(sw);
        c.addView(lr);
        TextView bt = text(getString(R.string.background), 14, false, S.mute(this));
        bt.setPadding(dp(14), dp(6), 0, dp(10));
        c.addView(bt);
        LinearLayout sr = new LinearLayout(this);
        sr.setPadding(dp(10), 0, dp(10), dp(14));
        for (final int col : SWATCH) {
            View d = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(col);
            g.setStroke(dp(3), S.base(this) == col ? S.ink(this) : 0x33888888);
            d.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
            lp.rightMargin = dp(10);
            d.setOnClickListener(v -> { S.p(this).edit().putInt("bg", col).apply(); changed(); });
            sr.addView(d, lp);
        }
        c.addView(sr);

        // reminders
        section(getString(R.string.reminders));
        c = card();
        int f = S.p(this).getInt("freq", 1);
        int[] ids = {R.string.rem_off, R.string.rem_due, R.string.rem_before, R.string.rem_daily};
        for (int i = 0; i < 4; i++) {
            final int val = i;
            choice(c, getString(ids[i]), f == i, v -> {
                S.p(this).edit().putInt("freq", val).apply();
                Notifier.schedule(this);
                build();
            });
        }
        int min = S.p(this).getInt("remMin", 8 * 60);
        String tm = String.format("%02d:%02d", min / 60, min % 60);
        choice(c, getString(R.string.rem_time, tm), false, v ->
                new TimePickerDialog(this, (tp, hh, mm) -> {
                    S.p(this).edit().putInt("remMin", hh * 60 + mm).apply();
                    Notifier.schedule(this);
                    build();
                }, min / 60, min % 60, true).show());
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            choice(c, getString(R.string.allow_notif), false, v -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 3));
        if (Build.VERSION.SDK_INT >= 31) {
            android.app.AlarmManager am = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (!am.canScheduleExactAlarms())
                choice(c, getString(R.string.allow_exact), false, v ->
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()))));
        }

        // storage
        section(getString(R.string.storage));
        c = card();
        String fn = S.folderName(this);
        TextView st = text(fn == null ? getString(R.string.folder_none) : getString(R.string.folder_set, fn), 15, false, S.mute(this));
        st.setPadding(dp(14), dp(12), dp(14), dp(4));
        c.addView(st);
        choice(c, getString(fn == null ? R.string.choose_folder : R.string.change_folder), false, v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(i, 5);
        });
    }

    void changed() { S.dirty = true; recreate(); }

    @Override protected void onActivityResult(int rq, int rc, Intent d) {
        super.onActivityResult(rq, rc, d);
        if (rq == 5 && rc == RESULT_OK && d != null && d.getData() != null) {
            Uri u = d.getData();
            try {
                getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception e) { }
            S.p(this).edit().putString("folder", u.toString()).apply();
            build();
        }
    }

    @Override public void onRequestPermissionsResult(int rq, String[] p, int[] r) {
        super.onRequestPermissionsResult(rq, p, r);
        build();
    }
}
