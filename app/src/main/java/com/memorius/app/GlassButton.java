package com.memorius.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

// The app's button, in the current style (typography is not touched here):
//   Flat       solid or lightly tinted, no gloss; pressing darkens it a little
//   Humanist   leaf-shaped, clay colour; pressing tilts it slightly, like a cut-out
//   Material   stadium, tonal; a state layer plus an ink ripple from the touch point
//   Swiss      square, black / white, an outline; pressing inverts it instantly
//   Corporate  liquid glass: gloss, glow, swells and follows the finger (see Interactive)
// A filled button uses the style's primary colour and the text colour with the best contrast on it.
// Every shape comes from one path (Surface.radiiFor), so corners, fill and outline always agree.
final class GlassButton extends TextView {
    private final boolean solid;
    private final Interactive inter;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF(), in = new RectF();
    private final Path body = new Path(), edge = new Path();
    private final float[] radii = new float[8];
    private float pressT, rippleT, rx, ry;
    private ValueAnimator pressAnim, rippleAnim;
    private final int textNormal;

    GlassButton(Context c, CharSequence label, boolean solid) {
        super(c);
        this.solid = solid;
        inter = new Interactive(this);
        setText(label);
        setTextSize(15);
        // filled buttons: the text colour with the best contrast on the fill, in every style and theme
        textNormal = solid ? Ui.onPrimary : Ui.fg;
        setTextColor(textNormal);
        setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        setIncludeFontPadding(false);
        setGravity(Gravity.CENTER);
        float sp = Style.id == Style.CORPORATE ? 1.1f : 1f;
        setPadding(Ui.dp(20 * sp), Ui.dp(12 * sp), Ui.dp(20 * sp), Ui.dp(12 * sp));
        setMinHeight(Ui.dp(48));
        setClickable(true);
        setFocusable(true);
        rim.setStyle(Paint.Style.STROKE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (isEnabled()) {
            if (Style.glass()) inter.handle(e);
            else press(e);
        }
        return super.onTouchEvent(e);
    }

    // press feedback for the non-glass styles: ~150 ms, no swelling
    private void press(MotionEvent e) {
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            rx = e.getX();
            ry = e.getY();
            animatePress(1f);
            if (Style.id == Style.MATERIAL) {
                if (rippleAnim != null) rippleAnim.cancel();
                rippleAnim = ValueAnimator.ofFloat(0f, 1f);
                rippleAnim.setDuration(Motion.dur(300));
                rippleAnim.setInterpolator(Ui.OUT_SHARP);
                rippleAnim.addUpdateListener(an -> {
                    rippleT = (Float) an.getAnimatedValue();
                    invalidate();
                });
                rippleAnim.start();
            }
            if (Style.id == Style.SWISS) setTextColor(solid ? Ui.fg : Ui.bg); // inverted, at once
        } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
            animatePress(0f);
            if (Style.id == Style.SWISS) setTextColor(textNormal);
        }
    }

    private void animatePress(final float to) {
        if (pressAnim != null) pressAnim.cancel();
        if (Style.id == Style.SWISS) { // no animation at all
            pressT = to;
            invalidate();
            return;
        }
        pressAnim = ValueAnimator.ofFloat(pressT, to);
        pressAnim.setDuration(Motion.dur(Motion.T_PRESS));
        pressAnim.setInterpolator(Ui.EASE);
        pressAnim.addUpdateListener(an -> {
            pressT = (Float) an.getAnimatedValue();
            if (Style.id == Style.HUMANIST) setRotation(-0.8f * pressT);
            invalidate();
        });
        pressAnim.start();
    }

    private void buildBody(float w, float h) {
        float r = Style.id == Style.SWISS ? 0f : h / 2f;
        Surface.radiiFor(w, h, r, Style.organic(), false, radii);
        rf.set(0, 0, w, h);
        body.reset();
        body.addRoundRect(rf, radii, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        buildBody(w, h);
        if (Style.glass()) drawGlass(c, w, h);
        else drawFlat(c, w, h);
        super.onDraw(c);
    }

    // ---- Flat / Humanist / Material / Swiss ----
    private void drawFlat(Canvas c, float w, float h) {
        boolean swissPressed = Style.id == Style.SWISS && pressT > 0.5f;
        int fillC;
        int strokeC = 0;
        float strokeW = 0f;
        switch (Style.id) {
            case Style.HUMANIST:
                fillC = solid ? Ui.primary : Ui.alpha(Ui.primary, 0x24);
                if (!solid) {
                    strokeC = Ui.primary;
                    strokeW = 1.5f;
                }
                break;
            case Style.MATERIAL:
                fillC = solid ? Ui.primary : Ui.alpha(Ui.primary, 0x2A); // tonal
                break;
            case Style.SWISS:
                fillC = solid ? Ui.fg : 0;
                strokeC = Ui.fg;
                strokeW = solid ? 0f : 1.5f;
                if (swissPressed) fillC = solid ? Ui.bg : Ui.fg; // inverted
                break;
            default: // Flat
                fillC = solid ? Ui.primary : Ui.alpha(Ui.fg, Ui.isDark ? 0x1A : 0x14);
        }
        fillC = solid && fillC != 0 ? (fillC | 0xFF000000) : fillC;
        if ((fillC >>> 24) != 0) {
            fill.setShader(null);
            fill.setColor(fillC);
            c.drawPath(body, fill);
        }
        c.save();
        c.clipPath(body);
        if (Style.id != Style.SWISS && pressT > 0.01f) { // state layer: a veil of the text colour
            fill.setColor(Ui.alpha(solid ? Ui.onPrimary : Ui.fg, Math.round(0x22 * pressT)));
            c.drawRect(rf, fill);
        }
        if (Style.id == Style.MATERIAL && rippleT > 0f && rippleT < 1f) { // ink ripple, clipped to the shape
            float maxR = (float) Math.hypot(Math.max(rx, w - rx), Math.max(ry, h - ry));
            fill.setColor(Ui.alpha(solid ? Ui.onPrimary : Ui.fg, Math.round(0x2E * (1f - rippleT))));
            c.drawCircle(rx, ry, maxR * rippleT, fill);
        }
        c.restore();
        if (strokeW > 0f) { // outline drawn inside the bounds so it is never cut
            float sw = Math.max(1f, Ui.dp(strokeW));
            in.set(sw / 2f, sw / 2f, w - sw / 2f, h - sw / 2f);
            float[] ri = new float[8];
            for (int i = 0; i < 8; i++) ri[i] = Math.max(0f, radii[i] - sw / 2f);
            edge.reset();
            edge.addRoundRect(in, ri, Path.Direction.CW);
            rim.setShader(null);
            rim.setStrokeWidth(sw);
            rim.setColor(strokeC);
            c.drawPath(edge, rim);
        }
    }

    // ---- Corporate: liquid glass ----
    private void drawGlass(Canvas c, float w, float h) {
        boolean dark = Ui.isDark;
        if (solid) fill.setColor(Ui.primary | 0xFF000000);
        else fill.setColor(dark ? 0x1AFFFFFF : 0x14000000);
        fill.setShader(null);
        c.drawPath(body, fill);
        if (!solid) { // glossy sheen over the top half (not on filled buttons: it would wash out their text)
            sheen.setShader(new LinearGradient(0, 0, 0, h * 0.6f,
                    dark ? 0x20FFFFFF : 0x60FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
            c.save();
            c.clipPath(body);
            c.drawRect(0, 0, w, h * 0.55f, sheen);
            c.restore();
        }
        float pr = inter.press();
        if (pr > 0.02f) { // glow under the finger, clipped to the shape
            glow.setShader(new RadialGradient(inter.glowX, inter.glowY, Math.max(w, h) * 0.7f,
                    new int[]{(int) (0x40 * Math.min(1f, pr)) << 24 | 0xFFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            c.save();
            c.clipPath(body);
            c.drawRect(rf, glow);
            c.restore();
        }
        float sw = Math.max(1f, Ui.density * (Ui.contrast ? 1.5f : 1f));
        int hi = dark ? 0x55FFFFFF : 0xC0FFFFFF, lo = dark ? 0x14FFFFFF : 0x30000000;
        if (Ui.contrast) {
            rim.setShader(null);
            rim.setColor(Ui.line);
        } else {
            rim.setShader(new LinearGradient(0, 0, w, h, new int[]{hi, lo, hi}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        }
        rim.setStrokeWidth(sw);
        in.set(sw / 2f, sw / 2f, w - sw / 2f, h - sw / 2f);
        float[] ri = new float[8];
        for (int i = 0; i < 8; i++) ri[i] = Math.max(0f, radii[i] - sw / 2f);
        edge.reset();
        edge.addRoundRect(in, ri, Path.Direction.CW);
        c.drawPath(edge, rim);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Button");
    }
}
