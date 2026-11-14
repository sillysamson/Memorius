package com.memorius.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

// Liquid glass slider: a 6 dp track with an accent fill and a 40 x 24 dp thumb that swells and
// bends the track while held. The value follows the finger through a damped spring; tapping the
// track moves it there.
final class LiquidSlider extends View {
    static final int ACCENT_LIGHT = 0xFF0088FF, ACCENT_DARK = 0xFF0091FF;

    private final int min, max, step;
    private final Ui.IntCb cb;
    private final FloatValueHolder vh = new FloatValueHolder(0f), ph = new FloatValueHolder(0f);
    private final SpringAnimation valueAnim = new SpringAnimation(vh), pressAnim = new SpringAnimation(ph);
    private float value, vel, press, target, lastX;
    private int lastSent;
    private boolean pressing, tapping, didDrag;
    private final GlassThumb thumb = new GlassThumb();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path track = new Path();
    private final RectF rf = new RectF();

    LiquidSlider(Context c, int min, int max, int cur, int step, Ui.IntCb cb) {
        super(c);
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.cb = cb;
        value = target = cur;
        lastSent = cur;
        vh.setValue(value);
        valueAnim.setSpring(new SpringForce(value).setStiffness(650f).setDampingRatio(0.72f));
        valueAnim.setMinimumVisibleChange(0.01f);
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
        setFocusable(true);
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Ui.dp(48));
    }

    private boolean rtl() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    private float trackLeft() {
        return Ui.dp(12); // room for the thumb to hang 10 dp over each end
    }

    private float trackW() {
        return getWidth() - 2f * trackLeft();
    }

    private float progress(float v) {
        return LiquidMath.clamp((v - min) / (float) (max - min), 0f, 1f);
    }

    private int snap(float v) {
        int s = Math.round((v - min) / step) * step + min;
        return Math.max(min, Math.min(max, s));
    }

    private void send(float v, boolean haptic) {
        int s = snap(v);
        if (s == lastSent) return;
        lastSent = s;
        if (haptic && s % 5 == 0) Ui.haptic(this, 0);
        cb.run(s);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled()) return false;
        float x = e.getX(), y = e.getY();
        float tl = trackLeft(), tw = trackW();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                float left = LiquidMath.sliderThumbLeft(progress(value), tl, tw, Ui.dp(40), rtl());
                pressing = x >= left - Ui.dp(8) && x <= left + Ui.dp(48) && Math.abs(y - getHeight() / 2f) < Ui.dp(24);
                tapping = !pressing;
                didDrag = false;
                lastX = x;
                if (pressing) {
                    target = value;
                    valueAnim.cancel();
                    Motion.to(pressAnim, 1f, 450f, 0.6f);
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                } else if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                if (pressing) {
                    float dx = x - lastX;
                    lastX = x;
                    didDrag |= dx != 0f;
                    float delta = (max - min) * (dx / tw);
                    target = LiquidMath.clamp(target + (rtl() ? -delta : delta), min, max);
                    Motion.to(valueAnim, target, 650f, 0.72f);
                    send(target, true);
                } else if (tapping && Math.abs(x - lastX) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    tapping = false;
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (pressing) {
                    pressing = false;
                    Motion.to(pressAnim, 0f, 450f, 0.6f);
                    float t = snap(target);
                    target = t;
                    Motion.to(valueAnim, t, 650f, 0.72f);
                    send(t, false);
                    Ui.haptic(this, 0);
                } else if (tapping && e.getActionMasked() == MotionEvent.ACTION_UP) {
                    float t = snap(LiquidMath.sliderValueAt(x, tl, tw, min, max, rtl()));
                    target = t;
                    valueAnim.setStartValue(value);
                    Motion.to(valueAnim, t, 650f, 0.72f);
                    send(t, true);
                }
                tapping = false;
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

    private void block(Canvas c, float l, float t, float r, float b, float radius, boolean organic, int color) {
        rf.set(l, t, r, b);
        Surface.radiiFor(r - l, b - t, radius, organic, false, rad);
        Path pp = new Path();
        pp.addRoundRect(rf, rad, Path.Direction.CW);
        p.setColor(color);
        c.drawPath(pp, p);
    }

    // Flat / Humanist / Material / Swiss: a plain track and a plain thumb
    private void drawFlat(Canvas c) {
        float w = getWidth(), h = getHeight();
        float tl = trackLeft(), tw = trackW(), cy = h / 2f;
        float pr = progress(value);
        float pp = Math.min(1f, press);
        int on = Ui.primary | 0xFF000000;
        float cx = rtl() ? tl + tw * (1f - pr) : tl + tw * pr;
        float s0 = rtl() ? tl + tw : tl;         // where the fill starts
        switch (Style.id) {
            case Style.SWISS: {
                block(c, tl, cy - Ui.dp(1), tl + tw, cy + Ui.dp(1), 0f, false, Ui.mix(Ui.bg, Ui.fg, 0.35f) | 0xFF000000);
                block(c, Math.min(s0, cx), cy - Ui.dp(2), Math.max(s0, cx), cy + Ui.dp(2), 0f, false, Ui.fg | 0xFF000000);
                float sz = Ui.dp(14) + Ui.dp(4) * pp; // a square thumb
                block(c, cx - sz / 2f, cy - sz / 2f, cx + sz / 2f, cy + sz / 2f, 0f, false, Ui.fg | 0xFF000000);
                break;
            }
            case Style.MATERIAL: {
                float th = Ui.dp(4);
                block(c, tl, cy - th / 2f, tl + tw, cy + th / 2f, th / 2f, false, Ui.mix(Ui.bg, on, 0.24f));
                block(c, Math.min(s0, cx), cy - th / 2f, Math.max(s0, cx), cy + th / 2f, th / 2f, false, on);
                if (pp > 0.02f) { // halo while held
                    p.setColor(Ui.alpha(on, (int) (0x2A * pp)));
                    c.drawCircle(cx, cy, Ui.dp(20) * pp + Ui.dp(8), p);
                }
                p.setColor(on);
                c.drawCircle(cx, cy, Ui.dp(10) + Ui.dp(1) * pp, p);
                break;
            }
            case Style.HUMANIST: {
                float th = Ui.dp(6);
                block(c, tl, cy - th / 2f, tl + tw, cy + th / 2f, th / 2f, true, Ui.mix(Ui.bg, Ui.fg, 0.14f) | 0xFF000000);
                block(c, Math.min(s0, cx), cy - th / 2f, Math.max(s0, cx), cy + th / 2f, th / 2f, true, on);
                float sz = Ui.dp(22) * (1f + 0.1f * pp); // a hand-cut tile, a little off-square
                c.save();
                c.rotate(8f, cx, cy);
                block(c, cx - sz / 2f, cy - sz / 2f, cx + sz / 2f, cy + sz / 2f, sz * 0.4f, true, on);
                c.restore();
                break;
            }
            default: { // Flat
                float th = Ui.dp(4);
                block(c, tl, cy - th / 2f, tl + tw, cy + th / 2f, th / 2f, false, Ui.mix(Ui.bg, Ui.fg, 0.18f) | 0xFF000000);
                block(c, Math.min(s0, cx), cy - th / 2f, Math.max(s0, cx), cy + th / 2f, th / 2f, false, on);
                p.setColor(on);
                c.drawCircle(cx, cy, Ui.dp(10) * (1f + 0.2f * pp), p);
            }
        }
    }

    // Corporate: liquid glass
    private void drawGlass(Canvas c) {
        float w = getWidth(), h = getHeight();
        final float tl = trackLeft(), tw = trackW(), th = Ui.dp(6);
        final float tt = (h - th) / 2f;
        int accent = Ui.isDark ? ACCENT_DARK : ACCENT_LIGHT;
        int trackC = Ui.isDark ? 0x5C787880 : 0x33787878;
        float pr = progress(value);

        rf.set(tl, tt, tl + tw, tt + th);
        track.reset();
        track.addRoundRect(rf, th / 2f, th / 2f, Path.Direction.CW);
        drawTrack(c, tw, th, tl, tt, pr, trackC, accent);

        final float thumbW = Ui.dp(40), thumbH = Ui.dp(24);
        final float left = LiquidMath.sliderThumbLeft(pr, tl, tw, thumbW, rtl());
        final float top = (h - thumbH) / 2f;
        final float cx = left + thumbW / 2f, cy = top + thumbH / 2f;
        final int fTrackC = trackC, fAccent = accent;
        final float fPr = pr, fTl = tl, fTw = tw, fTh = th, fTt = tt;
        thumb.draw(c, (int) w, (int) h, left, top, thumbW, thumbH, press, vel, 10f, 1.5f, 4f, 5f, (lc, pp) -> {
            drawTrack(lc, fTw, fTh, fTl, fTt, fPr, fTrackC, fAccent); // the track at its real size
        });
    }

    private void drawTrack(Canvas c, float tw, float th, float tl, float tt, float pr, int trackC, int accent) {
        c.save();
        c.clipPath(track);
        p.setColor(trackC);
        c.drawRect(tl, tt, tl + tw, tt + th, p);
        p.setColor(accent);
        if (rtl()) c.drawRect(tl + tw * (1f - pr), tt, tl + tw, tt + th, p);
        else c.drawRect(tl, tt, tl + tw * pr, tt + th, p);
        c.restore();
    }

    // ---------- screen readers: behaves like a normal slider ----------
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.SeekBar");
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, min, max, lastSent));
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle args) {
        float t = -1f;
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) t = lastSent + step;
        else if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) t = lastSent - step;
        else if (action == android.R.id.accessibilityActionSetProgress && args != null) t = args.getFloat("ACTION_ARGUMENT_PROGRESS_VALUE", lastSent);
        if (t < 0f) return super.performAccessibilityAction(action, args);
        target = snap(t);
        Motion.to(valueAnim, target, 650f, 0.72f);
        send(target, false);
        return true;
    }
}
