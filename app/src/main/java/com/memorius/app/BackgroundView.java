package com.memorius.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

// Texture overlay for the whole page background (paper, concrete, grain). Drawn above the page
// colour, below the content. Hidden when no texture is chosen.
final class BackgroundView extends View {
    private Paint[] tex = new Paint[2];

    BackgroundView(Context c) {
        super(c);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void recolor() {
        tex = new Paint[2];
        if (Ui.texPaper) tex[0] = Textures.paint(Textures.PAPER, 2f);
        if (Ui.texGrain) tex[1] = Textures.paint(Textures.GRAIN, 1.5f);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        for (Paint p : tex) if (p != null) c.drawRect(0, 0, getWidth(), getHeight(), p);
    }
}
