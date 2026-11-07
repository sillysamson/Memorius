package com.memorius.app;

import android.content.Context;
import android.view.View;
import android.widget.ScrollView;

class MaxHeightScroll extends ScrollView {
    private final int maxH;

    MaxHeightScroll(Context c, int maxH) {
        super(c);
        this.maxH = maxH;
        setVerticalScrollBarEnabled(false);
    }

    @Override
    protected void onMeasure(int w, int h) {
        int cap = Math.min(maxH, View.MeasureSpec.getSize(h));
        if (cap <= 0) cap = maxH;
        super.onMeasure(w, View.MeasureSpec.makeMeasureSpec(cap, View.MeasureSpec.AT_MOST));
    }
}
