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
import android.view.View;
import android.widget.FrameLayout;

// A frosted glass capsule. It records the page behind it, blurs it (Android 12+) and adds a lens
// refraction at the rim (Android 13+), then paints a light tint and a thin highlight on top.
// On older phones, or when switched off, it is a plain translucent capsule.
class GlassFrame extends FrameLayout {
    private View root;
    private View[] sources = new View[0];
    private boolean glass = true, broken = false, dark = true;
    protected boolean fxActive = false;
    protected Object bgNode;
    private float blurDp = 6f, lensRimDp = 12f, lensAmountDp = 8f;
    private Interactive inter;
    private Object lensShader;
    private boolean lensTried = false;
    protected final RectF rf = new RectF();
    protected final Path capsule = new Path();
    private final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowP = new Paint(Paint.ANTI_ALIAS_FLAG);

    GlassFrame(Context c) {
        super(c);
        setWillNotDraw(false);
        setClipChildren(false);
        rimP.setStyle(Paint.Style.STROKE);
    }

    // root = the page root; sources = the views that make up the page behind this glass
    void setGlassSource(View root, View... sources) {
        this.root = root;
        this.sources = sources;
    }

    void setGlass(boolean on) {
        glass = on;
        invalidate();
    }

    void setDark(boolean d) {
        dark = d;
        invalidate();
    }

    // glass strength: blur radius and lens (rim depth, bend amount), all in dp
    void setGlassLook(float blurDp, float lensRimDp, float lensAmountDp) {
        this.blurDp = blurDp;
        this.lensRimDp = lensRimDp;
        this.lensAmountDp = lensAmountDp;
        invalidate();
    }

    // optional: a glow that follows the finger (see Interactive)
    void setInteractive(Interactive i) {
        inter = i;
    }

    boolean isDark() {
        return dark;
    }

    // call when something behind the glass changed (scroll, page move...)
    void refreshBackdrop() {
        invalidate();
    }

    protected boolean effects() {
        return glass && Style.glass() && !broken && root != null && Build.VERSION.SDK_INT >= 31;
    }

    // corner radius of this surface in the current style (px)
    protected float radius() {
        float h = Math.min(getWidth(), getHeight());
        boolean square = Math.abs(getWidth() - getHeight()) < Ui.dp(4); // the ⋯ button
        switch (Style.id) {
            case Style.SWISS: return 0f;
            case Style.MATERIAL: return Math.min(h / 2f, Ui.dp(square ? 20 : 24));
            default: return h / 2f;
        }
    }

    // radius at the top-start corner, for shapes that grow out of this one
    float cornerPx() {
        return Math.min(radius(), Math.min(getWidth(), getHeight()) / 2f);
    }

    private final float[] radii = new float[8];
    private RadialGradient flatShadow;

    protected void drawGlass(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float r = radius();
        rf.set(0, 0, w, h);
        Surface.radiiFor(w, h, r, Style.organic(), false, radii);
        capsule.reset();
        capsule.addRoundRect(rf, radii, Path.Direction.CW);
        fxActive = false;
        if (!Style.glass()) {
            drawFlat(c, w, h);
            return;
        }
        if (effects() && c.isHardwareAccelerated()) {
            try {
                if (bgNode == null) bgNode = Glass31.newNode();
                Glass31.record(bgNode, w, h, this, root, root.getBackground(), sources);
                Object lens = null;
                if (Build.VERSION.SDK_INT >= 33) {
                    if (!lensTried) {
                        lensTried = true;
                        lensShader = Glass33.makeShader();
                    }
                    lens = Glass33.lens(lensShader, 0, 0, w, h, r, Ui.dp(lensRimDp), Ui.dp(lensAmountDp), 0f);
                }
                int key = 31 * (31 * (31 * w + h) + Float.floatToIntBits(blurDp)) + (lens != null ? 1 : 0);
                if (key != effectKey) { // rebuild only when something changed: a fresh effect every frame can flicker
                    effectKey = key;
                    Glass31.setEffect(bgNode, Ui.dp(blurDp), 1.5f, lens);
                }
                c.save();
                c.clipPath(capsule);
                Glass31.draw(c, bgNode);
                c.restore();
                fxActive = true;
            } catch (Throwable t) {
                broken = true; // never crash the app over a visual effect
            }
        }
        // tint: 40% light / dark over real glass, more solid when there is nothing to see through
        int base = dark ? 0x121212 : 0xFAFAFA;
        fillP.setColor((fxActive ? 0x66 : 0xE6) << 24 | base);
        c.drawPath(capsule, fillP);
        // thin highlight along the rim, brightest on the top-left
        float sw = Math.max(1f, Ui.density);
        rimP.setStrokeWidth(sw);
        int hi = dark ? 0x40FFFFFF : 0xB0FFFFFF, lo = dark ? 0x10FFFFFF : 0x30FFFFFF;
        rimP.setShader(new LinearGradient(0, 0, w, h, new int[]{hi, lo, hi}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        rimPath.reset();
        float[] ri = new float[8];
        for (int i = 0; i < 8; i++) ri[i] = Math.max(0f, radii[i] - sw / 2f);
        rimPath.addRoundRect(sw / 2f, sw / 2f, w - sw / 2f, h - sw / 2f, ri, Path.Direction.CW);
        c.drawPath(rimPath, rimP); // same path family as the fill: the highlight can never be cut by a corner
        if (inter != null && inter.press() > 0.02f) { // soft glow under the finger
            glowP.setShader(new android.graphics.RadialGradient(inter.glowX, inter.glowY, Math.max(w, h) * 0.8f,
                    new int[]{(int) (0x40 * Math.min(1f, inter.press())) << 24 | 0xFFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            c.save();
            c.clipPath(capsule);
            c.drawRect(rf, glowP);
            c.restore();
        }
    }

    private final Path rimPath = new Path();
    private int effectKey = 0;

    // Flat, Humanist, Material and Swiss: plain surfaces in their own shape language
    private void drawFlat(Canvas c, int w, int h) {
        int fillC;
        switch (Style.id) {
            case Style.HUMANIST:
            case Style.MATERIAL: fillC = CardDrawable.base(); break;
            case Style.SWISS: fillC = Ui.bg; break;
            default: fillC = Ui.surface;
        }
        if (Style.id == Style.MATERIAL && !dark) { // elevation level 2
            if (flatShadow == null) {
                float rr = Math.max(1f, w * 0.5f);
                flatShadow = new RadialGradient(0, 0, rr, new int[]{0x2A000000, 0x12000000, 0x00000000}, new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP);
            }
            c.save();
            c.translate(w / 2f, h + Ui.dp(2));
            c.scale(1f, Math.max(0.05f, Ui.dp(6) / (w * 0.5f)));
            glowP.setShader(flatShadow);
            c.drawCircle(0, 0, w * 0.5f, glowP);
            c.restore();
            flatShadow = null;
        }
        fillP.setColor(fillC);
        c.drawPath(capsule, fillP);
        if (Style.id == Style.SWISS) { // a heavy rule instead of a box
            fillP.setColor(Ui.fg);
            c.drawRect(0, 0, w, Ui.dp(2), fillP);
        }
        if (Style.id == Style.HUMANIST && Ui.texPaper) {
            c.save();
            c.clipPath(capsule);
            c.drawRect(rf, Textures.paint(Textures.PAPER, 1f));
            c.restore();
        }
    }

    protected void drawContent(Canvas c) {
        drawGlass(c);
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        drawContent(c);
        super.dispatchDraw(c);
    }
}
