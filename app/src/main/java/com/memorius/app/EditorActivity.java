package com.memorius.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.InputStream;
import java.io.OutputStream;

public class EditorActivity extends Activity {
    static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    EditText et, title;
    String mode;
    Uri uri;
    boolean existing;

    @Override protected void attachBaseContext(Context b) { super.attachBaseContext(S.wrap(b)); }

    int dp(float v) { return S.dp(this, v); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setTheme(S.light(this) ? R.style.AppThemeLight : R.style.AppTheme);
        mode = getIntent().getStringExtra("mode");
        String u = getIntent().getStringExtra("uri");
        String name = getIntent().getStringExtra("name");
        existing = u != null;
        uri = existing ? Uri.parse(u) : null;
        if (existing) mode = name != null && name.toLowerCase().endsWith(".pdf") ? "pdf" : "docx";
        build(name);
    }

    TextView btn(String label, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(16);
        t.setTextColor(S.ink(this));
        t.setGravity(Gravity.CENTER);
        t.setTypeface(t.getTypeface(), Typeface.BOLD);
        S.round(t, S.field(this), dp(12));
        t.setOnClickListener(l);
        return t;
    }

    void build(String name) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(S.bg(this));
        root.setFitsSystemWindows(true);
        root.setPadding(dp(14), dp(8), dp(14), dp(8));
        setContentView(root);
        getWindow().setStatusBarColor(S.bg(this));
        getWindow().setNavigationBarColor(S.bg(this));
        if (S.light(this)) root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        boolean pdfView = existing && mode.equals("pdf");

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = new TextView(this);
        back.setText("←");
        back.setTextSize(28);
        back.setTextColor(S.ink(this));
        back.setPadding(0, 0, dp(12), 0);
        back.setOnClickListener(v -> finish());
        top.addView(back);
        title = new EditText(this);
        title.setHint(R.string.doc_title);
        title.setSingleLine(true);
        title.setTextColor(S.ink(this));
        title.setHintTextColor(S.mute(this));
        title.setBackground(null);
        title.setTextSize(20);
        if (existing && name != null) {
            int dot = name.lastIndexOf('.');
            title.setText(dot > 0 ? name.substring(0, dot) : name);
            title.setEnabled(false);
        }
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        if (!pdfView) {
            TextView save = new TextView(this);
            save.setText(R.string.save);
            save.setTextColor(0xFFFFFFFF);
            save.setTextSize(15);
            save.setGravity(Gravity.CENTER);
            save.setTypeface(save.getTypeface(), Typeface.BOLD);
            save.setBackgroundResource(R.drawable.bg_navy);
            save.setPadding(dp(20), 0, dp(20), 0);
            save.setOnClickListener(v -> save());
            top.addView(save, new LinearLayout.LayoutParams(-2, dp(44)));
        }
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(56)));

        if (pdfView) {
            TextView note = new TextView(this);
            note.setText(R.string.pdf_readonly);
            note.setTextColor(S.mute(this));
            note.setPadding(0, dp(8), 0, dp(12));
            root.addView(note);
            ScrollView sv = new ScrollView(this);
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            sv.addView(box);
            root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));
            showPdf(box);
            return;
        }

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout tb = new LinearLayout(this);
        String[] labels = {"B", "I", "H", "•", "A−", "A+"};
        for (int i = 0; i < labels.length; i++) {
            final int k = i;
            TextView b = btn(labels[i], v -> tool(k));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(56), dp(44));
            lp.rightMargin = dp(8);
            tb.addView(b, lp);
        }
        hs.addView(tb);
        root.addView(hs, new LinearLayout.LayoutParams(-1, dp(52)));

        et = new EditText(this);
        et.setGravity(Gravity.TOP);
        et.setTextSize(14);
        et.setTextColor(S.ink(this));
        et.setBackground(null);
        et.setPadding(dp(4), dp(10), dp(4), dp(10));
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.addView(et, new FrameLayout.LayoutParams(-1, -1));
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        if (existing) {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                et.setText(Docx.read(in));
            } catch (Exception e) {
                Toast.makeText(this, R.string.open_fail, Toast.LENGTH_LONG).show();
            }
        }
    }

    void showPdf(LinearLayout box) {
        try {
            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
            PdfRenderer r = new PdfRenderer(pfd);
            int w = getResources().getDisplayMetrics().widthPixels - dp(28);
            for (int i = 0; i < Math.min(r.getPageCount(), 60); i++) {
                PdfRenderer.Page pg = r.openPage(i);
                Bitmap bm = Bitmap.createBitmap(w, (int) (w * (pg.getHeight() / (float) pg.getWidth())), Bitmap.Config.ARGB_8888);
                bm.eraseColor(0xFFFFFFFF);
                pg.render(bm, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                pg.close();
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(bm);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = dp(10);
                box.addView(iv, lp);
            }
            r.close();
            pfd.close();
        } catch (Exception e) {
            Toast.makeText(this, R.string.open_fail, Toast.LENGTH_LONG).show();
        }
    }

    // ---------- formatting ----------
    void tool(int k) {
        Editable e = et.getText();
        int s = Math.min(et.getSelectionStart(), et.getSelectionEnd()), t = Math.max(et.getSelectionStart(), et.getSelectionEnd());
        if (k == 3) { bullets(e, s, t); return; }
        if (s == t) { Toast.makeText(this, R.string.select_text, Toast.LENGTH_SHORT).show(); return; }
        if (k == 0) toggleStyle(e, s, t, Typeface.BOLD);
        else if (k == 1) toggleStyle(e, s, t, Typeface.ITALIC);
        else if (k == 2) heading(e, s, t);
        else if (k == 4) resize(e, s, t, -2);
        else if (k == 5) resize(e, s, t, 2);
    }

    void toggleStyle(Editable e, int s, int t, int style) {
        boolean had = false;
        for (StyleSpan sp : e.getSpans(s, t, StyleSpan.class))
            if ((sp.getStyle() & style) != 0) { had = true; e.removeSpan(sp); }
        if (!had) e.setSpan(new StyleSpan(style), s, t, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    int sizeAt(Editable e, int s, int t) {
        AbsoluteSizeSpan[] a = e.getSpans(s, t, AbsoluteSizeSpan.class);
        return a.length > 0 ? a[a.length - 1].getSize() : 14;
    }

    void setSize(Editable e, int s, int t, int size) {
        for (AbsoluteSizeSpan sp : e.getSpans(s, t, AbsoluteSizeSpan.class)) e.removeSpan(sp);
        e.setSpan(new AbsoluteSizeSpan(Math.max(8, Math.min(48, size)), true), s, t, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    void resize(Editable e, int s, int t, int d) { setSize(e, s, t, sizeAt(e, s, t) + d); }

    void heading(Editable e, int s, int t) {
        boolean big = sizeAt(e, s, t) >= 24;
        setSize(e, s, t, big ? 14 : 26);
        for (StyleSpan sp : e.getSpans(s, t, StyleSpan.class)) e.removeSpan(sp);
        if (!big) e.setSpan(new StyleSpan(Typeface.BOLD), s, t, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    void bullets(Editable e, int s, int t) {
        String str = e.toString();
        int start = s == 0 ? 0 : str.lastIndexOf('\n', s - 1) + 1;
        int end = t;
        int pos = start;
        boolean allHave = true;
        int p = start;
        while (p <= end) {
            if (!str.startsWith("• ", p)) allHave = false;
            int nl = str.indexOf('\n', p);
            if (nl < 0 || nl >= end) break;
            p = nl + 1;
        }
        // process from the last line backwards so offsets stay valid
        java.util.ArrayList<Integer> starts = new java.util.ArrayList<>();
        p = start;
        while (p <= end) {
            starts.add(p);
            int nl = str.indexOf('\n', p);
            if (nl < 0 || nl >= end) break;
            p = nl + 1;
        }
        for (int i = starts.size() - 1; i >= 0; i--) {
            int at = starts.get(i);
            if (allHave) e.delete(at, at + 2); else e.insert(at, "• ");
        }
    }

    // ---------- save ----------
    void save() {
        try {
            Uri out = uri;
            String name = null;
            if (!existing) {
                String t = title.getText().toString().trim();
                if (t.isEmpty()) t = getString(R.string.untitled);
                boolean pdf = mode.equals("pdf");
                name = t + (pdf ? ".pdf" : ".docx");
                out = S.create(this, pdf ? "application/pdf" : DOCX, name);
                if (out == null) {
                    Toast.makeText(this, R.string.need_folder, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, SettingsActivity.class));
                    return;
                }
            }
            OutputStream os = getContentResolver().openOutputStream(out, "wt");
            if (mode.equals("pdf")) PdfMaker.write(os, new SpannableStringBuilder(et.getText()));
            else Docx.write(os, new SpannableStringBuilder(et.getText()));
            os.close();
            Intent r = new Intent();
            if (!existing) { r.putExtra("name", name); r.putExtra("uri", out.toString()); }
            setResult(RESULT_OK, r);
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, R.string.save_fail, Toast.LENGTH_LONG).show();
        }
    }
}
