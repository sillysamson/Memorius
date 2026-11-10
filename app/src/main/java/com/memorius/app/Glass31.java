package com.memorius.app;

import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

// Android 12+ only: records what is behind a glass surface and blurs it with RenderEffect.
// Kept in its own class so older phones never load it.
final class Glass31 {
    private Glass31() {
    }

    static Object newNode() {
        return new RenderNode("glass");
    }

    // Draws the page (background, texture layer, content) into the node, aligned with this view.
    static void record(Object node, int w, int h, View self, View root, Drawable bg, View[] sources) {
        RenderNode rn = (RenderNode) node;
        rn.setPosition(0, 0, w, h);
        RecordingCanvas rc = rn.beginRecording(w, h);
        int[] a = new int[2], b = new int[2];
        self.getLocationInWindow(a);
        root.getLocationInWindow(b);
        rc.translate(b[0] - a[0], b[1] - a[1]);
        if (bg != null) bg.draw(rc);
        for (View v : sources) if (v != null && v.getVisibility() == View.VISIBLE) v.draw(rc);
        rn.endRecording();
    }

    // blur + a little extra saturation ("vibrancy"), optionally followed by a lens effect
    static void setEffect(Object node, float blurPx, float saturation, Object lens) {
        RenderEffect blur = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(saturation);
        RenderEffect sat = RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(cm));
        RenderEffect e = RenderEffect.createChainEffect(sat, blur); // blur first, then saturation
        if (lens != null) e = RenderEffect.createChainEffect((RenderEffect) lens, e);
        ((RenderNode) node).setRenderEffect(e);
    }

    static void setLens(Object node, Object lens) {
        ((RenderNode) node).setRenderEffect(lens == null ? null : (RenderEffect) lens);
    }

    static Canvas begin(Object node, int w, int h) {
        RenderNode rn = (RenderNode) node;
        rn.setPosition(0, 0, w, h);
        return rn.beginRecording(w, h);
    }

    static void end(Object node) {
        ((RenderNode) node).endRecording();
    }

    static void draw(Canvas c, Object node) {
        c.drawRenderNode((RenderNode) node);
    }
}
