package com.memorius.app;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;

// The one rounded-shape drawable for the whole app (buttons, selectors, inputs, panels, sheets, swatches).
//
// Why it exists: shapes used to come from many GradientDrawable setups with their own radius,
// stroke and clipping rules, so pill ("stadium") shapes, borders and gradients could disagree
// (square corners, strokes cut by the edge, gradients spilling past the rounded edge).
// Here every shape is one Path, built from the real bounds:
//   - the radius is clamped to half of the shorter side, so a stadium is always a true stadium
//   - fill, gradient and stroke are all drawn with that same path, so nothing can spill over
//   - the stroke is drawn inside the bounds (inset by half its width), so it is never cut off
//   - the outline given to clipToOutline / elevation is the same path
final class Surface extends Drawable {
    static final float STADIUM = 9999f;     // "as round as the height allows"
    static final float STADIUM_MIN = 500f;  // any requested radius from here up (999 in the code) means "stadium"

    private int fill, strokeColor, gradA, gradB, alpha = 255;
    private boolean oval, organic, topOnly, grad;
    private float radiusPx = 0f; // STADIUM_PX = as round as possible
    private float strokePx, dashOnPx, dashOffPx;

    private final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokeP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path(), strokePath = new Path();
    private final RectF rect = new RectF(), inner = new RectF();
    private final float[] radii = new float[8];
    private boolean uniform = true;
    private float uniformR;

    private static final float STADIUM_PX = 1e6f;

    private Surface(int fill) {
        this.fill = fill;
        strokeP.setStyle(Paint.Style.STROKE);
    }

    static Surface rect(int fill, float radiusDp) {
        Surface s = new Surface(fill);
        s.radiusPx = radiusDp >= STADIUM_MIN ? STADIUM_PX : Ui.dp(radiusDp);
        return s;
    }

    static Surface oval(int fill) {
        Surface s = new Surface(fill);
        s.oval = true;
        return s;
    }

    Surface stroke(int color, float dp) {
        strokeColor = color;
        strokePx = Math.max(1f, Ui.dp(dp));
        return this;
    }

    Surface dash(float onDp, float offDp) {
        dashOnPx = Ui.dp(onDp);
        dashOffPx = Ui.dp(offDp);
        return this;
    }

    Surface gradient(int a, int b) {
        grad = true;
        gradA = a;
        gradB = b;
        return this;
    }

    // leaf-like: two opposite corners round, the other two much tighter (Humanist)
    Surface organic() {
        organic = true;
        return this;
    }

    // only the top corners are round (sheets)
    Surface top() {
        topOnly = true;
        return this;
    }

    void setRadiusPx(float px) {
        radiusPx = px;
        rebuild();
        invalidateSelf();
    }

    // radius actually drawn at the top-start corner (for shapes that morph into each other)
    float cornerPx() {
        Rect b = getBounds();
        return Math.min(radiusPx, Math.min(b.width(), b.height()) / 2f);
    }

    // ---- geometry (pure, testable) ----
    static void radiiFor(float w, float h, float radiusPx, boolean organic, boolean topOnly, float[] out) {
        float max = Math.max(0f, Math.min(w, h) / 2f);
        float r = Math.max(0f, Math.min(radiusPx, max));
        float small = organic ? Math.min(max, r * 0.35f) : r;
        float tl = r, tr = small, br = topOnly ? 0f : r, bl = topOnly ? 0f : small;
        if (!organic) {
            tr = r;
            bl = topOnly ? 0f : r;
        }
        out[0] = tl; out[1] = tl;
        out[2] = tr; out[3] = tr;
        out[4] = br; out[5] = br;
        out[6] = bl; out[7] = bl;
    }

    @Override
    protected void onBoundsChange(Rect b) {
        rebuild();
    }

    private void rebuild() {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        rect.set(b);
        path.reset();
        strokePath.reset();
        if (oval) {
            path.addOval(rect, Path.Direction.CW);
            uniform = true;
            uniformR = Math.min(b.width(), b.height()) / 2f;
            if (strokePx > 0f) {
                inner.set(rect);
                inner.inset(strokePx / 2f, strokePx / 2f);
                strokePath.addOval(inner, Path.Direction.CW);
            }
        } else {
            radiiFor(b.width(), b.height(), radiusPx, organic, topOnly, radii);
            path.addRoundRect(rect, radii, Path.Direction.CW);
            uniform = !organic && !topOnly;
            uniformR = radii[0];
            if (strokePx > 0f) {
                inner.set(rect);
                inner.inset(strokePx / 2f, strokePx / 2f);
                float[] ri = new float[8];
                for (int i = 0; i < 8; i++) ri[i] = Math.max(0f, radii[i] - strokePx / 2f);
                strokePath.addRoundRect(inner, ri, Path.Direction.CW);
            }
        }
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        if (grad) {
            fillP.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom, gradA, gradB, Shader.TileMode.CLAMP));
            fillP.setColor(0xFF000000);
            fillP.setAlpha(alpha);
            c.drawPath(path, fillP); // the gradient is painted through the path itself: it cannot spill over the corners
        } else if ((fill >>> 24) != 0) {
            fillP.setShader(null);
            fillP.setColor(fill);
            fillP.setAlpha(Math.round((fill >>> 24) * alpha / 255f));
            c.drawPath(path, fillP);
        }
        if (strokePx > 0f && (strokeColor >>> 24) != 0) {
            strokeP.setStrokeWidth(strokePx);
            strokeP.setColor(strokeColor);
            strokeP.setAlpha(Math.round((strokeColor >>> 24) * alpha / 255f));
            strokeP.setPathEffect(dashOnPx > 0f ? new DashPathEffect(new float[]{dashOnPx, dashOffPx}, 0f) : null);
            c.drawPath(strokePath, strokeP); // inside the bounds: never cut off at the edge
        }
    }

    @Override
    public void getOutline(Outline o) {
        Rect b = getBounds();
        if (b.isEmpty()) {
            o.setEmpty();
            return;
        }
        if (oval) o.setOval(b);
        else if (uniform) o.setRoundRect(b, uniformR);
        else if (Build.VERSION.SDK_INT >= 30) o.setPath(path);
        else o.setConvexPath(path);
        o.setAlpha(alpha / 255f);
    }

    @Override
    public void setAlpha(int a) {
        alpha = a;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter f) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
