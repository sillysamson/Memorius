package com.memorius.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

class CardsPage extends Page {
    static final String[] TYPES = {"homework", "exam", "prepping"};
    final LinearLayout content;

    CardsPage(MainActivity a) {
        super(a);
        ScrollView sv = new ScrollView(a);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(120));
        sv.addView(content, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    static String fmtDue(Card c) {
        if (c.due.isEmpty()) return "—";
        return (c.type.equals("prepping") ? Str.t("date") : Str.t("deadline")) + ": " + c.due;
    }

    // card surface: tinted body, colour bar on the start edge. Shared by the list card and the expanded card.
    static LinearLayout shell(MainActivity a, Card c) {
        int ci = Math.max(0, Math.min(5, c.color));
        int col = Ui.palette()[ci];
        LinearLayout s = new LinearLayout(a);
        s.setOrientation(LinearLayout.HORIZONTAL);
        s.setBackground(Ui.shape(Ui.mix(Ui.bg, col, 0.22f), 28, 0, 0));
        s.setClipToOutline(true);
        View bar = new View(a);
        bar.setBackgroundColor(col);
        s.addView(bar, new LinearLayout.LayoutParams(Ui.dp(6), -1));
        LinearLayout in = new LinearLayout(a);
        in.setOrientation(LinearLayout.VERTICAL);
        in.setPaddingRelative(Ui.dp(18), Ui.dp(16), Ui.dp(24), Ui.dp(16));
        s.addView(in, new LinearLayout.LayoutParams(0, -2, 1f));
        return s;
    }

    static LinearLayout head(MainActivity a, Card c) {
        int ci = Math.max(0, Math.min(5, c.color));
        LinearLayout row = Ui.hbox(a);
        if (Store.prefs.cb) {
            TextView sym = Ui.text(a, Ui.SYM[ci], 14, Ui.fg, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMarginEnd(Ui.dp(8));
            row.addView(sym, lp);
        }
        TextView subj = Ui.text(a, c.subject, 18, Ui.fg, true);
        if (c.done) subj.setPaintFlags(subj.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        row.addView(subj, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-2, -2);
        tl.setMarginStart(Ui.dp(8));
        row.addView(Ui.tag(a, Str.t(c.type)), tl);
        return row;
    }

    @Override
    void refresh() {
        content.setPadding(Ui.dp(16), Ui.dp(16), Ui.dp(16), Ui.dp(120) + Ui.insetBottom);
        content.removeAllViews();
        final List<Card> list = Store.cards;
        if (list.isEmpty()) {
            TextView e = Ui.text(a, Str.t("empty"), 16, Ui.mute, false);
            e.setGravity(Gravity.CENTER);
            content.addView(e, Ui.mw());
            return;
        }
        // first card sits at the bottom, new ones stack upward
        for (int i = list.size() - 1; i >= 0; i--) {
            content.addView(cardView(list.get(i), i), Ui.mw(i == list.size() - 1 ? 0 : 16));
        }
        if (a.mode.isEmpty()) {
            TextView hint = Ui.text(a, Str.t("holdHint"), 13, Ui.mute, false);
            hint.setGravity(Gravity.CENTER);
            content.addView(hint, Ui.mw(16));
        }
    }

    private View cardView(final Card c, int i) {
        final LinearLayout s = shell(a, c);
        LinearLayout in = (LinearLayout) s.getChildAt(1);
        LinearLayout head = head(a, c);
        in.addView(head, Ui.mw());
        in.addView(Ui.text(a, fmtDue(c), 14, Ui.mute, false), Ui.mw(4));
        if (c.done) s.setAlpha(0.45f);

        if (a.mode.equals("delete")) {
            s.setOnClickListener(v -> a.confirm(() -> {
                Store.cards.remove(c);
                Store.save();
                a.render();
            }));
        } else if (a.mode.equals("move")) {
            head.addView(Ui.moveBtns(a, Store.cards, i, true, null));
        } else {
            TextView check = Ui.text(a, "✓", 14, c.done ? Ui.bg : Ui.fg, false);
            check.setGravity(Gravity.CENTER);
            check.setBackground(c.done ? Ui.oval(Ui.fg, 0, 0) : Ui.oval(Color.TRANSPARENT, Ui.line, 1));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(32), Ui.dp(32));
            lp.setMarginStart(Ui.dp(8));
            check.setOnClickListener(v -> {
                c.done = !c.done;
                Store.save();
                a.render();
            });
            head.addView(check, lp);
            s.setOnClickListener(v -> a.expandCard(c, s, true));
            s.setOnLongClickListener(v -> {
                a.expandCard(c, s, false);
                return true;
            });
            Ui.press(s);
        }
        return s;
    }

    // ---------- add card ----------
    void addForm() {
        final String[] type = {"homework"};
        final int[] color = {0};
        final String[] due = {""};
        final List<FileRef> files = new ArrayList<>();

        final EditText subj = Ui.input(a, Str.t("subject"), false);
        final EditText note = Ui.input(a, Str.t("note"), true);

        final TextView dueBtn = Ui.btn(a, "—", false, null);
        final Runnable paintDue = () -> dueBtn.setText(due[0].isEmpty() ? "—" : due[0]);

        LinearLayout typeSeg = Ui.seg(a, new String[]{Str.t("homework"), Str.t("exam"), Str.t("prepping")}, 0, i -> {
            type[0] = TYPES[i];
            if (type[0].equals("prepping") && due[0].length() > 10) due[0] = due[0].substring(0, 10);
            paintDue.run();
        });

        dueBtn.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(a, (view, y, m, d) -> {
                final String date = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                if (type[0].equals("prepping")) {
                    due[0] = date;
                    paintDue.run();
                } else {
                    new TimePickerDialog(a, (tp, hh, mm) -> {
                        due[0] = date + " " + String.format(Locale.US, "%02d:%02d", hh, mm);
                        paintDue.run();
                    }, 23, 59, true).show();
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        final LinearLayout swatches = new LinearLayout(a);
        swatches.setOrientation(LinearLayout.HORIZONTAL);
        final Runnable paintSw = new Runnable() {
            @Override
            public void run() {
                swatches.removeAllViews();
                int[] pal = Ui.palette();
                for (int i = 0; i < pal.length; i++) {
                    final int idx = i;
                    TextView sw = Ui.text(a, Store.prefs.cb ? Ui.SYM[i] : "", 16, 0xFF1A1A1A, false);
                    sw.setGravity(Gravity.CENTER);
                    sw.setBackground(Ui.oval(pal[i], idx == color[0] ? Ui.fg : Ui.line, idx == color[0] ? 3 : 1));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(40), Ui.dp(40));
                    lp.setMarginEnd(Ui.dp(8));
                    sw.setOnClickListener(v -> {
                        color[0] = idx;
                        this.run();
                    });
                    swatches.addView(sw, lp);
                }
            }
        };
        paintSw.run();

        final LinearLayout fileList = Ui.vbox(a);
        final Runnable paintFiles = () -> {
            fileList.removeAllViews();
            for (FileRef f : files) fileList.addView(Ui.text(a, f.name, 14, Ui.fg, false), Ui.mw(4));
        };
        TextView drop = Ui.text(a, Str.t("drop"), 15, Ui.mute, false);
        drop.setGravity(Gravity.CENTER);
        drop.setPadding(Ui.dp(16), Ui.dp(24), Ui.dp(16), Ui.dp(24));
        drop.setBackground(Ui.dashed(Ui.line, 16));
        drop.setOnClickListener(v -> a.pickFiles(files, paintFiles));
        LinearLayout filesBox = Ui.vbox(a);
        filesBox.addView(drop, Ui.mw());
        filesBox.addView(fileList, Ui.mw());

        List<View> rows = new ArrayList<>();
        rows.add(Ui.field(a, Str.t("subject"), subj));
        rows.add(typeSeg);
        rows.add(Ui.field(a, Str.t("deadline"), dueBtn));
        rows.add(Ui.field(a, Str.t("color"), swatches));
        rows.add(Ui.field(a, Str.t("note"), note));
        rows.add(Ui.field(a, Str.t("files"), filesBox));

        a.showForm(rows, () -> {
            String sj = subj.getText().toString().trim();
            if (sj.isEmpty()) {
                subj.requestFocus();
                return false;
            }
            Card c = new Card();
            c.subject = sj;
            c.type = type[0];
            c.due = due[0];
            c.color = color[0];
            c.note = note.getText().toString();
            c.files.addAll(files);
            Store.cards.add(c);
            Store.save();
            return true;
        });
    }
}
