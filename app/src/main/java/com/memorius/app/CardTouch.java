package com.memorius.app;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

// Touch handling for one list card: tap, or press-and-hold followed by a drag.
// Drag positions use raw screen coordinates, so nothing jumps when views move under the finger.
final class CardTouch implements View.OnTouchListener {
    interface Listener {
        void onHold(View v, float rawY);

        void onDrag(float rawY);

        void onRelease(boolean cancelled);
    }

    static final long HOLD_MS = 500; // press and hold for half a second to preview

    private final Listener l;
    private boolean holding, moved;
    private float downX, downY, lastY;
    private Runnable holdTask;

    CardTouch(Listener l) {
        this.l = l;
    }

    @Override
    public boolean onTouch(final View v, MotionEvent e) {
        int slop = ViewConfiguration.get(v.getContext()).getScaledTouchSlop();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getRawX();
                downY = e.getRawY();
                lastY = downY;
                holding = false;
                moved = false;
                Motion.scale(v, 0.97f, Motion.EFFECT_STIFF, 0.9f);
                holdTask = () -> {
                    holding = true;
                    if (v.getParent() != null) v.getParent().requestDisallowInterceptTouchEvent(true);
                    Motion.scale(v, 1f, Motion.EFFECT_STIFF, 0.9f);
                    Ui.haptic(v, 1);
                    l.onHold(v, lastY);
                };
                v.postDelayed(holdTask, HOLD_MS);
                return true;
            case MotionEvent.ACTION_MOVE:
                lastY = e.getRawY();
                if (holding) {
                    l.onDrag(lastY);
                    return true;
                }
                if (!moved && (Math.abs(e.getRawX() - downX) > slop || Math.abs(e.getRawY() - downY) > slop)) {
                    moved = true;
                    v.removeCallbacks(holdTask);
                    Motion.scale(v, 1f, Motion.EFFECT_STIFF, 0.9f);
                }
                return true;
            case MotionEvent.ACTION_UP:
                v.removeCallbacks(holdTask);
                Motion.scale(v, 1f, Motion.EFFECT_STIFF, 0.9f);
                if (holding) {
                    holding = false;
                    l.onRelease(false);
                } else if (!moved) {
                    v.performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                v.removeCallbacks(holdTask);
                Motion.scale(v, 1f, Motion.EFFECT_STIFF, 0.9f);
                if (holding) {
                    holding = false;
                    l.onRelease(true);
                }
                return true;
        }
        return false;
    }
}
