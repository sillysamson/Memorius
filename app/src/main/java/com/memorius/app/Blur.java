package com.memorius.app;

import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

// Native blur (Android 12+). Older phones get a darker dim instead.
final class Blur {
    private Blur() {
    }

    static boolean supported() {
        return Build.VERSION.SDK_INT >= 31;
    }

    // blur that follows speed: soft while moving, sharp when still. Honours the "Blur in motion" switch.
    static void motion(View v, float radiusPx) {
        if (!Ui.motionBlurOn) radiusPx = 0f;
        view(v, radiusPx);
    }

    static void view(View v, float radiusPx) {
        if (v == null || Build.VERSION.SDK_INT < 31) return;
        if (radiusPx < 0.5f) v.setRenderEffect(null);
        else v.setRenderEffect(RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP));
    }
}
