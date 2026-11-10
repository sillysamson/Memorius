package com.memorius.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;

// Liquid glass button: capsule, 48 dp high, glossy rim and sheen. Interactive: it swells when
// pressed, follows the finger a little and stretches the way it is pulled (see Interactive).
// A filled button is tinted with the foreground colour.
final class GlassButton extends TextView {
    private final boolean solid;
    private final Interactive inter;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rf = new RectF();

    GlassButton(Context c, CharSequence label, boolean solid) {
        super(c);
        this.solid = solid;
        inter = new Interactive(this);
        setText(label);
        setTextSize(15);
        setTextColor(solid ? Ui.bg : Ui.fg);
        setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        setIncludeFontPadding(false);
        setGravity(Gravity.CENTER);
        setPadding(Ui.dp(20), Ui.dp(12), Ui.dp(20), Ui.dp(12));
        setMinHeight(Ui.dp(48));
        setClickable(true);
        setFocusable(true);
        rim.setStyle(Paint.Style.STROKE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (isEnabled()) inter.handle(e);
        return super.onTouchEvent(e);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), r = h / 2f;
        rf.set(0, 0, w, h);
        boolean dark = Ui.isDark;
        // body
        if (solid) fill.setColor(Ui.alpha(Ui.fg, 0xE6));
        else fill.setColor(dark ? 0x22FFFFFF : 0x14000000);
        c.drawRoundRect(rf, r, r, fill);
        // glossy sheen over the top half
        sheen.setShader(new LinearGradient(0, 0, 0, h * 0.6f,
                dark ? 0x30FFFFFF : 0x60FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.save();
        c.clipRect(0, 0, w, h * 0.55f);
        c.drawRoundRect(rf, r, r, sheen);
        c.restore();
        // glow under the finger
        float pr = inter.press();
        if (pr > 0.02f) {
            glow.setShader(new RadialGradient(inter.glowX, inter.glowY, Math.max(w, h) * 0.7f,
                    new int[]{(int) (0x40 * Math.min(1f, pr)) << 24 | 0xFFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            c.drawRoundRect(rf, r, r, glow);
        }
        // thin highlight along the rim
        float sw = Math.max(1f, Ui.density * (Ui.contrast ? 1.5f : 1f));
        int hi = dark ? 0x55FFFFFF : 0xC0FFFFFF, lo = dark ? 0x14FFFFFF : 0x30000000;
        if (Ui.contrast) {
            rim.setShader(null);
            rim.setColor(Ui.line);
        } else {
            rim.setShader(new LinearGradient(0, 0, w, h, new int[]{hi, lo, hi}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        }
        rim.setStrokeWidth(sw);
        c.drawRoundRect(sw / 2f, sw / 2f, w - sw / 2f, h - sw / 2f, r, r, rim);
        super.onDraw(c);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName("android.widget.Button");
    }
}
