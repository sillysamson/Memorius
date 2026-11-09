package com.memorius.app;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

// One card material for every state (standby, preview, full view).
//   light mode: a soft curved shadow under the card; dark mode: a soft curved highlight on the top edge
//   chosen textures (paper, concrete, grain) are laid over the surface
//   the colour strip on the start edge is painted here, and the corner radius can be animated
final class CardDrawable extends Drawable {
    static final float STANDBY_DP = 24f, PREVIEW_DP = 28f, FULL_DP = 32f;

    // room at the bottom of the card for the shadow (light mode only)
    static int inset() {
        return Ui.isDark ? 0 : Ui.dp(12);
    }

    private final int inset = inset();
    private float radius = Ui.dp(STANDBY_DP);
    private final float strip = Ui.dp(6);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint[] tex = new Paint[3];
    private final RectF body = new RectF();
    private final Path clip = new Path();
    private RadialGradient shadow, glow;
    private LinearGradient edge;
    private float shadowScale, glowScale, shadowR, glowR;

    CardDrawable(int tint) {
        fill.setColor(Ui.mix(Ui.bg, tint, Ui.contrast ? 0.14f : 0.22f));
        barP.setColor(tint);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, Ui.density * (Ui.contrast ? 1.25f : 0.75f)));
        if (Ui.texPaper) tex[0] = Textures.paint(Textures.PAPER, 1f);
        if (Ui.texConcrete) tex[1] = Textures.paint(Textures.CONCRETE, 1f);
        if (Ui.texGrain) tex[2] = Textures.paint(Textures.GRAIN, 1f);
    }

    // corner radius in dp; the card morph calls this every frame so the corners change smoothly
    void setRadiusDp(float dp) {
        radius = Ui.dp(dp);
        rebuild();
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect b) {
        body.set(b.left, b.top, b.right, b.bottom - inset);
        rebuild();
    }

    private void rebuild() {
        if (body.isEmpty()) return;
        clip.reset();
        clip.addRoundRect(body, radius, radius, Path.Direction.CW);
        float w = Math.max(1f, body.width());
        if (!Ui.isDark) {
            shadowR = w * 0.46f;
            shadowScale = Math.max(1f, inset * 1.2f) / shadowR;
            shadow = new RadialGradient(0, 0, shadowR, new int[]{0x34000000, 0x16000000, 0x00000000},
                    new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP);
        } else {
            glowR = w * 0.62f;
            glowScale = Math.min(body.height() * 0.5f, radius * 2.5f) / glowR;
            glow = new RadialGradient(0, 0, glowR, new int[]{0x26FFFFFF, 0x0DFFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP);
            edge = new LinearGradient(0, body.top, 0, body.bottom, 0x40FFFFFF, 0x0AFFFFFF, Shader.TileMode.CLAMP);
        }
    }

    @Override
    public void draw(Canvas c) {
        if (body.isEmpty()) return;
        if (!Ui.isDark && shadow != null) {
            c.save();
            c.translate(body.centerX(), body.bottom - inset * 0.2f);
            c.scale(1f, shadowScale);
            fx.setShader(shadow);
            c.drawCircle(0, 0, shadowR, fx);
            c.restore();
        }
        c.drawRoundRect(body, radius, radius, fill);

        c.save();
        c.clipPath(clip);
        boolean rtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        if (rtl) c.drawRect(body.right - strip, body.top, body.right, body.bottom, barP);
        else c.drawRect(body.left, body.top, body.left + strip, body.bottom, barP);
        for (Paint t : tex) if (t != null) c.drawRect(body, t);
        if (Ui.isDark && glow != null) {
            c.translate(body.centerX(), body.top);
            c.scale(1f, glowScale);
            fx.setShader(glow);
            c.drawCircle(0, 0, glowR, fx);
        }
        c.restore();

        if (Ui.isDark && edge != null && !Ui.contrast) {
            stroke.setShader(edge);
        } else {
            stroke.setShader(null);
            stroke.setColor(Ui.line);
        }
        c.drawRoundRect(body, radius, radius, stroke);
    }

    @Override
    public void getOutline(Outline o) {
        Rect b = getBounds();
        o.setRoundRect(b.left, b.top, b.right, b.bottom - inset, radius);
        o.setAlpha(1f);
    }

    @Override
    public void setAlpha(int a) {
    }

    @Override
    public void setColorFilter(ColorFilter f) {
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
