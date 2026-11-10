package com.memorius.app;

import android.view.View;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.HashMap;

// Spring motion in the Material 3 Expressive style. One set of numbers for the whole app.
final class Motion {
    // spatial: moves and scales, settles with at most a hint of overshoot
    static final float SPATIAL_STIFF = 700f, SPATIAL_DAMP = 0.85f;
    // effects: opacity and quick feedback, no overshoot
    static final float EFFECT_STIFF = 1500f, EFFECT_DAMP = 1f;

    // contextual timing (ms): hover, press, modal, page / scene
    static final long T_HOVER = 100, T_PRESS = 150, T_MODAL = 250, T_SCENE = 350;

    // true when the system "remove animations" setting or our "reduce motion" switch is on
    static boolean off = false;

    // start a spring toward `to`; with reduced motion it settles almost at once (very stiff, no wobble)
    static void to(SpringAnimation a, float to, float stiff, float damp) {
        a.getSpring().setStiffness(off ? 10000f : stiff);
        a.getSpring().setDampingRatio(off ? 1f : damp);
        a.animateToFinalPosition(to);
    }

    static long dur(long ms) {
        return off ? 0L : ms;
    }

    interface Progress {
        void at(float f);
    }

    @SuppressWarnings("unchecked")
    static void to(View v, DynamicAnimation.ViewProperty p, float target, float stiff, float damp) {
        if (off) {
            p.setValue(v, target);
            return;
        }
        HashMap<DynamicAnimation.ViewProperty, SpringAnimation> m =
                (HashMap<DynamicAnimation.ViewProperty, SpringAnimation>) v.getTag(R.id.m_anim);
        if (m == null) {
            m = new HashMap<>();
            v.setTag(R.id.m_anim, m);
        }
        SpringAnimation a = m.get(p);
        if (a == null) {
            a = new SpringAnimation(v, p);
            a.setSpring(new SpringForce());
            m.put(p, a);
        }
        a.cancel();
        a.getSpring().setFinalPosition(target).setStiffness(stiff).setDampingRatio(damp);
        a.start();
    }

    // stop everything running on this view and put it back to normal size
    @SuppressWarnings("unchecked")
    static void reset(View v) {
        HashMap<DynamicAnimation.ViewProperty, SpringAnimation> m =
                (HashMap<DynamicAnimation.ViewProperty, SpringAnimation>) v.getTag(R.id.m_anim);
        if (m != null) for (SpringAnimation a : m.values()) a.cancel();
        v.setScaleX(1f);
        v.setScaleY(1f);
    }

    static void spatial(View v, DynamicAnimation.ViewProperty p, float target) {
        to(v, p, target, SPATIAL_STIFF, SPATIAL_DAMP);
    }

    static void effect(View v, DynamicAnimation.ViewProperty p, float target) {
        to(v, p, target, EFFECT_STIFF, EFFECT_DAMP);
    }

    static void scale(View v, float s, float stiff, float damp) {
        to(v, DynamicAnimation.SCALE_X, s, stiff, damp);
        to(v, DynamicAnimation.SCALE_Y, s, stiff, damp);
    }

    // drives a 0..1 style progress with a spring (used for the card morph)
    static SpringAnimation progress(float from, final float to, float stiff, float damp, final Progress p, final Runnable end) {
        if (off) {
            p.at(to);
            if (end != null) end.run();
            return null;
        }
        SpringAnimation a = new SpringAnimation(new FloatValueHolder(from));
        a.setSpring(new SpringForce(to).setStiffness(stiff).setDampingRatio(damp));
        a.setMinimumVisibleChange(0.002f);
        a.addUpdateListener((anim, value, vel) -> p.at(value));
        a.addEndListener((anim, canceled, value, vel) -> {
            if (!canceled) {
                p.at(to);
                if (end != null) end.run();
            }
        });
        a.start();
        return a;
    }
}
