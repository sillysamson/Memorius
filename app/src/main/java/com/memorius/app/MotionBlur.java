package com.memorius.app;

import android.view.View;

// Subtle motion blur that follows real speed. Feed it the view's position every frame:
// the blur is only along the direction the view is moving, grows with speed, is capped small,
// and goes away as soon as the view is still. Honours the "Blur in motion" switch.
final class MotionBlur {
    private static final float EXPOSURE = 0.0015f; // seconds-ish: blur px per px/s of speed
    private final View v;
    private float lastX, lastY, vx, vy;
    private long lastT;
    private boolean on;

    MotionBlur(View v) {
        this.v = v;
    }

    void sample(float x, float y) {
        long now = System.nanoTime();
        if (lastT == 0) {
            lastT = now;
            lastX = x;
            lastY = y;
            return;
        }
        float dt = (now - lastT) / 1e9f;
        if (dt < 0.004f) return;
        vx = vx * 0.5f + (x - lastX) / dt * 0.5f; // smoothed speed in px per second
        vy = vy * 0.5f + (y - lastY) / dt * 0.5f;
        lastX = x;
        lastY = y;
        lastT = now;
        if (!Ui.motionBlurOn || !Blur.supported()) return;
        float cap = Ui.dp(2.5f);
        float rx = Math.min(cap, Math.abs(vx) * EXPOSURE);
        float ry = Math.min(cap, Math.abs(vy) * EXPOSURE);
        float peak = Math.max(rx, ry);
        if (peak < (on ? 0.25f : 0.5f)) { // switch on at 0.5 px, off at 0.25 px: no flicker around the limit
            if (on) {
                on = false;
                Blur.axes(v, 0f, 0f);
            }
            return;
        }
        on = true;
        Blur.axes(v, rx, ry);
    }

    void stop() {
        lastT = 0;
        vx = 0f;
        vy = 0f;
        if (on) {
            on = false;
            Blur.axes(v, 0f, 0f);
        }
    }
}
