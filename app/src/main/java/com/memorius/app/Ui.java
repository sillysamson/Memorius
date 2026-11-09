package com.memorius.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.animation.ValueAnimator;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.content.res.ColorStateList;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import java.util.List;

final class Ui {
    static float density = 3f;
    static final Interpolator EASE = new PathInterpolator(0.4f, 0f, 0.2f, 1f);

    static final Interpolator OUT_SHARP = new PathInterpolator(0f, 0f, 0.2f, 1f);

    static boolean isDark = false;
    static boolean texPaper = false, texConcrete = false, texGrain = true;
    static boolean contrast = false, hapticsOn = true, motionBlurOn = true, warmthOn = false;
    static int gradient = -1;
    static int insetTop = 0, insetBottom = 0; // system bar sizes (edge to edge)
    static int bg = 0xFFFFFFFF, fg = 0xFF1A1A1A, surface, line, mute;

    static final String[] LIGHT_NAMES = {"Snow white", "Pastel blue", "Pastel pink", "Pastel green", "Pastel yellow", "Beige"};
    static final int[] LIGHT = {0xFFFFFFFF, 0xFFDCEBFA, 0xFFFBE1EA, 0xFFDDF3E0, 0xFFFFF6CC, 0xFFF3EAD8};
    static final String[] DARK_NAMES = {"AMOLED black", "Eigengrau", "Dark purple", "Navy", "Burgundy", "Maroon", "Dark beige", "Walnut brown"};
    static final int[] DARK = {0xFF000000, 0xFF16161D, 0xFF1E1530, 0xFF0F1B33, 0xFF2E0B17, 0xFF2B0D0D, 0xFF2B261C, 0xFF2A1F17};
    static final int[] COLORS = {0xFFF28B82, 0xFFFBBC74, 0xFFF7E26B, 0xFF81C995, 0xFF78B9F2, 0xFFC58AF9};
    static final int[] CB = {0xFFE69F00, 0xFF56B4E9, 0xFF009E73, 0xFFF0E442, 0xFF0072B2, 0xFFD55E00};
    static final String[] GRAD_NAMES = {"Dawn", "Ocean", "Forest", "Berry", "Sunset", "Mono"};
    static final int[] GRAD_A = {0xFFF28B82, 0xFF78B9F2, 0xFF81C995, 0xFFC58AF9, 0xFFFBBC74, 0xFF9AA0A6};
    static final int[] GRAD_B = {0xFFFBBC74, 0xFF81C995, 0xFF78B9F2, 0xFFF28B82, 0xFFC58AF9, 0xFF3C4043};
    static final String[] SYM = {"●", "▲", "■", "◆", "★", "✚"};

    static {
        applyTheme(new Prefs());
    }

    static int[] palette() {
        return Store.prefs.cb ? CB : COLORS;
    }

    static boolean anyTex() {
        return texPaper || texConcrete || texGrain;
    }

    static void applyTheme(Prefs p) {
        int[] l = p.dark ? DARK : LIGHT;
        isDark = p.dark;
        texPaper = p.texPaper;
        texConcrete = p.texConcrete;
        texGrain = p.texGrain;
        contrast = p.contrast;
        hapticsOn = p.haptics;
        motionBlurOn = p.motionBlur;
        warmthOn = p.warmth;
        gradient = p.gradient;
        bg = l[Math.max(0, Math.min(p.theme, l.length - 1))];
        fg = p.dark ? (p.contrast ? 0xFFFFFFFF : 0xFFF1F1F1) : (p.contrast ? 0xFF000000 : 0xFF1A1A1A);
        surface = mix(bg, fg, p.contrast ? 0.1f : 0.06f);
        line = mix(bg, fg, p.contrast ? 0.45f : 0.14f);
        // small grey text: checked to stay above 4.5:1 on every background and card colour
        mute = mix(bg, fg, p.contrast ? 1f : 0.8f);
    }

    // page background: plain colour or soft gradient, with optional see-through for window blur
    static Drawable pageBg(int alpha) {
        if (gradient < 0 || gradient >= GRAD_A.length) return new ColorDrawable(alpha(bg, alpha));
        float k = isDark ? 0.22f : 0.32f;
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{mix(bg, GRAD_A[gradient], k), mix(bg, GRAD_B[gradient], k)});
        g.setAlpha(alpha);
        return g;
    }

    static int alpha(int color, int a) {
        return (color & 0x00FFFFFF) | (a << 24);
    }

    static int mix(int a, int b, float t) {
        return ColorUtils.blendARGB(a, b, t);
    }

    // 0 tick, 1 long press, 2 confirm, 3 reject
    static void haptic(View v, int kind) {
        if (!hapticsOn) return;
        int h;
        switch (kind) {
            case 1:
                h = HapticFeedbackConstants.LONG_PRESS;
                break;
            case 2:
                h = Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY;
                break;
            case 3:
                h = Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS;
                break;
            default:
                h = HapticFeedbackConstants.CLOCK_TICK;
        }
        v.performHapticFeedback(h);
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

    // press ~150 ms, hover ~100 ms with a sharp ease-out; both scale from the centre
    static void press(final View v) {
        v.setOnTouchListener((view, e) -> {
            int act = e.getActionMasked();
            if (act == MotionEvent.ACTION_DOWN) {
                view.animate().cancel();
                view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(Motion.dur(Motion.T_PRESS)).setInterpolator(EASE).start();
            } else if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(Motion.dur(Motion.T_PRESS)).setInterpolator(EASE).start();
            }
            return false;
        });
        v.setOnHoverListener((view, e) -> {
            int act = e.getActionMasked();
            if (act == MotionEvent.ACTION_HOVER_ENTER) {
                view.animate().scaleX(1.03f).scaleY(1.03f).setDuration(Motion.dur(Motion.T_HOVER)).setInterpolator(OUT_SHARP).start();
            } else if (act == MotionEvent.ACTION_HOVER_EXIT) {
                view.animate().scaleX(1f).scaleY(1f).setDuration(Motion.dur(Motion.T_HOVER)).setInterpolator(OUT_SHARP).start();
            }
            return false;
        });
    }

    // fade + blur in (used instead of popping things in)
    static void blurIn(final View v, long dur, final float fromDp) {
        v.setAlpha(0f);
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(Motion.dur(dur));
        va.setInterpolator(EASE);
        va.addUpdateListener(an -> {
            float f = (Float) an.getAnimatedValue();
            v.setAlpha(f);
            if (motionBlurOn) Blur.view(v, (1f - f) * dp(fromDp));
        });
        va.start();
    }

    // row with a switch on the right
    static LinearLayout toggleRow(Context c, String title, String hint, boolean on, final CompoundButton.OnCheckedChangeListener l) {
        LinearLayout row = hbox(c);
        row.setMinimumHeight(dp(56));
        LinearLayout t = vbox(c);
        t.addView(text(c, title, 16, fg, false), mw());
        if (hint != null) t.addView(text(c, hint, 13, mute, false), mw(2));
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        final Switch s = new Switch(c);
        s.setShowText(false);
        s.setChecked(on);
        s.setThumbTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{bg, mute}));
        s.setTrackTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{fg, line}));
        s.setContentDescription(title);
        s.setOnCheckedChangeListener((b, v) -> {
            haptic(b, 0);
            l.onCheckedChanged(b, v);
        });
        row.addView(s, new LinearLayout.LayoutParams(-2, dp(48)));
        row.setOnClickListener(x -> s.toggle());
        return row;
    }

    static SeekBar slider(Context c, final int min, int max, int cur, final IntCb cb) {
        SeekBar s = new SeekBar(c);
        s.setMax(max - min);
        s.setProgress(cur - min);
        s.setProgressTintList(ColorStateList.valueOf(fg));
        s.setThumbTintList(ColorStateList.valueOf(fg));
        s.setProgressBackgroundTintList(ColorStateList.valueOf(line));
        s.setMinimumHeight(dp(48));
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int last = -1;

            @Override
            public void onProgressChanged(SeekBar sb, int p, boolean user) {
                if (!user) return;
                int v = p + min;
                if (v % 5 == 0 && v != last) haptic(sb, 0);
                last = v;
                cb.run(v);
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
            }
        });
        return s;
    }

    // label, minus, value, plus
    static LinearLayout stepper(Context c, String label, final int value, final int min, final int max, final String fmt, final IntCb cb) {
        LinearLayout row = hbox(c);
        row.setMinimumHeight(dp(56));
        row.addView(text(c, label, 16, fg, false), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(circleBtn(c, "−", "−", () -> cb.run(Math.max(min, value - 1))));
        TextView v = text(c, String.format(java.util.Locale.US, fmt, value), 16, fg, true);
        v.setGravity(Gravity.CENTER);
        v.setMinWidth(dp(48));
        row.addView(v);
        row.addView(circleBtn(c, "+", "+", () -> cb.run(Math.min(max, value + 1))));
        return row;
    }

    static TextView btn(Context c, String label, boolean solid, View.OnClickListener l) {
        TextView t = text(c, label, 15, solid ? bg : fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(14), dp(16), dp(14));
        t.setMinHeight(dp(48));
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

    static TextView circleBtn(Context c, String label, String desc, final Runnable r) {
        TextView t = text(c, label, 16, fg, false);
        t.setGravity(Gravity.CENTER);
        t.setContentDescription(desc);
        t.setBackground(oval(Color.TRANSPARENT, line, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(44), dp(44));
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
        w.addView(circleBtn(a, "↑", Str.t("moveUp"), () -> move(a, list, i, rev ? 1 : -1, same)));
        w.addView(circleBtn(a, "↓", Str.t("moveDown"), () -> move(a, list, i, rev ? -1 : 1, same)));
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
