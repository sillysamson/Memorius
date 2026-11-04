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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.*;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class MainActivity extends Activity {
    static final int RQ_PICK = 7, RQ_DOC = 8, RQ_NOTE = 9;
    static final int TINT_OFF = 0x00000000;

    /** Progress ring */
    static class Ring extends View {
        float pct;
        int track, fill, ink;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Ring(Context c) { super(c); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), s = Math.min(w, h), sw = s * .12f;
            RectF r = new RectF((w - s) / 2 + sw / 2, (h - s) / 2 + sw / 2, (w + s) / 2 - sw / 2, (h + s) / 2 - sw / 2);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(sw);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(track);
            c.drawArc(r, 0, 360, false, p);
            p.setColor(fill);
            if (pct > 0) c.drawArc(r, -90, 360 * pct, false, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(ink);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(s * .22f);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText(Math.round(pct * 100) + "%", w / 2, h / 2 + s * .08f, p);
        }
    }

    final ArrayList<Task> tasks = new ArrayList<>();
    final Handler h = new Handler(Looper.getMainLooper());
    final PathInterpolator spring = new PathInterpolator(.32f, .72f, 0f, 1f);
    final PathInterpolator ease = new PathInterpolator(.4f, 0f, .2f, 1f);
    final OvershootInterpolator over = new OvershootInterpolator(.7f);
    final boolean frost = Build.VERSION.SDK_INT >= 31;
    final ArrayList<FrameLayout> hosts = new ArrayList<>();
    final Choreographer.FrameCallback frameCb = this::frameTick;

    FrameLayout root, content, sheet, navBar, moreBtn, gearBtn, barRm, barCancel, selPop;
    ScrollView scroll, formPane, progScroll, schedScroll, noteScroll;
    HorizontalScrollView hScroll;
    LinearLayout activeList, finList, hRow, dock, bar, menuPop, formContent, dFiles, detailRow, popOpts, detailPane;
    LinearLayout progBox, schedBox, noteBox, noteList, navRow;
    LinearLayout[] navItems = new LinearLayout[4];
    ImageView[] navIcons = new ImageView[4];
    TextView[] navTexts = new TextView[4];
    View navHi;
    TextView quote, emptyMsg, finHead, dKind, dTitle, dNotes, finBtn, menuBtn, ghost, pickFiles, addSubmit, popCheck, barRmLabel;
    View scrim, tint;
    ImageView backdrop;
    EditText subj, notes, noteSearch;
    Spinner typeSp;
    NumberPicker pm, pd, py;
    GradientDrawable sheetBg;
    ColorDrawable tintBg = new ColorDrawable(TINT_OFF);
    ValueAnimator rectAnim, menuAnim;
    Ring ring;
    float ringTarget;
    Bitmap frostBm;
    long frostUntil;
    boolean frostLoop;
    Task cur, editing;
    long goneId = -1;
    boolean open, peeking, sharedMode, menuOpen;
    boolean sideways, group;
    float popRadius;
    int mode = 0, pane = 0, yearBase, tab = 0, liftShift;
    final Set<Long> sel = new HashSet<>();
    ArrayList<String> formFiles = new ArrayList<>();

    @Override protected void attachBaseContext(Context b) { super.attachBaseContext(S.wrap(b)); }

    int dp(float v) { return S.dp(this, v); }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTheme(S.light(this) ? R.style.AppThemeLight : R.style.AppTheme);
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.root); content = findViewById(R.id.content); sheet = findViewById(R.id.sheet);
        scroll = findViewById(R.id.scroll); progScroll = findViewById(R.id.progScroll);
        schedScroll = findViewById(R.id.schedScroll); noteScroll = findViewById(R.id.noteScroll);
        progBox = findViewById(R.id.progBox); schedBox = findViewById(R.id.schedBox); noteBox = findViewById(R.id.noteBox);
        hScroll = findViewById(R.id.hScroll); hRow = findViewById(R.id.hRow);
        formPane = findViewById(R.id.formPane); formContent = findViewById(R.id.formContent);
        detailPane = findViewById(R.id.detailPane); detailRow = findViewById(R.id.detailRow);
        activeList = findViewById(R.id.activeList); finList = findViewById(R.id.finList);
        dock = findViewById(R.id.dock); bar = findViewById(R.id.bar); selPop = findViewById(R.id.selPop); menuPop = findViewById(R.id.menuPop);
        popOpts = findViewById(R.id.popOpts); popCheck = findViewById(R.id.popCheck);
        navBar = findViewById(R.id.navBar); navRow = findViewById(R.id.navRow); navHi = findViewById(R.id.navHi);
        moreBtn = findViewById(R.id.moreBtn); gearBtn = findViewById(R.id.gearBtn);
        barRm = findViewById(R.id.barRm); barCancel = findViewById(R.id.barCancel); barRmLabel = findViewById(R.id.barRmLabel);
        quote = findViewById(R.id.quote); emptyMsg = findViewById(R.id.emptyMsg); finHead = findViewById(R.id.finHead);
        dKind = findViewById(R.id.dKind); dTitle = findViewById(R.id.dTitle); dNotes = findViewById(R.id.dNotes); dFiles = findViewById(R.id.dFiles);
        finBtn = findViewById(R.id.finBtn); menuBtn = findViewById(R.id.menuBtn); ghost = findViewById(R.id.ghost);
        pickFiles = findViewById(R.id.pickFiles); addSubmit = findViewById(R.id.addSubmit); scrim = findViewById(R.id.scrim);
        subj = findViewById(R.id.subj); notes = findViewById(R.id.notes); typeSp = findViewById(R.id.typeSp);
        pm = findViewById(R.id.pm); pd = findViewById(R.id.pd); py = findViewById(R.id.py);
        backdrop = findViewById(R.id.backdrop); tint = findViewById(R.id.tint); tint.setBackground(tintBg);
        navItems[0] = findViewById(R.id.navCard); navItems[1] = findViewById(R.id.navProg);
        navItems[2] = findViewById(R.id.navSched); navItems[3] = findViewById(R.id.navNote);
        navIcons[0] = findViewById(R.id.navCardIc); navIcons[1] = findViewById(R.id.navProgIc);
        navIcons[2] = findViewById(R.id.navSchedIc); navIcons[3] = findViewById(R.id.navNoteIc);
        navTexts[0] = findViewById(R.id.navCardTx); navTexts[1] = findViewById(R.id.navProgTx);
        navTexts[2] = findViewById(R.id.navSchedTx); navTexts[3] = findViewById(R.id.navNoteTx);

        popRadius = dp(32);
        for (FrameLayout f : new FrameLayout[]{navBar, moreBtn, gearBtn, barRm, barCancel, selPop}) makeFrost(f);
        S.apply(this, root);
        getWindow().setStatusBarColor(S.bg(this));
        getWindow().setNavigationBarColor(S.bg(this));
        if (S.light(this)) root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        sheetBg = new GradientDrawable();
        sheetBg.setCornerRadius(dp(32));
        sheetBg.setColor(S.card(this));
        sheet.setBackground(sheetBg);

        GradientDrawable hi = new GradientDrawable();
        hi.setCornerRadius(dp(26));
        hi.setColor(S.field(this));
        hi.setStroke(dp(2), S.ink(this));
        navHi.setBackground(hi);

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

        for (int i = 0; i < 4; i++) { final int k = i; navItems[i].setOnClickListener(v -> selectTab(k)); }
        navRow.addOnLayoutChangeListener((v, l, t, r, bt, ol, ot, or, ob) -> moveHi(false));
        updateNavColors();

        gearBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        moreBtn.setOnClickListener(v -> { if (mode == 0 && !open) { if (menuOpen) closeMenu(); else openMenu(); } });
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
        findViewById(R.id.sAdd).setOnClickListener(v -> { if (menuOpen) openSheet(selPop, null, false); });
        findViewById(R.id.sQuick).setOnClickListener(v -> { closeMenu(); removeFinished(); });
        findViewById(R.id.sSel).setOnClickListener(v -> { closeMenu(); h.postDelayed(() -> startSel(3), 420); });
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
        ScrollView[] tabs = {scroll, progScroll, schedScroll, noteScroll};
        for (ScrollView sv : tabs) sv.setOnScrollChangeListener((v, x, y, ox, oy) -> pokeFrost(180));
        hScroll.setOnScrollChangeListener((v, x, y, ox, oy) -> { pokeFrost(180); snapSoon(); });
        root.addOnLayoutChangeListener((v, l, t, r, bt, ol, ot, or, ob) -> pokeFrost(200));

        buildNotesHeader();
        String[] qs = getResources().getStringArray(R.array.quotes);
        quote.setText("\"" + qs[LocalDate.now().getDayOfYear() % qs.length] + "\"");

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
        renderNotesList();
        pokeFrost(300);
    }

    // ---------- frosted glass (live: redrawn every frame while content moves) ----------
    void makeFrost(final FrameLayout host) {
        host.setClipToOutline(true);
        host.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                float r = v == selPop ? popRadius : Math.min(v.getHeight() / 2f, dp(32));
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), r);
            }
        });
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.FIT_XY);
        View t = new View(this);
        host.addView(iv, 0, new FrameLayout.LayoutParams(1, 1));
        host.addView(t, 1, new FrameLayout.LayoutParams(-1, -1));
        GradientDrawable st = new GradientDrawable();
        st.setCornerRadius(dp(32));
        st.setColor(0);
        st.setStroke(dp(1), 0x40FFFFFF);
        host.setForeground(st);
        hosts.add(host);
    }

    void pokeFrost(long ms) {
        frostUntil = Math.max(frostUntil, SystemClock.uptimeMillis() + ms);
        if (!frostLoop) { frostLoop = true; Choreographer.getInstance().postFrameCallback(frameCb); }
    }

    void frameTick(long t) {
        refreshFrost();
        if (SystemClock.uptimeMillis() < frostUntil) Choreographer.getInstance().postFrameCallback(frameCb);
        else frostLoop = false;
    }

    void refreshFrost() {
        int w = root.getWidth(), hh = root.getHeight();
        if (w == 0 || hh == 0) return;
        boolean fresh = false;
        if (frost) {
            int bw = Math.max(1, w / 5), bh = Math.max(1, hh / 5);
            if (frostBm == null || frostBm.getWidth() != bw || frostBm.getHeight() != bh) {
                frostBm = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
                fresh = true;
            }
            Canvas c = new Canvas(frostBm);
            c.drawColor(S.bg(this));
            c.scale(bw / (float) w, bh / (float) hh);
            int[] a = new int[2], b = new int[2];
            content.getLocationInWindow(a);
            root.getLocationInWindow(b);
            c.translate(a[0] - b[0], a[1] - b[1]);
            content.draw(c);
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
            if (fresh || iv.getTag() == null) {
                iv.setImageDrawable(new BitmapDrawable(getResources(), frostBm));
                iv.setRenderEffect(RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP));
                iv.setTag("fx");
            }
            iv.invalidate();
        }
    }

    // ---------- tabs ----------
    View tabView(int i) { return i == 0 ? scroll : i == 1 ? progScroll : i == 2 ? schedScroll : noteScroll; }

    void selectTab(int i) {
        if (i == tab || open) return;
        final View old = tabView(tab), nu = tabView(i);
        tab = i;
        nu.animate().cancel();
        nu.setAlpha(0);
        nu.setVisibility(View.VISIBLE);
        nu.animate().alpha(1).setStartDelay(60).setDuration(240).start();
        old.animate().cancel();
        old.animate().alpha(0).setStartDelay(0).setDuration(160).withEndAction(() -> { if (tabView(tab) != old) old.setVisibility(View.GONE); }).start();
        moveHi(true);
        updateNavColors();
        if (i == 1 && ring != null) {
            ValueAnimator va = ValueAnimator.ofFloat(0, ringTarget);
            va.setDuration(700); va.setInterpolator(spring);
            va.addUpdateListener(x -> { ring.pct = (float) x.getAnimatedValue(); ring.invalidate(); });
            va.start();
        }
        pokeFrost(500);
    }

    void updateNavColors() {
        for (int i = 0; i < 4; i++) {
            float a = i == tab ? 1f : .55f;
            navIcons[i].setAlpha(a);
            navTexts[i].setAlpha(a);
        }
    }

    void moveHi(boolean anim) {
        int iw = navRow.getWidth() / 4;
        if (iw == 0) return;
        int m = dp(6);
        ViewGroup.LayoutParams lp = navHi.getLayoutParams();
        if (lp.width != iw - 2 * m) { lp.width = iw - 2 * m; navHi.setLayoutParams(lp); }
        float x = tab * iw + m;
        if (!anim) { navHi.setTranslationX(x); return; }
        navHi.animate().translationX(x).scaleX(1.12f).setDuration(260).setInterpolator(spring)
                .withEndAction(() -> navHi.animate().scaleX(1f).setDuration(220).start()).start();
    }

    // ---------- list ----------
    void save() { Store.save(this, tasks); }

    TextView tv(String s, float sp, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), Typeface.BOLD);
        return t;
    }

    String groupTitle(LocalDate d) {
        LocalDate now = LocalDate.now();
        if (d.isBefore(now)) return getString(R.string.g_overdue);
        if (d.equals(now)) return getString(R.string.g_today);
        if (d.equals(now.plusDays(1))) return getString(R.string.g_tomorrow);
        return d.format(DateTimeFormatter.ofPattern("EEE, d MMM", S.locale(this)));
    }

    String groupKey(LocalDate d) { return d.isBefore(LocalDate.now()) ? "overdue" : d.toString(); }

    TextView groupHeader(String s) {
        TextView t = tv(s, 14, true, S.mute(this));
        t.setPadding(dp(6), dp(10), 0, dp(8));
        return t;
    }

    void render() {
        sideways = S.p(this).getBoolean("exp_side", false);
        group = S.p(this).getBoolean("exp_group", false);
        renderCards();
        renderProgress();
        renderSchedule();
        renderNotesList();
        barRmLabel.setText(sel.isEmpty() ? getString(R.string.remove) : getString(R.string.remove_n, sel.size()));
        pokeFrost(300);
    }

    void renderCards() {
        activeList.removeAllViews();
        hRow.removeAllViews();
        finList.removeAllViews();
        ArrayList<Task> a = new ArrayList<>(), f = new ArrayList<>();
        for (Task t : tasks) (t.done ? f : a).add(t);
        Collections.sort(a, (x, y) -> x.due.compareTo(y.due));
        hScroll.setVisibility(sideways && !a.isEmpty() ? View.VISIBLE : View.GONE);
        activeList.setVisibility(sideways ? View.GONE : View.VISIBLE);
        if (sideways) {
            for (Task t : a) hRow.addView(card(t, true));
        } else {
            String last = null;
            for (Task t : a) {
                if (group) {
                    String k = groupKey(t.due);
                    if (!k.equals(last)) { activeList.addView(groupHeader(groupTitle(t.due))); last = k; }
                }
                activeList.addView(card(t, false));
            }
        }
        for (Task t : f) finList.addView(card(t, false));
        finHead.setVisibility(f.isEmpty() ? View.GONE : View.VISIBLE);
        emptyMsg.setVisibility(a.isEmpty() ? View.VISIBLE : View.GONE);
    }

    View card(final Task t, boolean side) {
        View v = getLayoutInflater().inflate(R.layout.item_work, null);
        S.apply(this, v);
        if (side) {
            int w = (int) (getResources().getDisplayMetrics().widthPixels * .78f);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w, dp(340));
            lp.rightMargin = dp(12);
            v.setLayoutParams(lp);
            ((TextView) v.findViewById(R.id.iNotes)).setMaxLines(10);
        } else {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(160));
            lp.bottomMargin = dp(10);
            v.setLayoutParams(lp);
        }
        ((TextView) v.findViewById(R.id.iKind)).setText(S.kindLabel(this, t) + " · " + S.label(this, t.due));
        TextView title = v.findViewById(R.id.iTitle);
        title.setText(t.subject);
        if (t.done) title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        ((TextView) v.findViewById(R.id.iNotes)).setText(t.notes);
        v.setTag(t.id);
        View ring = v.findViewById(R.id.iRing);
        boolean pick = (mode == 3) || (mode == 1 && t.done) || (mode == 2 && !t.done);
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
                            r = () -> {
                                peeked = true;
                                if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(true);
                                v.animate().cancel(); v.setScaleX(1); v.setScaleY(1);
                                openSheet(v, t, true);
                            };
                            h.postDelayed(r, 400);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!moved && !peeked && Math.hypot(e.getRawX() - sx, e.getRawY() - sy) > dp(8)) {
                            moved = true;
                            if (r != null) h.removeCallbacks(r);
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

    // sideways cards: snap to the nearest card
    final Runnable snapRun = () -> {
        if (hRow.getChildCount() == 0) return;
        int cw = hRow.getChildAt(0).getWidth() + dp(12);
        if (cw <= 0) return;
        int idx = Math.round(hScroll.getScrollX() / (float) cw);
        hScroll.smoothScrollTo(idx * cw, 0);
    };

    void snapSoon() { h.removeCallbacks(snapRun); h.postDelayed(snapRun, 140); }

    // ---------- progress / schedule / notes tabs ----------
    LinearLayout statRow(String label, int value) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        S.round(r, S.card(this), dp(24));
        r.setPadding(dp(20), dp(16), dp(20), dp(16));
        TextView a = tv(label, 16, false, S.ink(this));
        r.addView(a, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(tv(String.valueOf(value), 20, true, S.ink(this)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        r.setLayoutParams(lp);
        return r;
    }

    void renderProgress() {
        progBox.removeAllViews();
        progBox.addView(tv(getString(R.string.tab_progress), 40, true, S.ink(this)));
        int total = tasks.size(), done = 0, over = 0, today = 0;
        LocalDate now = LocalDate.now();
        for (Task t : tasks) {
            if (t.done) done++;
            else if (t.due.isBefore(now)) over++;
            else if (t.due.equals(now)) today++;
        }
        ring = new Ring(this);
        ring.track = S.field(this);
        ring.fill = 0xFF34C759;
        ring.ink = S.ink(this);
        ringTarget = total == 0 ? 0f : done / (float) total;
        ring.pct = tab == 1 ? ringTarget : 0f;
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(dp(200), dp(200));
        rl.gravity = Gravity.CENTER_HORIZONTAL;
        rl.topMargin = dp(24);
        rl.bottomMargin = dp(24);
        progBox.addView(ring, rl);
        progBox.addView(statRow(getString(R.string.st_finished), done));
        progBox.addView(statRow(getString(R.string.st_todo), total - done));
        progBox.addView(statRow(getString(R.string.st_overdue), over));
        progBox.addView(statRow(getString(R.string.st_today), today));
    }

    void renderSchedule() {
        schedBox.removeAllViews();
        schedBox.addView(tv(getString(R.string.tab_schedule), 40, true, S.ink(this)));
        ArrayList<Task> a = new ArrayList<>();
        for (Task t : tasks) if (!t.done) a.add(t);
        Collections.sort(a, (x, y) -> x.due.compareTo(y.due));
        if (a.isEmpty()) {
            TextView e = tv(getString(R.string.sched_empty), 16, false, S.mute(this));
            e.setPadding(0, dp(120), 0, 0);
            e.setGravity(Gravity.CENTER);
            schedBox.addView(e, new LinearLayout.LayoutParams(-1, -2));
            return;
        }
        String last = null;
        for (final Task t : a) {
            String k = groupKey(t.due);
            if (!k.equals(last)) { TextView hd = groupHeader(groupTitle(t.due)); hd.setPadding(dp(6), dp(18), 0, dp(8)); schedBox.addView(hd); last = k; }
            final LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.VERTICAL);
            S.round(r, S.card(this), dp(24));
            r.setPadding(dp(20), dp(14), dp(20), dp(14));
            r.addView(tv(t.subject, 18, true, S.ink(this)));
            r.addView(tv(S.kindLabel(this, t) + " · " + S.label(this, t.due), 13, false, S.mute(this)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8);
            r.setLayoutParams(lp);
            r.setTag(t.id);
            if (t.id == goneId) r.setVisibility(View.INVISIBLE);
            r.setOnClickListener(v -> { if (mode == 0 && !open && !menuOpen) openSheet(r, t, false); });
            schedBox.addView(r);
        }
    }

    void buildNotesHeader() {
        noteBox.addView(tv(getString(R.string.notes_title), 40, true, S.ink(this)));
        noteSearch = new EditText(this);
        noteSearch.setHint(R.string.search_notes);
        noteSearch.setSingleLine(true);
        noteSearch.setTextColor(S.ink(this));
        noteSearch.setHintTextColor((S.mute(this) & 0xFFFFFF) | 0x99000000);
        S.round(noteSearch, S.field(this), dp(16));
        noteSearch.setPadding(dp(16), 0, dp(16), 0);
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-1, dp(48));
        sl.topMargin = dp(16);
        noteBox.addView(noteSearch, sl);
        noteSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { renderNotesList(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        TextView nn = tv("+  " + getString(R.string.new_note), 16, true, S.ink(this));
        nn.setGravity(Gravity.CENTER);
        S.round(nn, S.field(this), dp(16));
        nn.setOnClickListener(v -> openNote(-1));
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(-1, dp(48));
        nl.topMargin = dp(10);
        nl.bottomMargin = dp(14);
        noteBox.addView(nn, nl);
        noteList = new LinearLayout(this);
        noteList.setOrientation(LinearLayout.VERTICAL);
        noteBox.addView(noteList, new LinearLayout.LayoutParams(-1, -2));
    }

    void renderNotesList() {
        if (noteList == null) return;
        noteList.removeAllViews();
        String q = noteSearch == null ? "" : noteSearch.getText().toString().trim().toLowerCase();
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, S.locale(this));
        int shown = 0;
        for (final Notes.Note n : Notes.list(this)) {
            String pv = Notes.preview(this, n.id);
            if (!q.isEmpty() && !n.title.toLowerCase().contains(q) && !pv.toLowerCase().contains(q)) continue;
            shown++;
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.VERTICAL);
            S.round(r, S.card(this), dp(24));
            r.setPadding(dp(20), dp(16), dp(20), dp(16));
            r.addView(tv(n.title, 18, true, S.ink(this)));
            if (!pv.isEmpty()) {
                TextView p = tv(pv, 14, false, S.mute(this));
                p.setMaxLines(2);
                p.setEllipsize(TextUtils.TruncateAt.END);
                p.setPadding(0, dp(4), 0, 0);
                r.addView(p);
            }
            TextView d = tv(df.format(new Date(n.updated)), 12, false, S.mute(this));
            d.setPadding(0, dp(6), 0, 0);
            r.addView(d);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8);
            r.setLayoutParams(lp);
            r.setOnClickListener(v -> openNote(n.id));
            r.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.del_note)
                        .setPositiveButton(R.string.remove, (dg, w) -> { Notes.delete(this, n.id); renderNotesList(); })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            });
            noteList.addView(r);
        }
        if (shown == 0) {
            TextView e = tv(getString(R.string.no_notes), 16, false, S.mute(this));
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, dp(80), 0, 0);
            noteList.addView(e, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    void openNote(long id) {
        Intent i = new Intent(this, EditorActivity.class);
        i.putExtra("mode", "note");
        i.putExtra("noteId", id);
        startActivityForResult(i, RQ_NOTE);
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

    Rect lr(Rect a, Rect b, float t) { return new Rect(lerp(a.left, b.left, t), lerp(a.top, b.top, t), lerp(a.right, b.right, t), lerp(a.bottom, b.bottom, t)); }

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

    // snapshot of the content behind the card, blurred = frosted glass
    void captureBackdrop(boolean fromAdd) {
        try {
            int w = root.getWidth(), hh = root.getHeight();
            Bitmap bm = Bitmap.createBitmap(w / 2, hh / 2, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bm);
            c.scale(.5f, .5f);
            c.drawColor(S.bg(this));
            int[] a = new int[2], b = new int[2];
            content.getLocationInWindow(a);
            root.getLocationInWindow(b);
            c.translate(a[0] - b[0], a[1] - b[1]);
            content.draw(c);
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

    // ---------- gap filling: the cards below slide up into the lifted card's slot ----------
    boolean inList(View card) { ViewParent p = card.getParent(); return p == activeList || p == finList; }

    void gap(View card, boolean lift) {
        if (!inList(card)) return;
        ViewGroup p = (ViewGroup) card.getParent();
        int sh = card.getHeight() + dp(10);
        if (lift) liftShift = sh;
        int i = p.indexOfChild(card);
        for (int k = i + 1; k < p.getChildCount(); k++)
            p.getChildAt(k).animate().translationY(lift ? -sh : 0).setStartDelay(0).setDuration(500).setInterpolator(spring).start();
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
        boolean fromAdd = src == moreBtn || src == selPop;
        Rect from = wasOpen ? rectOf(sheet) : rectOf(src);
        Rect to = peek ? center(340, 420) : center(360, 640);
        if (fromAdd) {
            if (src == selPop) {
                if (menuAnim != null) menuAnim.cancel();
                menuOpen = false;
                selPop.setVisibility(View.GONE);
                selPop.setRotation(0);
            }
            moreBtn.setAlpha(0);
        } else {
            if (!wasOpen && src.getTag() instanceof Long) gap(src, true);
            src.setVisibility(View.INVISIBLE);
        }
        sheet.setElevation(dp(12));
        dock.animate().alpha(0).setStartDelay(0).setDuration(200).start();
        gearBtn.animate().alpha(0).setStartDelay(0).setDuration(200).start();
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
            Rect to = toAdd ? rectOf(moreBtn) : rectOf(target);
            if (frost) {
                if (toAdd) animTint(S.sheetTint(this), S.frostTint(this, true), 450);
                else {
                    animTint(S.sheetTint(this), S.sheetTint(this) & 0xFFFFFF, 450);
                    backdrop.animate().cancel();
                    backdrop.animate().alpha(0).setDuration(450).start();
                }
            }
            if (toAdd) { ghost.setVisibility(View.VISIBLE); ghost.setAlpha(0); ghost.animate().alpha(1).setStartDelay(250).setDuration(250).start(); }
            if (target != null && inList(target) && !sideways) {
                // the slot is empty again: push the cards below it up, then let them slide back down as the card lands
                ViewGroup p = (ViewGroup) target.getParent();
                int shiftBy = liftShift > 0 ? liftShift : dp(170);
                for (int k = p.indexOfChild(target) + 1; k < p.getChildCount(); k++) p.getChildAt(k).setTranslationY(-shiftBy);
                gap(target, false);
            }
            rectAnim = animView(sheet, from, to, 480, ease, () -> {
                sheet.setVisibility(View.GONE);
                ghost.setVisibility(View.GONE);
                backdrop.setVisibility(View.GONE);
                tint.setVisibility(View.GONE);
                clearBlur(backdrop);
                goneId = -1; cur = null;
                moreBtn.setAlpha(1);
                dock.animate().alpha(1).setStartDelay(0).setDuration(200).start();
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
        ViewGroup[] groups = tab == 2 ? new ViewGroup[]{schedBox} : new ViewGroup[]{activeList, hRow, finList};
        for (ViewGroup l : groups)
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
        if (rq == RQ_NOTE) { renderNotesList(); return; }
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

    // ---------- the ••• button: squeezes into a droplet, then opens into the menu ----------
    Rect menuRect(Rect c) {
        int w = Math.min(dp(300), root.getWidth() - dp(36)), hh = dp(188);
        return new Rect(c.right - w, c.bottom - hh, c.right, c.bottom);
    }

    Rect blobRect(Rect s, Rect m) {
        int bw = dp(34), bh = dp(46);
        float cx = s.exactCenterX() + (m.exactCenterX() - s.exactCenterX()) * .12f;
        float cy = s.exactCenterY() + (m.exactCenterY() - s.exactCenterY()) * .12f;
        return new Rect(Math.round(cx - bw / 2f), Math.round(cy - bh / 2f), Math.round(cx + bw / 2f), Math.round(cy + bh / 2f));
    }

    void menuFrame(float f, Rect s, Rect b, Rect m) {
        Rect r;
        float rot, rad;
        if (f < .3f) {
            float u = ease.getInterpolation(f / .3f);
            r = lr(s, b, u);
            rot = -14f * u;
            rad = Math.min(r.width(), r.height()) / 2f;
        } else {
            float u = (f - .3f) / .7f;
            r = lr(b, m, over.getInterpolation(u));
            rot = -14f * (1f - Math.min(1f, u * 1.8f));
            float full = Math.min(r.width(), r.height()) / 2f;
            float blobHalf = Math.min(b.width(), b.height()) / 2f;
            rad = Math.min(full, blobHalf + (dp(32) - blobHalf) * Math.min(1f, u * 2f));
        }
        popRadius = Math.max(1f, rad);
        setRect(selPop, r.left, r.top, Math.max(1, r.width()), Math.max(1, r.height()));
        selPop.setRotation(rot);
        selPop.invalidateOutline();
    }

    void openMenu() {
        if (menuOpen || open) return;
        menuOpen = true;
        final Rect s = rectOf(moreBtn), m = menuRect(s), b = blobRect(s, m);
        selPop.setVisibility(View.VISIBLE);
        popRadius = dp(32);
        setRect(selPop, s.left, s.top, s.width(), s.height());
        selPop.setRotation(0);
        selPop.getChildAt(1).setBackgroundColor(S.frostTint(this, frost));
        pokeFrost(1000);
        popOpts.animate().cancel(); popCheck.animate().cancel();
        popOpts.setAlpha(0); popCheck.setAlpha(1);
        clearBlur(popOpts);
        moreBtn.setAlpha(0);
        if (menuAnim != null) menuAnim.cancel();
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(560);
        va.setInterpolator(new LinearInterpolator());
        va.addUpdateListener(x -> menuFrame((float) x.getAnimatedValue(), s, b, m));
        va.start();
        menuAnim = va;
        popCheck.animate().alpha(0).setStartDelay(0).setDuration(130).start();
        popOpts.animate().alpha(1).setStartDelay(300).setDuration(260).start();
        blurAnim(popOpts, 14f, 0f, 300, 320);
    }

    void closeMenu() {
        if (!menuOpen) return;
        menuOpen = false;
        final Rect s = rectOf(moreBtn), m = menuRect(s), b = blobRect(s, m);
        popOpts.animate().cancel(); popCheck.animate().cancel();
        popOpts.animate().alpha(0).setStartDelay(0).setDuration(120).start();
        popCheck.animate().alpha(1).setStartDelay(260).setDuration(160).start();
        if (menuAnim != null) menuAnim.cancel();
        ValueAnimator va = ValueAnimator.ofFloat(1f, 0f);
        va.setDuration(440);
        va.setInterpolator(ease);
        va.addUpdateListener(x -> menuFrame((float) x.getAnimatedValue(), s, b, m));
        va.addListener(new AnimatorListenerAdapter() {
            boolean cancelled;
            @Override public void onAnimationCancel(Animator an) { cancelled = true; }
            @Override public void onAnimationEnd(Animator an) {
                if (cancelled || menuOpen) return;
                selPop.setVisibility(View.GONE);
                selPop.setRotation(0);
                moreBtn.setAlpha(1);
            }
        });
        va.start();
        menuAnim = va;
        pokeFrost(600);
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
        mode = m;
        sel.clear();
        dock.animate().translationY(dp(140)).alpha(0).setStartDelay(0).setDuration(400).setInterpolator(spring).start();
        bar.setVisibility(View.VISIBLE);
        bar.setTranslationY(dp(120));
        bar.animate().translationY(0).setDuration(450).setInterpolator(spring).start();
        render();
    }

    void endSel() {
        mode = 0;
        sel.clear();
        dock.animate().translationY(0).alpha(1).setStartDelay(0).setDuration(400).setInterpolator(spring).start();
        bar.animate().translationY(dp(120)).setDuration(350).withEndAction(() -> bar.setVisibility(View.GONE)).start();
        render();
    }

    void fadeOut(View v) {
        v.animate().alpha(0).scaleX(.94f).scaleY(.94f).setDuration(350).start();
        blurAnim(v, 0f, 14f, 0, 350);
    }

    void removeSelected() {
        if (sel.isEmpty()) return;
        for (ViewGroup l : new ViewGroup[]{activeList, hRow, finList})
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
