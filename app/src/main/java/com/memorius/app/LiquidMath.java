package com.memorius.app;

// The numbers behind the liquid bottom bar, kept free of Android classes so they can be tested.
final class LiquidMath {
    private LiquidMath() {
    }

    static final float PRESSED_SCALE = 78f / 56f; // thumb grows to 78 from 56 while held

    static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // tab slot (0 = left edge) that a tab index sits in: mirrored in right-to-left layouts
    static int slot(int index, int count, boolean rtl) {
        return rtl ? count - 1 - index : index;
    }

    static float tabWidth(float barWidth, float pad, int count) {
        return (barWidth - 2f * pad) / count;
    }

    static float thumbLeft(float value, float barWidth, float pad, int count, boolean rtl) {
        float tw = tabWidth(barWidth, pad, count);
        float slotPos = rtl ? (count - 1 - value) : value;
        return pad + slotPos * tw;
    }

    // which tab is under x
    static int tabAt(float x, float barWidth, float pad, int count, boolean rtl) {
        float tw = tabWidth(barWidth, pad, count);
        int s = (int) Math.floor((x - pad) / tw);
        s = Math.max(0, Math.min(count - 1, s));
        return rtl ? count - 1 - s : s;
    }

    // dragging by dx pixels changes the value by dx / tabWidth (the other way round in right-to-left)
    static float dragValue(float value, float dx, float tabW, boolean rtl, int count) {
        return clamp(value + dx / tabW * (rtl ? -1f : 1f), 0f, count - 1f);
    }

    // Compose's EaseOut = cubic-bezier(0, 0, 0.58, 1)
    static float easeOut(float t) {
        t = clamp(t, 0f, 1f);
        float lo = 0f, hi = 1f, u = t;
        for (int i = 0; i < 30; i++) {
            u = (lo + hi) / 2f;
            float x = 3f * (1 - u) * (1 - u) * u * 0f + 3f * (1 - u) * u * u * 0.58f + u * u * u;
            if (x < t) lo = u;
            else hi = u;
        }
        return 3f * (1 - u) * (1 - u) * u * 0f + 3f * (1 - u) * u * u * 1f + u * u * u;
    }

    // The whole bar leans into a drag: up to 4 dp, easing out
    static float panelOffset(float dragPx, float barWidth, float maxPx) {
        float f = clamp(dragPx / barWidth, -1f, 1f);
        return maxPx * Math.signum(f) * easeOut(Math.abs(f));
    }

    // thumb squash and stretch from speed (value units per second / 10)
    static float stretchX(float scale, float v) {
        return scale / (1f - clamp(v * 0.75f, -0.2f, 0.2f));
    }

    static float stretchY(float scale, float v) {
        return scale * (1f - clamp(v * 0.25f, -0.2f, 0.2f));
    }

    static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // ---------- liquid button: it follows the finger a little and stretches the way it is pulled ----------
    static float buttonTranslation(float offset, float maxOffset) {
        return maxOffset * (float) Math.tanh(0.05f * offset / maxOffset);
    }

    static float buttonScaleX(float scale, float maxDragScale, float offX, float offY, float w, float h) {
        float angle = (float) Math.atan2(offY, offX);
        return scale + maxDragScale * Math.abs((float) Math.cos(angle) * offX / Math.max(w, h)) * Math.min(w / h, 1f);
    }

    static float buttonScaleY(float scale, float maxDragScale, float offX, float offY, float w, float h) {
        float angle = (float) Math.atan2(offY, offX);
        return scale + maxDragScale * Math.abs((float) Math.sin(angle) * offY / Math.max(w, h)) * Math.min(h / w, 1f);
    }

    // ---------- liquid toggle ----------
    // left edge of the thumb for a value 0..1 (it travels dragW, starting pad from the edge)
    static float toggleThumbLeft(float value, float viewW, float thumbW, float pad, float dragW, boolean rtl) {
        float travel = pad + dragW * value;
        return rtl ? viewW - thumbW - travel : travel;
    }

    static float toggleDrag(float fraction, float dx, float dragW, boolean rtl) {
        return clamp(fraction + (rtl ? -1f : 1f) * dx / dragW, 0f, 1f);
    }

    // ---------- liquid slider ----------
    // The thumb centre may reach 10 dp past each end of the track (thumbW / 4)
    static float sliderThumbOffset(float progress, float trackW, float thumbW) {
        return clamp(-thumbW / 2f + trackW * progress, -thumbW / 4f, trackW - thumbW * 3f / 4f);
    }

    static float sliderThumbLeft(float progress, float trackLeft, float trackW, float thumbW, boolean rtl) {
        float x = sliderThumbOffset(progress, trackW, thumbW);
        return rtl ? trackLeft + trackW - x - thumbW : trackLeft + x;
    }

    static float sliderValueAt(float x, float trackLeft, float trackW, float min, float max, boolean rtl) {
        float f = clamp((x - trackLeft) / trackW, 0f, 1f);
        if (rtl) f = 1f - f;
        return min + (max - min) * f;
    }
}
