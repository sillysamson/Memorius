package com.memorius.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class S {
    static boolean dirty = false;

    static SharedPreferences p(Context c) { return c.getSharedPreferences("memorius", Context.MODE_PRIVATE); }

    // ---------- language ----------
    static String lang(Context c) { return p(c).getString("lang", "en-GB"); }

    static Locale locale(Context c) {
        String l = lang(c);
        if (l.equals("id")) return new Locale("in", "ID");
        if (l.equals("fr")) return Locale.FRANCE;
        return Locale.UK;
    }

    static Context wrap(Context b) {
        Locale l = locale(b);
        Locale.setDefault(l);
        Configuration cf = new Configuration(b.getResources().getConfiguration());
        cf.setLocale(l);
        return b.createConfigurationContext(cf);
    }

    static String label(Context c, LocalDate d) {
        long n = ChronoUnit.DAYS.between(LocalDate.now(), d);
        if (n < 0) return n == -1 ? c.getString(R.string.overdue_1) : c.getString(R.string.overdue_n, -n);
        if (n == 0) return c.getString(R.string.due_today);
        if (n == 1) return c.getString(R.string.due_tomorrow);
        return c.getString(R.string.due_in, n);
    }

    static String kindLabel(Context c, Task t) { return c.getString(t.exam() ? R.string.exam : R.string.homework); }

    // ---------- theme ----------
    static boolean light(Context c) { return p(c).getBoolean("light", false); }
    static int base(Context c) { return p(c).getInt("bg", 0xFF38385A); }

    static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    static int bg(Context c) { int b = base(c); return light(c) ? mix(b, 0xFFFFFFFF, .88f) : b; }
    static int card(Context c) {
        if (light(c)) return 0xFFFFFFFF;
        int b = base(c);
        return b == 0xFF38385A ? 0xFF1A0F28 : mix(b, 0xFF000000, .62f);
    }
    static int ink(Context c) { return light(c) ? 0xFF1C1C2E : 0xFFFFFFFF; }
    static int mute(Context c) { return light(c) ? 0xFF6C6C85 : 0xFFB9B6D6; }
    static int field(Context c) { return light(c) ? 0x14000000 : 0x1FFFFFFF; }

    // frosted dock buttons: a darker copy of the background
    static int frostTint(Context c, boolean blur) {
        int col = light(c) ? mix(bg(c), 0xFF000000, .14f) : card(c);
        return ((blur ? 0xB8 : 0xEE) << 24) | (col & 0xFFFFFF);
    }

    // frosted card
    static int sheetTint(Context c) { return (0x99 << 24) | (card(c) & 0xFFFFFF); }

    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    static void round(View v, int color, float rPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(rPx);
        v.setBackground(g);
    }

    static void apply(Context c, View v) {
        Object t = v.getTag();
        if ("ink".equals(t)) {
            if (v instanceof TextView) ((TextView) v).setTextColor(ink(c));
            else if (v instanceof ImageView) ((ImageView) v).setColorFilter(ink(c));
        } else if ("mute".equals(t)) {
            if (v instanceof TextView) ((TextView) v).setTextColor(mute(c));
        } else if ("field".equals(t)) {
            round(v, field(c), dp(c, 10));
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setTextColor(ink(c));
                tv.setHintTextColor((mute(c) & 0xFFFFFF) | 0x99000000);
            }
        } else if ("card".equals(t)) {
            round(v, card(c), dp(c, 32));
        } else if ("fade".equals(t)) {
            int cc = card(c);
            v.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{cc & 0xFFFFFF, cc}));
        } else if ("page".equals(t)) {
            v.setBackgroundColor(bg(c));
        } else if ("drop".equals(t)) {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(c, 16));
            g.setStroke(dp(c, 1.5f), (mute(c) & 0xFFFFFF) | 0x66000000, dp(c, 6), dp(c, 4));
            v.setBackground(g);
            if (v instanceof TextView) ((TextView) v).setTextColor(mute(c));
        } else if ("menu".equals(t)) {
            round(v, mix(card(c), light(c) ? 0xFF000000 : 0xFFFFFFFF, .08f), dp(c, 24));
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) apply(c, g.getChildAt(i));
        }
    }

    // ---------- storage folder ----------
    static Uri folder(Context c) {
        String s = p(c).getString("folder", null);
        return s == null ? null : Uri.parse(s);
    }

    static String folderName(Context c) {
        Uri u = folder(c);
        if (u == null) return null;
        try {
            String id = DocumentsContract.getTreeDocumentId(u);
            int i = id.lastIndexOf(':');
            String n = i >= 0 ? id.substring(i + 1) : id;
            return n.isEmpty() ? id : n;
        } catch (Exception e) { return u.toString(); }
    }

    static Uri create(Context c, String mime, String name) {
        try {
            Uri tree = folder(c);
            if (tree == null) return null;
            Uri dir = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
            return DocumentsContract.createDocument(c.getContentResolver(), dir, mime, name);
        } catch (Exception e) { return null; }
    }

    static String displayName(Context c, Uri u) {
        String n = u.getLastPathSegment();
        try (Cursor cur = c.getContentResolver().query(u, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                int ix = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (ix >= 0) n = cur.getString(ix);
            }
        } catch (Exception e) { }
        return n == null ? "file" : n;
    }

    // copy a picked file into the dedicated folder; returns encoded "name\u0001uri" or null
    static String copyIn(Context c, Uri src, String name) {
        try {
            String mime = c.getContentResolver().getType(src);
            if (mime == null) mime = "application/octet-stream";
            Uri dst = create(c, mime, name);
            if (dst == null) return null;
            try (InputStream in = c.getContentResolver().openInputStream(src);
                 OutputStream out = c.getContentResolver().openOutputStream(dst)) {
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) out.write(b, 0, n);
            }
            return Task.enc(name, dst.toString());
        } catch (Exception e) { return null; }
    }
}
