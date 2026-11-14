package com.memorius.app;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;

// Native blur (Android 12+). Older phones use the existing dim fallback.
// Effects are quantized to half-pixel increments and shared, avoiding a new RenderEffect allocation
// for every animation frame while keeping the progression visually continuous.
final class Blur {
    private Blur() { }

    private static final SparseArray<RenderEffect> UNIFORM = new SparseArray<>();
    private static final SparseArray<RenderEffect> AXIAL = new SparseArray<>();

    static boolean supported() { return Build.VERSION.SDK_INT >= 31; }

    static float progressive(float t, float maxPx) {
        t = Math.max(0f, Math.min(1f, t));
        return maxPx * t * t * (3f - 2f * t);
    }

    private static float quantize(float px) {
        return Math.max(0f, Math.round(px * 2f) * 0.5f);
    }

    static void view(View v, float radiusPx) {
        if (v == null || Build.VERSION.SDK_INT < 31) return;
        float r = quantize(radiusPx);
        if (r < 0.5f) {
            if (v.getRenderEffect() != null) v.setRenderEffect(null);
            return;
        }
        int key = Math.round(r * 2f);
        RenderEffect effect = UNIFORM.get(key);
        if (effect == null) {
            effect = RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP);
            UNIFORM.put(key, effect);
        }
        if (v.getRenderEffect() != effect) v.setRenderEffect(effect);
    }

    static void axes(View v, float rx, float ry) {
        if (v == null || Build.VERSION.SDK_INT < 31) return;
        rx = quantize(rx);
        ry = quantize(ry);
        if (rx < 0.5f && ry < 0.5f) {
            if (v.getRenderEffect() != null) v.setRenderEffect(null);
            return;
        }
        int x = Math.round(rx * 2f), y = Math.round(ry * 2f);
        int key = (x << 16) | y;
        RenderEffect effect = AXIAL.get(key);
        if (effect == null) {
            effect = RenderEffect.createBlurEffect(Math.max(rx, 0.01f), Math.max(ry, 0.01f), Shader.TileMode.CLAMP);
            AXIAL.put(key, effect);
        }
        if (v.getRenderEffect() != effect) v.setRenderEffect(effect);
    }
}
