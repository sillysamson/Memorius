package com.memorius.app;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

class SettingsPage extends Page {
    final LinearLayout content;

    SettingsPage(MainActivity a) {
        super(a);
        ScrollView sv = Page.scroller(a);
        content = Ui.vbox(a);
        content.setPadding(Ui.dp(16), 0, Ui.dp(16), Ui.dp(120));
        sv.addView(content, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    private LinearLayout group(String title) {
        LinearLayout g = Ui.vbox(a);
        g.setPadding(0, Ui.dp(16), 0, Ui.dp(16));
        TextView l = Ui.text(a, title.toUpperCase(), 12, Ui.mute, true);
        l.setLetterSpacing(0.06f);
        g.addView(l, Ui.mw());
        return g;
    }

    private void addGroup(LinearLayout g) {
        content.addView(g, Ui.mw());
        View div = new View(a);
        div.setBackgroundColor(Ui.line);
        content.addView(div, new LinearLayout.LayoutParams(-1, 1));
    }

    @Override
    void refresh() {
        content.setPadding(Ui.dp(16), 0, Ui.dp(16), Ui.dp(120) + Ui.insetBottom);
        content.removeAllViews();
        final Prefs d = a.draft;
        if (d == null) return;

        // notifications
        LinearLayout g = group(Str.t("notifFreq"));
        final String[] keys = {"off", "due", "d1", "daily"};
        final String[] labels = {Str.t("off"), Str.t("onDue"), Str.t("dMinus1"), Str.t("daily")};
        for (int i = 0; i < keys.length; i++) {
            final String k = keys[i];
            g.addView(Ui.option(a, labels[i], d.freq.equals(k), v -> {
                d.freq = k;
                if (!k.equals("off")) a.ensureNotifPermission();
                a.render();
            }), Ui.mw(8));
        }
        if (!d.freq.equals("off") && !a.notificationsEnabled()) {
            g.addView(Ui.btn(a, Str.t("allow"), false, v -> a.requestNotifications()), Ui.mw(8));
        }
        TextView timeBtn = Ui.btn(a, d.time, false, v -> {
            int hh = 8, mm = 0;
            try {
                String[] t = d.time.split(":");
                hh = Integer.parseInt(t[0]);
                mm = Integer.parseInt(t[1]);
            } catch (Exception ignored) {
            }
            new TimePickerDialog(a, (tp, h2, m2) -> {
                d.time = String.format(Locale.US, "%02d:%02d", h2, m2);
                a.render();
            }, hh, mm, true).show();
        });
        g.addView(Ui.field(a, Str.t("remTime"), timeBtn), Ui.mw(16));
        addGroup(g);

        // language
        g = group(Str.t("language"));
        g.addView(Ui.option(a, Str.NAMES[d.lang], false, v -> a.chooseLanguage(d)), Ui.mw(8));
        addGroup(g);

        // appearance
        g = group(Str.t("appearance"));
        g.addView(Ui.seg(a, new String[]{Str.t("light"), Str.t("dark")}, d.dark ? 1 : 0, i -> {
            d.dark = i == 1;
            d.theme = 0;
            a.render();
        }), Ui.mw(8));
        final int[] colors = d.dark ? Ui.DARK : Ui.LIGHT;
        final String[] names = d.dark ? Ui.DARK_NAMES : Ui.LIGHT_NAMES;
        LinearLayout sw = new LinearLayout(a);
        sw.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < colors.length; i++) {
            final int idx = i;
            View v = new View(a);
            v.setBackground(Ui.oval(colors[i], idx == d.theme ? Ui.fg : Ui.line, idx == d.theme ? 3 : 1));
            v.setOnClickListener(x -> {
                d.theme = idx;
                a.render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(36), Ui.dp(36));
            lp.setMarginEnd(Ui.dp(8));
            sw.addView(v, lp);
        }
        g.addView(sw, Ui.mw(12));
        g.addView(Ui.text(a, names[Math.max(0, Math.min(d.theme, names.length - 1))], 14, Ui.mute, false), Ui.mw(8));
        addGroup(g);

        // colorblind
        g = group(Str.t("colorblind"));
        g.addView(Ui.seg(a, new String[]{Str.t("off"), Str.t("on")}, d.cb ? 1 : 0, i -> {
            d.cb = i == 1;
            a.render();
        }), Ui.mw(8));
        addGroup(g);

        // storage
        g = group(Str.t("storage"));
        g.addView(Ui.text(a, d.folder.isEmpty() ? "—" : d.folder, 14, Ui.mute, false), Ui.mw(8));
        g.addView(Ui.btn(a, Str.t("chooseFolder"), false, v -> a.pickFolder()), Ui.mw(8));
        content.addView(g, Ui.mw());
    }
}
