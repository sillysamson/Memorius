package com.memorius.app;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

// One card material for every state (standby, preview, full view), drawn in the current Style:
//   Flat       plain 2D surface and a colour strip. No shadow, gradient or texture.
//   Humanist   leaf-shaped corners, a second sheet peeking out underneath, a painted blob, paper texture
//   Material   tonal surface, a soft elevation shadow (light mode), a pill as the colour marker
//   Swiss      square, a heavy rule on top, a solid colour bar on the start edge
//   Corporate  slightly translucent surface, a very soft large shadow, a small dot as the colour marker
// Base colour comes from the theme background (see Style.cardBase). No outlines or borders in any style.
final class CardDrawable extends Drawable {
    static final float STANDBY_DP = 24f, PREVIEW_DP = 28f, FULL_DP = 32f;

    // room at the bottom of the card for its shadow or second sheet
    static int inset() {
        switch (Style.id) {
            case Style.HUMANIST: return Ui.dp(6);
            case Style.MATERIAL: return Ui.isDark ? 0 : Ui.dp(8);
            case Style.CORPORATE: return Ui.isDark ? 0 : Ui.dp(14);
            default: return 0;
        }
    }

    // width reserved at the start edge for the colour marker (the card text begins after it)
    static int stripSpace() {
        switch (Style.id) {
            case Style.MATERIAL: return Ui.dp(14);
            case Style.CORPORATE: return 0;
            default: return Ui.dp(10);
        }
    }

    static int base() {
        int primary = Style.primary(Ui.isDark, Ui.fg);
        return Style.cardBase(Ui.bg, Ui.fg, Ui.isDark, primary);
    }

    private final int tint;
    private final int inset = inset();
    private float radius = Ui.dp(Style.cardRadius(STANDBY_DP));
    private final int style = Style.id;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fx = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint[] tex = new Paint[2];
    private final RectF body = new RectF(), tmp = new RectF();
    private final Path clip = new Path(), blob = new Path();
    private final float[] radii = new float[8];
    private RadialGradient shadow;
    private float shadowScale, shadowR;

    CardDrawable(int tint) {
        this.tint = tint;
        fill.setColor(base());
        if (style == Style.CORPORATE) fill.setAlpha(0xEB); // subtle translucency
        barP.setColor(tint);
        if (Ui.texPaper) tex[0] = Textures.paint(Textures.PAPER, 1f);
        if (Ui.texGrain) tex[1] = Textures.paint(Textures.GRAIN, 1f);
    }

    // corner radius in dp (24 / 28 / 32), mapped through the style; the morph calls this every frame
    void setRadiusDp(float dp) {
        radius = Ui.dp(Style.cardRadius(dp));
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
        Surface.radiiFor(body.width(), body.height(), radius, style == Style.HUMANIST, false, radii);
        clip.reset();
        clip.addRoundRect(body, radii, Path.Direction.CW);
        float w = Math.max(1f, body.width());
        if (inset > 0 && (style == Style.MATERIAL || style == Style.CORPORATE)) {
            shadowR = w * (style == Style.CORPORATE ? 0.5f : 0.46f);
            shadowScale = Math.max(1f, inset * (style == Style.CORPORATE ? 1.5f : 1.1f)) / shadowR;
            int a = style == Style.CORPORATE ? 0x1C : 0x28;
            shadow = new RadialGradient(0, 0, shadowR, new int[]{a << 24, (a / 2) << 24, 0x00000000},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        }
        blob.reset();
        if (style == Style.HUMANIST) { // a hand-painted blob in the card colour, top-end corner
            float s = Math.min(body.width(), body.height()) * 0.55f;
            float cx = body.right - s * 0.2f, cy = body.top + s * 0.1f;
            blob.moveTo(cx - s * 0.5f, cy);
            blob.cubicTo(cx - s * 0.5f, cy - s * 0.55f, cx + s * 0.1f, cy - s * 0.6f, cx + s * 0.45f, cy - s * 0.2f);
            blob.cubicTo(cx + s * 0.7f, cy + s * 0.2f, cx + s * 0.35f, cy + s * 0.65f, cx - s * 0.1f, cy + s * 0.55f);
            blob.cubicTo(cx - s * 0.45f, cy + s * 0.5f, cx - s * 0.5f, cy + s * 0.25f, cx - s * 0.5f, cy);
            blob.close();
        }
    }

    @Override
    public void draw(Canvas c) {
        if (body.isEmpty()) return;
        boolean rtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

        // ---- what sits under the card: shadow or a second sheet ----
        if (shadow != null && !Ui.isDark) {
            c.save();
            c.translate(body.centerX(), body.bottom - inset * 0.2f);
            c.scale(1f, shadowScale);
            fx.setShader(shadow);
            c.drawCircle(0, 0, shadowR, fx);
            c.restore();
        }
        if (style == Style.HUMANIST) { // the sheet underneath, slightly turned, like paper stacked on a desk
            fx.setShader(null);
            fx.setColor(Ui.mix(base(), Ui.fg, 0.13f));
            c.save();
            c.rotate(rtl ? -0.9f : 0.9f, body.centerX(), body.bottom);
            tmp.set(body.left + Ui.dp(3), body.top + Ui.dp(4), body.right - Ui.dp(3), body.bottom + Ui.dp(3));
            c.drawRoundRect(tmp, radius, radius, fx);
            c.restore();
        }

        // ---- the card ----
        c.drawPath(clip, fill);
        if (style == Style.FLAT) {
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(Math.max(1f, Ui.dp(0.7f)));
            border.setColor(Style.line(base(), Ui.fg, false));
            c.save();
            c.clipPath(clip);
            c.drawPath(clip, border);
            c.restore();
        }
        c.save();
        c.clipPath(clip);
        float mark = Ui.dp(6);
        switch (style) {
            case Style.HUMANIST:
                fx.setShader(null);
                fx.setColor((tint & 0x00FFFFFF) | 0x38000000);
                c.drawPath(blob, fx);
                barP.setColor(tint);
                if (rtl) c.drawRect(body.right - Ui.dp(5), body.top, body.right, body.bottom, barP);
                else c.drawRect(body.left, body.top, body.left + Ui.dp(5), body.bottom, barP);
                break;
            case Style.MATERIAL: { // pill marker, like a navigation indicator
                float pw = Ui.dp(4), top = body.top + Math.min(Ui.dp(20), body.height() * 0.2f);
                float x = rtl ? body.right - Ui.dp(8) - pw : body.left + Ui.dp(8);
                tmp.set(x, top, x + pw, body.bottom - (top - body.top));
                c.drawRoundRect(tmp, pw / 2f, pw / 2f, barP);
                break;
            }
            case Style.SWISS:
                if (rtl) c.drawRect(body.right - mark, body.top, body.right, body.bottom, barP);
                else c.drawRect(body.left, body.top, body.left + mark, body.bottom, barP);
                fx.setShader(null);
                fx.setColor(Ui.fg);
                c.drawRect(body.left, body.top, body.right, body.top + Ui.dp(2), fx); // the rule
                break;
            case Style.CORPORATE: {
                float r = Ui.dp(4);
                float x = rtl ? body.left + Ui.dp(22) : body.right - Ui.dp(22);
                c.drawCircle(x, body.bottom - Ui.dp(22), r, barP);
                break;
            }
            default: // Flat: inset rounded marker, following the card's corner contour.
                float mx = rtl ? body.right - Ui.dp(8) : body.left + Ui.dp(8);
                float my = body.top + Math.min(radius * 0.72f, body.height() * 0.22f);
                float mb = body.bottom - Math.min(radius * 0.72f, body.height() * 0.22f);
                tmp.set(mx, my, mx + Ui.dp(3), Math.max(my + Ui.dp(3), mb));
                c.drawRoundRect(tmp, Ui.dp(2), Ui.dp(2), barP);
        }
        for (Paint t : tex) if (t != null) c.drawRect(body, t);
        c.restore();
    }

    @Override
    public void getOutline(Outline o) {
        Rect b = getBounds();
        if (style == Style.HUMANIST) {
            if (Build.VERSION.SDK_INT >= 30) o.setPath(clip);
            else o.setConvexPath(clip);
        } else {
            o.setRoundRect(b.left, b.top, b.right, b.bottom - inset, radii[0]);
        }
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
