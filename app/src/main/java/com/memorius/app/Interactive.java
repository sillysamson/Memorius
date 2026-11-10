package com.memorius.app;

import android.view.MotionEvent;
import android.view.View;

import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

// The "liquid button" feel for any view: it swells while held, follows the finger a little
// (limited by tanh, so it never runs away), stretches the way it is pulled, and springs back.
// A soft glow can follow the touch point (read glowX / glowY / press when drawing).
final class Interactive {
    private final View v;
    private final FloatValueHolder ph = new FloatValueHolder(0f), oxh = new FloatValueHolder(0f), oyh = new FloatValueHolder(0f);
    private final SpringAnimation pressA = new SpringAnimation(ph), oxA = new SpringAnimation(oxh), oyA = new SpringAnimation(oyh);
    private float press, offX, offY, downRawX, downRawY;
    float glowX, glowY;

    Interactive(View view) {
        v = view;
        pressA.setSpring(new SpringForce(0f).setStiffness(400f).setDampingRatio(0.6f));
        pressA.setMinimumVisibleChange(0.001f);
        pressA.addUpdateListener((a, val, vel) -> {
            press = val;
            apply();
        });
        oxA.setSpring(new SpringForce(0f).setStiffness(300f).setDampingRatio(0.5f));
        oyA.setSpring(new SpringForce(0f).setStiffness(300f).setDampingRatio(0.5f));
        oxA.setMinimumVisibleChange(0.5f);
        oyA.setMinimumVisibleChange(0.5f);
        oxA.addUpdateListener((a, val, vel) -> {
            offX = val;
            apply();
        });
        oyA.addUpdateListener((a, val, vel) -> {
            offY = val;
            apply();
        });
    }

    float press() {
        return press;
    }

    // feed every touch event; always lets the view handle the event itself afterwards
    void handle(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = e.getRawX();
                downRawY = e.getRawY();
                glowX = e.getX();
                glowY = e.getY();
                oxA.cancel();
                oyA.cancel();
                offX = 0f;
                offY = 0f;
                Motion.to(pressA, 1f, 400f, 0.6f);
                break;
            case MotionEvent.ACTION_MOVE:
                offX = e.getRawX() - downRawX;
                offY = e.getRawY() - downRawY;
                glowX = e.getX();
                glowY = e.getY();
                apply();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                Motion.to(pressA, 0f, 400f, 0.6f);
                oxh.setValue(offX);
                oyh.setValue(offY);
                Motion.to(oxA, 0f, 300f, 0.5f);
                Motion.to(oyA, 0f, 300f, 0.5f);
                break;
        }
    }

    private void apply() {
        float w = v.getWidth(), h = v.getHeight();
        if (w <= 0 || h <= 0) return;
        float maxOffset = Math.min(w, h);
        float grow = Ui.dp(4) / h;
        float scale = LiquidMath.lerp(1f, 1f + grow, press);
        v.setTranslationX(LiquidMath.buttonTranslation(offX, maxOffset));
        v.setTranslationY(LiquidMath.buttonTranslation(offY, maxOffset));
        v.setScaleX(LiquidMath.buttonScaleX(scale, grow, offX, offY, w, h));
        v.setScaleY(LiquidMath.buttonScaleY(scale, grow, offX, offY, w, h));
        v.invalidate();
    }
}
