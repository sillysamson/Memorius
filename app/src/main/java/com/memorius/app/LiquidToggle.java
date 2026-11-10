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
        float w = getWidth(), h = getHeight();
        float tw = Ui.dp(64), th = Ui.dp(28);
        float tl = (w - tw) / 2f, tt = (h - th) / 2f;
        int track = Ui.isDark ? 0x5C787880 : 0x33787878;
        int accent = Ui.isDark ? GREEN_DARK : GREEN_LIGHT;
        trackP.setColor(Ui.mix(track, accent, LiquidMath.clamp(value, 0f, 1f)));
        rf.set(tl, tt, tl + tw, tt + th);
        capsule.reset();
        capsule.addRoundRect(rf, th / 2f, th / 2f, Path.Direction.CW);
        c.drawPath(capsule, trackP);

        final float thumbW = Ui.dp(40), thumbH = Ui.dp(24);
        final float left = tl + LiquidMath.toggleThumbLeft(value, tw, thumbW, Ui.dp(2), Ui.dp(20), rtl());
        final float top = (h - thumbH) / 2f;
        final float cx = left + thumbW / 2f, cy = top + thumbH / 2f;
        thumb.draw(c, (int) w, (int) h, left, top, thumbW, thumbH, press, vel, 50f, 1.5f, 5f, 10f, (lc, pr) -> {
            // what the thumb sees: the track, squeezed to a thin line at rest and growing as it is held
            lc.save();
            lc.scale(LiquidMath.lerp(2f / 3f, 0.75f, pr), LiquidMath.lerp(0f, 0.75f, pr), cx, cy);
            lc.drawPath(capsule, trackP);
            lc.restore();
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
