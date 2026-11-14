package com.memorius.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Path;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

// Settings: one index of categories (no controls on it), each category opens its own page.
//   Look:   Theme, Display          Alerts: Notifications, Reminders
//   System: Language, Directory, Accessibility          More: Experimental, About
// Changes still go into a draft; the banner at the bottom applies or cancels them.
class SettingsPage extends Page {
    static final String VERSION = "1.3.2";
    static final String REPO = "https://github.com/sillysamson/Memorius";

    private static final String[][] GROUPS = {
            {"grpLook", "theme", "display"},
            {"grpAlerts", "notifs", "reminders"},
            {"grpSystem", "language", "directory", "accessibility"},
            {"grpMore", "experimental", "about"}};

    private static int icon(String cat) {
        switch (cat) {
            case "theme": return IconView.THEME;
            case "display": return IconView.DISPLAY;
            case "notifs": return IconView.NOTIF;
            case "reminders": return IconView.REMINDER;
            case "language": return IconView.LANGUAGE;
            case "directory": return IconView.FOLDER;
            case "accessibility": return IconView.ACCESS;
            case "experimental": return IconView.EXPERIMENT;
            default: return IconView.ABOUT;
        }
    }

    private final ScrollView sv;
    private final LinearLayout content;
    String cat = null; // null = the index

    SettingsPage(MainActivity a) {
        super(a);
        sv = Page.scroller(a);
        content = Ui.vbox(a);
        sv.addView(content, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    boolean inSub() {
        return cat != null;
    }

    String catTitle() {
        return cat == null ? "" : Str.t(cat);
    }

    View contentView() {
        return content;
    }

    void reset() {
        cat = null;
        refresh();
    }

    void open(String c) {
        cat = c;
        a.refreshHeader(true, 1);
        refresh();
        enter(1);
    }

    void up() {
        if (cat == null) return;
        cat = null;
        a.refreshHeader(true, -1);
        refresh();
        enter(-1);
    }

    // category pages open immediately (no sliding)
    private void enter(int dir) {
        sv.scrollTo(0, 0);
    }

    // ---------- small building blocks ----------
    private TextView label(LinearLayout col, String s) {
        TextView l = Ui.text(a, s.toUpperCase(), 12, Ui.mute, true);
        l.setLetterSpacing(0.06f);
        col.addView(l, Ui.mw(24));
        return l;
    }

    private LinearLayout panel(LinearLayout col, int topDp) {
        LinearLayout p = Ui.vbox(a);
        float sp = Style.space();
        p.setPadding(Ui.dp(16 * sp), Ui.dp(8 * sp), Ui.dp(16 * sp), Ui.dp(8 * sp));
        p.setBackground(Ui.shape(Ui.surface, 20, Ui.line, 1));
        col.addView(p, Ui.mw(topDp));
        return p;
    }

    private void divider(LinearLayout p) {
        View v = new View(a);
        v.setBackgroundColor(Ui.line);
        p.addView(v, new LinearLayout.LayoutParams(-1, 1));
    }

    private LinearLayout.LayoutParams wrapTop(int dp) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.topMargin = Ui.dp(dp);
        return l;
    }

    @Override
    void refresh() {
        content.setPadding(Ui.dp(16), 0, Ui.dp(16), a.settingsBottomSpace());
        content.removeAllViews();
        final Prefs d = a.draft;
        if (d == null) return;
        if (cat == null) {
            index();
            return;
        }
        switch (cat) {
            case "theme": theme(d); break;
            case "display": display(d); break;
            case "notifs": notifs(d); break;
            case "reminders": reminders(d); break;
            case "language": language(d); break;
            case "directory": directory(d); break;
            case "accessibility": accessibility(d); break;
            case "experimental": experimental(d); break;
            default: about();
        }
    }

    // ---------- index: stacked rounded containers with icons ----------
    private void index() {
        for (String[] g : GROUPS) {
            label(content, Str.t(g[0]));
            for (int i = 1; i < g.length; i++) {
                final String c = g[i];
                LinearLayout row = Ui.hbox(a);
                row.setMinimumHeight(Ui.dp(60));
                row.setPaddingRelative(Ui.dp(16), Ui.dp(8), Ui.dp(12), Ui.dp(8));
                row.setBackground(Ui.shape(Ui.surface, 20, Ui.line, 1));
                row.addView(new IconView(a, icon(c), Ui.fg), new LinearLayout.LayoutParams(Ui.dp(24), Ui.dp(24)));
                TextView t = Ui.text(a, Str.t(c), 16, Ui.fg, false);
                LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, -2, 1f);
                tl.setMarginStart(Ui.dp(16));
                row.addView(t, tl);
                row.addView(new IconView(a, IconView.CHEVRON, Ui.mute), new LinearLayout.LayoutParams(Ui.dp(20), Ui.dp(20)));
                row.setOnClickListener(v -> {
                    Ui.haptic(v, 0);
                    open(c);
                });
                Ui.press(row);
                content.addView(row, Ui.mw(i == 1 ? 8 : 8));
            }
        }
    }

    // ---------- Theme: typography, contrast, textures, colours, gradients ----------
    private void theme(final Prefs d) {
        // ---- visual style: five complete looks, applied at once ----
        label(content, Str.t("visualStyle"));
        for (int i = 0; i < Style.NAMES.length; i++) content.addView(styleRow(i, Style.id == i), Ui.mw(8));

        label(content, Str.t("typography"));
        LinearLayout p = panel(content, 8);
        LinearLayout head = Ui.hbox(a);
        head.addView(Ui.text(a, Str.t("textSize"), 16, Ui.fg, false), new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView val = Ui.text(a, d.fontPct + "%", 16, Ui.fg, true);
        head.addView(val);
        p.addView(head, Ui.mw(8));
        final TextView sample = Ui.text(a, "Aa  Memorius", 16, Ui.mute, false);
        sample.setTextSize(16f * d.fontPct / 100f);
        View sb = Ui.slider(a, 75, 130, d.fontPct, v -> {
            d.fontPct = v;
            val.setText(v + "%");
            sample.setTextSize(16f * v / 100f);
            a.updateBanner(); // keep the page (and the slider) alive while dragging
        });
        sb.setContentDescription(Str.t("textSize"));
        p.addView(sb, Ui.mw(4));
        p.addView(sample, Ui.mw(4));
        p.addView(Ui.toggleRow(a, Str.t("contrast"), null, d.contrast, (b, v) -> {
            d.contrast = v;
            a.render();
        }), Ui.mw(8));

        label(content, Str.t("textures"));
        p = panel(content, 8);
        final boolean texOk = Style.userTextures();
        if (!texOk) p.setAlpha(0.4f);
        final LinearLayout texPanel = p;
        p.addView(Ui.toggleRow(a, Str.t("texPaper"), texOk ? null : Str.t("styleNA"), d.texPaper, (b, v) -> {
            d.texPaper = v;
            a.render();
        }));
        divider(p);
        p.addView(Ui.toggleRow(a, Str.t("texGrain"), null, d.texGrain, (b, v) -> {
            d.texGrain = v;
            a.render();
        }));

        if (!texOk) disable(texPanel);

        label(content, Str.t("colors"));
        final int[] colors = Style.bgs(d.dark);
        final String[] names = Style.bgNames(d.dark);
        LinearLayout sw = new LinearLayout(a);
        for (int i = 0; i < colors.length; i++) {
            final int idx = i;
            View v = new View(a);
            v.setContentDescription(names[i]);
            v.setBackground(Ui.oval(colors[i], idx == d.theme ? Ui.fg : Ui.line, idx == d.theme ? 3 : 1));
            v.setOnClickListener(x -> {
                Ui.haptic(x, 0);
                d.theme = idx;
                a.render();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(44), Ui.dp(44));
            lp.setMarginEnd(Ui.dp(4));
            sw.addView(v, lp);
        }
        content.addView(sw, Ui.mw(8));
        content.addView(Ui.text(a, names[Math.max(0, Math.min(d.theme, names.length - 1))], 14, Ui.mute, false), Ui.mw(8));

        label(content, Str.t("gradients"));
        final boolean gradOk = Style.userGradients();
        LinearLayout gr = new LinearLayout(a);
        if (!gradOk) gr.setAlpha(0.4f);
        for (int i = -1; i < Ui.GRAD_A.length; i++) {
            final int idx = i;
            FrameLayout cell = new FrameLayout(a);
            boolean on = d.gradient == idx;
            // one shape: the gradient, the ring and the circle are the same path, so nothing spills over the edge
            Surface g = i < 0 ? Surface.oval(Ui.bg | 0xFF000000) : Surface.oval(0).gradient(Ui.GRAD_A[i], Ui.GRAD_B[i]);
            g.stroke(on ? Ui.fg : Ui.line, on ? 3f : 1f);
            cell.setBackground(g);
            cell.setContentDescription(i < 0 ? Str.t("off") : Ui.GRAD_NAMES[i]);
            if (i < 0) {
                TextView x = Ui.text(a, "—", 14, Ui.mute, false);
                x.setGravity(Gravity.CENTER);
                cell.addView(x, new FrameLayout.LayoutParams(-1, -1));
            }
            if (gradOk) {
                cell.setOnClickListener(x -> {
                    Ui.haptic(x, 0);
                    d.gradient = idx;
                    a.render();
                });
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(44), Ui.dp(44));
            lp.setMarginEnd(Ui.dp(4));
            gr.addView(cell, lp);
        }
        content.addView(gr, Ui.mw(8));
        content.addView(Ui.text(a, !gradOk ? Str.t("styleNA") : d.gradient < 0 ? Str.t("off") : Ui.GRAD_NAMES[d.gradient], 14, Ui.mute, false), Ui.mw(8));
    }

    private static void disable(View v) {
        v.setEnabled(false);
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) disable(g.getChildAt(i));
        }
    }

    // ---------- visual style rows ----------
    private View styleRow(final int sid, boolean on) {
        LinearLayout row = Ui.hbox(a);
        float sp = Style.space();
        row.setMinimumHeight(Ui.dp(64));
        row.setPaddingRelative(Ui.dp(12 * sp), Ui.dp(8), Ui.dp(16 * sp), Ui.dp(8));
        row.setBackground(on ? Ui.shape(Ui.alpha(Ui.primary, 0x1F), 20, Ui.primary | 0xFF000000, 2)
                : Ui.shape(Ui.surface, 20, Ui.line, 1));
        row.addView(new StylePreview(a, sid), new LinearLayout.LayoutParams(Ui.dp(64), Ui.dp(44)));
        TextView t = Ui.text(a, Style.NAMES[sid], 16, Ui.fg, on);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, -2, 1f);
        tl.setMarginStart(Ui.dp(16));
        row.addView(t, tl);
        row.setContentDescription(Style.NAMES[sid]);
        row.setSelected(on);
        row.setOnClickListener(v -> {
            Ui.haptic(v, 2);
            a.setStyle(sid);
        });
        return row;
    }

    // a tiny card drawn in that style (its own palette, corners, marker and button)
    private static class StylePreview extends View {
        private final int sid;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF r = new RectF();
        private final float[] radii = new float[8];

        StylePreview(Context c, int sid) {
            super(c);
            this.sid = sid;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        private void box(Canvas c, RectF rr, float rad, boolean organic, int color) {
            Surface.radiiFor(rr.width(), rr.height(), rad, organic, false, radii);
            path.reset();
            path.addRoundRect(rr, radii, Path.Direction.CW);
            p.setColor(color);
            c.drawPath(path, p);
        }

        @Override
        protected void onDraw(Canvas c) {
            boolean dark = Ui.isDark;
            float w = getWidth(), h = getHeight();
            int bg = Style.bgsOf(sid, dark)[0] | 0xFF000000;
            int fg = Style.fgOf(sid, dark, false) | 0xFF000000;
            int pr = Style.primaryOf(sid, dark, fg) | 0xFF000000;
            int card = Style.cardBaseOf(sid, bg, fg, dark, pr) | 0xFF000000;
            boolean sw = sid == Style.SWISS, hum = sid == Style.HUMANIST;
            r.set(0, 0, w, h);
            box(c, r, sw ? 0f : Ui.dp(10), hum, bg);
            // the card
            r.set(Ui.dp(7), Ui.dp(7), w - Ui.dp(7), h - Ui.dp(7));
            float cr = sw ? 0f : sid == Style.CORPORATE ? Ui.dp(8) : Ui.dp(10);
            box(c, r, cr, hum, card);
            c.save();
            c.clipPath(path);
            p.setColor(0xFFE8684A);
            switch (sid) {
                case Style.MATERIAL:
                    r.set(r.left + Ui.dp(4), r.top + Ui.dp(6), r.left + Ui.dp(7), r.bottom - Ui.dp(6));
                    c.drawRoundRect(r, Ui.dp(2), Ui.dp(2), p);
                    break;
                case Style.CORPORATE:
                    c.drawCircle(w - Ui.dp(15), h - Ui.dp(14), Ui.dp(2.5f), p);
                    break;
                case Style.SWISS:
                    c.drawRect(Ui.dp(7), Ui.dp(7), Ui.dp(11), h - Ui.dp(7), p);
                    p.setColor(fg);
                    c.drawRect(Ui.dp(7), Ui.dp(7), w - Ui.dp(7), Ui.dp(9), p);
                    break;
                case Style.HUMANIST:
                    p.setColor((0xFFE8684A & 0x00FFFFFF) | 0x55000000);
                    c.drawCircle(w - Ui.dp(14), Ui.dp(14), Ui.dp(10), p);
                    p.setColor(0xFFE8684A);
                    c.drawRect(Ui.dp(7), Ui.dp(7), Ui.dp(10), h - Ui.dp(7), p);
                    break;
                default:
                    c.drawRect(Ui.dp(7), Ui.dp(7), Ui.dp(10), h - Ui.dp(7), p);
            }
            c.restore();
            // two lines of "text" and a small button in the style's shape
            p.setColor(fg);
            r.set(Ui.dp(16), Ui.dp(14), Ui.dp(40), Ui.dp(17));
            c.drawRoundRect(r, Ui.dp(1.5f), Ui.dp(1.5f), p);
            p.setAlpha(110);
            r.set(Ui.dp(16), Ui.dp(21), Ui.dp(34), Ui.dp(23.5f));
            c.drawRoundRect(r, Ui.dp(1.25f), Ui.dp(1.25f), p);
            p.setAlpha(255);
            r.set(Ui.dp(16), h - Ui.dp(19), Ui.dp(34), h - Ui.dp(12));
            box(c, r, sw ? 0f : Ui.dp(4), hum, pr);
        }
    }

    // ---------- Display: light / dark with pictures, warmth reading, battery saver ----------
    private void display(final Prefs d) {
        LinearLayout tiles = new LinearLayout(a);
        tiles.addView(tileBox(Str.t("light"), new ModeSample(a, ModeSample.LIGHT, d), !d.dark, () -> {
            d.dark = false;
            d.theme = 0;
            a.render();
        }), new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(0, -2, 1f);
        dl.setMarginStart(Ui.dp(12));
        tiles.addView(tileBox(Str.t("dark"), new ModeSample(a, ModeSample.DARK, d), d.dark, () -> {
            d.dark = true;
            d.theme = 0;
            a.render();
        }), dl);
        content.addView(tiles, Ui.mw(8));

        label(content, Str.t("warmth"));
        LinearLayout p = panel(content, 8);
        p.addView(new ModeSample(a, ModeSample.WARM, d), new LinearLayout.LayoutParams(-1, Ui.dp(96)));
        p.addView(Ui.toggleRow(a, Str.t("warmth"), Str.t("warmthHint"), d.warmth, (b, v) -> {
            d.warmth = v;
            a.render();
        }), Ui.mw(8));

        label(content, Str.t("battery"));
        p = panel(content, 8);
        p.addView(new ModeSample(a, ModeSample.BATTERY, d), new LinearLayout.LayoutParams(-1, Ui.dp(96)));
        p.addView(Ui.toggleRow(a, Str.t("battery"), Str.t("batteryHint"), d.battery, (b, v) -> {
            d.battery = v;
            a.render();
        }), Ui.mw(8));
    }

    private LinearLayout tileBox(String name, ModeSample s, boolean on, final Runnable pick) {
        LinearLayout box = Ui.vbox(a);
        box.addView(s, new LinearLayout.LayoutParams(-1, Ui.dp(110)));
        TextView t = Ui.text(a, name, 15, Ui.fg, on);
        t.setGravity(Gravity.CENTER);
        box.addView(t, Ui.mw(8));
        box.setBackground(Ui.shape(0, 20, on ? Ui.fg : Ui.line, on ? 2 : 1));
        box.setPadding(Ui.dp(8), Ui.dp(8), Ui.dp(8), Ui.dp(10));
        box.setContentDescription(name);
        box.setOnClickListener(v -> {
            Ui.haptic(v, 0);
            pick.run();
        });
        Ui.press(box);
        return box;
    }

    // small picture of a mode: page colour, a mini card, and (for warmth / battery) their own mark
    private static class ModeSample extends View {
        static final int LIGHT = 0, DARK = 1, WARM = 2, BATTERY = 3;
        private final int kind;
        private final Prefs d;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        ModeSample(Context c, int kind, Prefs d) {
            super(c);
            this.kind = kind;
            this.d = d;
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = Ui.dp(14);
            boolean dark = kind == DARK || ((kind == WARM || kind == BATTERY) && d.dark);
            int bg = kind == DARK ? 0xFF0E0E10 : kind == LIGHT ? 0xFFF6F4EF : (d.dark ? 0xFF16161D : 0xFFF6F4EF);
            int fg = dark ? 0xFFF1F1F1 : 0xFF1A1A1A;
            if (kind == WARM) bg = Ui.mix(bg, 0xFFFF9A3C, dark ? 0.22f : 0.3f);
            p.setStyle(Paint.Style.FILL);
            p.setColor(bg);
            c.drawRoundRect(new RectF(0, 0, w, h), r, r, p);
            // mini card
            float cx = w * 0.12f, cy = h * 0.22f, cw = w * 0.76f, ch = h * 0.56f;
            p.setColor(Ui.mix(bg, 0xFFE8684A, 0.22f));
            c.drawRoundRect(new RectF(cx, cy, cx + cw, cy + ch), Ui.dp(10), Ui.dp(10), p);
            p.setColor(0xFFE8684A);
            c.drawRoundRect(new RectF(cx, cy, cx + Ui.dp(4), cy + ch), Ui.dp(2), Ui.dp(2), p);
            p.setColor(fg);
            c.drawRoundRect(new RectF(cx + w * 0.07f, cy + ch * 0.22f, cx + w * 0.45f, cy + ch * 0.22f + Ui.dp(6)), Ui.dp(3), Ui.dp(3), p);
            p.setAlpha(110);
            c.drawRoundRect(new RectF(cx + w * 0.07f, cy + ch * 0.55f, cx + w * 0.6f, cy + ch * 0.55f + Ui.dp(5)), Ui.dp(3), Ui.dp(3), p);
            p.setAlpha(255);
            if (kind == BATTERY) { // battery with "30 FPS"
                p.setColor(fg);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Ui.dp(1.5f));
                float bx = w - Ui.dp(52), by = Ui.dp(10);
                c.drawRoundRect(new RectF(bx, by, bx + Ui.dp(28), by + Ui.dp(14)), Ui.dp(3), Ui.dp(3), p);
                p.setStyle(Paint.Style.FILL);
                c.drawRect(bx + Ui.dp(29), by + Ui.dp(4), bx + Ui.dp(31), by + Ui.dp(10), p);
                c.drawRect(bx + Ui.dp(3), by + Ui.dp(3), bx + Ui.dp(12), by + Ui.dp(11), p);
                p.setTextSize(Ui.dp(11));
                p.setFakeBoldText(true);
                c.drawText("30 FPS", w - Ui.dp(86), h - Ui.dp(10), p);
                p.setFakeBoldText(false);
            }
        }
    }

    // ---------- Notifications: Android behaviour, permission, sounds ----------
    private void notifs(final Prefs d) {
        LinearLayout p = panel(content, 8);
        LinearLayout row = Ui.hbox(a);
        row.setMinimumHeight(Ui.dp(56));
        row.addView(Ui.text(a, Str.t("notifPerm"), 16, Ui.fg, false), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(Ui.text(a, a.notificationsEnabled() ? Str.t("granted") : Str.t("denied"), 14, Ui.mute, true));
        p.addView(row);
        if (!a.notificationsEnabled()) p.addView(Ui.btn(a, Str.t("allow"), true, v -> a.requestNotifications()), Ui.mw(4));
        p.addView(Ui.btn(a, Str.t("openAndroid"), false, v -> a.openAndroidNotifSettings()), Ui.mw(8));
        p.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(16));

        soundBlock(Str.t("soundGeneral"), d.soundGeneral, false);
        soundBlock(Str.t("soundDeadline"), d.soundDeadline, true);
    }

    private void soundBlock(String title, String value, final boolean deadline) {
        label(content, title);
        LinearLayout p = panel(content, 8);
        p.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
        p.addView(Ui.text(a, a.soundName(value), 16, Ui.fg, true), Ui.mw());
        LinearLayout acts = Ui.hbox(a);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1f);
        l1.setMarginEnd(Ui.dp(8));
        acts.addView(Ui.btn(a, Str.t("choose"), true, v -> a.pickSound(deadline)), l1);
        acts.addView(Ui.btn(a, Str.t("test"), false, v -> Notifier.test(a, deadline)), new LinearLayout.LayoutParams(0, -2, 1f));
        p.addView(acts, Ui.mw(12));
    }

    // ---------- Reminders: frequency + exact time (separate from notification settings) ----------
    private void reminders(final Prefs d) {
        label(content, Str.t("freq"));
        final String[] keys = {"off", "due", "dx", "daily"};
        final String[] labels = {Str.t("off"), Str.t("onDue"), Str.t("dMinusX"), Str.t("daily")};
        for (int i = 0; i < keys.length; i++) {
            final String k = keys[i];
            content.addView(Ui.option(a, labels[i], d.freq.equals(k), v -> {
                Ui.haptic(v, 0);
                d.freq = k;
                if (!k.equals("off")) a.ensureNotifPermission();
                a.render();
            }), Ui.mw(8));
        }
        if (d.freq.equals("dx")) {
            LinearLayout p = panel(content, 8);
            p.addView(Ui.stepper(a, Str.t("daysBefore"), d.daysBefore, 1, 30, "%d", v -> {
                d.daysBefore = v;
                a.render();
            }));
        }
        if (d.freq.equals("off")) return;

        label(content, Str.t("remTime"));
        LinearLayout p = panel(content, 8);
        final int[] t = Notifier.parseTime(d.time);
        p.addView(Ui.stepper(a, Str.t("hour"), t[0], 0, 23, "%02d", v -> setTime(d, v, t[1], t[2])));
        divider(p);
        p.addView(Ui.stepper(a, Str.t("minute"), t[1], 0, 59, "%02d", v -> setTime(d, t[0], v, t[2])));
        divider(p);
        p.addView(Ui.stepper(a, Str.t("second"), t[2], 0, 59, "%02d", v -> setTime(d, t[0], t[1], v)));
        if (!a.canExactAlarms()) {
            content.addView(Ui.text(a, Str.t("exactHint"), 13, Ui.mute, false), Ui.mw(12));
            content.addView(Ui.btn(a, Str.t("exactBtn"), false, v -> a.requestExactAlarms()), Ui.mw(8));
        }
    }

    private void setTime(Prefs d, int h, int m, int s) {
        d.time = String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
        a.render();
    }

    // ---------- Language ----------
    private void language(final Prefs d) {
        for (int i = 0; i < Str.NAMES.length; i++) {
            final int idx = i;
            content.addView(Ui.option(a, Str.NAMES[i], d.lang == i, v -> {
                Ui.haptic(v, 0);
                d.lang = idx;
                a.render();
            }), Ui.mw(i == 0 ? 8 : 8));
        }
    }

    // ---------- Directory (unchanged behaviour: same folder picker and saved values) ----------
    private void directory(final Prefs d) {
        LinearLayout p = panel(content, 8);
        p.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
        p.addView(Ui.text(a, Str.t("storage"), 16, Ui.fg, true), Ui.mw());
        p.addView(Ui.text(a, d.folder.isEmpty() ? "—" : d.folder, 14, Ui.mute, false), Ui.mw(4));
        p.addView(Ui.btn(a, Str.t("chooseFolder"), false, v -> a.pickFolder()), Ui.mw(12));
    }

    // ---------- Accessibility ----------
    private void accessibility(final Prefs d) {
        LinearLayout p = panel(content, 8);
        p.addView(Ui.toggleRow(a, Str.t("colorblind"), null, d.cb, (b, v) -> {
            d.cb = v;
            a.render();
        }));
        divider(p);
        p.addView(Ui.toggleRow(a, Str.t("reduceMotion"), null, d.reduceMotion, (b, v) -> {
            d.reduceMotion = v;
            a.render();
        }));
        divider(p);
        p.addView(Ui.toggleRow(a, Str.t("haptics"), null, d.haptics, (b, v) -> {
            d.haptics = v;
            a.render();
        }));
    }

    // ---------- Experimental ----------
    private void experimental(final Prefs d) {
        LinearLayout p = panel(content, 8);
        p.addView(Ui.toggleRow(a, Str.t("windowBlur"), Str.t("windowBlurHint"), d.windowBlur, (b, v) -> {
            d.windowBlur = v;
            a.render();
        }));
        divider(p);
        p.addView(Ui.toggleRow(a, Str.t("motionBlur"), Str.t("motionBlurHint"), d.motionBlur, (b, v) -> {
            d.motionBlur = v;
            a.render();
        }));
    }

    // ---------- About ----------
    private void about() {
        TextView name = Ui.text(a, "Memorius", 32, Ui.fg, true);
        name.setLetterSpacing(-0.03f);
        content.addView(name, Ui.mw(16));
        LinearLayout p = panel(content, 16);
        p.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
        LinearLayout v = Ui.hbox(a);
        v.addView(Ui.text(a, Str.t("version"), 16, Ui.fg, false), new LinearLayout.LayoutParams(0, -2, 1f));
        v.addView(Ui.text(a, VERSION, 16, Ui.fg, true));
        p.addView(v);
        divider(p);
        TextView link = Ui.text(a, REPO, 14, Ui.fg, false);
        link.setPaintFlags(link.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        link.setMinHeight(Ui.dp(48));
        link.setGravity(Gravity.CENTER_VERTICAL);
        link.setContentDescription(Str.t("repo"));
        link.setOnClickListener(x -> a.openUrl(REPO));
        p.addView(Ui.text(a, Str.t("repo"), 13, Ui.mute, false), Ui.mw(12));
        p.addView(link, Ui.mw());

        label(content, Str.t("credits"));
        p = panel(content, 8);
        p.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
        p.addView(Ui.text(a, "Sillysamson", 16, Ui.fg, true), Ui.mw());
        p.addView(Ui.text(a, Str.t("roleDesign") + " · " + Str.t("roleCode") + " · " + Str.t("roleIdea"), 14, Ui.mute, false), Ui.mw(2));
        p.addView(Ui.text(a, "Dina", 16, Ui.fg, true), Ui.mw(16));
        p.addView(Ui.text(a, Str.t("roleIdea"), 14, Ui.mute, false), Ui.mw(2));
    }
}
