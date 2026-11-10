package com.memorius.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

// The bottom tab bar as liquid glass.
//  - glass capsule behind the tabs (blur, saturation, lens at the rim)
//  - a glass "thumb" over the selected tab; hold it and it swells (78/56), bends the light
//    (lens + chromatic aberration), grows a highlight and shadow, and stretches with speed
//  - the thumb value follows the finger through a damped spring; the whole bar leans up to 4 dp
//  - tap a tab, or drag the thumb and let go: it settles on the nearest tab
final class LiquidBar extends GlassFrame {
    interface Listener {
        void onPress();

        void onTick();

        void onSelect(int index);
    }

    static final int ACCENT_LIGHT = 0xFF0088FF, ACCENT_DARK = 0xFF0091FF;

    private final int count;
    private final int[] kinds;
    private final View[] items;
    private Listener listener;

    private final FloatValueHolder vh = new FloatValueHolder(0f), ph = new FloatValueHolder(0f), oh = new FloatValueHolder(0f);
    private final SpringAnimation valueAnim = new SpringAnimation(vh);
    private final SpringAnimation pressAnim = new SpringAnimation(ph);
    private final SpringAnimation offAnim = new SpringAnimation(oh);

    private float value = 0f, vel = 0f, press = 0f, dragPx = 0f;
    private boolean pressing, tapping;
    private float lastX, dragTarget, lastPos;
    private long lastPosT;
    private int lastNear;
    private Runnable settle;
    private Object layerNode, thumbShader;
    private boolean thumbShaderTried;

    private int iconColor = 0xFFFFFFFF;
    private final Paint iconP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint surfP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimP2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tr = new RectF();
    private final Path clip = new Path();

    LiquidBar(Context c, int[] kinds) {
        super(c);
        this.kinds = kinds;
        this.count = kinds.length;
        rimP2.setStyle(Paint.Style.STROKE);

        valueAnim.setSpring(new SpringForce(0f).setStiffness(650f).setDampingRatio(0.72f));
        valueAnim.setMinimumVisibleChange(0.001f);
        valueAnim.addUpdateListener((a, v, vl) -> {
            value = v;
            vel = vl;
            invalidate();
        });
        valueAnim.addEndListener((a, canceled, v, vl) -> {
            vel = 0f;
            invalidate();
            if (!canceled && settle != null) {
                Runnable r = settle;
                settle = null;
                r.run();
            }
        });
        pressAnim.setSpring(new SpringForce(0f).setStiffness(450f).setDampingRatio(0.55f));
        pressAnim.setMinimumVisibleChange(0.001f);
        pressAnim.addUpdateListener((a, v, vl) -> {
            press = v;
            invalidate();
        });
        offAnim.setSpring(new SpringForce(0f).setStiffness(300f).setDampingRatio(0.5f));
        offAnim.setMinimumVisibleChange(0.5f);
        offAnim.addUpdateListener((a, v, vl) -> {
            dragPx = v;
            invalidate();
        });

        // invisible items: only there so screen readers can focus and activate each tab
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(Ui.dp(4), Ui.dp(4), Ui.dp(4), Ui.dp(4));
        items = new View[count];
        for (int i = 0; i < count; i++) {
            final int idx = i;
            View it = new View(c);
            it.setOnClickListener(v -> {
                if (listener != null) listener.onSelect(idx);
            });
            items[i] = it;
            row.addView(it, new LinearLayout.LayoutParams(0, -1, 1f));
        }
        addView(row, new LayoutParams(-1, -1));
    }

    void setListener(Listener l) {
        listener = l;
    }

    void setLabels(String[] labels) {
        for (int i = 0; i < count; i++) items[i].setContentDescription(labels[i]);
    }

    void setSelectedIndex(int idx) {
        for (int i = 0; i < count; i++) items[i].setSelected(i == idx);
    }

    void setIconColor(int c) {
        iconColor = c;
        invalidate();
    }

    float getValue() {
        return value;
    }

    // driven from outside (the pager moving): follows exactly, no spring lag
    void setPosition(float p) {
        if (pressing) return;
        valueAnim.cancel();
        long now = System.nanoTime();
        if (lastPosT != 0 && now > lastPosT) vel = (p - lastPos) / ((now - lastPosT) / 1e9f);
        else vel = 0f;
        lastPos = p;
        lastPosT = now;
        vh.setValue(p);
        value = p;
        invalidate();
    }

    // settle on a tab with the damped spring; end runs when it has stopped
    void animateTo(int idx, Runnable end) {
        lastPosT = 0;
        if (Math.abs(value - idx) < 0.001f && !valueAnim.isRunning()) {
            vel = 0f;
            invalidate();
            if (end != null) end.run();
            return;
        }
        settle = end;
        Motion.to(valueAnim, idx, 650f, 0.72f);
    }

    private boolean rtl() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    // ---------- touch ----------
    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float w = getWidth(), pad = Ui.dp(4);
        float tabW = LiquidMath.tabWidth(w, pad, count);
        float x = e.getX();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                float left = LiquidMath.thumbLeft(value, w, pad, count, rtl()) + LiquidMath.panelOffset(dragPx, w, Ui.dp(4));
                pressing = x >= left && x <= left + tabW; // only the thumb can be held and dragged
                tapping = !pressing;
                lastX = x;
                if (pressing) {
                    dragTarget = value;
                    lastNear = Math.round(value);
                    valueAnim.cancel();
                    Motion.to(pressAnim, 1f, 450f, 0.55f);
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    if (listener != null) {
                        listener.onPress();
                        listener.onTick();
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                if (pressing) {
                    float dx = x - lastX;
                    lastX = x;
                    dragTarget = LiquidMath.dragValue(dragTarget, dx, tabW, rtl(), count);
                    dragPx += dx;
                    offAnim.cancel();
                    Motion.to(valueAnim, dragTarget, 650f, 0.72f); // damped: the thumb trails the finger a little
                    int near = Math.round(dragTarget);
                    if (near != lastNear) {
                        lastNear = near;
                        if (listener != null) listener.onTick();
                    }
                    invalidate();
                } else if (tapping && Math.abs(x - lastX) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    tapping = false;
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                boolean up = e.getActionMasked() == MotionEvent.ACTION_UP;
                if (pressing) {
                    pressing = false;
                    int idx = Math.max(0, Math.min(count - 1, Math.round(dragTarget)));
                    Motion.to(pressAnim, 0f, 450f, 0.55f);
                    offAnim.setStartValue(dragPx);
                    Motion.to(offAnim, 0f, 300f, 0.5f);
                    Motion.to(valueAnim, idx, 650f, 0.72f);
                    if (listener != null) listener.onSelect(idx);
                } else if (tapping && up) {
                    int idx = LiquidMath.tabAt(x, w, pad, count, rtl());
                    if (listener != null) listener.onSelect(idx);
                }
                tapping = false;
                return true;
            }
        }
        return true;
    }

    // ---------- drawing ----------
    private void drawIcons(Canvas c, Paint p, float scale) {
        float w = getWidth(), h = getHeight(), pad = Ui.dp(4);
        float tabW = LiquidMath.tabWidth(w, pad, count);
        float size = Ui.dp(26) * scale;
        for (int i = 0; i < count; i++) {
            float cx = pad + (LiquidMath.slot(i, count, rtl()) + 0.5f) * tabW;
            IconView.draw(c, kinds[i], p, cx - size / 2f, h / 2f - size / 2f, size, false);
        }
    }

    @Override
    protected void drawContent(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float prog = LiquidMath.clamp(press, 0f, 1.2f);
        float panel = LiquidMath.panelOffset(dragPx, w, Ui.dp(4));
        float barScale = 1f + Ui.dp(16) / w * prog;
        boolean dark = isDark();

        c.save();
        c.scale(barScale, barScale, w / 2f, h / 2f);
        c.translate(panel, 0f);

        drawGlass(c);

        iconP.setColor(iconColor);
        drawIcons(c, iconP, 1f);

        // ---- the thumb ----
        float pad = Ui.dp(4);
        float tabW = LiquidMath.tabWidth(w, pad, count);
        float left = LiquidMath.thumbLeft(value, w, pad, count, rtl());
        float cx = left + tabW / 2f, cy = h / 2f, th = h - 2f * pad;
        float s = LiquidMath.lerp(1f, LiquidMath.PRESSED_SCALE, prog);
        float v = vel / 10f;
        float sx = LiquidMath.stretchX(s, v), sy = LiquidMath.stretchY(s, v);
        tr.set(cx - tabW * sx / 2f, cy - th * sy / 2f, cx + tabW * sx / 2f, cy + th * sy / 2f);
        float rad = Math.min(tr.width(), tr.height()) / 2f;
        clip.reset();
        clip.addRoundRect(tr, rad, rad, Path.Direction.CW);
        int accent = dark ? ACCENT_DARK : ACCENT_LIGHT;
        accentP.setColor(accent);
        float iconScale = LiquidMath.lerp(1f, 1.2f, prog);

        if (prog > 0.02f) { // soft shadow under the swollen thumb
            fx.setShader(new RadialGradient(0, 0, tr.width() * 0.5f,
                    new int[]{(int) (0x40 * Math.min(1f, prog)) << 24, 0x00000000}, null, Shader.TileMode.CLAMP));
            c.save();
            c.translate(cx, tr.bottom - Ui.dp(2));
            c.scale(1f, 0.28f);
            c.drawCircle(0, 0, tr.width() * 0.5f, fx);
            c.restore();
        }

        c.save();
        c.clipPath(clip);
        boolean lens = prog > 0.02f && fxActive && Build.VERSION.SDK_INT >= 31 && bgNode != null;
        boolean drewLayer = false;
        if (lens) {
            try {
                if (layerNode == null) layerNode = Glass31.newNode();
                Canvas lc = Glass31.begin(layerNode, (int) w, (int) h);
                Glass31.draw(lc, bgNode);
                fx.setShader(null);
                fx.setColor((0x66 << 24) | (dark ? 0x121212 : 0xFAFAFA));
                lc.drawRect(0, 0, w, h, fx);
                drawIcons(lc, accentP, iconScale);
                Glass31.end(layerNode);
                Object eff = null;
                if (Build.VERSION.SDK_INT >= 33) {
                    if (!thumbShaderTried) {
                        thumbShaderTried = true;
                        thumbShader = Glass33.makeShader();
                    }
                    eff = Glass33.lens(thumbShader, left, pad, left + tabW, h - pad, th / 2f,
                            Ui.dp(10) * prog + 1f, Ui.dp(14) * prog, Ui.dp(3) * prog);
                }
                Glass31.setLens(layerNode, eff);
                c.save();
                c.scale(sx, sy, cx, cy); // magnifies what is under the thumb
                Glass31.draw(c, layerNode);
                c.restore();
                drewLayer = true;
            } catch (Throwable t) {
                drewLayer = false;
            }
        }
        if (!drewLayer) { // at rest: the selected icon in the accent colour, inside the thumb
            c.save();
            c.scale(sx, sy, cx, cy);
            drawIcons(c, accentP, iconScale);
            c.restore();
        }
        // surface: a faint grey capsule at rest, clearer glass while held
        surfP.setColor(dark ? 0xFFFFFFFF : 0xFF000000);
        surfP.setAlpha((int) (0x1A * (1f - Math.min(1f, prog))));
        c.drawRect(tr, surfP);
        if (prog > 0.02f) {
            surfP.setColor(0xFF000000);
            surfP.setAlpha((int) (0x08 * Math.min(1f, prog)));
            c.drawRect(tr, surfP);
            // touch glow
            fx.setShader(new RadialGradient(cx, cy, tr.width() * 0.8f,
                    new int[]{(int) (0x38 * Math.min(1f, prog)) << 24 | 0xFFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            c.drawRect(tr, fx);
            // inner shadow: darker just inside the rim
            rimP2.setShader(null);
            rimP2.setStrokeWidth(Ui.dp(8));
            rimP2.setColor(0xFF000000);
            rimP2.setAlpha((int) (0x14 * Math.min(1f, prog)));
            c.drawRoundRect(tr, rad, rad, rimP2);
        }
        c.restore();

        if (prog > 0.02f) { // highlight along the thumb's rim
            float sw = Math.max(1.5f, Ui.density * 1.5f);
            int a = (int) (0xE0 * Math.min(1f, prog));
            rimP2.setStrokeWidth(sw);
            rimP2.setAlpha(255);
            rimP2.setShader(new LinearGradient(tr.left, tr.top, tr.right, tr.bottom,
                    new int[]{(a << 24) | 0xFFFFFF, 0x20FFFFFF, (a / 2 << 24) | 0xFFFFFF}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            c.drawRoundRect(tr.left + sw / 2, tr.top + sw / 2, tr.right - sw / 2, tr.bottom - sw / 2, rad, rad, rimP2);
        }
        c.restore();
    }
}
