package com.memorius.app;

import android.animation.*;
import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class MainActivity extends Activity {
    static class Task {
        long id; String kind = "Homework", subject = "", notes = "";
        LocalDate due = LocalDate.now().plusDays(1); boolean done;
        ArrayList<String> files = new ArrayList<>();
    }

    static final String[] MN = {"January","February","March","April","May","June","July","August","September","October","November","December"};
    static final int PLUM = 0xF21A0F28, GLASS = 0x40FFFFFF;

    final ArrayList<Task> tasks = new ArrayList<>();
    final Handler h = new Handler(Looper.getMainLooper());
    final PathInterpolator spring = new PathInterpolator(.32f, .72f, 0f, 1f);
    SharedPreferences sp;
    FrameLayout root, sheet;
    ScrollView scroll, formPane;
    LinearLayout activeList, finList, dock, bar, selPop, menuPop, formContent, dFiles, detailRow;
    LinearLayout detailPane;
    TextView addBtn, selBtn, sub, finHead, dKind, dTitle, dNotes, finBtn, menuBtn, barRm, ghost, pickFiles, addSubmit;
    View scrim;
    EditText subj, notes;
    Spinner typeSp;
    NumberPicker pm, pd, py;
    GradientDrawable sheetBg;
    ValueAnimator rectAnim;
    Task cur, editing;
    long goneId = -1;
    boolean open, peeking, sharedMode;
    int mode = 0, pane = 0, yearBase;
    final Set<Long> sel = new HashSet<>();
    ArrayList<String> formFiles = new ArrayList<>();

    int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        sp = getSharedPreferences("memorius", MODE_PRIVATE);
        root = findViewById(R.id.root); sheet = findViewById(R.id.sheet); scroll = findViewById(R.id.scroll);
        formPane = findViewById(R.id.formPane); formContent = findViewById(R.id.formContent);
        detailPane = findViewById(R.id.detailPane); detailRow = findViewById(R.id.detailRow);
        activeList = findViewById(R.id.activeList); finList = findViewById(R.id.finList);
        dock = findViewById(R.id.dock); bar = findViewById(R.id.bar); selPop = findViewById(R.id.selPop); menuPop = findViewById(R.id.menuPop);
        addBtn = findViewById(R.id.addBtn); selBtn = findViewById(R.id.selBtn); sub = findViewById(R.id.sub); finHead = findViewById(R.id.finHead);
        dKind = findViewById(R.id.dKind); dTitle = findViewById(R.id.dTitle); dNotes = findViewById(R.id.dNotes); dFiles = findViewById(R.id.dFiles);
        finBtn = findViewById(R.id.finBtn); menuBtn = findViewById(R.id.menuBtn); barRm = findViewById(R.id.barRm); ghost = findViewById(R.id.ghost);
        pickFiles = findViewById(R.id.pickFiles); addSubmit = findViewById(R.id.addSubmit); scrim = findViewById(R.id.scrim);
        subj = findViewById(R.id.subj); notes = findViewById(R.id.notes); typeSp = findViewById(R.id.typeSp);
        pm = findViewById(R.id.pm); pd = findViewById(R.id.pd); py = findViewById(R.id.py);

        sheetBg = new GradientDrawable(); sheetBg.setCornerRadius(dp(26)); sheetBg.setColor(PLUM);
        sheet.setBackground(sheetBg);

        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Homework", "Incoming Exam"});
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSp.setAdapter(ad);

        yearBase = LocalDate.now().getYear() - 1;
        pm.setMinValue(0); pm.setMaxValue(11); pm.setDisplayedValues(MN);
        pd.setMinValue(1); pd.setMaxValue(31);
        py.setMinValue(yearBase); py.setMaxValue(yearBase + 9);
        NumberPicker.OnValueChangeListener upd = (p, o, n) -> updateDays();
        pm.setOnValueChangedListener(upd); py.setOnValueChangedListener(upd);

        addBtn.setOnClickListener(v -> { if (mode == 0 && !open) openSheet(addBtn, null, false); });
        selBtn.setOnClickListener(v -> { if (selPop.getVisibility() == View.VISIBLE) hideSelPop(); else showSelPop(); });
        scrim.setOnClickListener(v -> { if (open && !peeking) closeSheet(); });
        finBtn.setOnClickListener(v -> { if (cur != null) { cur.done = !cur.done; save(); closeSheet(); } });
        menuBtn.setOnClickListener(v -> toggleMenu());
        findViewById(R.id.mPost).setOnClickListener(v -> shift(1));
        findViewById(R.id.mAdv).setOnClickListener(v -> shift(-1));
        findViewById(R.id.mEdit).setOnClickListener(v -> { hideMenu(); if (cur != null) openForm(cur); });
        findViewById(R.id.sAll).setOnClickListener(v -> { hideSelPop(); removeFinished(); });
        findViewById(R.id.sFin).setOnClickListener(v -> startSel(1));
        findViewById(R.id.sAct).setOnClickListener(v -> startSel(2));
        findViewById(R.id.barCancel).setOnClickListener(v -> endSel());
        barRm.setOnClickListener(v -> removeSelected());
        addSubmit.setOnClickListener(v -> submit());
        pickFiles.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*"); i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, 7);
        });
        pickFiles.setOnDragListener((v, e) -> {
            int a = e.getAction();
            if (a == DragEvent.ACTION_DRAG_STARTED) return true;
            if (a == DragEvent.ACTION_DRAG_ENTERED) { v.setAlpha(.6f); return true; }
            if (a == DragEvent.ACTION_DRAG_EXITED || a == DragEvent.ACTION_DRAG_ENDED) { v.setAlpha(1); return true; }
            if (a == DragEvent.ACTION_DROP) {
                v.setAlpha(1);
                try { requestDragAndDropPermissions(e); } catch (Exception ex) { }
                ClipData cd = e.getClipData();
                if (cd != null) for (int i = 0; i < cd.getItemCount(); i++) if (cd.getItemAt(i).getUri() != null) addFile(cd.getItemAt(i).getUri());
                updateFilesText();
                return true;
            }
            return false;
        });
        load(); render();
    }

    // ---------- data ----------
    void save() {
        try {
            JSONArray a = new JSONArray();
            for (Task t : tasks) {
                JSONObject o = new JSONObject();
                o.put("id", t.id); o.put("kind", t.kind); o.put("subject", t.subject); o.put("notes", t.notes);
                o.put("due", t.due.toString()); o.put("done", t.done); o.put("files", new JSONArray(t.files));
                a.put(o);
            }
            sp.edit().putString("tasks", a.toString()).apply();
        } catch (Exception e) { }
    }

    void load() {
        try {
            JSONArray a = new JSONArray(sp.getString("tasks", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i); Task t = new Task();
                t.id = o.getLong("id"); t.kind = o.optString("kind", "Homework"); t.subject = o.optString("subject");
                t.notes = o.optString("notes"); t.due = LocalDate.parse(o.getString("due")); t.done = o.optBoolean("done");
                JSONArray f = o.optJSONArray("files");
                if (f != null) for (int j = 0; j < f.length(); j++) t.files.add(f.getString(j));
                tasks.add(t);
            }
        } catch (Exception e) { }
    }

    String label(LocalDate d) {
        long n = ChronoUnit.DAYS.between(LocalDate.now(), d);
        if (n < 0) return (-n) + " day" + (n == -1 ? "" : "s") + " overdue";
        if (n == 0) return "Due today";
        if (n == 1) return "Due tomorrow";
        return "Due in " + n + " days";
    }

    // ---------- list ----------
    void render() {
        activeList.removeAllViews(); finList.removeAllViews();
        ArrayList<Task> a = new ArrayList<>(), f = new ArrayList<>();
        for (Task t : tasks) (t.done ? f : a).add(t);
        Collections.sort(a, (x, y) -> x.due.compareTo(y.due));
        for (Task t : a) activeList.addView(card(t));
        for (Task t : f) finList.addView(card(t));
        finHead.setVisibility(f.isEmpty() ? View.GONE : View.VISIBLE);
        sub.setText(a.isEmpty() ? "All clear" : a.size() + " to do");
        barRm.setText(sel.isEmpty() ? "Remove" : "Remove " + sel.size());
    }

    View card(final Task t) {
        View v = getLayoutInflater().inflate(R.layout.item_work, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(160)); lp.bottomMargin = dp(10);
        v.setLayoutParams(lp);
        ((TextView) v.findViewById(R.id.iKind)).setText(t.kind + " · " + label(t.due));
        TextView title = v.findViewById(R.id.iTitle); title.setText(t.subject);
        if (t.done) title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        ((TextView) v.findViewById(R.id.iNotes)).setText(t.notes);
        v.setTag(t.id);
        View ring = v.findViewById(R.id.iRing);
        boolean pick = (mode == 1 && t.done) || (mode == 2 && !t.done);
        ring.setVisibility(pick ? View.VISIBLE : View.GONE);
        ring.setBackgroundResource(sel.contains(t.id) ? R.drawable.bg_ring_on : R.drawable.bg_ring);
        if (mode != 0 && !pick) v.setAlpha(.4f);
        if (t.id == goneId) v.setVisibility(View.INVISIBLE);
        v.setOnTouchListener(touch(t, pick));
        return v;
    }

    View.OnTouchListener touch(final Task t, final boolean pick) {
        return new View.OnTouchListener() {
            float sx, sy; boolean moved, peeked; Runnable r;
            @Override public boolean onTouch(final View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = e.getRawX(); sy = e.getRawY(); moved = false; peeked = false;
                        if (mode == 0) {
                            v.animate().scaleX(.965f).scaleY(.965f).setDuration(400).start();
                            r = () -> { peeked = true; v.animate().cancel(); v.setScaleX(1); v.setScaleY(1); openSheet(v, t, true); };
                            h.postDelayed(r, 400);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!moved && Math.hypot(e.getRawX() - sx, e.getRawY() - sy) > dp(10)) {
                            moved = true; if (r != null) h.removeCallbacks(r);
                            v.animate().scaleX(1).scaleY(1).setDuration(150).start();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (r != null) h.removeCallbacks(r);
                        v.animate().cancel(); v.setScaleX(1); v.setScaleY(1);
                        if (peeked) { closeSheet(); }
                        else if (e.getActionMasked() == MotionEvent.ACTION_UP && !moved) {
                            if (mode != 0) { if (pick) { if (!sel.remove(t.id)) sel.add(t.id); render(); } }
                            else openSheet(v, t, false);
                        }
                        return true;
                }
                return true;
            }
        };
    }

    // ---------- geometry ----------
    Rect rectOf(View v) {
        int[] a = new int[2], b = new int[2];
        v.getLocationInWindow(a); root.getLocationInWindow(b);
        int l = a[0] - b[0], t = a[1] - b[1];
        return new Rect(l, t, l + v.getWidth(), t + v.getHeight());
    }

    Rect center(int wdp, int hdp) {
        int rw = root.getWidth(), rh = root.getHeight();
        int w = Math.min(dp(wdp), rw - dp(32)), hh = Math.min(dp(hdp), rh - dp(60));
        int l = (rw - w) / 2, t = (rh - hh) / 2;
        return new Rect(l, t, l + w, t + hh);
    }

    void setSheetRect(int l, int t, int w, int hh) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) sheet.getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.START; lp.leftMargin = l; lp.topMargin = t; lp.width = w; lp.height = hh;
        sheet.setLayoutParams(lp);
    }

    int lerp(int a, int b, float f) { return Math.round(a + (b - a) * f); }

    void animRect(final Rect a, final Rect b, final Runnable end) {
        if (rectAnim != null) rectAnim.cancel();
        final boolean[] cancelled = new boolean[1];
        rectAnim = ValueAnimator.ofFloat(0, 1);
        rectAnim.setDuration(600); rectAnim.setInterpolator(spring);
        rectAnim.addUpdateListener(x -> {
            float f = (float) x.getAnimatedValue();
            setSheetRect(lerp(a.left, b.left, f), lerp(a.top, b.top, f), lerp(a.width(), b.width(), f), lerp(a.height(), b.height(), f));
        });
        rectAnim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationCancel(Animator an) { cancelled[0] = true; }
            @Override public void onAnimationEnd(Animator an) { if (!cancelled[0] && end != null) end.run(); }
        });
        rectAnim.start();
    }

    void animBg(int from, int to, long dur) {
        ValueAnimator va = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        va.setDuration(dur); va.addUpdateListener(x -> sheetBg.setColor((int) x.getAnimatedValue())); va.start();
    }

    // ---------- blur helpers (real blur on Android 12+, plain fade before) ----------
    void blurAnim(final View v, float from, float to, long delay, long dur) {
        if (Build.VERSION.SDK_INT < 31) return;
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setStartDelay(delay); a.setDuration(dur);
        a.addUpdateListener(x -> {
            float r = (float) x.getAnimatedValue();
            v.setRenderEffect(r < .5f ? null : RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP));
        });
        a.start();
    }

    void reset(View c) {
        c.animate().cancel(); c.setAlpha(1); c.setTranslationY(0); c.setScaleX(1); c.setScaleY(1);
        if (Build.VERSION.SDK_INT >= 31) c.setRenderEffect(null);
    }

    void stagger(ViewGroup g, int skip) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            reset(c);
            if (i < skip) continue;
            c.setAlpha(0); c.setTranslationY(dp(50));
            long d = 120 + 50L * (i - skip);
            c.animate().alpha(1).translationY(0).setStartDelay(d).setDuration(600).setInterpolator(spring).start();
            blurAnim(c, 14f, 0f, d, 600);
        }
    }

    // ---------- sheet ----------
    void showPane(int p, boolean shared) {
        pane = p; sharedMode = shared; hideMenu();
        formPane.setVisibility(p == 0 ? View.VISIBLE : View.GONE);
        detailPane.setVisibility(p == 1 ? View.VISIBLE : View.GONE);
        if (p == 0) stagger(formContent, 0);
        else { detailRow.setVisibility(peeking ? View.INVISIBLE : View.VISIBLE); stagger(detailPane, shared ? 3 : 0); }
    }

    void openSheet(final View src, Task t, boolean peek) {
        if (rectAnim != null) rectAnim.cancel();
        h.removeCallbacksAndMessages(null);
        boolean wasOpen = sheet.getVisibility() == View.VISIBLE;
        cur = t; goneId = t == null ? -1 : t.id; peeking = peek; open = true;
        boolean fromAdd = src == addBtn;
        Rect from = wasOpen ? rectOf(sheet) : rectOf(src);
        Rect to = peek ? center(340, 420) : center(360, 640);
        if (fromAdd) addBtn.setAlpha(0); else src.setVisibility(View.INVISIBLE);
        selBtn.animate().alpha(0).setDuration(200).start();
        sheet.setVisibility(View.VISIBLE);
        setSheetRect(from.left, from.top, from.width(), from.height());
        scrim.setVisibility(View.VISIBLE); scrim.animate().alpha(1).setDuration(400).start();
        if (!wasOpen) {
            sheetBg.setColor(fromAdd ? GLASS : PLUM);
            if (fromAdd) {
                ghost.setVisibility(View.VISIBLE); ghost.setAlpha(1);
                ghost.animate().alpha(0).setStartDelay(100).setDuration(250).start();
                animBg(GLASS, PLUM, 500);
            }
        }
        animRect(from, to, null);
        if (fromAdd) openForm(null); else { bindDetail(t); showPane(1, !wasOpen); }
    }

    void closeSheet() {
        if (!open) return;
        open = false; peeking = false;
        hideMenu();
        ViewGroup g = pane == 0 ? formContent : detailPane;
        int skip = (pane == 1 && sharedMode) ? 3 : 0;
        for (int i = skip; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            c.animate().cancel(); c.animate().alpha(0).translationY(dp(30)).setDuration(250).start();
            blurAnim(c, 0f, 14f, 0, 250);
        }
        scrim.animate().alpha(0).setDuration(450).withEndAction(() -> { if (!open) scrim.setVisibility(View.GONE); }).start();
        hideKb();
        final Task t = cur;
        render();
        final Rect from = rectOf(sheet);
        scroll.post(() -> {
            if (open) return;
            View target = findCard(t);
            boolean toAdd = target == null;
            Rect to = toAdd ? rectOf(addBtn) : rectOf(target);
            if (toAdd) {
                ghost.setVisibility(View.VISIBLE); ghost.setAlpha(0); ghost.animate().alpha(1).setStartDelay(250).setDuration(250).start();
                animBg(PLUM, GLASS, 450);
                addBtn.animate().alpha(1).setStartDelay(450).setDuration(150).start();
            }
            animRect(from, to, () -> {
                sheet.setVisibility(View.GONE); ghost.setVisibility(View.GONE);
                goneId = -1; cur = null; addBtn.setAlpha(1);
                selBtn.animate().alpha(1).setStartDelay(0).setDuration(200).start();
                for (int i = 0; i < formContent.getChildCount(); i++) reset(formContent.getChildAt(i));
                for (int i = 0; i < detailPane.getChildCount(); i++) reset(detailPane.getChildAt(i));
                render();
            });
        });
    }

    View findCard(Task t) {
        if (t == null) return null;
        for (LinearLayout l : new LinearLayout[]{activeList, finList})
            for (int i = 0; i < l.getChildCount(); i++)
                if (l.getChildAt(i).getTag() != null && (long) l.getChildAt(i).getTag() == t.id) return l.getChildAt(i);
        return null;
    }

    void bindDetail(Task t) {
        dKind.setText(t.kind + " · " + label(t.due));
        dTitle.setText(t.subject); dNotes.setText(t.notes);
        finBtn.setText(t.done ? "Restore" : "Finish work");
        dFiles.removeAllViews();
        if (t.files.isEmpty()) dFiles.addView(fileRow("No files attached", true));
        else for (String f : t.files) dFiles.addView(fileRow("📄  " + f, false));
    }

    TextView fileRow(String s, boolean dim) {
        TextView tv = new TextView(this); tv.setText(s); tv.setTextSize(16);
        tv.setTextColor(dim ? 0xFFB9B6D6 : 0xFFFFFFFF); tv.setPadding(0, dp(6), 0, dp(6)); return tv;
    }

    // ---------- form ----------
    void openForm(Task t) {
        editing = t;
        subj.setText(t == null ? "" : t.subject); notes.setText(t == null ? "" : t.notes);
        typeSp.setSelection(t != null && "Incoming Exam".equals(t.kind) ? 1 : 0);
        formFiles = t == null ? new ArrayList<String>() : new ArrayList<>(t.files);
        updateFilesText();
        setDate(t == null ? LocalDate.now().plusDays(1) : t.due);
        addSubmit.setText(t == null ? "Add" : "Save");
        showPane(0, false);
    }

    void updateDays() { pd.setMaxValue(YearMonth.of(py.getValue(), pm.getValue() + 1).lengthOfMonth()); }

    void setDate(LocalDate d) {
        py.setValue(Math.max(yearBase, Math.min(yearBase + 9, d.getYear())));
        pm.setValue(d.getMonthValue() - 1); updateDays(); pd.setValue(d.getDayOfMonth());
    }

    void submit() {
        String s = subj.getText().toString().trim();
        if (s.isEmpty()) { subj.setError("Required"); subj.requestFocus(); return; }
        Task t = editing != null ? editing : new Task();
        if (editing == null) { t.id = System.currentTimeMillis(); tasks.add(t); }
        t.kind = typeSp.getSelectedItem().toString(); t.subject = s; t.notes = notes.getText().toString().trim();
        t.due = LocalDate.of(py.getValue(), pm.getValue() + 1, pd.getValue());
        t.files = new ArrayList<>(formFiles);
        cur = t; goneId = t.id; save(); hideKb();
        bindDetail(t); showPane(1, false);
        render();
    }

    void hideKb() {
        InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(root.getWindowToken(), 0);
    }

    void updateFilesText() {
        if (formFiles.isEmpty()) pickFiles.setText("Tap to choose files, or drop them here");
        else pickFiles.setText(TextUtils.join(", ", formFiles));
    }

    void addFile(Uri u) {
        String n = u.getLastPathSegment();
        try (Cursor c = getContentResolver().query(u, null, null, null, null)) {
            if (c != null && c.moveToFirst()) { int ix = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (ix >= 0) n = c.getString(ix); }
        } catch (Exception e) { }
        formFiles.add(n);
    }

    @Override protected void onActivityResult(int rq, int rc, Intent d) {
        super.onActivityResult(rq, rc, d);
        if (rq == 7 && rc == RESULT_OK && d != null) {
            if (d.getClipData() != null) for (int i = 0; i < d.getClipData().getItemCount(); i++) addFile(d.getClipData().getItemAt(i).getUri());
            else if (d.getData() != null) addFile(d.getData());
            updateFilesText();
        }
    }

    // ---------- menus ----------
    void shift(int n) {
        hideMenu(); if (cur == null) return;
        cur.due = cur.due.plusDays(n); save();
        dKind.setText(cur.kind + " · " + label(cur.due)); render();
    }

    void toggleMenu() {
        if (menuPop.getVisibility() == View.VISIBLE) { hideMenu(); return; }
        menuPop.setVisibility(View.VISIBLE); menuPop.setAlpha(0); menuPop.setScaleX(.4f); menuPop.setScaleY(.4f);
        menuPop.post(() -> {
            menuPop.setPivotX(menuPop.getWidth()); menuPop.setPivotY(menuPop.getHeight());
            menuPop.animate().alpha(1).scaleX(1).scaleY(1).setDuration(350).setInterpolator(spring).start();
        });
    }

    void hideMenu() {
        if (menuPop.getVisibility() != View.VISIBLE) return;
        menuPop.animate().alpha(0).scaleX(.4f).scaleY(.4f).setDuration(250).withEndAction(() -> menuPop.setVisibility(View.GONE)).start();
    }

    void showSelPop() {
        selPop.setVisibility(View.VISIBLE); selPop.setAlpha(0); selPop.setScaleX(.3f); selPop.setScaleY(.3f);
        selPop.post(() -> {
            Rect r = rectOf(selBtn);
            selPop.setPivotX(r.centerX() - selPop.getLeft()); selPop.setPivotY(selPop.getHeight() + dp(38));
            selPop.animate().alpha(1).scaleX(1).scaleY(1).setDuration(500).setInterpolator(spring).start();
        });
        addBtn.animate().translationX(dp(32)).setDuration(500).setInterpolator(spring).start();
        selBtn.animate().alpha(0).scaleX(.5f).scaleY(.5f).setDuration(300).start();
    }

    void hideSelPop() {
        if (selPop.getVisibility() != View.VISIBLE) return;
        selPop.animate().alpha(0).scaleX(.3f).scaleY(.3f).setDuration(300).withEndAction(() -> selPop.setVisibility(View.GONE)).start();
        addBtn.animate().translationX(0).setDuration(500).setInterpolator(spring).start();
        selBtn.animate().alpha(1).scaleX(1).scaleY(1).setDuration(300).start();
    }

    boolean hit(View v, MotionEvent e) {
        int[] a = new int[2]; v.getLocationOnScreen(a);
        return e.getRawX() >= a[0] && e.getRawX() <= a[0] + v.getWidth() && e.getRawY() >= a[1] && e.getRawY() <= a[1] + v.getHeight();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (selPop.getVisibility() == View.VISIBLE && !hit(selPop, e) && !hit(selBtn, e)) hideSelPop();
            if (menuPop.getVisibility() == View.VISIBLE && !hit(menuPop, e) && !hit(menuBtn, e)) hideMenu();
        }
        return super.dispatchTouchEvent(e);
    }

    // ---------- selection / removal ----------
    void startSel(int m) {
        selPop.setVisibility(View.GONE); mode = m; sel.clear();
        addBtn.setTranslationX(0); selBtn.setAlpha(1); selBtn.setScaleX(1); selBtn.setScaleY(1);
        dock.animate().translationY(dp(140)).alpha(0).setDuration(400).setInterpolator(spring).start();
        bar.setVisibility(View.VISIBLE); bar.setTranslationY(dp(120));
        bar.animate().translationY(0).setDuration(450).setInterpolator(spring).start();
        render();
    }

    void endSel() {
        mode = 0; sel.clear();
        dock.animate().translationY(0).alpha(1).setDuration(400).setInterpolator(spring).start();
        bar.animate().translationY(dp(120)).setDuration(350).withEndAction(() -> bar.setVisibility(View.GONE)).start();
        render();
    }

    void fadeOut(View v) {
        v.animate().alpha(0).scaleX(.94f).scaleY(.94f).setDuration(350).start();
        blurAnim(v, 0f, 14f, 0, 350);
    }

    void removeSelected() {
        if (sel.isEmpty()) return;
        for (LinearLayout l : new LinearLayout[]{activeList, finList})
            for (int i = 0; i < l.getChildCount(); i++)
                if (sel.contains((Long) l.getChildAt(i).getTag())) fadeOut(l.getChildAt(i));
        h.postDelayed(() -> { tasks.removeIf(t -> sel.contains(t.id)); save(); endSel(); }, 380);
    }

    void removeFinished() {
        for (int i = 0; i < finList.getChildCount(); i++) fadeOut(finList.getChildAt(i));
        h.postDelayed(() -> { tasks.removeIf(t -> t.done); save(); render(); }, 380);
    }

    @Override public void onBackPressed() {
        if (open) closeSheet(); else if (mode != 0) endSel(); else super.onBackPressed();
    }
}
