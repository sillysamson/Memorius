package com.memorius.app;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;

// The round white thumb of the liquid toggle and slider.
// At rest: a white capsule with a soft shadow. Held: it swells, turns into clear glass that magnifies
// and bends what is under it (the track), grows a highlight and an inner shadow, and stretches with speed.
final class GlassThumb {
    interface Painter {
        // draw what sits under the thumb, in the view's own coordinates
        void paint(Canvas c, float press);
    }

    private Object layerNode, shader;
    private boolean shaderTried, broken;
    private final Paint surf = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tr = new RectF();
    private final Path clip = new Path();

    void draw(Canvas c, int vw, int vh, float left, float top, float w, float h, float press, float vel, float velDiv,
              float pressedScale, float lensRimDp, float lensAmountDp, Painter painter) {
        float p = LiquidMath.clamp(press, 0f, 1.2f);
        float s = LiquidMath.lerp(1f, pressedScale, p);
        float v = vel / velDiv;
        float sx = LiquidMath.stretchX(s, v), sy = LiquidMath.stretchY(s, v);
        float cx = left + w / 2f, cy = top + h / 2f;
        tr.set(cx - w * sx / 2f, cy - h * sy / 2f, cx + w * sx / 2f, cy + h * sy / 2f);
        float rad = Math.min(tr.width(), tr.height()) / 2f;
        clip.reset();
        clip.addRoundRect(tr, rad, rad, Path.Direction.CW);
        float pc = Math.min(1f, p);

        // soft shadow (4 dp, 5% black), a little deeper while held
        fx.setShader(new RadialGradient(0, 0, tr.width() * 0.55f,
                new int[]{(int) ((0x0D + 0x18 * pc)) << 24, 0x00000000}, null, Shader.TileMode.CLAMP));
        c.save();
        c.translate(cx, tr.bottom - Ui.dp(2));
        c.scale(1f, 0.3f);
        c.drawCircle(0, 0, tr.width() * 0.55f, fx);
        c.restore();

        c.save();
        c.clipPath(clip);
        boolean layered = false;
        // The thumb is clear glass while held: it shows the track slightly magnified (30% of the swell),
        // bent a little at the rim. One lens, no colour splitting, one highlight.
        float mx = 1f + (sx - 1f) * 0.3f, my = 1f + (sy - 1f) * 0.3f;
        if (pc > 0.02f && Build.VERSION.SDK_INT >= 31 && c.isHardwareAccelerated() && !broken) {
            try {
                if (layerNode == null) layerNode = Glass31.newNode();
                Canvas lc = Glass31.begin(layerNode, vw, vh);
                painter.paint(lc, pc);
                Glass31.end(layerNode);
                Object eff = null;
                if (Build.VERSION.SDK_INT >= 33) {
                    if (!shaderTried) {
                        shaderTried = true;
                        shader = Glass33.makeShader();
                    }
                    // lens geometry is that of the thumb after the same magnification the canvas applies
                    float hw = w * mx / 2f, hh = h * my / 2f;
                    eff = Glass33.lens(shader, cx - hw, cy - hh, cx + hw, cy + hh, hh,
                            Ui.dp(lensRimDp) * pc + 1f, Ui.dp(lensAmountDp) * pc, 0f);
                }
                Glass31.setLens(layerNode, eff);
                c.save();
                c.scale(mx, my, cx, cy);
                Glass31.draw(c, layerNode);
                c.restore();
                layered = true;
            } catch (Throwable t) {
                broken = true;
            }
        }
        if (!layered && pc > 0.02f) { // older phones: the same view without bending
            c.save();
            c.scale(mx, my, cx, cy);
            painter.paint(c, pc);
            c.restore();
        }
        surf.setColor(0xFFFFFFFF);
        surf.setAlpha((int) (255 * (1f - pc))); // white at rest, clear when held
        c.drawRect(tr, surf);
        c.restore();

        if (pc > 0.02f) { // a single thin highlight along the rim (no inner shadow, no second outline)
            float sw = Math.max(1f, Ui.density);
            int a = (int) (0x90 * pc);
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(sw);
            rim.setAlpha(255);
            rim.setShader(new LinearGradient(tr.left, tr.top, tr.right, tr.bottom,
                    new int[]{(a << 24) | 0xFFFFFF, 0x10FFFFFF, (a / 2 << 24) | 0xFFFFFF}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            c.drawRoundRect(tr.left + sw / 2, tr.top + sw / 2, tr.right - sw / 2, tr.bottom - sw / 2, rad, rad, rim);
        }
    }
}
