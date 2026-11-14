package com.memorius.app;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;

import java.util.Random;

// Procedurally drawn, tileable texture overlays (paper, grain) (no image files). Pixels are black or white with a
// low alpha, so they work on any colour. Each tile is made once and reused everywhere.
final class Textures {
    private Textures() {
    }

    static final int PAPER = 0, GRAIN = 1;
    private static final Bitmap[] TILES = new Bitmap[2];

    static Bitmap tile(int kind) {
        if (TILES[kind] == null) TILES[kind] = kind == PAPER ? paper() : grain();
        return TILES[kind];
    }

    static Paint paint(int kind, float scale) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader s = new BitmapShader(tile(kind), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        if (scale != 1f) {
            Matrix m = new Matrix();
            m.setScale(scale, scale);
            s.setLocalMatrix(m);
        }
        p.setShader(s);
        return p;
    }

    private static int px(boolean light, int a) {
        int g = light ? 255 : 0;
        return (a << 24) | (g << 16) | (g << 8) | g;
    }

    private static Bitmap grain() {
        int n = 96;
        int[] d = new int[n * n];
        Random r = new Random(7);
        for (int i = 0; i < d.length; i++) d[i] = px(r.nextBoolean(), r.nextInt(46));
        return Bitmap.createBitmap(d, n, n, Bitmap.Config.ARGB_8888);
    }

    // soft fibres: lots of short, slightly tilted hairlines over a very fine speckle
    private static Bitmap paper() {
        int n = 128;
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Random r = new Random(11);
        int[] d = new int[n * n];
        for (int i = 0; i < d.length; i++) d[i] = px(r.nextBoolean(), r.nextInt(14));
        b.setPixels(d, 0, n, 0, 0, n, n);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStrokeWidth(1f);
        for (int i = 0; i < 160; i++) {
            float x = r.nextInt(n), y = r.nextInt(n);
            double ang = (r.nextDouble() - 0.5) * 0.9 + (r.nextInt(5) == 0 ? Math.PI / 2 : 0);
            float len = 5 + r.nextInt(14);
            p.setColor(px(r.nextBoolean(), 14 + r.nextInt(16)));
            for (int dx = -n; dx <= n; dx += n) { // wrap so the tile has no seam
                for (int dy = -n; dy <= n; dy += n) {
                    c.drawLine(x + dx, y + dy, x + dx + (float) Math.cos(ang) * len, y + dy + (float) Math.sin(ang) * len, p);
                }
            }
        }
        return b;
    }
}
