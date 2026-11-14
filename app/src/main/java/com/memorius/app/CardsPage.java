package com.memorius.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

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
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content = new LinearLayout(a);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(136));
        sv.addView(content, new FrameLayout.LayoutParams(-1, -2));
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    // "2025-11-20" or "2025-11-20 18:30" as a time; a bare date means the end of that day
    static long dueMillis(Card c) {
        if (c.due.isEmpty()) return Long.MIN_VALUE;
        try {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat(c.due.length() > 10 ? "yyyy-MM-dd HH:mm" : "yyyy-MM-dd", Locale.US);
            java.util.Date d = f.parse(c.due);
            long t = d.getTime();
            return c.due.length() > 10 ? t : t + 24L * 3600_000L - 60_000L;
        } catch (Exception e) {
            return Long.MIN_VALUE;
        }
    }

    // Standby line: time left until the deadline, e.g. "2d 4h left"
    static String timeLeft(Card c) {
        long t = dueMillis(c);
        if (t == Long.MIN_VALUE) return Str.t("noDeadline");
        long min = (t - System.currentTimeMillis()) / 60000L;
        if (min < 0) return Str.t("overdue");
        long d = min / 1440, h = (min % 1440) / 60, m = min % 60;
        String s;
        if (d > 0) s = d + Str.t("unitD") + " " + h + Str.t("unitH");
        else if (h > 0) s = h + Str.t("unitH") + " " + m + Str.t("unitM");
        else s = Math.max(1, m) + Str.t("unitM");
        return String.format(Str.t("left"), s);
    }

    // Preview / full view line: the deadline itself
    static String fmtDue(Card c) {
        if (c.due.isEmpty()) return Str.t("noDeadline");
        return (c.type.equals("prepping") ? Str.t("date") : Str.t("deadline")) + ": " + c.due;
    }

    static final int STANDBY_MIN_DP = 112;

    // card surface: tinted body, 3D shadow or highlight, colour strip on the start edge.
    // Shared by the list card, the preview and the full view.
    static LinearLayout shell(MainActivity a, Card c) {
        int ci = Math.max(0, Math.min(5, c.color));
        int col = Ui.palette()[ci];
        LinearLayout s = new LinearLayout(a);
        s.setOrientation(LinearLayout.HORIZONTAL);
        s.setBackground(new CardDrawable(col));
        s.setPadding(0, 0, 0, CardDrawable.inset());
        s.setMinimumHeight(Ui.dp(STANDBY_MIN_DP) + CardDrawable.inset());
        // spacer: the strip itself is painted by CardDrawable
        s.addView(new View(a), new LinearLayout.LayoutParams(Math.max(1, CardDrawable.stripSpace()), -1));
        LinearLayout in = new LinearLayout(a);
        in.setOrientation(LinearLayout.VERTICAL);
        float sp = Style.space(); // Corporate: more air, Humanist: a little
        in.setPaddingRelative(Ui.dp(18 * sp), Ui.dp(16 * sp), Ui.dp(24 * sp), Ui.dp(16 * sp));
        s.addView(in, new LinearLayout.LayoutParams(0, -2, 1f));
        return s;
    }

    static LinearLayout head(MainActivity a, Card c) {
        int ci = Math.max(0, Math.min(5, c.color));
        LinearLayout row = Ui.hbox(a);
        if (Store.prefs.cb) {
            TextView sym = Ui.text(a, Ui.SYM[ci], 14, Ui.fg, false);
            sym.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
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
        content.setPadding(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(136) + Ui.insetBottom);
        content.removeAllViews();
        final List<Card> list = Store.cards;
        if (list.isEmpty()) {
            TextView e = Ui.text(a, Phrases.home(), 20, Ui.mute, false);
            e.setGravity(Gravity.CENTER);
            e.setLineSpacing(0f, 1.15f);
            content.addView(e, Ui.mw(120));
            return;
        }
        // top to bottom, newest first (the list keeps the oldest card first)
        int gap = Math.max(2, 16 - Math.round(CardDrawable.inset() / Ui.density));
        for (int i = list.size() - 1; i >= 0; i--) {
            content.addView(cardView(list.get(i), i), Ui.mw(i == list.size() - 1 ? 0 : gap));
        }
        if (a.mode.isEmpty()) {
            TextView hint = Ui.text(a, Str.t("holdHint"), 13, Ui.mute, false);
            hint.setGravity(Gravity.CENTER);
            content.addView(hint, Ui.mw(16));
        }
    }

    // STANDBY: title, short description, time left. No action controls (except while deleting / rearranging).
    private View cardView(final Card c, int i) {
        final LinearLayout s = shell(a, c);
        LinearLayout in = (LinearLayout) s.getChildAt(1);
        LinearLayout head = head(a, c);
        in.addView(head, Ui.mw());
        if (!c.note.isEmpty()) {
            TextView d = Ui.text(a, c.note, 14, Ui.mute, false);
            d.setMaxLines(2);
            d.setEllipsize(android.text.TextUtils.TruncateAt.END);
            in.addView(d, Ui.mw(4));
        }
        in.addView(Ui.text(a, timeLeft(c), 13, c.done ? Ui.mute : Ui.fg, true), Ui.mw(8));
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
            s.setOnClickListener(v -> a.openCard(c, s));
            s.setOnTouchListener(new CardTouch(new CardTouch.Listener() {
                @Override
                public void onHold(View v, float rawY) {
                    a.peekStart(c, s, rawY);
                }

                @Override
                public void onDrag(float rawY) {
                    a.peekDrag(rawY);
                }

                @Override
                public void onRelease(boolean cancelled) {
                    a.peekEnd();
                }
            }));
        }
        return s;
    }

    // ---------- add / edit card ----------
    void addForm() {
        cardForm(null);
    }

    void cardForm(final Card existing) {
        final Card base = existing;
        final String[] type = {base == null ? "homework" : base.type};
        final int[] color = {base == null ? 0 : base.color};
        final String[] due = {base == null ? "" : base.due};
        final List<FileRef> files = new ArrayList<>();
        if (base != null) files.addAll(base.files);

        final EditText subj = Ui.input(a, Str.t("subject"), false);
        final EditText note = Ui.input(a, Str.t("note"), true);
        if (base != null) {
            subj.setText(base.subject);
            note.setText(base.note);
        }

        final TextView dueBtn = Ui.btn(a, "—", false, null);
        final Runnable paintDue = () -> dueBtn.setText(due[0].isEmpty() ? "—" : due[0]);
        paintDue.run();

        int typeIdx = 0;
        for (int i = 0; i < TYPES.length; i++) if (TYPES[i].equals(type[0])) typeIdx = i;
        LinearLayout typeSeg = Ui.seg(a, new String[]{Str.t("homework"), Str.t("exam"), Str.t("prepping")}, typeIdx, i -> {
            type[0] = TYPES[i];
            Ui.haptic(subj, 0);
            if (type[0].equals("prepping") && due[0].length() > 10) due[0] = due[0].substring(0, 10);
            paintDue.run();
        });

        dueBtn.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new StyledDatePickerDialog(a, (view, y, m, d) -> {
                final String date = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                if (type[0].equals("prepping")) {
                    due[0] = date;
                    paintDue.run();
                } else {
                    new StyledTimePickerDialog(a, (tp, hh, mm) -> {
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
                    sw.setContentDescription(Str.t("color") + " " + (i + 1));
                    sw.setBackground(Ui.oval(pal[i], idx == color[0] ? Ui.fg : Ui.line, idx == color[0] ? 3 : 1));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(44), Ui.dp(44));
                    lp.setMarginEnd(Ui.dp(8));
                    sw.setOnClickListener(v -> {
                        color[0] = idx;
                        Ui.haptic(v, 0);
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
        paintFiles.run();
        final TextView drop = Ui.text(a, Str.t("drop"), 15, Ui.mute, false);
        drop.setGravity(Gravity.CENTER);
        drop.setPadding(Ui.dp(16), Ui.dp(24), Ui.dp(16), Ui.dp(24));
        drop.setBackground(Ui.dashed(Ui.line, 16));
        drop.setOnClickListener(v -> a.pickFiles(files, paintFiles));
        // drag and drop from other apps (split screen, tablets, desktop mode)
        drop.setOnDragListener((v, e) -> {
            switch (e.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return e.getClipDescription() != null;
                case DragEvent.ACTION_DRAG_ENTERED:
                    drop.setBackground(Ui.dashed(Ui.fg, 16));
                    Ui.haptic(v, 0);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    drop.setBackground(Ui.dashed(Ui.line, 16));
                    return true;
                case DragEvent.ACTION_DROP:
                    drop.setBackground(Ui.dashed(Ui.line, 16));
                    ClipData cd = e.getClipData();
                    if (cd == null) return false;
                    try {
                        a.requestDragAndDropPermissions(e);
                    } catch (Exception ignored) {
                    }
                    for (int i = 0; i < cd.getItemCount(); i++) {
                        Uri u = cd.getItemAt(i).getUri();
                        if (u != null) files.add(new FileRef(Files.queryName(a, u), u.toString()));
                    }
                    paintFiles.run();
                    Ui.haptic(v, 2);
                    return true;
            }
            return true;
        });
        LinearLayout filesBox = Ui.vbox(a);
        filesBox.addView(drop, Ui.mw());
        filesBox.addView(fileList, Ui.mw());

        List<View> rows = new ArrayList<>();
        if (base != null) rows.add(Ui.text(a, Str.t("editCard"), 20, Ui.fg, true));
        rows.add(Ui.field(a, Str.t("subject"), subj));
        rows.add(typeSeg);
        rows.add(Ui.field(a, Str.t("deadline"), dueBtn));
        rows.add(Ui.field(a, Str.t("color"), swatches));
        rows.add(Ui.field(a, Str.t("note"), note));
        rows.add(Ui.field(a, Str.t("files"), filesBox));

        a.showForm(rows, () -> {
            String sj = subj.getText().toString().trim();
            if (sj.isEmpty()) {
                Ui.haptic(subj, 3);
                subj.requestFocus();
                return false;
            }
            Card c = base != null ? base : new Card();
            c.subject = sj;
            c.type = type[0];
            c.due = due[0];
            c.color = color[0];
            c.note = note.getText().toString();
            c.files.clear();
            c.files.addAll(files);
            if (base == null) Store.cards.add(c);
            Store.save();
            Ui.haptic(subj, 2);
            return true;
        });
    }
}
