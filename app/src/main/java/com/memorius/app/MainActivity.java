package com.memorius.app;

import android.Manifest;
import android.animation.AnimatorListenerAdapter;
import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.RingtoneManager;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import androidx.dynamicanimation.animation.SpringAnimation;
import android.graphics.drawable.GradientDrawable;
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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    static final int REQ_OPUS = 1, REQ_ATTACH = 2, REQ_FOLDER = 3, REQ_CREATE = 4, REQ_PERM = 10, REQ_SOUND_GEN = 11, REQ_SOUND_DL = 12;
    static final int DOCK_H = 64; // dp
    static final String[] KEYS = {"cards", "progress", "schedule", "settings"};
    static final int[] NAV_KIND = {IconView.CARDS, IconView.PROJECTS, IconView.SCHEDULE, IconView.SETTINGS};

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
    FrameLayout root, layer, splash;
    LiquidBar bar;
    GlassFrame dots;
    LinearLayout main, header, dock, banner, menu;
    ViewPager2 pager;
    TextView brand, splashText;
    FrameLayout backBtn;
    IconView backIcon;
    View scrim;
    BackgroundView texBg;
    IconView dotsIcon;
    boolean navLock = false, springBusy = false, pagerWait = false, glass = false, systemAnimOff = false;
    float lastPagePos = 0f;
    String titleShown = "";
    java.util.function.Consumer<Boolean> blurListener;
    final Page[] pages = new Page[4];
    CardsPage cardsPage;
    ProjectsPage projectsPage;
    SchedulePage schedulePage;
    SettingsPage settingsPage;

    boolean menuOpen = false, merged = false, lastRtl = false;
    int navSel = 0;
    int layerToken = 0;
    Runnable layerBack = null;
    View sheetView = null, dimView = null;
    ValueAnimator mergeAnim;
    View fade, backView;
    int imeBottom = 0;
    BackHandler back;
    FrameLayout.LayoutParams dockLp, bannerLp, menuLp;

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
    protected void attachBaseContext(Context base) {
        Store.ensure(base);
        Configuration c = new Configuration(base.getResources().getConfiguration());
        c.fontScale = Math.min(2f, c.fontScale * Store.prefs.fontPct / 100f);
        super.attachBaseContext(base.createConfigurationContext(c));
    }

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
        back = new BackHandler(this);
        buildUi();
        render();
        if (Build.VERSION.SDK_INT >= 31) {
            blurListener = enabled -> render();
            getWindowManager().addCrossWindowBlurEnabledListener(getMainExecutor(), blurListener);
        }
        Notifier.ensureChannel(this);
        Notifier.schedule(this);
        if (!"off".equals(Store.prefs.freq)) ensureNotifPermission();
        hideSplash();
    }

    @Override
    protected void onResume() {
        super.onResume();
        systemAnimOff = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        Motion.off = systemAnimOff || Store.prefs.reduceMotion;
        if (pager != null && floatCard == null) render();
    }

    @Override
    protected void onDestroy() {
        if (blurListener != null) getWindowManager().removeCrossWindowBlurEnabledListener(blurListener);
        super.onDestroy();
    }

    private boolean crossBlurOk() {
        if (Build.VERSION.SDK_INT < 31) return false;
        try {
            return getWindowManager().isCrossWindowBlurEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------- UI construction ----------
    private void buildUi() {
        // edge to edge: content draws behind transparent system bars, insets move the controls instead
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }

        root = new FrameLayout(this);
        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (rv, in) -> {
            Insets bars = in.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = in.getInsets(WindowInsetsCompat.Type.ime());
            int newIme = ime.bottom > bars.bottom ? ime.bottom : 0;
            boolean changed = bars.top != Ui.insetTop || bars.bottom != Ui.insetBottom;
            Ui.insetTop = bars.top;
            Ui.insetBottom = bars.bottom;
            if (newIme != imeBottom) {
                imeBottom = newIme;
                layer.setPadding(0, 0, 0, imeBottom);
            }
            if (changed) applyInsets();
            return in;
        });

        texBg = new BackgroundView(this);
        texBg.setVisibility(View.GONE);
        root.addView(texBg, new FrameLayout.LayoutParams(-1, -1));

        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));

        // header: the current page title (with a back chevron inside a settings category)
        header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPaddingRelative(dp(20), dp(24) + Ui.insetTop, dp(20), dp(16));
        backIcon = new IconView(this, IconView.BACK, Ui.fg);
        backBtn = new FrameLayout(this);
        backBtn.addView(backIcon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        backBtn.setVisibility(View.GONE);
        backBtn.setOnClickListener(v -> {
            Ui.haptic(v, 0);
            settingsPage.up();
        });
        header.addView(backBtn, new LinearLayout.LayoutParams(dp(48), dp(48)));
        brand = Ui.text(this, "", 28, Ui.fg, true);
        brand.setLetterSpacing(-0.03f);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));
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
                float p = position + offset;
                if (!navLock) bar.setPosition(p);
                bar.refreshBackdrop();
                // blur in motion: pages blur a little while they move, sharp again when still
                Blur.motion(pager, Math.min(dp(5), Math.abs(p - lastPagePos) * dp(70)));
                lastPagePos = p;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    Blur.motion(pager, 0f);
                    pagerWait = false;
                    if (!springBusy && navLock) {
                        navLock = false;
                        bar.setPosition(pager.getCurrentItem());
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (position != tab) go(position, false);
            }
        });
        main.addView(pager, new LinearLayout.LayoutParams(-1, 0, 1f));

        // soft fade so scrolling content settles behind the dock and the system bar
        fade = new View(this);
        root.addView(fade, new FrameLayout.LayoutParams(-1, dp(170) + Ui.insetBottom, Gravity.BOTTOM));

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
        bannerLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        bannerLp.setMargins(dp(16), 0, dp(16), dp(16 + DOCK_H + 16) + Ui.insetBottom);
        root.addView(banner, bannerLp);

        // dock: liquid glass tab bar (drag the glass thumb) + multifunction button
        root.setClipChildren(false);
        dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER_VERTICAL);
        dock.setClipChildren(false);
        dockLp = new FrameLayout.LayoutParams(-1, dp(DOCK_H), Gravity.BOTTOM);
        dockLp.setMargins(dp(16), 0, dp(16), dp(16) + Ui.insetBottom);
        root.addView(dock, dockLp);

        bar = new LiquidBar(this, NAV_KIND);
        bar.setGlassSource(root, texBg, main);
        bar.setListener(new LiquidBar.Listener() {
            @Override
            public void onPress() { // the thumb is held: the pager waits for the finger
                closeMenu();
                navLock = true;
                springBusy = false;
            }

            @Override
            public void onTick() {
                Ui.haptic(bar, 0);
            }

            @Override
            public void onSelect(int idx) {
                navTo(idx);
            }
        });
        dock.addView(bar, new LinearLayout.LayoutParams(0, -1, 1f));

        dots = new GlassFrame(this);
        dots.setGlassSource(root, texBg, main);
        dotsIcon = new IconView(this, IconView.MORE, Ui.fg);
        dots.addView(dotsIcon, new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
        dots.setClickable(true);
        dots.setOnClickListener(v -> {
            Ui.haptic(v, 0);
            toggleMenu();
        });
        // liquid button: swells when pressed, follows the finger a little, stretches the way it is pulled
        final Interactive dotsInter = new Interactive(dots);
        dots.setInteractive(dotsInter);
        dots.setGlassLook(2f, 12f, 24f); // thin blur, strong lens: like a glass bead
        dots.setOnTouchListener((v, e) -> {
            dotsInter.handle(e);
            return false;
        });
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(dp(DOCK_H), dp(DOCK_H));
        dlp.setMarginStart(dp(8));
        dock.addView(dots, dlp);
        // the glass shows the page behind it: redraw it whenever that page moves
        root.getViewTreeObserver().addOnScrollChangedListener(() -> {
            bar.refreshBackdrop();
            dots.refreshBackdrop();
        });

        // popup menu: grows out of the dots button, same surface
        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(4), dp(4), dp(4), dp(4));
        menu.setMinimumWidth(dp(200));
        menu.setVisibility(View.INVISIBLE);
        menu.setAlpha(0f);
        menuLp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        menuLp.setMarginEnd(dp(16));
        menuLp.bottomMargin = dp(16 + DOCK_H + 8) + Ui.insetBottom;
        root.addView(menu, menuLp);

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

    // system bar sizes changed: move the controls, give scrolling pages room under the dock
    private void applyInsets() {
        header.setPaddingRelative(dp(20), dp(24) + Ui.insetTop, dp(20), dp(16));
        dockLp.bottomMargin = dp(16) + Ui.insetBottom;
        dock.setLayoutParams(dockLp);
        bannerLp.bottomMargin = dp(16 + DOCK_H + 16) + Ui.insetBottom;
        banner.setLayoutParams(bannerLp);
        menuLp.bottomMargin = dp(16 + DOCK_H + 8) + Ui.insetBottom;
        menu.setLayoutParams(menuLp);
        ViewGroup.LayoutParams fl = fade.getLayoutParams();
        fl.height = dp(170) + Ui.insetBottom;
        fade.setLayoutParams(fl);
        render();
    }

    int sheetInset() {
        return imeBottom > 0 ? 0 : Ui.insetBottom;
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
        Prefs cur = draft != null ? draft : Store.prefs;
        // window blur: a see-through window that Android blurs behind (Android 12+), else a plain solid page
        glass = cur.windowBlur && crossBlurOk();
        root.setBackground(Ui.pageBg(glass ? 0xD8 : 0xFF));
        getWindow().setBackgroundDrawable(new ColorDrawable(glass ? Color.TRANSPARENT : Ui.bg));
        if (Build.VERSION.SDK_INT >= 31) getWindow().setBackgroundBlurRadius(glass ? dp(28) : 0);
        root.setForeground(Ui.warmthOn ? new ColorDrawable(0x2EFF9A3C) : null);
        WindowManager.LayoutParams wl = getWindow().getAttributes();
        float rate = cur.battery ? 30f : 0f; // battery saver: ask for 30 frames per second
        if (wl.preferredRefreshRate != rate) {
            wl.preferredRefreshRate = rate;
            getWindow().setAttributes(wl);
        }
        root.setLayoutDirection(Str.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        int surf = Ui.alpha(Ui.surface, glass ? 0xCC : 0xFF);
        brand.setTextColor(Ui.fg);
        backIcon.setColor(Ui.fg);
        backBtn.setContentDescription(Str.t("back"));
        boolean fxOn = cur.windowBlur && !cur.battery; // glass effects: off in battery saver
        bar.setDark(Ui.isDark);
        dots.setDark(Ui.isDark);
        bar.setGlass(fxOn);
        dots.setGlass(fxOn);
        bar.setIconColor(Ui.fg);
        dotsIcon.setColor(Ui.fg);
        dots.setContentDescription(Str.t("more"));
        texBg.setVisibility(Ui.anyTex() ? View.VISIBLE : View.GONE);
        texBg.recolor();
        menu.setBackground(Ui.shape(surf, 28, Ui.line, 1));
        banner.setBackground(Ui.shape(Ui.fg, 28, 0, 0));
        splash.setBackgroundColor(Ui.bg);
        splashText.setTextColor(Ui.fg);
        fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{ColorUtils.setAlphaComponent(Ui.bg, 0), ColorUtils.setAlphaComponent(Ui.bg, glass ? 0xA0 : 0xE6)}));
        String[] labels = new String[4];
        for (int i = 0; i < 4; i++) labels[i] = Str.t(KEYS[i]);
        bar.setLabels(labels);
        bar.setSelectedIndex(navSel);
        refreshHeader(false, 0);

        // bars stay transparent; only the icon colour follows the theme
        WindowInsetsControllerCompat wc = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        wc.setAppearanceLightStatusBars(!Ui.isDark);
        wc.setAppearanceLightNavigationBars(!Ui.isDark);
    }

    // page title in the header; slides + blurs in when it changes
    void refreshHeader(boolean animate, int dir) {
        boolean inSub = tab == 3 && settingsPage != null && settingsPage.inSub();
        final String t = inSub ? settingsPage.catTitle() : Str.t(KEYS[tab]);
        int vis = inSub ? View.VISIBLE : View.GONE;
        if (backBtn.getVisibility() != vis) backBtn.setVisibility(vis);
        if (t.equals(titleShown)) return;
        titleShown = t;
        brand.animate().cancel();
        if (!animate) {
            brand.setText(t);
            brand.setAlpha(1f);
            brand.setTranslationX(0f);
            Blur.view(brand, 0f);
            return;
        }
        final int d = Str.rtl() ? -dir : dir;
        brand.animate().alpha(0f).translationX(-d * dp(14)).setDuration(Motion.dur(120)).setInterpolator(Ui.EASE)
                .withEndAction(() -> {
                    brand.setText(t);
                    brand.setTranslationX(d * dp(14));
                    brand.animate().translationX(0f).setDuration(Motion.dur(230)).setInterpolator(Ui.EASE).start();
                    Ui.blurIn(brand, 230, 6);
                }).start();
    }

    void render() {
        Str.lang = Store.prefs.lang;
        Prefs cur = draft != null ? draft : Store.prefs;
        Ui.applyTheme(cur);
        Motion.off = systemAnimOff || cur.reduceMotion;
        applyChrome();
        for (Page p : pages) p.refresh();
        updateBanner();
        pager.setUserInputEnabled(openOpus == null);
        if (lastRtl != Str.rtl()) {
            lastRtl = Str.rtl();
            pager.post(() -> {
                pager.setCurrentItem(tab, false);
                bar.setPosition(tab);
            });
        }
        syncBack();
    }

    // ---------- navigation ----------
    void go(int i, boolean scroll) {
        closeMenu();
        boolean dirty = !mode.isEmpty() || openOpus != null || (draft != null && !draft.same(Store.prefs));
        boolean changed = i != tab;
        int old = tab;
        tab = i;
        mode = "";
        openOpus = null;
        draft = Store.prefs.copy();
        if (old == 3 && changed) settingsPage.reset();
        if (scroll && pager.getCurrentItem() != i) pager.setCurrentItem(i, true);
        setNavSelected(i);
        setMerged(i == 3);
        if (changed) refreshHeader(true, i > old ? 1 : -1);
        if (dirty) render();
        else updateBanner();
        syncBack();
    }

    // the glass thumb springs to the destination while the page slides (350 ms scene change)
    void navTo(final int idx) {
        closeMenu();
        navLock = true;
        springBusy = true;
        pagerWait = pager.getCurrentItem() != idx;
        bar.animateTo(idx, () -> {
            springBusy = false;
            if (!pagerWait) {
                navLock = false;
                bar.setPosition(idx);
            }
        });
        go(idx, true);
    }

    private void setNavSelected(int i) {
        navSel = i;
        bar.setSelectedIndex(i);
    }

    // In Settings the dots button dissolves into the pill: width, gap and opacity all go to zero.
    private void setMerged(final boolean m) {
        if (merged == m) return;
        merged = m;
        if (mergeAnim != null) mergeAnim.cancel();
        final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dots.getLayoutParams();
        final int w0 = lp.width, m0 = lp.getMarginStart();
        final int w1 = m ? 0 : dp(DOCK_H), m1 = m ? 0 : dp(8);
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
                boolean sizeChanged = draft.fontPct != Store.prefs.fontPct;
                boolean soundChanged = !draft.soundGeneral.equals(Store.prefs.soundGeneral)
                        || !draft.soundDeadline.equals(Store.prefs.soundDeadline);
                if (soundChanged) draft.soundRev = Store.prefs.soundRev + 1; // new channel ids: Android cannot edit a channel's sound
                Store.prefs = draft.copy();
                Store.save();
                if (soundChanged) Notifier.rebuildChannels(this);
                Notifier.schedule(this);
                draft = Store.prefs.copy();
                Ui.haptic(banner, 2);
                if (sizeChanged) recreate();
                else render();
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
            if (banner.getVisibility() != View.VISIBLE || banner.getAlpha() < 0.5f) {
                banner.setVisibility(View.VISIBLE);
                banner.setTranslationY(dp(12));
                banner.animate().translationY(0f).setDuration(Motion.dur(Motion.T_MODAL)).setInterpolator(Ui.EASE).start();
                Ui.blurIn(banner, Motion.T_MODAL, 8);
            }
        } else if (banner.getVisibility() == View.VISIBLE) {
            banner.animate().alpha(0f).translationY(dp(8)).setDuration(Motion.dur(200)).setInterpolator(Ui.EASE)
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
        t.setOnClickListener(v -> {
            Ui.haptic(v, 0);
            r.run();
        });
        Ui.press(t);
        return t;
    }

    // ---------- multifunction popup ----------
    private void addMenuItem(String label, final Runnable r) {
        TextView t = Ui.text(this, label, 16, Ui.fg, false);
        t.setPadding(dp(16), dp(14), dp(16), dp(14));
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(Ui.line), null, Ui.shape(Color.WHITE, 20, 0, 0)));
        t.setOnClickListener(v -> {
            Ui.haptic(v, 0);
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
        Motion.scale(menu, 1f, 500f, 0.62f); // elastic: grows out of the button with a little overshoot
        Motion.effect(menu, DynamicAnimation.ALPHA, 1f);
        for (int i = 0; i < menu.getChildCount(); i++) {
            final View ch = menu.getChildAt(i);
            ch.setAlpha(0f);
            ch.postDelayed(() -> Ui.blurIn(ch, Motion.T_MODAL, 6), 40L * i);
        }
        Motion.spatial(dotsIcon, DynamicAnimation.ROTATION, 90f);
        syncBack();
    }

    void closeMenu() {
        if (!menuOpen) return;
        menuOpen = false;
        scrim.setVisibility(View.GONE);
        Motion.scale(menu, 0.4f, 1200f, 1f);
        Motion.effect(menu, DynamicAnimation.ALPHA, 0f);
        Motion.spatial(dotsIcon, DynamicAnimation.ROTATION, 0f);
        handler.postDelayed(() -> {
            if (!menuOpen) menu.setVisibility(View.INVISIBLE);
        }, 220);
        syncBack();
    }

    // ---------- overlay: sheets ----------
    // slide up from the bottom edge, ~250 ms, fading and sharpening in
    void showSheet(final View body) {
        layerToken++;
        layer.removeAllViews();
        layer.setVisibility(View.VISIBLE);
        dimView = new View(this);
        dimView.setBackgroundColor(Color.BLACK);
        dimView.setAlpha(0f);
        layer.addView(dimView, new FrameLayout.LayoutParams(-1, -1));
        dimView.animate().alpha(0.4f).setDuration(Motion.dur(Motion.T_MODAL)).setInterpolator(Ui.EASE).start();

        final MaxHeightScroll sc = new MaxHeightScroll(this, (int) (root.getHeight() * 0.88f));
        sc.setBackground(Ui.topRound(Ui.alpha(Ui.bg, glass ? 0xEE : 0xFF), 28));
        sc.addView(body, new ViewGroup.LayoutParams(-1, -2));
        layer.addView(sc, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        sheetView = sc;
        backView = sc;
        sc.setAlpha(0f);
        sc.post(() -> {
            sc.setTranslationY(sc.getHeight() * 0.35f);
            sc.animate().translationY(0f).setDuration(Motion.dur(Motion.T_MODAL)).setInterpolator(Ui.EASE).start();
            Ui.blurIn(sc, Motion.T_MODAL, 10);
        });
        layerBack = this::closeSheet;
        syncBack();
    }

    void closeSheet() {
        final int tok = ++layerToken;
        layerBack = null;
        if (sheetView != null) {
            sheetView.animate().translationY(sheetView.getHeight() * 0.35f).alpha(0f).setDuration(Motion.dur(200)).setInterpolator(Ui.EASE).start();
        }
        if (dimView != null) dimView.animate().alpha(0f).setDuration(Motion.dur(200)).setInterpolator(Ui.EASE).start();
        handler.postDelayed(() -> {
            if (tok == layerToken) {
                layer.setVisibility(View.GONE);
                layer.removeAllViews();
                backView = null;
            }
        }, Motion.dur(220));
        syncBack();
    }

    void showForm(List<View> rows, final BoolSupplier onSave) {
        LinearLayout col = Ui.vbox(this);
        col.setPadding(dp(24), dp(24), dp(24), dp(24) + sheetInset());
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
        col.setPadding(dp(24), dp(24), dp(24), dp(24) + sheetInset());
        col.addView(Ui.text(this, Str.t("confirmDel"), 18, Ui.fg, true), Ui.mw());
        LinearLayout acts = Ui.hbox(this);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1f);
        l1.setMarginEnd(dp(8));
        acts.addView(Ui.btn(this, Str.t("cancel"), false, v -> closeSheet()), l1);
        acts.addView(Ui.btn(this, Str.t("delete"), true, v -> {
            Ui.haptic(v, 2);
            closeSheet();
            yes.run();
        }), new LinearLayout.LayoutParams(0, -2, 1f));
        col.addView(acts, Ui.mw(24));
        showSheet(col);
    }

    void chooseLanguage(final Prefs d) {
        LinearLayout col = Ui.vbox(this);
        col.setPadding(dp(24), dp(24), dp(24), dp(24) + sheetInset());
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

    // ---------- cards: Standby (list) -> Preview (hold) / Fullview (tap) ----------
    private CardFloat floatCard;

    // FULLVIEW: opened straight from Standby, never through Preview
    void openCard(Card c, View src) {
        if (floatCard != null) return;
        floatCard = new CardFloat(c, src, true);
        floatCard.open();
    }

    // PREVIEW: held for 0.5 s; stays until the finger lifts, never turns into Fullview
    void peekStart(Card c, View src, float rawY) {
        if (floatCard != null) floatCard.finishNow();
        floatCard = new CardFloat(c, src, false);
        floatCard.setLower(rawY > root.getHeight() / 2f);
        floatCard.open();
    }

    void peekDrag(float rawY) {
        if (floatCard != null && !floatCard.full) floatCard.drag(rawY);
    }

    void peekEnd() {
        if (floatCard != null && !floatCard.full) floatCard.close();
    }

    void chooseFile(final List<FileRef> files) {
        String[] names = new String[files.size()];
        for (int i = 0; i < names.length; i++) names[i] = files.get(i).name;
        new android.app.AlertDialog.Builder(this).setItems(names, (d, i) -> openFile(files.get(i))).show();
    }

    private float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // One card view for all three states. A single progress value "o" (0 = sitting on the Standby card,
    // 1 = fully open) drives size, position, corner radius, fades, blur and shadow, so opening, closing,
    // changing your mind mid-way and the predictive back gesture all share the same continuous motion.
    // Title and tag are the same views the whole time (shared element); only the rest cross-fades.
    private class CardFloat {
        final MainActivity a = MainActivity.this;
        final Card c;
        final View src;
        final boolean full;
        final LinearLayout shell;
        final CardDrawable bgd;
        final View dimV;
        final FrameLayout.LayoutParams lp;
        final List<View> extras = new ArrayList<>();
        TextView descShort, descFull, leftTv, dueTv;
        final int W, H, ex, tw, th, upY, lowY, fullY, rootH, tok;
        final int[] rl = new int[2];
        int sx, sy;
        float o = 0f, snap = 0f, lastSnap = 0f, mb = 0f;
        boolean lower = false, closing, dead;
        ValueAnimator anim;
        SpringAnimation snapAnim;
        Runnable afterClose;

        CardFloat(Card card, View source, boolean fullView) {
            c = card;
            src = source;
            full = fullView;
            Motion.reset(src);
            tok = ++layerToken;
            layer.removeAllViews();
            layer.setVisibility(View.VISIBLE);
            sheetView = null;
            dimV = new View(a);
            dimV.setBackgroundColor(Color.BLACK);
            dimV.setAlpha(0f);
            layer.addView(dimV, new FrameLayout.LayoutParams(-1, -1));

            shell = CardsPage.shell(a, c);
            bgd = (CardDrawable) shell.getBackground();
            LinearLayout in = (LinearLayout) shell.getChildAt(1);
            in.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1f));
            in.addView(CardsPage.head(a, c), Ui.mw());

            ScrollView sc = Page.scroller(a);
            LinearLayout body = Ui.vbox(a);
            if (!c.note.isEmpty()) {
                FrameLayout db = new FrameLayout(a);
                descFull = Ui.text(a, c.note, 16, Ui.fg, false);
                descShort = Ui.text(a, c.note, 14, Ui.mute, false);
                descShort.setMaxLines(2);
                descShort.setEllipsize(TextUtils.TruncateAt.END);
                db.addView(descFull, new FrameLayout.LayoutParams(-1, -2));
                db.addView(descShort, new FrameLayout.LayoutParams(-1, -2));
                body.addView(db, Ui.mw(4));
            }
            FrameLayout tb = new FrameLayout(a);
            dueTv = Ui.text(a, CardsPage.fmtDue(c), 13, Ui.fg, true);
            leftTv = Ui.text(a, CardsPage.timeLeft(c), 13, c.done ? Ui.mute : Ui.fg, true);
            tb.addView(dueTv, new FrameLayout.LayoutParams(-2, -2));
            tb.addView(leftTv, new FrameLayout.LayoutParams(-2, -2));
            body.addView(tb, Ui.mw(8));
            if (full) {
                TextView lf = Ui.text(a, CardsPage.timeLeft(c), 13, Ui.mute, false);
                body.addView(lf, Ui.mw(2));
                extras.add(lf);
            }
            if (!c.files.isEmpty()) {
                TextView lbl = Ui.text(a, Str.t("files").toUpperCase(), 12, Ui.mute, true);
                lbl.setLetterSpacing(0.06f);
                body.addView(lbl, Ui.mw(16));
                extras.add(lbl);
                for (final FileRef f : c.files) {
                    TextView t = Ui.text(a, f.name, 14, Ui.fg, false);
                    t.setGravity(Gravity.CENTER_VERTICAL);
                    if (full) { // attachments stay reachable: underlined links with 48 dp targets
                        t.setPaintFlags(t.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
                        t.setMinHeight(dp(48));
                        t.setOnClickListener(v -> openFile(f));
                    }
                    body.addView(t, Ui.mw(full ? 0 : 4));
                    extras.add(t);
                }
            }
            sc.addView(body, new ScrollView.LayoutParams(-1, -2));
            in.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1f));

            if (full) { // interactive controls exist only here
                LinearLayout row = Ui.hbox(a);
                TextView edit = Ui.btn(a, Str.t("edit"), true, v -> {
                    Ui.haptic(v, 0);
                    closeThen(() -> cardsPage.cardForm(c));
                });
                TextView open = Ui.btn(a, Str.t("openFile"), false, v -> {
                    Ui.haptic(v, 0);
                    if (c.files.size() == 1) openFile(c.files.get(0));
                    else chooseFile(c.files);
                });
                open.setEnabled(!c.files.isEmpty());
                open.setAlpha(c.files.isEmpty() ? 0.4f : 1f);
                TextView fin = Ui.btn(a, Str.t(c.done ? "reopen" : "finish"), false, v -> {
                    Ui.haptic(v, 2);
                    c.done = !c.done;
                    Store.save();
                    closeThen(() -> render());
                });
                for (TextView t : new TextView[]{edit, open, fin}) {
                    t.setTextSize(14);
                    t.setPadding(dp(8), dp(12), dp(8), dp(12));
                    t.setSingleLine(true);
                    t.setEllipsize(TextUtils.TruncateAt.END);
                }
                LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, -2, 1f);
                l1.setMarginEnd(dp(6));
                LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, -2, 1f);
                l2.setMarginEnd(dp(6));
                row.addView(edit, l1);
                row.addView(open, l2);
                row.addView(fin, new LinearLayout.LayoutParams(0, -2, 1f));
                in.addView(row, Ui.mw(12));
                extras.add(row);
            }
            for (View x : extras) x.setAlpha(0f);
            if (descFull != null) descFull.setAlpha(0f);
            dueTv.setAlpha(0f);

            // where the card sits now, and where it goes
            W = src.getWidth();
            H = src.getHeight();
            int[] sl = new int[2];
            src.getLocationOnScreen(sl);
            root.getLocationOnScreen(rl);
            sx = sl[0] - rl[0];
            sy = sl[1] - rl[1];
            rootH = root.getHeight();
            int rootW = root.getWidth();
            int availH = rootH - Ui.insetTop - Ui.insetBottom;
            ex = dp(16);
            tw = rootW - dp(32);
            if (full) {
                th = Math.round(availH * 0.8f);
                fullY = Ui.insetTop + (availH - th) / 2;
                upY = fullY;
                lowY = fullY;
            } else {
                th = Math.round(availH * 0.46f); // fits inside either half of the screen
                upY = Ui.insetTop + dp(20);
                lowY = rootH - Ui.insetBottom - dp(20) - th;
                fullY = upY;
            }

            lp = new FrameLayout.LayoutParams(W, H, Gravity.LEFT | Gravity.TOP);
            lp.leftMargin = sx;
            lp.topMargin = sy;
            layer.addView(shell, lp);
            src.setVisibility(View.INVISIBLE);
            if (full) {
                backView = shell;
                dimV.setOnClickListener(v -> close()); // tap outside the card collapses it
            }
            // screen readers: only the open card
            main.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            dock.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            setO(0f);
        }

        void setLower(boolean l) {
            lower = l;
            snap = l ? 1f : 0f;
            lastSnap = snap;
        }

        private void fade(View v, float a) {
            if (v != null) v.setAlpha(a);
        }

        void setO(float v) {
            float prev = o;
            o = v;
            float ey = full ? fullY : upY + (lowY - upY) * snap;
            lp.leftMargin = sx + Math.round((ex - sx) * o);
            lp.topMargin = sy + Math.round((ey - sy) * o);
            lp.width = W + Math.round((tw - W) * o);
            lp.height = H + Math.round((th - H) * o);
            shell.setLayoutParams(lp);
            shell.setElevation(dp(12) * o);
            float r1 = full ? CardDrawable.FULL_DP : CardDrawable.PREVIEW_DP;
            bgd.setRadiusDp(CardDrawable.STANDBY_DP + (r1 - CardDrawable.STANDBY_DP) * o);
            float swap = clamp01(o / 0.25f);
            fade(descShort, 1f - swap);
            fade(descFull, swap);
            fade(leftTv, 1f - swap);
            fade(dueTv, swap);
            float ef = clamp01((o - 0.35f) / 0.65f);
            for (View x : extras) x.setAlpha(ef);
            dimV.setAlpha(o * (Blur.supported() ? 0.35f : 0.6f));
            float rb = o * dp(18);
            Blur.view(main, rb);
            Blur.view(dock, rb);
            // blur in motion: soft while it moves (open, close, snap), sharp when still
            float vel = Math.abs(o - prev) + Math.abs(snap - lastSnap) * 0.7f;
            lastSnap = snap;
            mb = mb * 0.5f + Math.min(dp(7), vel * dp(110)) * 0.5f;
            Blur.motion(shell, mb);
        }

        void animateTo(final float target, long baseMs, final Runnable end) {
            if (anim != null) anim.cancel();
            final float from = o;
            if (from == target) {
                if (end != null) end.run();
                return;
            }
            anim = ValueAnimator.ofFloat(from, target);
            anim.setDuration(Motion.dur((long) (baseMs * Math.max(0.35f, Math.abs(target - from)))));
            anim.setInterpolator(Ui.EASE); // cubic-bezier(0.4, 0, 0.2, 1)
            anim.addUpdateListener(an -> setO((Float) an.getAnimatedValue()));
            anim.addListener(new AnimatorListenerAdapter() {
                boolean cancelled;

                @Override
                public void onAnimationCancel(Animator an) {
                    cancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator an) {
                    if (cancelled) return;
                    setO(target);
                    mb = 0f;
                    Blur.motion(shell, 0f);
                    if (end != null) end.run();
                }
            });
            anim.start();
        }

        void open() {
            if (full) layerBack = () -> close();
            animateTo(1f, full ? Motion.T_SCENE : Motion.T_MODAL, null);
            syncBack();
        }

        // Preview has exactly two places: upper half and lower half. The finger only chooses between them.
        void drag(float rawY) {
            float mid = rootH / 2f, band = dp(28); // small dead zone so it cannot flicker at the middle
            boolean want = lower;
            if (rawY > mid + band) want = true;
            else if (rawY < mid - band) want = false;
            if (want == lower) return;
            lower = want;
            Ui.haptic(shell, 0);
            if (snapAnim != null) snapAnim.cancel();
            snapAnim = Motion.progress(snap, lower ? 1f : 0f, 380f, 0.78f, f -> {
                snap = f;
                setO(o);
            }, null);
        }

        void backDrag(float p) { // predictive back: the card collapses along with the gesture
            if (closing || dead) return;
            if (anim != null) anim.cancel();
            setO(1f - 0.85f * p);
        }

        void backCancel() {
            if (closing || dead) return;
            animateTo(1f, Motion.T_MODAL, null);
        }

        void close() {
            if (closing || dead) return;
            closing = true;
            layerBack = null;
            if (!full) Ui.haptic(shell, 0);
            if (src.isAttachedToWindow()) { // it may have scrolled or been rebuilt
                int[] l2 = new int[2];
                src.getLocationOnScreen(l2);
                sx = l2[0] - rl[0];
                sy = l2[1] - rl[1];
            }
            animateTo(0f, full ? 300 : Motion.T_MODAL, this::cleanup);
            syncBack();
        }

        void closeThen(Runnable r) {
            afterClose = r;
            close();
        }

        void finishNow() {
            if (anim != null) anim.cancel();
            if (snapAnim != null) snapAnim.cancel();
            cleanup();
        }

        void cleanup() {
            if (dead) return;
            dead = true;
            src.setVisibility(View.VISIBLE);
            if (tok == layerToken) {
                layer.setVisibility(View.GONE);
                layer.removeAllViews();
                layerBack = null;
                backView = null;
            }
            Blur.view(main, 0f);
            Blur.view(dock, 0f);
            Blur.motion(shell, 0f);
            main.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            dock.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
            if (floatCard == this) floatCard = null;
            syncBack();
            if (afterClose != null) afterClose.run();
        }
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
            } else if (req == REQ_SOUND_GEN || req == REQ_SOUND_DL) {
                @SuppressWarnings("deprecation")
                Uri u = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                String v = u == null ? "silent" : u.equals(Settings.System.DEFAULT_NOTIFICATION_URI) ? "" : u.toString();
                if (req == REQ_SOUND_DL) draft.soundDeadline = v;
                else draft.soundGeneral = v;
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

    // ringtone picker for general and card-deadline notifications (separate sounds)
    void pickSound(boolean deadline) {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        String cur = deadline ? draft.soundDeadline : draft.soundGeneral;
        Uri existing = cur.isEmpty() ? Settings.System.DEFAULT_NOTIFICATION_URI : cur.equals("silent") ? null : Uri.parse(cur);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing);
        startActivityForResult(i, deadline ? REQ_SOUND_DL : REQ_SOUND_GEN);
    }

    String soundName(String v) {
        if (v.isEmpty()) return Str.t("soundDefault");
        if (v.equals("silent")) return Str.t("soundSilent");
        try {
            android.media.Ringtone r = RingtoneManager.getRingtone(this, Uri.parse(v));
            if (r != null) return r.getTitle(this);
        } catch (Exception ignored) {
        }
        return Str.t("soundDefault");
    }

    // Android's own notification settings for this app (channels, banners, lock screen...)
    void openAndroidNotifSettings() {
        Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(i);
    }

    boolean canExactAlarms() {
        return Notifier.canExact(this);
    }

    void requestExactAlarms() {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                toast(Str.t("cantRead"));
            }
        }
    }

    void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            toast(Str.t("cantRead"));
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        render();
    }

    // ---------- back (predictive) ----------
    boolean needBack() {
        return layerBack != null || menuOpen || openOpus != null || !mode.isEmpty() || tab != 0;
    }

    void syncBack() {
        if (back != null) back.update(needBack());
    }

    // closes the top-most thing; false when nothing is left (the system then handles back)
    boolean handleBack() {
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
        } else if (tab == 3 && settingsPage.inSub()) {
            settingsPage.up();
        } else if (tab != 0) {
            go(0, true);
        } else {
            return false;
        }
        return true;
    }

    private View backTarget() {
        if (layerBack != null) return backView != null ? backView : layer;
        if (tab == 3 && settingsPage.inSub()) return settingsPage.contentView();
        return main;
    }

    // live preview while the back gesture is dragged: the top layer shrinks and eases away from the edge
    void backProgress(float p, boolean fromLeft) {
        if (floatCard != null && floatCard.full && layerBack != null) { // card collapses with the gesture
            floatCard.backDrag(p);
            return;
        }
        View t = backTarget();
        t.setPivotX(t.getWidth() / 2f);
        t.setPivotY(t.getHeight() / 2f);
        float s = 1f - 0.1f * p;
        t.setScaleX(s);
        t.setScaleY(s);
        t.setTranslationX((fromLeft ? 1f : -1f) * dp(8) * p);
    }

    void backCancelled() {
        if (floatCard != null && floatCard.full && layerBack != null) floatCard.backCancel();
        else backReset();
    }

    void backReset() {
        View[] ts = {main, backView, settingsPage != null ? settingsPage.contentView() : null};
        for (View t : ts) {
            if (t == null) continue;
            Motion.scale(t, 1f, Motion.SPATIAL_STIFF, Motion.SPATIAL_DAMP);
            Motion.spatial(t, DynamicAnimation.TRANSLATION_X, 0f);
        }
    }

    @Override
    public void onBackPressed() {
        if (!handleBack()) super.onBackPressed();
    }
}
