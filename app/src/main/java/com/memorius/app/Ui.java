package com.memorius.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import java.util.List;

final class Ui {
    static float density = 3f;
    static final Interpolator EASE = new PathInterpolator(0.4f, 0f, 0.2f, 1f);

    static boolean isDark = false;
    static int insetTop = 0, insetBottom = 0; // system bar sizes (edge to edge)
    static int bg = 0xFFFFFFFF, fg = 0xFF1A1A1A, surface, line, mute;

    static final String[] LIGHT_NAMES = {"Snow white", "Pastel blue", "Pastel pink", "Pastel green", "Pastel yellow", "Beige"};
    static final int[] LIGHT = {0xFFFFFFFF, 0xFFDCEBFA, 0xFFFBE1EA, 0xFFDDF3E0, 0xFFFFF6CC, 0xFFF3EAD8};
    static final String[] DARK_NAMES = {"AMOLED black", "Eigengrau", "Dark purple", "Navy", "Burgundy", "Maroon", "Dark beige", "Walnut brown"};
    static final int[] DARK = {0xFF000000, 0xFF16161D, 0xFF1E1530, 0xFF0F1B33, 0xFF2E0B17, 0xFF2B0D0D, 0xFF2B261C, 0xFF2A1F17};
    static final int[] COLORS = {0xFFF28B82, 0xFFFBBC74, 0xFFF7E26B, 0xFF81C995, 0xFF78B9F2, 0xFFC58AF9};
    static final int[] CB = {0xFFE69F00, 0xFF56B4E9, 0xFF009E73, 0xFFF0E442, 0xFF0072B2, 0xFFD55E00};
    static final String[] SYM = {"●", "▲", "■", "◆", "★", "✚"};

    static {
        applyTheme(new Prefs());
    }

    static int[] palette() {
        return Store.prefs.cb ? CB : COLORS;
    }

    static void applyTheme(Prefs p) {
        int[] l = p.dark ? DARK : LIGHT;
        isDark = p.dark;
        bg = l[Math.max(0, Math.min(p.theme, l.length - 1))];
        fg = p.dark ? 0xFFF1F1F1 : 0xFF1A1A1A;
        surface = mix(bg, fg, 0.06f);
        line = mix(bg, fg, 0.14f);
        mute = mix(bg, fg, 0.55f);
    }

    static int mix(int a, int b, float t) {
        return ColorUtils.blendARGB(a, b, t);
    }

    static int dp(float v) {
        return Math.round(v * density);
    }

    // ---------- drawables ----------
    static GradientDrawable shape(int fill, float radiusDp, int stroke, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) g.setStroke(Math.max(1, dp(strokeDp)), stroke);
        return g;
    }

    static GradientDrawable oval(int fill, int stroke, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        if (strokeDp > 0) g.setStroke(Math.max(1, dp(strokeDp)), stroke);
        return g;
    }

    static GradientDrawable topRound(int fill, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        float r = dp(radiusDp);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        return g;
    }

    static GradientDrawable dashed(int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.TRANSPARENT);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(2), stroke, dp(6), dp(4));
        return g;
    }

    // ---------- layout helpers ----------
    static LinearLayout vbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout hbox(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout.LayoutParams mw() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    static LinearLayout.LayoutParams mw(int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(topDp);
        return p;
    }

    static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, -2, w);
    }

    // ---------- widgets ----------
    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
        return t;
    }

    static void press(final View v) {
        v.setOnTouchListener((view, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                Motion.scale(view, 0.96f, Motion.EFFECT_STIFF, 0.9f);
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                Motion.scale(view, 1f, Motion.EFFECT_STIFF, 0.9f);
            }
            return false;
        });
    }

    static TextView btn(Context c, String label, boolean solid, View.OnClickListener l) {
        TextView t = text(c, label, 15, solid ? bg : fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(14), dp(16), dp(14));
        t.setBackground(solid ? shape(fg, 999, 0, 0) : shape(Color.TRANSPARENT, 999, line, 1));
        if (l != null) t.setOnClickListener(l);
        press(t);
        return t;
    }

    // full-width selectable row (notification options, language list)
    static TextView option(Context c, String label, boolean on, View.OnClickListener l) {
        TextView t = text(c, label, 16, on ? bg : fg, false);
        t.setPadding(dp(16), dp(14), dp(16), dp(14));
        t.setBackground(on ? shape(fg, 999, 0, 0) : shape(Color.TRANSPARENT, 999, line, 1));
        t.setOnClickListener(l);
        press(t);
        return t;
    }

    static TextView tag(Context c, String s) {
        TextView t = text(c, s, 12, mute, false);
        t.setSingleLine(true);
        t.setPadding(dp(8), dp(2), dp(8), dp(2));
        t.setBackground(shape(Color.TRANSPARENT, 999, line, 1));
        return t;
    }

    static TextView circleBtn(Context c, String label, final Runnable r) {
        TextView t = text(c, label, 14, fg, false);
        t.setGravity(Gravity.CENTER);
        t.setBackground(oval(Color.TRANSPARENT, line, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(32), dp(32));
        lp.setMarginStart(dp(4));
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> r.run());
        press(t);
        return t;
    }

    static EditText input(Context c, String hint, boolean multi) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextColor(fg);
        e.setHintTextColor(mute);
        e.setTextSize(16);
        e.setBackground(shape(surface, 16, line, 1));
        e.setPadding(dp(16), dp(12), dp(16), dp(12));
        if (multi) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP);
        } else {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setSingleLine(true);
        }
        return e;
    }

    static LinearLayout field(Context c, String label, View v) {
        LinearLayout l = vbox(c);
        TextView t = text(c, label.toUpperCase(), 12, mute, true);
        t.setLetterSpacing(0.06f);
        l.addView(t, mw());
        l.addView(v, mw(4));
        return l;
    }

    interface IntCb {
        void run(int i);
    }

    interface Same<T> {
        boolean ok(T a, T b);
    }

    // segmented control; labels restyle themselves on select
    static LinearLayout seg(final Context c, final String[] labels, int selected, final IntCb cb) {
        final LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(4), dp(4), dp(4), dp(4));
        row.setBackground(shape(surface, 999, 0, 0));
        final TextView[] items = new TextView[labels.length];
        final int[] sel = {selected};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView t = text(c, labels[i], 14, mute, false);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(8), dp(8), dp(8), dp(8));
            t.setSingleLine(true);
            t.setOnClickListener(v -> {
                sel[0] = idx;
                paintSeg(items, sel[0]);
                if (cb != null) cb.run(idx);
            });
            items[i] = t;
            row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        }
        paintSeg(items, selected);
        return row;
    }

    private static void paintSeg(TextView[] items, int sel) {
        for (int i = 0; i < items.length; i++) {
            boolean on = i == sel;
            items[i].setTextColor(on ? bg : mute);
            items[i].setBackground(on ? shape(fg, 999, 0, 0) : null);
        }
    }

    static <T> LinearLayout moveBtns(final MainActivity a, final List<T> list, final int i, final boolean rev, final Same<T> same) {
        LinearLayout w = hbox(a);
        w.addView(circleBtn(a, "↑", () -> move(a, list, i, rev ? 1 : -1, same)));
        w.addView(circleBtn(a, "↓", () -> move(a, list, i, rev ? -1 : 1, same)));
        return w;
    }

    private static <T> void move(MainActivity a, List<T> list, int i, int d, Same<T> same) {
        int j = i + d;
        while (j >= 0 && j < list.size() && same != null && !same.ok(list.get(i), list.get(j))) j += d;
        if (j >= 0 && j < list.size()) {
            T x = list.get(i);
            list.set(i, list.get(j));
            list.set(j, x);
            Store.save();
            a.render();
        }
    }
}
