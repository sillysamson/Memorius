package com.memorius.app;

import android.Manifest;
import android.animation.AnimatorListenerAdapter;
import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    static final int REQ_OPUS = 1, REQ_ATTACH = 2, REQ_FOLDER = 3, REQ_CREATE = 4, REQ_PERM = 10;
    static final String[] KEYS = {"cards", "progress", "schedule", "settings"};
    static final int[] NAV_ICONS = {R.drawable.ic_cards, R.drawable.ic_projects, R.drawable.ic_schedule, R.drawable.ic_settings};

    interface BoolSupplier {
        boolean get();
    }

    // ---------- state (pages read these) ----------
    int tab = 0;
    int selDay;
    String mode = "";            // "", "delete", "move"
    Opus openOpus = null;
    Prefs draft;                 // settings being edited, applied only after "Apply"

    // ---------- views ----------
    FrameLayout root, layer, splash, pill, dots;
    LinearLayout main, header, dock, banner, menu;
    ViewPager2 pager;
    TextView brand, sub, splashText;
    View ind, scrim;
    ImageView dotsIcon;
    final ImageView[] navIcon = new ImageView[4];
    final TextView[] navLabel = new TextView[4];
    final Page[] pages = new Page[4];
    CardsPage cardsPage;
    ProjectsPage projectsPage;
    SchedulePage schedulePage;
    SettingsPage settingsPage;

    boolean menuOpen = false, merged = false, lastRtl = false;
    int navSel = 0;
    float itemW = 0f, pos = 0f;
    int layerToken = 0;
    Runnable layerBack = null;
    View sheetView = null, dimView = null;
    ValueAnimator mergeAnim;

    List<FileRef> attachTarget;
    Runnable attachDone;
    byte[] pendingBytes;
    final Handler handler = new Handler(Looper.getMainLooper());

    static int dp(float v) {
        return Ui.dp(v);
    }

    static int todayIdx() {
        int d = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // Sunday = 1
        return d == Calendar.SUNDAY ? 0 : d - 2;
    }

    // ---------- lifecycle ----------
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Store.init(this);
        Ui.density = getResources().getDisplayMetrics().density;
        Str.lang = Store.prefs.lang;
        Ui.applyTheme(Store.prefs);
        draft = Store.prefs.copy();
        selDay = todayIdx();
        lastRtl = Str.rtl();
        buildUi();
        render();
        Notifier.ensureChannel(this);
        Notifier.schedule(this);
        if (!"off".equals(Store.prefs.freq)) ensureNotifPermission();
        hideSplash();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pager != null) render();
    }

    // ---------- UI construction ----------
    private void buildUi() {
        root = new FrameLayout(this);
        setContentView(root);

        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));

        header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPaddingRelative(dp(20), dp(24), dp(20), dp(16));
        brand = Ui.text(this, "Memorius", 28, Ui.fg, true);
        brand.setLetterSpacing(-0.03f);
        sub = Ui.text(this, Str.t(KEYS[0]), 14, Ui.mute, false);
        header.addView(brand);
        header.addView(sub);
        main.addView(header, new LinearLayout.LayoutParams(-1, -2));

        cardsPage = new CardsPage(this);
        projectsPage = new ProjectsPage(this);
        schedulePage = new SchedulePage(this);
        settingsPage = new SettingsPage(this);
        pages[0] = cardsPage;
        pages[1] = projectsPage;
        pages[2] = schedulePage;
        pages[3] = settingsPage;

        pager = new ViewPager2(this);
        pager.setOffscreenPageLimit(3);
        pager.setAdapter(new PagerAdapter());
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float offset, int px) {
                updateInd(position + offset);
            }

            @Override
            public void onPageSelected(int position) {
                if (position != tab) go(position, false);
            }
        });
        main.addView(pager, new LinearLayout.LayoutParams(-1, 0, 1f));

        // tap-away area for the popup
        scrim = new View(this);
        scrim.setVisibility(View.GONE);
        scrim.setOnClickListener(v -> closeMenu());
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));

        // banner (apply / mode hint)
        banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setPaddingRelative(dp(16), dp(8), dp(8), dp(8));
        banner.setVisibility(View.INVISIBLE);
        banner.setAlpha(0f);
        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        bl.setMargins(dp(16), 0, dp(16), dp(88));
        root.addView(banner, bl);

        // dock: pill + multifunction button
        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(-1, dp(56), Gravity.BOTTOM);
        dl.setMargins(dp(16), 0, dp(16), dp(16));
        root.addView(dock, dl);

        pill = new FrameLayout(this);
        pill.setPadding(dp(4), dp(4), dp(4), dp(4));
        ind = new View(this);
        pill.addView(ind, new FrameLayout.LayoutParams(dp(60), dp(48), Gravity.START | Gravity.TOP));
        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            ImageView ic = new ImageView(this);
            ic.setImageResource(NAV_ICONS[i]);
            item.addView(ic, new LinearLayout.LayoutParams(dp(20), dp(20)));
            TextView lb = Ui.text(this, "", 10.5f, Ui.mute, true);
            lb.setSingleLine(true);
            lb.setEllipsize(TextUtils.TruncateAt.END);
            lb.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-1, -2);
            ll.topMargin = dp(2);
            item.addView(lb, ll);
            item.setOnClickListener(v -> go(idx, true));
            Ui.press(item);
            navIcon[i] = ic;
            navLabel[i] = lb;
            navRow.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
        }
        pill.addView(navRow, new FrameLayout.LayoutParams(-1, -1));
        pill.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> layoutInd());
        dock.addView(pill, new LinearLayout.LayoutParams(0, -1, 1f));

        dots = new FrameLayout(this);
        dotsIcon = new ImageView(this);
        dotsIcon.setImageResource(R.drawable.ic_dots);
        dots.addView(dotsIcon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        dots.setOnClickListener(v -> toggleMenu());
        Ui.press(dots);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(56), dp(56));
        dlp.setMarginStart(dp(8));
        dock.addView(dots, dlp);

        // popup menu: grows out of the dots button
        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(4), dp(4), dp(4), dp(4));
        menu.setMinimumWidth(dp(200));
        menu.setVisibility(View.INVISIBLE);
        menu.setAlpha(0f);
        FrameLayout.LayoutParams ml = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        ml.setMarginEnd(dp(16));
        ml.bottomMargin = dp(80);
        root.addView(menu, ml);

        // overlay for sheets and expanded cards
        layer = new FrameLayout(this);
        layer.setVisibility(View.GONE);
        layer.setClickable(true);
        root.addView(layer, new FrameLayout.LayoutParams(-1, -1));

        // splash
        splash = new FrameLayout(this);
        splash.setClickable(true);
        splashText = Ui.text(this, "Memorius", 36, Ui.fg, true);
        splashText.setLetterSpacing(-0.03f);
        splashText.setAlpha(0f);
        splashText.setTranslationY(dp(8));
        splash.addView(splashText, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        root.addView(splash, new FrameLayout.LayoutParams(-1, -1));
        splashText.animate().alpha(1f).translationY(0f).setDuration(500).setInterpolator(Ui.EASE).start();
    }

    private class PagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemCount() {
            return 4;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View v = pages[type].root;
            v.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            return new RecyclerView.ViewHolder(v) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        }
    }

    private void hideSplash() {
        long since = SystemClock.uptimeMillis() - Process.getStartUptimeMillis();
        long wait = Math.max(0L, 900L - since);
        handler.postDelayed(() -> splash.animate().alpha(0f).setDuration(400).setInterpolator(Ui.EASE)
                .withEndAction(() -> root.removeView(splash)).start(), wait);
    }

    // ---------- chrome / theme ----------
    void applyChrome() {
        root.setBackgroundColor(Ui.bg);
        root.setLayoutDirection(Str.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        brand.setTextColor(Ui.fg);
        sub.setTextColor(Ui.mute);
        pill.setBackground(Ui.shape(Ui.surface, 28, Ui.line, 1));
        ind.setBackground(Ui.shape(Ui.fg, 24, 0, 0));
        dots.setBackground(Ui.shape(Ui.surface, 28, Ui.line, 1));
        dotsIcon.setColorFilter(Ui.fg);
        menu.setBackground(Ui.shape(Ui.surface, 24, Ui.line, 1));
        banner.setBackground(Ui.shape(Ui.fg, 28, 0, 0));
        splash.setBackgroundColor(Ui.bg);
        splashText.setTextColor(Ui.fg);
        for (int i = 0; i < 4; i++) {
            navLabel[i].setText(Str.t(KEYS[i]));
            int c = i == navSel ? Ui.bg : Ui.mute;
            navLabel[i].setTextColor(c);
            navIcon[i].setColorFilter(c);
        }
        sub.setText(Str.t(KEYS[tab]));

        Window w = getWindow();
        w.setStatusBarColor(Ui.bg);
        w.setNavigationBarColor(Ui.bg);
        View dec = w.getDecorView();
        int f = dec.getSystemUiVisibility();
        if (!Ui.isDark) {
            f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            f &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) f &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        dec.setSystemUiVisibility(f);
    }

    void render() {
        Str.lang = Store.prefs.lang;
        Ui.applyTheme(draft != null ? draft : Store.prefs);
        applyChrome();
        for (Page p : pages) p.refresh();
        updateBanner();
        pager.setUserInputEnabled(openOpus == null);
        if (lastRtl != Str.rtl()) {
            lastRtl = Str.rtl();
            pager.post(() -> {
                pager.setCurrentItem(tab, false);
                layoutInd();
            });
        }
    }

    // ---------- navigation ----------
    void go(int i, boolean scroll) {
        closeMenu();
        boolean dirty = !mode.isEmpty() || openOpus != null || (draft != null && !draft.same(Store.prefs));
        boolean changed = i != tab;
        tab = i;
        mode = "";
        openOpus = null;
        draft = Store.prefs.copy();
        if (scroll && pager.getCurrentItem() != i) pager.setCurrentItem(i, true);
        setNavSelected(i);
        setMerged(i == 3);
        if (changed) updateSub();
        if (dirty) render();
        else updateBanner();
    }

    private void updateSub() {
        sub.animate().alpha(0f).setDuration(100).withEndAction(() -> {
            sub.setText(Str.t(KEYS[tab]));
            sub.animate().alpha(1f).setDuration(150).setInterpolator(Ui.EASE).start();
        }).start();
    }

    private void layoutInd() {
        int w = pill.getWidth();
        if (w <= 0) return;
        itemW = (w - dp(8)) / 4f;
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) ind.getLayoutParams();
        int ih = pill.getHeight() - dp(8);
        if (lp.width != (int) itemW || lp.height != ih) {
            lp.width = (int) itemW;
            lp.height = ih;
            ind.setLayoutParams(lp);
        }
        updateInd(pos);
    }

    private void updateInd(float p) {
        pos = p;
        ind.setTranslationX((Str.rtl() ? -1f : 1f) * p * itemW);
    }

    private void setNavSelected(int i) {
        if (navSel == i) return;
        int old = navSel;
        navSel = i;
        tintNav(old, Ui.mute);
        tintNav(i, Ui.bg);
    }

    private void tintNav(int idx, int to) {
        final ImageView ic = navIcon[idx];
        final TextView lb = navLabel[idx];
        ValueAnimator va = ValueAnimator.ofObject(new ArgbEvaluator(), lb.getCurrentTextColor(), to);
        va.setDuration(150);
        va.addUpdateListener(an -> {
            int c = (Integer) an.getAnimatedValue();
            ic.setColorFilter(c);
            lb.setTextColor(c);
        });
        va.start();
    }

    // In Settings the dots button dissolves into the pill: width, gap and opacity all go to zero.
    private void setMerged(final boolean m) {
        if (merged == m) return;
        merged = m;
        if (mergeAnim != null) mergeAnim.cancel();
        final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dots.getLayoutParams();
        final int w0 = lp.width, m0 = lp.getMarginStart();
        final int w1 = m ? 0 : dp(56), m1 = m ? 0 : dp(8);
        final float a0 = dots.getAlpha(), a1 = m ? 0f : 1f;
        if (!m) dots.setVisibility(View.VISIBLE);
        mergeAnim = ValueAnimator.ofFloat(0f, 1f);
        mergeAnim.setDuration(400);
        mergeAnim.setInterpolator(Ui.EASE);
        mergeAnim.addUpdateListener(an -> {
            float f = (Float) an.getAnimatedValue();
            lp.width = (int) (w0 + (w1 - w0) * f);
            lp.setMarginStart((int) (m0 + (m1 - m0) * f));
            dots.setLayoutParams(lp);
            dots.setAlpha(a0 + (a1 - a0) * f);
        });
        mergeAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator an) {
                if (merged) dots.setVisibility(View.GONE);
            }
        });
        mergeAnim.start();
    }

    // ---------- banner ----------
    void updateBanner() {
        boolean show = false;
        if (tab == 3 && draft != null && !draft.same(Store.prefs)) {
            banner.removeAllViews();
            TextView msg = Ui.text(this, Str.t("applyAsk"), 14, Ui.bg, false);
            banner.addView(msg, new LinearLayout.LayoutParams(0, -2, 1f));
            banner.addView(bannerBtn(Str.t("cancel"), () -> {
                draft = Store.prefs.copy();
                render();
            }));
            banner.addView(bannerBtn(Str.t("apply"), () -> {
                Store.prefs = draft.copy();
                Store.save();
                Notifier.schedule(this);
                draft = Store.prefs.copy();
                render();
            }));
            show = true;
        } else if (!mode.isEmpty()) {
            banner.removeAllViews();
            banner.addView(Ui.text(this, Str.t(mode.equals("delete") ? "tapDel" : "tapMove"), 14, Ui.bg, false),
                    new LinearLayout.LayoutParams(0, -2, 1f));
            banner.addView(bannerBtn(Str.t("ok"), () -> {
                mode = "";
                render();
            }));
            show = true;
        }
        if (show) {
            banner.setVisibility(View.VISIBLE);
            banner.animate().alpha(1f).translationY(0f).setDuration(250).setInterpolator(Ui.EASE).start();
        } else if (banner.getVisibility() == View.VISIBLE) {
            banner.animate().alpha(0f).translationY(dp(8)).setDuration(200).setInterpolator(Ui.EASE)
                    .withEndAction(() -> {
                        if (banner.getAlpha() == 0f) banner.setVisibility(View.INVISIBLE);
                    }).start();
        }
    }

    private TextView bannerBtn(String label, final Runnable r) {
        TextView t = Ui.text(this, label, 14, Ui.fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(16), dp(8), dp(16), dp(8));
        t.setBackground(Ui.shape(Ui.bg, 999, 0, 0));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMarginStart(dp(4));
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> r.run());
        Ui.press(t);
        return t;
    }

    // ---------- multifunction popup ----------
    private void addMenuItem(String label, final Runnable r) {
        TextView t = Ui.text(this, label, 16, Ui.fg, false);
        t.setPadding(dp(16), dp(14), dp(16), dp(14));
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(Ui.line), null, Ui.shape(Color.WHITE, 20, 0, 0)));
        t.setOnClickListener(v -> {
            closeMenu();
            r.run();
        });
        menu.addView(t, new LinearLayout.LayoutParams(-1, -2));
    }

    void toggleMenu() {
        if (menuOpen) {
            closeMenu();
            return;
        }
        if (tab == 3 || openOpus != null) return;
        menu.removeAllViews();
        if (tab == 0) {
            addMenuItem(Str.t("add"), () -> cardsPage.addForm());
        } else if (tab == 1) {
            addMenuItem(Str.t("write"), () -> projectsPage.writeForm());
            addMenuItem(Str.t("import"), this::pickOpus);
        } else {
            addMenuItem(Str.t("add"), () -> schedulePage.addForm());
        }
        addMenuItem(Str.t("delete"), () -> {
            mode = "delete";
            render();
        });
        addMenuItem(Str.t("rearrange"), () -> {
            mode = "move";
            render();
        });
        menuOpen = true;
        scrim.setVisibility(View.VISIBLE);
        menu.setVisibility(View.VISIBLE);
        menu.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        menu.setPivotX(Str.rtl() ? 0f : menu.getMeasuredWidth());
        menu.setPivotY(menu.getMeasuredHeight());
        menu.setScaleX(0.4f);
        menu.setScaleY(0.4f);
        menu.setAlpha(0f);
        menu.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).setInterpolator(Ui.EASE).start();
        dotsIcon.animate().rotation(90f).setDuration(300).setInterpolator(Ui.EASE).start();
    }

    void closeMenu() {
        if (!menuOpen) return;
        menuOpen = false;
        scrim.setVisibility(View.GONE);
        menu.animate().scaleX(0.4f).scaleY(0.4f).alpha(0f).setDuration(200).setInterpolator(Ui.EASE)
                .withEndAction(() -> {
                    if (!menuOpen) menu.setVisibility(View.INVISIBLE);
                }).start();
        dotsIcon.animate().rotation(0f).setDuration(200).setInterpolator(Ui.EASE).start();
    }

    // ---------- overlay: sheets ----------
    void showSheet(final View body) {
        layerToken++;
        layer.removeAllViews();
        layer.setVisibility(View.VISIBLE);
        dimView = new View(this);
        dimView.setBackgroundColor(Color.BLACK);
        dimView.setAlpha(0f);
        layer.addView(dimView, new FrameLayout.LayoutParams(-1, -1));
        dimView.animate().alpha(0.4f).setDuration(300).setInterpolator(Ui.EASE).start();

        final MaxHeightScroll sc = new MaxHeightScroll(this, (int) (root.getHeight() * 0.88f));
        sc.setBackground(Ui.topRound(Ui.bg, 24));
        sc.addView(body, new ViewGroup.LayoutParams(-1, -2));
        layer.addView(sc, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        sheetView = sc;
        sc.setAlpha(0f);
        sc.post(() -> {
            sc.setPivotX(sc.getWidth() / 2f);
            sc.setPivotY(sc.getHeight());
            sc.setTranslationY(dp(24));
            sc.setScaleX(0.95f);
            sc.setScaleY(0.95f);
            sc.animate().translationY(0f).scaleX(1f).scaleY(1f).alpha(1f).setDuration(300).setInterpolator(Ui.EASE).start();
        });
        layerBack = this::closeSheet;
    }

    void closeSheet() {
        final int tok = ++layerToken;
        layerBack = null;
        if (sheetView != null) {
            sheetView.animate().translationY(dp(24)).alpha(0f).setDuration(200).setInterpolator(Ui.EASE).start();
        }
        if (dimView != null) {
            dimView.animate().alpha(0f).setDuration(250).setInterpolator(Ui.EASE).withEndAction(() -> {
                if (tok == layerToken) {
                    layer.setVisibility(View.GONE);
                    layer.removeAllViews();
                }
            }).start();
        }
    }

    void showForm(List<View> rows, final BoolSupplier onSave) {
        LinearLayout col = Ui.vbox(this);
        col.setPadding(dp(24), dp(24), dp(24), dp(24));
        for (int i = 0; i < rows.size(); i++) col.addView(rows.get(i), Ui.mw(i == 0 ? 0 : 16));
        LinearLayout acts = Ui.hbox(this);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1f);
        l1.setMarginEnd(dp(8));
        acts.addView(Ui.btn(this, Str.t("cancel"), false, v -> closeSheet()), l1);
        acts.addView(Ui.btn(this, Str.t("save"), true, v -> {
            if (onSave.get()) {
                closeSheet();
                render();
            }
        }), new LinearLayout.LayoutParams(0, -2, 1f));
        col.addView(acts, Ui.mw(24));
        showSheet(col);
    }

    void confirm(final Runnable yes) {
        LinearLayout col = Ui.vbox(this);
        col.setPadding(dp(24), dp(24), dp(24), dp(24));
        col.addView(Ui.text(this, Str.t("confirmDel"), 18, Ui.fg, true), Ui.mw());
        LinearLayout acts = Ui.hbox(this);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1f);
        l1.setMarginEnd(dp(8));
        acts.addView(Ui.btn(this, Str.t("cancel"), false, v -> closeSheet()), l1);
        acts.addView(Ui.btn(this, Str.t("delete"), true, v -> {
            closeSheet();
            yes.run();
        }), new LinearLayout.LayoutParams(0, -2, 1f));
        col.addView(acts, Ui.mw(24));
        showSheet(col);
    }

    void chooseLanguage(final Prefs d) {
        LinearLayout col = Ui.vbox(this);
        col.setPadding(dp(24), dp(24), dp(24), dp(24));
        for (int i = 0; i < Str.NAMES.length; i++) {
            final int idx = i;
            col.addView(Ui.option(this, Str.NAMES[i], d.lang == i, v -> {
                d.lang = idx;
                closeSheet();
                render();
            }), Ui.mw(i == 0 ? 0 : 8));
        }
        showSheet(col);
    }

    // ---------- overlay: expanded card / preview grows out of the tapped card ----------
    void expandCard(final Card c, final View src, final boolean full) {
        final int tok = ++layerToken;
        layer.removeAllViews();
        layer.setVisibility(View.VISIBLE);
        sheetView = null;
        dimView = new View(this);
        dimView.setBackgroundColor(Color.BLACK);
        dimView.setAlpha(0f);
        layer.addView(dimView, new FrameLayout.LayoutParams(-1, -1));
        dimView.animate().alpha(0.4f).setDuration(300).setInterpolator(Ui.EASE).start();
        final View dimV = dimView;

        final LinearLayout shell = CardsPage.shell(this, c);
        LinearLayout in = (LinearLayout) shell.getChildAt(1);
        in.addView(CardsPage.head(this, c), Ui.mw());
        in.addView(Ui.text(this, CardsPage.fmtDue(c), 14, Ui.mute, false), Ui.mw(4));
        final LinearLayout extra = Ui.vbox(this);
        extra.setAlpha(0f);
        TextView note = Ui.text(this, c.note.isEmpty() ? "—" : c.note, 16, Ui.fg, false);
        if (!full) {
            note.setMaxLines(3);
            note.setEllipsize(TextUtils.TruncateAt.END);
        }
        extra.addView(note, Ui.mw(16));
        if (!c.files.isEmpty()) {
            if (full) {
                TextView lbl = Ui.text(this, Str.t("files").toUpperCase(), 12, Ui.mute, true);
                lbl.setLetterSpacing(0.06f);
                extra.addView(lbl, Ui.mw(16));
                for (final FileRef f : c.files) {
                    TextView link = Ui.text(this, f.name, 14, Ui.fg, false);
                    link.setPaintFlags(link.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                    link.setPadding(0, dp(6), 0, dp(6));
                    link.setOnClickListener(v -> openFile(f));
                    extra.addView(link, Ui.mw());
                }
            } else {
                extra.addView(Ui.text(this, Str.t("files") + ": " + c.files.size(), 14, Ui.mute, false), Ui.mw(8));
            }
        }
        in.addView(extra, Ui.mw());

        final int W = src.getWidth(), H = src.getHeight();
        int[] sl = new int[2], rl = new int[2];
        src.getLocationOnScreen(sl);
        root.getLocationOnScreen(rl);
        final int sx = sl[0] - rl[0], sy = sl[1] - rl[1];
        final int rootH = root.getHeight();
        shell.measure(View.MeasureSpec.makeMeasureSpec(W, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(rootH - dp(48), View.MeasureSpec.AT_MOST));
        final int th = Math.max(H, shell.getMeasuredHeight());
        final int ey = Math.max(dp(24), (rootH - th) / 2);

        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(W, H, Gravity.LEFT | Gravity.TOP);
        lp.leftMargin = sx;
        lp.topMargin = sy;
        layer.addView(shell, lp);
        src.setVisibility(View.INVISIBLE);

        final ValueAnimator open = ValueAnimator.ofFloat(0f, 1f);
        open.setDuration(300);
        open.setInterpolator(Ui.EASE);
        open.addUpdateListener(an -> {
            float f = (Float) an.getAnimatedValue();
            lp.topMargin = sy + (int) ((ey - sy) * f);
            lp.height = H + (int) ((th - H) * f);
            shell.setLayoutParams(lp);
            extra.setAlpha(Math.max(0f, (f - 0.2f) / 0.8f));
            shell.setElevation(dp(16) * f);
        });
        open.start();

        final boolean[] closed = {false};
        final Runnable close = () -> {
            if (closed[0]) return;
            closed[0] = true;
            open.cancel();
            int[] l2 = new int[2];
            src.getLocationOnScreen(l2);
            final int ty = l2[1] - rl[1];
            final int curTop = lp.topMargin, curH = lp.height;
            final float curA = extra.getAlpha();
            ValueAnimator back = ValueAnimator.ofFloat(0f, 1f);
            back.setDuration(300);
            back.setInterpolator(Ui.EASE);
            back.addUpdateListener(an -> {
                float f = (Float) an.getAnimatedValue();
                lp.topMargin = curTop + (int) ((ty - curTop) * f);
                lp.height = curH + (int) ((H - curH) * f);
                shell.setLayoutParams(lp);
                extra.setAlpha(curA * (1f - f));
                shell.setElevation(dp(16) * (1f - f));
            });
            back.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator an) {
                    src.setVisibility(View.VISIBLE);
                    if (tok == layerToken) {
                        layer.setVisibility(View.GONE);
                        layer.removeAllViews();
                        layerBack = null;
                    }
                }
            });
            back.start();
            dimV.animate().alpha(0f).setDuration(300).setInterpolator(Ui.EASE).start();
        };
        layerBack = close;
        shell.setOnClickListener(v -> close.run());
        dimV.setOnClickListener(v -> close.run());
    }

    private void openFile(FileRef f) {
        try {
            Uri u = Uri.parse(f.uri);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(u, getContentResolver().getType(u));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            toast(Str.t("cantRead"));
        }
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ---------- files ----------
    void pickFiles(List<FileRef> target, Runnable done) {
        attachTarget = target;
        attachDone = done;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(i, REQ_ATTACH);
    }

    void pickOpus() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "text/plain", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "text/html"});
        startActivityForResult(i, REQ_OPUS);
    }

    void pickFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_FOLDER);
    }

    // writes into the chosen storage folder, or asks where to save when none is set
    void exportFile(String name, String mime, byte[] data) {
        if (!Store.prefs.folderUri.isEmpty()) {
            try {
                Uri tree = Uri.parse(Store.prefs.folderUri);
                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
                Uri f = DocumentsContract.createDocument(getContentResolver(), docUri, mime, name);
                OutputStream os = getContentResolver().openOutputStream(f);
                os.write(data);
                os.close();
                toast(Str.t("saved") + ": " + name);
                return;
            } catch (Exception e) {
                // folder no longer usable: fall back to the system save dialog
            }
        }
        pendingBytes = data;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(i, REQ_CREATE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;
        try {
            if (req == REQ_ATTACH && attachTarget != null) {
                List<Uri> us = new ArrayList<>();
                ClipData cd = data.getClipData();
                if (cd != null) {
                    for (int i = 0; i < cd.getItemCount(); i++) us.add(cd.getItemAt(i).getUri());
                } else if (data.getData() != null) {
                    us.add(data.getData());
                }
                for (Uri u : us) {
                    try {
                        getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ignored) {
                    }
                    attachTarget.add(new FileRef(Files.queryName(this, u), u.toString()));
                }
                if (attachDone != null) attachDone.run();
            } else if (req == REQ_OPUS && data.getData() != null) {
                projectsPage.importUri(data.getData());
            } else if (req == REQ_FOLDER && data.getData() != null) {
                Uri u = data.getData();
                getContentResolver().takePersistableUriPermission(u,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                String seg = u.getLastPathSegment();
                String name = seg == null ? "" : seg.substring(Math.max(seg.lastIndexOf('/'), seg.lastIndexOf(':')) + 1);
                if (name.isEmpty()) name = seg == null ? "" : seg;
                draft.folderUri = u.toString();
                draft.folder = name;
                render();
            } else if (req == REQ_CREATE && data.getData() != null && pendingBytes != null) {
                OutputStream os = getContentResolver().openOutputStream(data.getData());
                os.write(pendingBytes);
                os.close();
                pendingBytes = null;
                toast(Str.t("saved"));
            }
        } catch (Exception e) {
            toast(Str.t("cantRead"));
        }
    }

    // ---------- notifications permission ----------
    boolean askedNotif = false;

    void ensureNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askedNotif = true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_PERM);
        }
    }

    boolean notificationsEnabled() {
        return ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled();
    }

    // first tap asks the system; after a denial the system dialog no longer shows, so open the app's notification settings
    void requestNotifications() {
        boolean notGranted = Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
        if (notGranted && !askedNotif) {
            ensureNotifPermission();
            return;
        }
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(i);
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        render();
    }

    // ---------- back ----------
    @Override
    public void onBackPressed() {
        if (layerBack != null) {
            layerBack.run();
        } else if (menuOpen) {
            closeMenu();
        } else if (openOpus != null) {
            openOpus = null;
            render();
        } else if (!mode.isEmpty()) {
            mode = "";
            render();
        } else if (tab != 0) {
            go(0, true);
        } else {
            super.onBackPressed();
        }
    }
}
