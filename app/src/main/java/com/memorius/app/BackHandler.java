package com.memorius.app;

import android.os.Build;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;

// Predictive back: while something can be closed inside the app we take the back gesture and
// preview it (the top layer shrinks as you swipe). When nothing is left, we step aside so
// Android plays its own "back to home" preview.
final class BackHandler {
    private final MainActivity a;
    private OnBackInvokedCallback callback;
    private boolean registered = false;

    BackHandler(MainActivity a) {
        this.a = a;
    }

    void update(boolean need) {
        if (Build.VERSION.SDK_INT < 33) return;
        if (need && !registered) {
            if (Build.VERSION.SDK_INT >= 34) callback = new Anim();
            else callback = () -> a.handleBack();
            a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            registered = true;
        } else if (!need && registered) {
            a.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(callback);
            registered = false;
        }
    }

    private class Anim implements OnBackAnimationCallback {
        @Override
        public void onBackStarted(@NonNull BackEvent e) {
        }

        @Override
        public void onBackProgressed(@NonNull BackEvent e) {
            a.backProgress(e.getProgress(), e.getSwipeEdge() == BackEvent.EDGE_LEFT);
        }

        @Override
        public void onBackCancelled() {
            a.backCancelled();
        }

        @Override
        public void onBackInvoked() {
            a.backReset();
            a.handleBack();
        }
    }
}
