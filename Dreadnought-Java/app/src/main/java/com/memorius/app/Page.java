package com.memorius.app;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ScrollView;

abstract class Page {
    final MainActivity a;
    final FrameLayout root;

    Page(MainActivity a) {
        this.a = a;
        this.root = new FrameLayout(a);
    }

    abstract void refresh();

    static ScrollView scroller(Context c) {
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
        return sv;
    }
}
