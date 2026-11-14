package com.memorius.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

// Liquid glass switch. 64 x 28 dp track, 40 x 24 dp thumb. Tap it, or hold and drag the thumb.
// Held: the thumb swells to 1.5x and becomes glass that bends the track under it.
final class LiquidToggle extends View {
    interface Listener {
        void onChanged(boolean checked);
    }

    static final int GREEN_LIGHT = 0xFF34C759, GREEN_DARK = 0xFF30D158;

    private boolean checked;
    private Listener listener;
    private final FloatValueHolder vh = new FloatValueHolder(0f), ph = new FloatValueHolder(0f);
    private final SpringAnimation valueAnim = new SpringAnimation(vh), pressAnim = new SpringAnimation(ph);
    private float value, vel, press, fraction, downX, lastX;
    private boolean pressing, didDrag;
    private final GlassThumb thumb = new GlassThumb();
    private final Paint trackP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path capsule = new Path();
    private final RectF rf = new RectF();

    LiquidToggle(Context c, boolean on) {
        super(c);
        checked = on;
        fraction = value = on ? 1f : 0f;
        vh.setValue(value);
        valueAnim.setSpring(new SpringForce(value).setStiffness(650f).setDampingRatio(0.72f));
        valueAnim.setMinimumVisibleChange(0.001f);
        valueAnim.addUpdateListener((a, v, vl) -> {
            value = v;
            vel = vl;
            invalidate();
        });
        valueAnim.addEndListener((a, canceled, v, vl) -> {
            vel = 0f;
            invalidate();
        });
        pressAnim.setSpring(new SpringForce(0f).setStiffness(450f).setDampingRatio(0.6f));
        pressAnim.setMinimumVisibleChange(0.001f);
        pressAnim.addUpdateListener((a, v, vl) -> {
            press = v;
            invalidate();
        });
        setClickable(true);
        setFocusable(true);
    }

    void setListener(Listener l) {
        listener = l;
    }

    boolean isChecked() {
        return checked;
    }

    void setChecked(boolean on, boolean notify) {
        checked = on;
        fraction = on ? 1f : 0f;
        Motion.to(valueAnim, fraction, 650f, 0.72f);
        if (notify && listener != null) listener.onChanged(on);
    }

    // tap, keyboard, or a screen reader's "activate"
    @Override
    public boolean performClick() {
        if (!isEnabled()) return false;
        setChecked(!checked, true);
        return super.performClick();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(Ui.dp(64), Ui.dp(48));
    }

    private boolean rtl() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) return false;
        float dragW = Ui.dp(20);
        float x = e.getX();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressing = true;
                didDrag = false;
                downX = lastX = x;
                Motion.to(pressAnim, 1f, 450f, 0.6f);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!pressing) return true;
                if (!didDrag && Math.abs(x - downX) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    didDrag = true;
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (didDrag) {
                    fraction = LiquidMath.toggleDrag(fraction, x - lastX, dragW, rtl());
                    Motion.to(valueAnim, fraction, 650f, 0.72f); // damped: follows the finger with a slight lag
                }
                lastX = x;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!pressing) return true;
                pressing = false;
                Motion.to(pressAnim, 0f, 450f, 0.6f);
                boolean up = e.getActionMasked() == MotionEvent.ACTION_UP;
                if (didDrag) {
                    boolean now = fraction >= 0.5f;
                    fraction = now ? 1f : 0f;
                    Motion.to(valueAnim, fraction, 650f, 0.72f);
                    if (now != checked) {
                        checked = now;
                        Ui.haptic(this, 2);
                        if (listener != null) listener.onChanged(now);
                    }
                } else if (up) {
                    performClick();
                } else {
                    Motion.to(valueAnim, checked ? 1f : 0f, 650f, 0.72f);
                }
                didDrag = false;
                return true;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (Style.glass()) drawGlass(c);
        else drawFlat(c);
    }

    private final float[] rad = new float[8];

    private void fillRect(Canvas c, float l, float t, float r, float b, float radius, boolean organic, int color) {
        rf.set(l, t, r, b);
        Surface.radiiFor(r - l, b - t, radius, organic, false, rad);
        capsule.reset();
        capsule.addRoundRect(rf, rad, Path.Direction.CW);
        trackP.setStyle(Paint.Style.FILL);
        trackP.setColor(color);
        c.drawPath(capsule, trackP);
    }

    private void ringRect(Canvas c, float l, float t, float r, float b, float radius, boolean organic, int color, float sw) {
        rf.set(l + sw / 2f, t + sw / 2f, r - sw / 2f, b - sw / 2f);
        Surface.radiiFor(rf.width(), rf.height(), Math.max(0f, radius - sw / 2f), organic, false, rad);
        capsule.reset();
        capsule.addRoundRect(rf, rad, Path.Direction.CW);
        trackP.setStyle(Paint.Style.STROKE);
        trackP.setStrokeWidth(sw);
        trackP.setColor(color);
        c.drawPath(capsule, trackP);
        trackP.setStyle(Paint.Style.FILL);
    }

    // Flat / Humanist / Material / Swiss
    private void drawFlat(Canvas c) {
        float w = getWidth(), h = getHeight();
        float v = LiquidMath.clamp(value, 0f, 1f);
        int on = Ui.primary | 0xFF000000;
        int off = Ui.mix(Ui.bg, Ui.fg, 0.28f);
        switch (Style.id) {
            case Style.MATERIAL: { // M3: 52 x 32 track, small thumb that grows when on or pressed
                float tw = Ui.dp(52), th = Ui.dp(32);
                float tl = (w - tw) / 2f, tt = (h - th) / 2f;
                fillRect(c, tl, tt, tl + tw, tt + th, th / 2f, false, Ui.mix(Ui.mix(Ui.bg, Ui.fg, 0.1f), on, v));
                if (v < 0.99f) ringRect(c, tl, tt, tl + tw, tt + th, th / 2f, false, Ui.alpha(off, (int) (255 * (1f - v))), Ui.dp(2));
                float d = Ui.dp(16) + Ui.dp(8) * Math.max(v, Math.min(1f, press)) + Ui.dp(4) * Math.min(1f, press);
                float travelL = tl + Ui.dp(4) + d / 2f, travelR = tl + tw - Ui.dp(4) - d / 2f;
                float cx = rtl() ? travelR + (travelL - travelR) * v : travelL + (travelR - travelL) * v;
                trackP.setColor(Ui.mix(off, Ui.onPrimary | 0xFF000000, v));
                c.drawCircle(cx, h / 2f, d / 2f, trackP);
                break;
            }
            case Style.SWISS: { // square track, square thumb, black or white
                float tw = Ui.dp(64), th = Ui.dp(28);
                float tl = (w - tw) / 2f, tt = (h - th) / 2f;
                if (v > 0.5f) fillRect(c, tl, tt, tl + tw, tt + th, 0f, false, Ui.fg | 0xFF000000);
                else ringRect(c, tl, tt, tl + tw, tt + th, 0f, false, Ui.fg | 0xFF000000, Ui.dp(2));
                float s = Ui.dp(18), pad = Ui.dp(5);
                float l = rtl() ? tl + tw - pad - s - (tw - 2 * pad - s) * v : tl + pad + (tw - 2 * pad - s) * v;
                fillRect(c, l, h / 2f - s / 2f, l + s, h / 2f + s / 2f, 0f, false, v > 0.5f ? Ui.bg | 0xFF000000 : Ui.fg | 0xFF000000);
                break;
            }
            default: { // Flat and Humanist: a capsule track and a round thumb
                boolean hum = Style.id == Style.HUMANIST;
                float tw = Ui.dp(64), th = Ui.dp(28);
                float tl = (w - tw) / 2f, tt = (h - th) / 2f;
                fillRect(c, tl, tt, tl + tw, tt + th, th / 2f, hum, Ui.mix(off, on, v));
                float d = Ui.dp(20) + Ui.dp(2) * Math.min(1f, press);
                float pad = Ui.dp(4) + d / 2f;
                float x0 = tl + pad, x1 = tl + tw - pad;
                float cx = rtl() ? x1 + (x0 - x1) * v : x0 + (x1 - x0) * v;
                trackP.setColor(v > 0.5f ? Ui.onPrimary | 0xFF000000 : Ui.bg | 0xFF000000);
                c.drawCircle(cx, h / 2f, d / 2f, trackP);
            }
        }
    }

    // Corporate: liquid glass
    private void drawGlass(Canvas c) {
        float w = getWidth(), h = getHeight();
        float tw = Ui.dp(64), th = Ui.dp(28);
        float tl = (w - tw) / 2f, tt = (h - th) / 2f;
        int track = Ui.isDark ? 0x5C787880 : 0x33787878;
        int accent = Ui.isDark ? GREEN_DARK : GREEN_LIGHT;
        trackP.setStyle(Paint.Style.FILL);
        trackP.setColor(Ui.mix(track, accent, LiquidMath.clamp(value, 0f, 1f)));
        rf.set(tl, tt, tl + tw, tt + th);
        capsule.reset();
        capsule.addRoundRect(rf, th / 2f, th / 2f, Path.Direction.CW);
        c.drawPath(capsule, trackP);

        final float thumbW = Ui.dp(40), thumbH = Ui.dp(24);
        final float left = tl + LiquidMath.toggleThumbLeft(value, tw, thumbW, Ui.dp(2), Ui.dp(20), rtl());
        final float top = (h - thumbH) / 2f;
        thumb.draw(c, (int) w, (int) h, left, top, thumbW, thumbH, press, vel, 50f, 1.5f, 3f, 4f, (lc, pr) -> {
            // what the thumb sees: the track, at its real size
            lc.drawPath(capsule, trackP);
        });
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Switch");
        info.setCheckable(true);
        info.setChecked(checked);
    }
}
