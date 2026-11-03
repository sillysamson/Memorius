package com.memorius.app;

import android.Manifest;
import android.animation.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

public class MainActivity extends Activity {
    static final int RQ_PICK = 7, RQ_DOC = 8;
    static final int TINT_OFF = 0x00000000;

    final ArrayList<Task> tasks = new ArrayList<>();
    final Handler h = new Handler(Looper.getMainLooper());
    final Handler fh = new Handler(Looper.getMainLooper());
    final PathInterpolator spring = new PathInterpolator(.32f, .72f, 0f, 1f);
    final PathInterpolator ease = new PathInterpolator(.4f, 0f, .2f, 1f);
    final boolean frost = Build.VERSION.SDK_INT >= 31;
    final ArrayList<FrameLayout> hosts = new ArrayList<>();

    FrameLayout root, sheet, addBtn, selBtn, gearBtn, barRm, barCancel, selPop;
    ScrollView scroll, formPane;
    LinearLayout activeList, finList, dock, bar, menuPop, formContent, dFiles, detailRow, popOpts, detailPane;
    TextView sub, finHead, dKind, dTitle, dNotes, finBtn, menuBtn, ghost, pickFiles, addSubmit, popCheck, barRmLabel;
    View scrim, tint;
    ImageView backdrop;
    EditText subj, notes;
    Spinner typeSp;
    NumberPicker pm, pd, py;
    GradientDrawable sheetBg;
    ColorDrawable tintBg = new ColorDrawable(TINT_OFF);
    ValueAnimator rectAnim, menuAnim;
    Task cur, editing;
    long goneId = -1;
    boolean open, peeking, sharedMode, menuOpen, frostQueued;
    int mode = 0, pane = 0, yearBase;
    final Set<Long> sel = new HashSet<>();
    ArrayList<String> formFiles = new ArrayList<>();

    @Override protected void attachBaseContext(Context b) { super.attachBaseContext(S.wrap(b)); }

    int dp(float v) { return S.dp(this, v); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTheme(S.light(this) ? R.style.AppThemeLight : R.style.AppTheme);
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.root); sheet = findViewById(R.id.sheet); scroll = findViewById(R.id.scroll);
        formPane = findViewById(R.id.formPane); formContent = findViewById(R.id.formContent);
        detailPane = findViewById(R.id.detailPane); detailRow = findViewById(R.id.detailRow);
        activeList = findViewById(R.id.activeList); finList = findViewById(R.id.finList);
        dock = findViewById(R.id.dock); bar = findViewById(R.id.bar); selPop = findViewById(R.id.selPop); menuPop = findViewById(R.id.menuPop);
        popOpts = findViewById(R.id.popOpts); popCheck = findViewById(R.id.popCheck);
        addBtn = findViewById(R.id.addBtn); selBtn = findViewById(R.id.selBtn); gearBtn = findViewById(R.id.gearBtn);
        barRm = findViewById(R.id.barRm); barCancel = findViewById(R.id.barCancel); barRmLabel = findViewById(R.id.barRmLabel);
        sub = findViewById(R.id.sub); finHead = findViewById(R.id.finHead);
        dKind = findViewById(R.id.dKind); dTitle = findViewById(R.id.dTitle); dNotes = findViewById(R.id.dNotes); dFiles = findViewById(R.id.dFiles);
        finBtn = findViewById(R.id.finBtn); menuBtn = findViewById(R.id.menuBtn); ghost = findViewById(R.id.ghost);
        pickFiles = findViewById(R.id.pickFiles); addSubmit = findViewById(R.id.addSubmit); scrim = findViewById(R.id.scrim);
        subj = findViewById(R.id.subj); notes = findViewById(R.id.notes); typeSp = findViewById(R.id.typeSp);
        pm = findViewById(R.id.pm); pd = findViewById(R.id.pd); py = findViewById(R.id.py);
        backdrop = findViewById(R.id.backdrop); tint = findViewById(R.id.tint); tint.setBackground(tintBg);

        for (FrameLayout f : new FrameLayout[]{addBtn, selBtn, gearBtn, barRm, barCancel, selPop}) makeFrost(f);
        S.apply(this, root);
        getWindow().setStatusBarColor(S.bg(this));
        getWindow().setNavigationBarColor(S.bg(this));
        if (S.light(this)) root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        sheetBg = new GradientDrawable();
        sheetBg.setCornerRadius(dp(26));
        sheetBg.setColor(S.card(this));
        sheet.setBackground(sheetBg);

        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{getString(R.string.homework), getString(R.string.exam)});
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSp.setAdapter(ad);

        yearBase = LocalDate.now().getYear() - 1;
        String[] months = new String[12];
        for (int i = 0; i < 12; i++) {
            String m = Month.of(i + 1).getDisplayName(TextStyle.FULL, S.locale(this));
            months[i] = m.substring(0, 1).toUpperCase(S.locale(this)) + m.substring(1);
        }
        pm.setMinValue(0); pm.setMaxValue(11); pm.setDisplayedValues(months);
        pd.setMinValue(1); pd.setMaxValue(31);
        py.setMinValue(yearBase); py.setMaxValue(yearBase + 9);
        NumberPicker.OnValueChangeListener upd = (p, o, n) -> updateDays();
        pm.setOnValueChangedListener(upd); py.setOnValueChangedListener(upd);

        addBtn.setOnClickListener(v -> { if (mode == 0 && !open && !menuOpen) openSheet(addBtn, null, false); });
        gearBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        selBtn.setOnClickListener(v -> { if (mode == 0 && !open) openMenu(); });
        scrim.setOnClickListener(v -> { if (open && !peeking) closeSheet(); });
        finBtn.setOnClickListener(v -> { if (cur != null) { cur.done = !cur.done; save(); closeSheet(); } });
        menuBtn.setOnClickListener(v -> toggleEditMenu());
        findViewById(R.id.mPost).setOnClickListener(v -> shift(1));
        findViewById(R.id.mAdv).setOnClickListener(v -> shift(-1));
        findViewById(R.id.mEdit).setOnClickListener(v -> { hideEditMenu(); if (cur != null) openForm(cur); });
        findViewById(R.id.mWord).setOnClickListener(v -> { hideEditMenu(); newDoc("docx"); });
        findViewById(R.id.mPdf).setOnClickListener(v -> { hideEditMenu(); newDoc("pdf"); });
        findViewById(R.id.btnWord).setOnClickListener(v -> newDoc("docx"));
        findViewById(R.id.btnPdf).setOnClickListener(v -> newDoc("pdf"));
        findViewById(R.id.sAll).setOnClickListener(v -> { closeMenu(); removeFinished(); });
        findViewById(R.id.sFin).setOnClickListener(v -> startSel(1));
        findViewById(R.id.sAct).setOnClickListener(v -> startSel(2));
        barCancel.setOnClickListener(v -> endSel());
        barRm.setOnClickListener(v -> removeSelected());
        addSubmit.setOnClickListener(v -> submit());
        pickFiles.setOnClickListener(v -> {
            if (!ensureFolder()) return;
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(i, RQ_PICK);
        });
        pickFiles.setOnDragListener((v, e) -> {
            int a = e.getAction();
            if (a == DragEvent.ACTION_DRAG_STARTED) return true;
            if (a == DragEvent.ACTION_DRAG_ENTERED) { v.setAlpha(.6f); return true; }
            if (a == DragEvent.ACTION_DRAG_EXITED || a == DragEvent.ACTION_DRAG_ENDED) { v.setAlpha(1); return true; }
            if (a == DragEvent.ACTION_DROP) {
                v.setAlpha(1);
                if (!ensureFolder()) return true;
                try { requestDragAndDropPermissions(e); } catch (Exception ex) { }
                ClipData cd = e.getClipData();
                if (cd != null) for (int i = 0; i < cd.getItemCount(); i++)
                    if (cd.getItemAt(i).getUri() != null) addFileUri(cd.getItemAt(i).getUri());
                return true;
            }
            return false;
        });
        scroll.setOnScrollChangeListener((v, x, y, ox, oy) -> scheduleFrost());
        root.addOnLayoutChangeListener((v, l, t, r, bt, ol, ot, or, ob) -> scheduleFrost());

        tasks.addAll(Store.load(this));
        render();

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !S.p(this).getBoolean("askedNotif", false)) {
            S.p(this).edit().putBoolean("askedNotif", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 3);
        }
        Notifier.schedule(this);
    }

    @Override protected void onResume() {
        super.onResume();
        if (S.dirty) { S.dirty = false; recreate(); return; }
        scheduleFrost();
    }

    // ---------- frosted glass buttons ----------
    void makeFrost(FrameLayout host) {
        host.setClipToOutline(true);
        host.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(26)); }
        });
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.FIT_XY);
        View t = new View(this);
        host.addView(iv, 0, new FrameLayout.LayoutParams(1, 1));
        host.addView(t, 1, new FrameLayout.LayoutParams(-1, -1));
        GradientDrawable st = new GradientDrawable();
        st.setCornerRadius(dp(26));
        st.setColor(0);
        st.setStroke(dp(1), 0x33FFFFFF);
        host.setForeground(st);
        hosts.add(host);
    }

    void scheduleFrost() {
        if (frostQueued) return;
        frostQueued = true;
        fh.postDelayed(() -> { frostQueued = false; refreshFrost(); }, 32);
    }

    void refreshFrost() {
        int w = root.getWidth(), hh = root.getHeight();
        if (w == 0 || hh == 0) return;
        Bitmap bm = null;
        if (frost) {
            bm = Bitmap.createBitmap(Math.max(1, w / 4), Math.max(1, hh / 4), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            c.scale(.25f, .25f);
            c.drawColor(S.bg(this));
            int[] a = new int[2], b = new int[2];
            scroll.getLocationInWindow(a);
            root.getLocationInWindow(b);
            c.translate(a[0] - b[0], a[1] - b[1]);
            scroll.draw(c);
        }
        for (FrameLayout host : hosts) {
            if (host.getVisibility() != View.VISIBLE || host.getWidth() == 0) continue;
            ImageView iv = (ImageView) host.getChildAt(0);
            host.getChildAt(1).setBackgroundColor(S.frostTint(this, frost));
            if (!frost) { iv.setVisibility(View.GONE); continue; }
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) iv.getLayoutParams();
            if (lp.width != w || lp.height != hh) { lp.width = w; lp.height = hh; lp.gravity = Gravity.TOP | Gravity.START; iv.setLayoutParams(lp); }
            Rect r = rectOf(host);
            iv.setTranslationX(-r.left);
            iv.setTranslationY(-r.top);
            iv.setImageBitmap(bm);
            if (iv.getTag() == null) { iv.setRenderEffect(RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP)); iv.setTag("fx"); }
        }
    }

    // ---------- list ----------
    void save() { Store.save(this, tasks); }

    void render() {
        activeList.removeAllViews();
        finList.removeAllViews();
        ArrayList<Task> a = new ArrayList<>(), f = new ArrayList<>();
        for (Task t : tasks) (t.done ? f : a).add(t);
        Collections.sort(a, (x, y) -> x.due.compareTo(y.due));
        for (Task t : a) activeList.addView(card(t));
        for (Task t : f) finList.addView(card(t));
        finHead.setVisibility(f.isEmpty() ? View.GONE : View.VISIBLE);
        sub.setText(a.isEmpty() ? getString(R.string.all_clear) : getString(R.string.n_todo, a.size()));
        barRmLabel.setText(sel.isEmpty() ? getString(R.string.remove) : getString(R.string.remove_n, sel.size()));
        scheduleFrost();
    }

    View card(final Task t) {
        View v = getLayoutInflater().inflate(R.layout.item_work, null);
        S.apply(this, v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(160));
        lp.bottomMargin = dp(10);
        v.setLayoutParams(lp);
        ((TextView) v.findViewById(R.id.iKind)).setText(S.kindLabel(this, t) + " · " + S.label(this, t.due));
        TextView title = v.findViewById(R.id.iTitle);
        title.setText(t.subject);
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
            float sx, sy;
            boolean moved, peeked;
            Runnable r;

            @Override public boolean onTouch(final View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        sx = e.getRawX(); sy = e.getRawY(); moved = false; peeked = false;
                        if (mode == 0 && !open && !menuOpen) {
                            v.animate().scaleX(.965f).scaleY(.965f).setDuration(400).start();
                            r = () -> { peeked = true; v.animate().cancel(); v.setScaleX(1); v.setScaleY(1); openSheet(v, t, true); };
                            h.postDelayed(r, 400);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!moved && Math.hypot(e.getRawX() - sx, e.getRawY() - sy) > dp(10)) {
                            moved = true;
                            if (r != null) h.removeCallbacks(r);
                            v.animate().scaleX(1).scaleY(1).setDuration(150).start();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (r != null) h.removeCallbacks(r);
                        v.animate().cancel(); v.setScaleX(1); v.setScaleY(1);
                        if (peeked) closeSheet();
                        else if (e.getActionMasked() == MotionEvent.ACTION_UP && !moved) {
                            if (mode != 0) { if (pick) { if (!sel.remove(t.id)) sel.add(t.id); render(); } }
                            else if (!open && !menuOpen) openSheet(v, t, false);
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
        v.getLocationInWindow(a);
        root.getLocationInWindow(b);
        int l = a[0] - b[0], t = a[1] - b[1];
        return new Rect(l, t, l + v.getWidth(), t + v.getHeight());
    }

    Rect center(int wdp, int hdp) {
        int rw = root.getWidth(), rh = root.getHeight();
        int w = Math.min(dp(wdp), rw - dp(32)), hh = Math.min(dp(hdp), rh - dp(60));
        int l = (rw - w) / 2, t = (rh - hh) / 2;
        return new Rect(l, t, l + w, t + hh);
    }

    void setRect(View v, int l, int t, int w, int hh) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = l - root.getPaddingLeft();
        lp.topMargin = t - root.getPaddingTop();
        lp.width = w; lp.height = hh;
        v.setLayoutParams(lp);
        if (v == sheet) { backdrop.setTranslationX(-l); backdrop.setTranslationY(-t); }
        else if (hosts.contains(v)) { View iv = ((ViewGroup) v).getChildAt(0); iv.setTranslationX(-l); iv.setTranslationY(-t); }
    }

    int lerp(int a, int b, float f) { return Math.round(a + (b - a) * f); }

    ValueAnimator animView(final View v, final Rect a, final Rect b, long dur, TimeInterpolator ip, final Runnable end) {
        final boolean[] cancelled = new boolean[1];
        ValueAnimator va = ValueAnimator.ofFloat(0, 1);
        va.setDuration(dur);
        va.setInterpolator(ip);
        va.addUpdateListener(x -> {
            float f = (float) x.getAnimatedValue();
            setRect(v, lerp(a.left, b.left, f), lerp(a.top, b.top, f), lerp(a.width(), b.width(), f), lerp(a.height(), b.height(), f));
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationCancel(Animator an) { cancelled[0] = true; }
            @Override public void onAnimationEnd(Animator an) { if (!cancelled[0] && end != null) end.run(); }
        });
        va.start();
        return va;
    }

    void animTint(int from, int to, long dur) {
        ValueAnimator va = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        va.setDuration(dur);
        va.addUpdateListener(x -> tintBg.setColor((int) x.getAnimatedValue()));
        va.start();
    }

    // ---------- blur helpers (real blur on Android 12+) ----------
    void blurAnim(final View v, float from, float to, long delay, long dur) {
        if (!frost) return;
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setStartDelay(delay);
        a.setDuration(dur);
        a.addUpdateListener(x -> {
            float r = (float) x.getAnimatedValue();
            v.setRenderEffect(r < .5f ? null : RenderEffect.createBlurEffect(r, r, Shader.TileMode.DECAL));
        });
        a.start();
    }

    void clearBlur(View v) { if (frost) v.setRenderEffect(null); }

    void reset(View c) {
        c.animate().cancel();
        c.setAlpha(1); c.setTranslationY(0); c.setScaleX(1); c.setScaleY(1);
        clearBlur(c);
    }

    void stagger(ViewGroup g, int skip) {
        View bt = g == formContent ? formPane : (skip == 0 ? detailPane : null);
        clearBlur(formPane); clearBlur(detailPane);
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            reset(c);
            if (i < skip) continue;
            c.setAlpha(0);
            c.setTranslationY(dp(50));
            long d = 120 + 50L * (i - skip);
            c.animate().alpha(1).translationY(0).setStartDelay(d).setDuration(600).setInterpolator(spring).start();
            if (bt == null) blurAnim(c, 10f, 0f, d, 600);
        }
        if (bt != null) blurAnim(bt, 10f, 0f, 100, 800);
    }

    // snapshot of the list behind the card, blurred = real frosted glass
    void captureBackdrop(boolean fromAdd) {
        try {
            int w = root.getWidth(), hh = root.getHeight();
            Bitmap bm = Bitmap.createBitmap(w / 2, hh / 2, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            c.scale(.5f, .5f);
            c.drawColor(S.bg(this));
            int[] a = new int[2], b = new int[2];
            scroll.getLocationInWindow(a);
            root.getLocationInWindow(b);
            c.translate(a[0] - b[0], a[1] - b[1]);
            scroll.draw(c);
            backdrop.setImageBitmap(bm);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) backdrop.getLayoutParams();
            lp.width = w; lp.height = hh; lp.gravity = Gravity.TOP | Gravity.START;
            backdrop.setLayoutParams(lp);
            backdrop.setVisibility(View.VISIBLE);
            tint.setVisibility(View.VISIBLE);
            backdrop.animate().cancel();
            if (fromAdd) {
                backdrop.setAlpha(1);
                backdrop.setRenderEffect(RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP));
                tintBg.setColor(S.frostTint(this, true));
            } else {
                backdrop.setAlpha(0);
                backdrop.animate().alpha(1).setDuration(500).start();
                tintBg.setColor(S.sheetTint(this) & 0xFFFFFF);
                blurAnim(backdrop, 0f, 28f, 0, 700);
            }
        } catch (Exception e) { }
    }

    // ---------- sheet ----------
    void showPane(int p, boolean shared) {
        pane = p; sharedMode = shared; hideEditMenu();
        formPane.setVisibility(p == 0 ? View.VISIBLE : View.GONE);
        detailPane.setVisibility(p == 1 ? View.VISIBLE : View.GONE);
        if (p == 0) stagger(formContent, 0);
        else {
            detailRow.setVisibility(peeking ? View.INVISIBLE : View.VISIBLE);
            stagger(detailPane, shared ? 3 : 0);
        }
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
        sheet.setElevation(dp(12));
        selBtn.animate().alpha(0).setDuration(200).start();
        gearBtn.animate().alpha(0).setDuration(200).start();
        if (frost && !wasOpen) captureBackdrop(fromAdd);
        if (frost && wasOpen) { backdrop.animate().cancel(); backdrop.animate().alpha(1).setDuration(300).start(); tintBg.setColor(S.sheetTint(this)); }
        sheet.setVisibility(View.VISIBLE);
        setRect(sheet, from.left, from.top, from.width(), from.height());
        scrim.setVisibility(View.VISIBLE);
        scrim.animate().alpha(1).setDuration(400).start();
        if (!wasOpen) {
            sheetBg.setColor(S.card(this));
            if (fromAdd) { ghost.setVisibility(View.VISIBLE); ghost.setAlpha(1); ghost.animate().alpha(0).setStartDelay(100).setDuration(250).start(); }
            if (frost) animTint(fromAdd ? S.frostTint(this, true) : (S.sheetTint(this) & 0xFFFFFF), S.sheetTint(this), 600);
        }
        rectAnim = animView(sheet, from, to, 600, spring, null);
        if (fromAdd) openForm(null);
        else { bindDetail(t); showPane(1, !wasOpen); }
    }

    void closeSheet() {
        if (!open) return;
        open = false; peeking = false;
        hideEditMenu();
        sheet.setElevation(0);
        ViewGroup g = pane == 0 ? formContent : detailPane;
        int skip = (pane == 1 && sharedMode) ? 3 : 0;
        View bt = pane == 0 ? formPane : (skip == 0 ? detailPane : null);
        for (int i = skip; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            c.animate().cancel();
            c.animate().alpha(0).translationY(dp(30)).setStartDelay(0).setDuration(250).start();
            if (bt == null) blurAnim(c, 0f, 10f, 0, 250);
        }
        if (bt != null) blurAnim(bt, 0f, 10f, 0, 250);
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
            if (frost) {
                if (toAdd) animTint(S.sheetTint(this), S.frostTint(this, true), 450);
                else {
                    animTint(S.sheetTint(this), S.sheetTint(this) & 0xFFFFFF, 450);
                    backdrop.animate().cancel();
                    backdrop.animate().alpha(0).setDuration(450).start();
                }
            }
            if (toAdd) { ghost.setVisibility(View.VISIBLE); ghost.setAlpha(0); ghost.animate().alpha(1).setStartDelay(250).setDuration(250).start(); }
            rectAnim = animView(sheet, from, to, 480, ease, () -> {
                sheet.setVisibility(View.GONE);
                ghost.setVisibility(View.GONE);
                backdrop.setVisibility(View.GONE);
                tint.setVisibility(View.GONE);
                clearBlur(backdrop);
                goneId = -1; cur = null;
                addBtn.setAlpha(1);
                selBtn.animate().alpha(1).setStartDelay(0).setDuration(200).start();
                gearBtn.animate().alpha(1).setStartDelay(0).setDuration(200).start();
                for (int i = 0; i < formContent.getChildCount(); i++) reset(formContent.getChildAt(i));
                for (int i = 0; i < detailPane.getChildCount(); i++) reset(detailPane.getChildAt(i));
                clearBlur(formPane); clearBlur(detailPane);
                render();
            });
        });
    }

    View findCard(Task t) {
        if (t == null) return null;
        for (LinearLayout l : new LinearLayout[]{activeList, finList})
            for (int i = 0; i < l.getChildCount(); i++) {
                Object tag = l.getChildAt(i).getTag();
                if (tag instanceof Long && (Long) tag == t.id) return l.getChildAt(i);
            }
        return null;
    }

    void bindDetail(Task t) {
        dKind.setText(S.kindLabel(this, t) + " · " + S.label(this, t.due));
        dTitle.setText(t.subject);
        dNotes.setText(t.notes);
        finBtn.setText(t.done ? R.string.restore : R.string.finish_work);
        dFiles.removeAllViews();
        if (t.files.isEmpty()) dFiles.addView(fileRow(getString(R.string.no_files), null, true));
        else for (String f : t.files) dFiles.addView(fileRow("📄  " + Task.nameOf(f), f, false));
    }

    TextView fileRow(String s, final String enc, boolean dim) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(16);
        tv.setTextColor(dim ? S.mute(this) : S.ink(this));
        tv.setPadding(0, dp(8), 0, dp(8));
        if (enc != null) tv.setOnClickListener(v -> openFile(enc));
        return tv;
    }

    void openFile(String enc) {
        String name = Task.nameOf(enc), u = Task.uriOf(enc);
        if (u.isEmpty()) { Toast.makeText(this, R.string.open_fail, Toast.LENGTH_SHORT).show(); return; }
        String low = name.toLowerCase();
        if (low.endsWith(".docx") || low.endsWith(".pdf")) {
            Intent i = new Intent(this, EditorActivity.class);
            i.putExtra("uri", u);
            i.putExtra("name", name);
            startActivityForResult(i, RQ_DOC);
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            String mime = getContentResolver().getType(Uri.parse(u));
            i.setDataAndType(Uri.parse(u), mime == null ? "*/*" : mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) { Toast.makeText(this, R.string.open_fail, Toast.LENGTH_SHORT).show(); }
    }

    // ---------- form ----------
    void openForm(Task t) {
        editing = t;
        subj.setText(t == null ? "" : t.subject);
        notes.setText(t == null ? "" : t.notes);
        typeSp.setSelection(t != null && t.exam() ? 1 : 0);
        formFiles = t == null ? new ArrayList<String>() : new ArrayList<>(t.files);
        updateFilesText();
        setDate(t == null ? LocalDate.now().plusDays(1) : t.due);
        addSubmit.setText(t == null ? R.string.add : R.string.save);
        showPane(0, false);
    }

    void updateDays() { pd.setMaxValue(YearMonth.of(py.getValue(), pm.getValue() + 1).lengthOfMonth()); }

    void setDate(LocalDate d) {
        py.setValue(Math.max(yearBase, Math.min(yearBase + 9, d.getYear())));
        pm.setValue(d.getMonthValue() - 1);
        updateDays();
        pd.setValue(d.getDayOfMonth());
    }

    void submit() {
        String s = subj.getText().toString().trim();
        if (s.isEmpty()) { subj.setError(getString(R.string.required)); subj.requestFocus(); return; }
        Task t = editing != null ? editing : new Task();
        if (editing == null) { t.id = System.currentTimeMillis(); tasks.add(t); }
        t.kind = typeSp.getSelectedItemPosition() == 1 ? "exam" : "homework";
        t.subject = s;
        t.notes = notes.getText().toString().trim();
        t.due = LocalDate.of(py.getValue(), pm.getValue() + 1, pd.getValue());
        t.files = new ArrayList<>(formFiles);
        cur = t; goneId = t.id;
        save();
        hideKb();
        bindDetail(t);
        showPane(1, false);
        render();
        Notifier.schedule(this);
    }

    void hideKb() {
        InputMethodManager im = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(root.getWindowToken(), 0);
    }

    void updateFilesText() {
        if (formFiles.isEmpty()) pickFiles.setText(R.string.files_hint);
        else {
            ArrayList<String> n = new ArrayList<>();
            for (String f : formFiles) n.add(Task.nameOf(f));
            pickFiles.setText(TextUtils.join(", ", n));
        }
    }

    boolean ensureFolder() {
        if (S.folder(this) != null) return true;
        new AlertDialog.Builder(this)
                .setMessage(R.string.need_folder)
                .setPositiveButton(R.string.choose_folder, (d, w) -> startActivity(new Intent(this, SettingsActivity.class)))
                .setNegativeButton(R.string.cancel, null)
                .show();
        return false;
    }

    void addFileUri(final Uri u) {
        final String name = S.displayName(this, u);
        new Thread(() -> {
            final String enc = S.copyIn(this, u, name);
            runOnUiThread(() -> {
                if (enc != null) { formFiles.add(enc); updateFilesText(); }
                else Toast.makeText(this, R.string.save_fail, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    void newDoc(String type) {
        if (!ensureFolder()) return;
        Intent i = new Intent(this, EditorActivity.class);
        i.putExtra("mode", type);
        startActivityForResult(i, RQ_DOC);
    }

    @Override protected void onActivityResult(int rq, int rc, Intent d) {
        super.onActivityResult(rq, rc, d);
        if (rc != RESULT_OK || d == null) return;
        if (rq == RQ_PICK) {
            if (d.getClipData() != null) for (int i = 0; i < d.getClipData().getItemCount(); i++) addFileUri(d.getClipData().getItemAt(i).getUri());
            else if (d.getData() != null) addFileUri(d.getData());
        } else if (rq == RQ_DOC && d.getStringExtra("uri") != null) {
            String enc = Task.enc(d.getStringExtra("name"), d.getStringExtra("uri"));
            if (open && pane == 1 && cur != null) { cur.files.add(enc); save(); bindDetail(cur); }
            else { formFiles.add(enc); updateFilesText(); }
        }
    }

    // ---------- edit menu (inside the card) ----------
    void shift(int n) {
        hideEditMenu();
        if (cur == null) return;
        cur.due = cur.due.plusDays(n);
        save();
        bindDetail(cur);
        render();
    }

    void toggleEditMenu() {
        if (menuPop.getVisibility() == View.VISIBLE) { hideEditMenu(); return; }
        menuPop.setVisibility(View.VISIBLE);
        menuPop.setAlpha(0); menuPop.setScaleX(.4f); menuPop.setScaleY(.4f);
        menuPop.post(() -> {
            menuPop.setPivotX(menuPop.getWidth());
            menuPop.setPivotY(menuPop.getHeight());
            menuPop.animate().alpha(1).scaleX(1).scaleY(1).setDuration(350).setInterpolator(spring).start();
        });
    }

    void hideEditMenu() {
        if (menuPop.getVisibility() != View.VISIBLE) return;
        menuPop.animate().alpha(0).scaleX(.4f).scaleY(.4f).setDuration(250).withEndAction(() -> menuPop.setVisibility(View.GONE)).start();
    }

    // ---------- clean-up menu: the check button itself grows into the menu ----------
    Rect menuRect(Rect c) {
        int w = Math.min(dp(300), root.getWidth() - dp(36)), hh = dp(176);
        return new Rect(c.right - w, c.bottom - hh, c.right, c.bottom);
    }

    void openMenu() {
        if (menuOpen) return;
        menuOpen = true;
        Rect c = rectOf(selBtn);
        selPop.setVisibility(View.VISIBLE);
        setRect(selPop, c.left, c.top, c.width(), c.height());
        popOpts.animate().cancel(); popCheck.animate().cancel();
        popOpts.setAlpha(0); popCheck.setAlpha(1);
        selBtn.setAlpha(0);
        selPop.getChildAt(1).setBackgroundColor(S.frostTint(this, frost));
        selPop.post(this::refreshFrost);
        if (menuAnim != null) menuAnim.cancel();
        menuAnim = animView(selPop, c, menuRect(c), 450, spring, null);
        popCheck.animate().alpha(0).setStartDelay(0).setDuration(150).start();
        popOpts.animate().alpha(1).setStartDelay(150).setDuration(300).start();
        blurAnim(popOpts, 8f, 0f, 150, 350);
    }

    void closeMenu() {
        if (!menuOpen) return;
        menuOpen = false;
        final Rect c = rectOf(selBtn);
        Rect from = rectOf(selPop);
        popOpts.animate().cancel(); popCheck.animate().cancel();
        popOpts.animate().alpha(0).setStartDelay(0).setDuration(150).start();
        popCheck.animate().alpha(1).setStartDelay(220).setDuration(200).start();
        if (menuAnim != null) menuAnim.cancel();
        menuAnim = animView(selPop, from, c, 380, ease, () -> {
            if (menuOpen) return;
            selPop.setVisibility(View.GONE);
            selBtn.setAlpha(1);
        });
    }

    boolean hit(View v, MotionEvent e) {
        int[] a = new int[2];
        v.getLocationOnScreen(a);
        return e.getRawX() >= a[0] && e.getRawX() <= a[0] + v.getWidth() && e.getRawY() >= a[1] && e.getRawY() <= a[1] + v.getHeight();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (menuOpen && !hit(selPop, e)) closeMenu();
            if (menuPop.getVisibility() == View.VISIBLE && !hit(menuPop, e) && !hit(menuBtn, e)) hideEditMenu();
        }
        return super.dispatchTouchEvent(e);
    }

    // ---------- selection / removal ----------
    void startSel(int m) {
        menuOpen = false;
        selPop.setVisibility(View.GONE);
        selBtn.setAlpha(1);
        mode = m;
        sel.clear();
        dock.animate().translationY(dp(140)).alpha(0).setDuration(400).setInterpolator(spring).start();
        bar.setVisibility(View.VISIBLE);
        bar.setTranslationY(dp(120));
        bar.animate().translationY(0).setDuration(450).setInterpolator(spring).start();
        render();
    }

    void endSel() {
        mode = 0;
        sel.clear();
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
            for (int i = 0; i < l.getChildCount(); i++) {
                Object tag = l.getChildAt(i).getTag();
                if (tag instanceof Long && sel.contains((Long) tag)) fadeOut(l.getChildAt(i));
            }
        h.postDelayed(() -> { tasks.removeIf(t -> sel.contains(t.id)); save(); endSel(); }, 380);
    }

    void removeFinished() {
        for (int i = 0; i < finList.getChildCount(); i++) fadeOut(finList.getChildAt(i));
        h.postDelayed(() -> { tasks.removeIf(t -> t.done); save(); render(); }, 380);
    }

    @Override public void onBackPressed() {
        if (open) closeSheet();
        else if (menuOpen) closeMenu();
        else if (mode != 0) endSel();
        else super.onBackPressed();
    }
}
