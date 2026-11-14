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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import java.util.List;

final class Ui {
    static float density = 3f;
    static final Interpolator EASE = new PathInterpolator(0.4f, 0f, 0.2f, 1f);

    static final Interpolator OUT_SHARP = new PathInterpolator(0f, 0f, 0.2f, 1f);

    static boolean isDark = false;
    static boolean texPaper = false, texGrain = true;
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
        return texPaper || texGrain;
    }

    static int primary, onPrimary; // filled buttons, selected options, "on" switches, slider fills

    static void applyTheme(Prefs p) {
        Style.id = Style.clamp(p.style);
        int[] l = Style.bgs(p.dark);
        isDark = p.dark;
        // the style decides which extras it allows (Flat and Swiss: no textures, no gradients)
        texPaper = Style.userTextures() ? (p.texPaper || Style.id == Style.HUMANIST) : false;
        texGrain = Style.userTextures() && p.texGrain;
        gradient = Style.userGradients() ? p.gradient : -1;
        contrast = p.contrast;
        hapticsOn = p.haptics;
        motionBlurOn = p.motionBlur;
        warmthOn = p.warmth;
        bg = l[Math.max(0, Math.min(p.theme, l.length - 1))];
        fg = Style.fg(p.dark, p.contrast);
        surface = Style.surface(bg, fg, p.contrast);
        line = Style.line(bg, fg, p.contrast);
        // small grey text: checked to stay above 4.5:1 on every background and card colour
        mute = mix(bg, fg, p.contrast ? 1f : 0.8f);
        primary = Style.primary(p.dark, fg);
        onPrimary = bestText(primary | 0xFF000000);
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
    // All shapes come from Surface (one path for fill, gradient, stroke and outline) and take their
    // radius, stroke and shape language from the current Style.
    static Surface shape(int fill, float radiusDp, int stroke, float strokeDp) {
        Surface sf = Surface.rect(fill, Style.radius(radiusDp));
        if (Style.organic() && radiusDp > 0f) sf.organic();
        float sd = Style.strokeDp(strokeDp);
        if (sd > 0f) sf.stroke(Style.strokeColor(stroke), sd);
        return sf;
    }

    static Surface oval(int fill, int stroke, float strokeDp) {
        Surface sf = Surface.oval(fill);
        if (strokeDp > 0f) sf.stroke(stroke, Style.id == Style.SWISS ? 1f : strokeDp);
        return sf;
    }

    static Surface topRound(int fill, float radiusDp) {
        Surface sf = Surface.rect(fill, Style.sheetRadius(radiusDp)).top();
        if (Style.organic()) sf.organic();
        return sf;
    }

    static Surface dashed(int stroke, float radiusDp) {
        return Surface.rect(0, Style.radius(radiusDp)).stroke(stroke, Style.id == Style.SWISS ? 1f : 2f).dash(6f, 4f);
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

    // ---------- dialogs ----------
    static int dialogTheme() {
        return isDark ? android.R.style.Theme_DeviceDefault_Dialog_Alert : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
    }

    // Dialogs (alerts, date and time pickers) follow the style: window shape, surface colour, text and buttons.
    static void styleDialog(android.app.Dialog d) {
        android.view.Window w = d.getWindow();
        if (w == null) return;
        float sp = Style.space();
        android.graphics.drawable.InsetDrawable bgd = new android.graphics.drawable.InsetDrawable(
                shape(bg | 0xFF000000, 28, 0, 0), dp(16 * sp));
        w.setBackgroundDrawable(bgd);
        w.setDimAmount(0.4f);
        if (Style.elevation() > 0f) w.getDecorView().setElevation(dp(Style.elevation() * 2f));
        android.content.res.Resources r = d.getContext().getResources();
        int[] ids = {r.getIdentifier("alertTitle", "id", "android"), android.R.id.message};
        for (int id : ids) {
            View v = d.findViewById(id);
            if (v instanceof TextView) ((TextView) v).setTextColor(fg);
        }
        if (d instanceof android.app.AlertDialog) {
            android.app.AlertDialog a = (android.app.AlertDialog) d;
            int[] which = {android.app.AlertDialog.BUTTON_POSITIVE, android.app.AlertDialog.BUTTON_NEGATIVE, android.app.AlertDialog.BUTTON_NEUTRAL};
            for (int i = 0; i < which.length; i++) {
                android.widget.Button b = a.getButton(which[i]);
                if (b != null) b.setTextColor(i == 0 ? primaryText() : fg);
            }
        }
    }

    // colour for text-style actions: the primary colour when it reads on the page, else the normal text colour
    static int primaryText() {
        return ratio(primary | 0xFF000000, bg | 0xFF000000) >= 4.5 ? primary | 0xFF000000 : fg;
    }

    // things appear with a short fade instead of popping in (no blur)
    static void fadeIn(final View v, long dur) {
        v.setAlpha(0f);
        v.animate().alpha(1f).setDuration(Motion.dur(dur)).setInterpolator(EASE).start();
    }

    // ---------- readable text ----------
    private static double lum(int c) {
        double[] ch = {(c >> 16 & 255) / 255.0, (c >> 8 & 255) / 255.0, (c & 255) / 255.0};
        for (int i = 0; i < 3; i++) ch[i] = ch[i] <= 0.03928 ? ch[i] / 12.92 : Math.pow((ch[i] + 0.055) / 1.055, 2.4);
        return 0.2126 * ch[0] + 0.7152 * ch[1] + 0.0722 * ch[2];
    }

    static double ratio(int a, int b) {
        double la = lum(a), lb = lum(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    // text colour for a solid fill: the page colour when it reads well, else black or white
    static int bestText(int fillOpaque) {
        int[] cand = {bg, 0xFFFFFFFF, 0xFF000000};
        if (ratio(cand[0], fillOpaque) >= 7.0) return cand[0];
        int best = cand[0];
        double br = 0;
        for (int c : cand) {
            double r = ratio(c, fillOpaque);
            if (r > br) {
                br = r;
                best = c;
            }
        }
        return best;
    }

    interface CheckCb {
        void onChanged(View v, boolean checked);
    }

    // row with a liquid glass switch on the right
    static LinearLayout toggleRow(Context c, String title, String hint, boolean on, final CheckCb l) {
        LinearLayout row = hbox(c);
        row.setMinimumHeight(dp(56));
        LinearLayout t = vbox(c);
        t.addView(text(c, title, 16, fg, false), mw());
        if (hint != null) t.addView(text(c, hint, 13, mute, false), mw(2));
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        final LiquidToggle s = new LiquidToggle(c, on);
        s.setContentDescription(title);
        s.setListener(v -> {
            haptic(s, 0);
            // let the thumb finish its glide before the page is rebuilt
            s.postDelayed(() -> l.onChanged(s, v), Motion.dur(260));
        });
        row.addView(s, new LinearLayout.LayoutParams(dp(64), dp(48)));
        row.setOnClickListener(x -> s.performClick());
        return row;
    }

    static View slider(Context c, int min, int max, int cur, IntCb cb) {
        return new LiquidSlider(c, min, max, cur, 1, cb);
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
        GlassButton t = new GlassButton(c, label, solid);
        if (l != null) t.setOnClickListener(l);
        return t;
    }

    static TextView option(Context c, String label, boolean on, View.OnClickListener l) {
        TextView t = text(c, label, 16, on ? onPrimary : fg, false);
        float sp = Style.space();
        t.setPadding(dp(16 * sp), dp(14 * sp), dp(16 * sp), dp(14 * sp));
        t.setBackground(on ? shape(primary, 999, 0, 0) : shape(Color.TRANSPARENT, 999, line, 1));
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
        e.setPadding(dp(16 * Style.space()), dp(12 * Style.space()), dp(16 * Style.space()), dp(12 * Style.space()));
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
            items[i].setTextColor(on ? onPrimary : mute);
            items[i].setBackground(on ? shape(primary, 999, 0, 0) : null);
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
