package com.memorius.app;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class SchedulePage extends Page {
    final LinearLayout content;

    SchedulePage(MainActivity a) {
        super(a);
        ScrollView sv = Page.scroller(a);
        content = Ui.vbox(a);
        content.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(136));
        sv.addView(content, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    @Override
    void refresh() {
        content.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(136) + Ui.insetBottom);
        content.removeAllViews();
        final int today = MainActivity.todayIdx();
        LinearLayout tabs = new LinearLayout(a);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            boolean on = i == a.selDay;
            TextView t = Ui.text(a, Str.DAYS[Str.lang][i], 13, on ? Ui.bg : Ui.fg, false);
            t.setGravity(Gravity.CENTER);
            t.setSingleLine(true);
            t.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
            t.setMinHeight(Ui.dp(48));
            t.setBackground(on ? Ui.shape(Ui.fg, 999, 0, 0)
                    : Ui.shape(Color.TRANSPARENT, 999, i == today ? Ui.fg : Ui.line, 1));
            t.setOnClickListener(v -> {
                Ui.haptic(v, 0);
                a.selDay = idx;
                refresh();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            if (i > 0) lp.setMarginStart(Ui.dp(4));
            tabs.addView(t, lp);
        }
        content.addView(tabs, Ui.mw());

        boolean any = false;
        for (int i = 0; i < Store.slots.size(); i++) {
            Slot s = Store.slots.get(i);
            if (s.day == a.selDay) {
                content.addView(row(s, i), Ui.mw(8));
                any = true;
            }
        }
        if (!any) {
            TextView n = Ui.text(a, Phrases.schedule(), 20, Ui.mute, false);
            n.setGravity(Gravity.CENTER);
            n.setLineSpacing(0f, 1.15f);
            content.addView(n, Ui.mw(120));
        }
    }

    private View row(final Slot s, int i) {
        LinearLayout r = Ui.vbox(a);
        r.setBackground(Ui.shape(Ui.surface, 16, 0, 0));
        r.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
        LinearLayout top = Ui.hbox(a);
        if (!s.time.isEmpty()) {
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
            tl.setMarginEnd(Ui.dp(8));
            top.addView(Ui.tag(a, s.time), tl);
        }
        top.addView(Ui.text(a, s.subject, 16, Ui.fg, true), new LinearLayout.LayoutParams(0, -2, 1f));
        if (a.mode.equals("move")) top.addView(Ui.moveBtns(a, Store.slots, i, false, (x, y) -> x.day == y.day));
        r.addView(top, Ui.mw());
        if (!s.note.isEmpty()) r.addView(Ui.text(a, s.note, 14, Ui.mute, false), Ui.mw(4));
        if (a.mode.equals("delete")) {
            r.setOnClickListener(v -> a.confirm(() -> {
                Store.slots.remove(s);
                Store.save();
                a.render();
            }));
        }
        return r;
    }

    void addForm() {
        final boolean[] sel = new boolean[6];
        sel[a.selDay] = true;
        final LinearLayout days = new LinearLayout(a);
        days.setOrientation(LinearLayout.HORIZONTAL);
        final Runnable paint = new Runnable() {
            @Override
            public void run() {
                days.removeAllViews();
                for (int i = 0; i < 6; i++) {
                    final int idx = i;
                    TextView t = Ui.text(a, Str.DAYS[Str.lang][i], 13, sel[i] ? Ui.bg : Ui.fg, false);
                    t.setGravity(Gravity.CENTER);
                    t.setSingleLine(true);
                    t.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
                    t.setBackground(sel[i] ? Ui.shape(Ui.fg, 999, 0, 0) : Ui.shape(Color.TRANSPARENT, 999, Ui.line, 1));
                    t.setOnClickListener(v -> {
                        int count = 0;
                        for (boolean b : sel) if (b) count++;
                        if (sel[idx] && count == 1) return;
                        sel[idx] = !sel[idx];
                        this.run();
                    });
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
                    if (i > 0) lp.setMarginStart(Ui.dp(4));
                    days.addView(t, lp);
                }
            }
        };
        paint.run();

        final EditText subj = Ui.input(a, Str.t("subject"), false);
        final EditText note = Ui.input(a, Str.t("note"), false);
        final String[] time = {""};
        final TextView timeBtn = Ui.btn(a, "—", false, null);
        timeBtn.setOnClickListener(v -> new TimePickerDialog(a, (tp, hh, mm) -> {
            time[0] = String.format(Locale.US, "%02d:%02d", hh, mm);
            timeBtn.setText(time[0]);
        }, 8, 0, true).show());

        List<View> rows = new ArrayList<>();
        rows.add(days);
        rows.add(Ui.field(a, Str.t("subject"), subj));
        rows.add(Ui.field(a, Str.t("time"), timeBtn));
        rows.add(Ui.field(a, Str.t("note"), note));
        a.showForm(rows, () -> {
            String sj = subj.getText().toString().trim();
            if (sj.isEmpty()) {
                subj.requestFocus();
                return false;
            }
            int first = -1;
            for (int i = 0; i < 6; i++) {
                if (!sel[i]) continue;
                if (first < 0) first = i;
                Slot s = new Slot();
                s.day = i;
                s.subject = sj;
                s.time = time[0];
                s.note = note.getText().toString();
                Store.slots.add(s);
            }
            if (first >= 0) a.selDay = first;
            Store.save();
            return true;
        });
    }
}
