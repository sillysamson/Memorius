package com.memorius.app;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ProjectsPage extends Page {
    final ScrollView sv;
    final LinearLayout list;
    PdfAdapter pdf;
    final Handler handler = new Handler(Looper.getMainLooper());
    final Runnable saver = Store::save;

    ProjectsPage(MainActivity a) {
        super(a);
        sv = Page.scroller(a);
        list = Ui.vbox(a);
        list.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(136));
        sv.addView(list, new FrameLayout.LayoutParams(-1, -2));
    }

    private void saveSoon() {
        handler.removeCallbacks(saver);
        handler.postDelayed(saver, 500);
    }

    private void closePdf() {
        if (pdf != null) {
            pdf.close();
            pdf = null;
        }
    }

    @Override
    void refresh() {
        closePdf();
        root.removeAllViews();
        if (a.openOpus != null) {
            root.addView(editor(a.openOpus), new FrameLayout.LayoutParams(-1, -1));
            return;
        }
        list.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(136) + Ui.insetBottom);
        list.removeAllViews();
        if (Store.opus.isEmpty()) {
            TextView e = Ui.text(a, Str.t("empty"), 16, Ui.mute, false);
            e.setGravity(Gravity.CENTER);
            list.addView(e, Ui.mw(Ui.dp(0) + 200));
        }
        for (int i = 0; i < Store.opus.size(); i++) list.addView(row(Store.opus.get(i), i), Ui.mw(i == 0 ? 0 : 8));
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    private View row(final Opus o, int i) {
        LinearLayout r = Ui.vbox(a);
        r.setBackground(Ui.shape(Ui.surface, 16, 0, 0));
        r.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
        LinearLayout top = Ui.hbox(a);
        top.addView(Ui.text(a, o.title, 16, Ui.fg, true), new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(Ui.tag(a, o.ext.toUpperCase(Locale.ROOT)));
        if (a.mode.equals("move")) top.addView(Ui.moveBtns(a, Store.opus, i, false, null));
        r.addView(top, Ui.mw());

        LinearLayout bar = new LinearLayout(a);
        bar.setBackground(Ui.shape(Ui.line, 999, 0, 0));
        View fill = new View(a);
        fill.setBackground(Ui.shape(Ui.fg, 999, 0, 0));
        bar.addView(fill, new LinearLayout.LayoutParams(0, -1, o.progress));
        bar.addView(new View(a), new LinearLayout.LayoutParams(0, -1, 100 - o.progress));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, Ui.dp(8));
        bl.topMargin = Ui.dp(8);
        r.addView(bar, bl);
        r.addView(Ui.text(a, o.progress + "%", 14, Ui.mute, false), Ui.mw(4));

        if (a.mode.equals("delete")) {
            r.setOnClickListener(v -> a.confirm(() -> {
                Store.opus.remove(o);
                Store.save();
                a.render();
            }));
        } else if (a.mode.isEmpty()) {
            r.setOnClickListener(v -> {
                a.openOpus = o;
                a.render();
            });
            Ui.press(r);
        }
        return r;
    }

    private View editor(final Opus o) {
        LinearLayout col = Ui.vbox(a);
        col.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(100) + Ui.insetBottom);

        LinearLayout bar = Ui.hbox(a);
        bar.addView(Ui.btn(a, "← " + Str.t("back"), false, v -> {
            a.openOpus = null;
            a.render();
        }), new LinearLayout.LayoutParams(-2, -2));
        TextView title = Ui.text(a, o.title, 18, Ui.fg, true);
        title.setSingleLine(true);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, -2, 1f);
        tl.setMarginStart(Ui.dp(16));
        bar.addView(title, tl);
        col.addView(bar, Ui.mw());

        LinearLayout pr = Ui.hbox(a);
        final TextView pct = Ui.text(a, o.progress + "%", 14, Ui.mute, false);
        LiquidSlider sb = new LiquidSlider(a, 0, 100, o.progress, 5, v -> {
            o.progress = v;
            pct.setText(o.progress + "%");
            saveSoon();
        });
        sb.setContentDescription(Str.t("progress"));
        pr.addView(sb, new LinearLayout.LayoutParams(0, -2, 1f));
        pr.addView(pct, new LinearLayout.LayoutParams(Ui.dp(48), -2));
        col.addView(pr, Ui.mw(8));

        if (o.kind.equals("pdf")) {
            RecyclerView rv = new RecyclerView(a);
            rv.setLayoutManager(new LinearLayoutManager(a));
            try {
                int w = a.getResources().getDisplayMetrics().widthPixels - Ui.dp(32);
                pdf = new PdfAdapter(a, o.content, w);
                rv.setAdapter(pdf);
            } catch (Exception e) {
                a.toast(Str.t("cantRead"));
            }
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, 0, 1f);
            rl.topMargin = Ui.dp(8);
            col.addView(rv, rl);
            col.addView(Ui.btn(a, Str.t("download"), false, v -> {
                try {
                    a.exportFile(o.title + ".pdf", "application/pdf", Files.readFile(o.content));
                } catch (Exception e) {
                    a.toast(Str.t("cantRead"));
                }
            }), Ui.mw(16));
        } else {
            final EditText ed = new EditText(a);
            ed.setText(o.content);
            ed.setTextColor(Ui.fg);
            ed.setHintTextColor(Ui.mute);
            ed.setTextSize(16);
            ed.setGravity(Gravity.TOP);
            ed.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            ed.setBackground(Ui.shape(Ui.surface, 16, Ui.line, 1));
            ed.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(16));
            ed.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int st, int c, int af) {
                }

                @Override
                public void onTextChanged(CharSequence s, int st, int b, int c) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    o.content = s.toString();
                    saveSoon();
                }
            });
            LinearLayout.LayoutParams el = new LinearLayout.LayoutParams(-1, 0, 1f);
            el.topMargin = Ui.dp(16);
            col.addView(ed, el);

            LinearLayout exp = Ui.hbox(a);
            String[] fmts = {"TXT", "DOC", "PDF"};
            for (int i = 0; i < fmts.length; i++) {
                final String f = fmts[i];
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
                if (i > 0) lp.setMarginStart(Ui.dp(8));
                exp.addView(Ui.btn(a, f, false, v -> export(o, f)), lp);
            }
            col.addView(exp, Ui.mw(16));
        }
        return col;
    }

    private void export(Opus o, String fmt) {
        try {
            handler.removeCallbacks(saver);
            Store.save();
            if (fmt.equals("TXT")) a.exportFile(o.title + ".txt", "text/plain", o.content.getBytes("UTF-8"));
            else if (fmt.equals("DOC")) a.exportFile(o.title + ".doc", "application/msword", Files.docBytes(o.content));
            else a.exportFile(o.title + ".pdf", "application/pdf", Files.pdfBytes(o.content));
        } catch (Exception e) {
            a.toast(Str.t("cantRead"));
        }
    }

    // ---------- add ----------
    void writeForm() {
        final String[] ext = {"txt"};
        final String[] exts = {"txt", "doc", "pdf"};
        final EditText ti = Ui.input(a, Str.t("title"), false);
        LinearLayout seg = Ui.seg(a, new String[]{"TXT", "DOC", "PDF"}, 0, i -> ext[0] = exts[i]);
        List<View> rows = new ArrayList<>();
        rows.add(Ui.field(a, Str.t("title"), ti));
        rows.add(seg);
        a.showForm(rows, () -> {
            String t = ti.getText().toString().trim();
            if (t.isEmpty()) {
                ti.requestFocus();
                return false;
            }
            Opus o = new Opus();
            o.title = t;
            o.ext = ext[0];
            Store.opus.add(o);
            a.openOpus = o;
            Store.save();
            return true;
        });
    }

    void importUri(Uri u) {
        String name = Files.queryName(a, u);
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "txt";
        try {
            Opus o = new Opus();
            o.title = base;
            if (ext.equals("pdf")) {
                o.kind = "pdf";
                o.ext = "pdf";
                o.content = Files.copyToApp(a, u);
            } else if (ext.equals("docx")) {
                o.ext = "doc";
                o.content = Files.docxText(a, u);
            } else {
                String t = Files.readText(a, u);
                if (t.indexOf('\u0000') >= 0) {
                    a.toast(Str.t("cantRead"));
                    return;
                }
                if (ext.equals("doc") && t.toLowerCase(Locale.ROOT).contains("<body")) t = Files.htmlToText(t);
                o.ext = ext.equals("doc") ? "doc" : "txt";
                o.content = t;
            }
            Store.opus.add(o);
            a.openOpus = o;
            Store.save();
            a.render();
        } catch (Exception e) {
            a.toast(Str.t("cantRead"));
        }
    }
}
