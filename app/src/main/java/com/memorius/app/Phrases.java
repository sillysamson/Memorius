package com.memorius.app;

import java.util.Random;

// Funny empty-state lines. A new one is picked every time a page is drawn.
final class Phrases {
    private Phrases() {
    }

    private static final String[] BOTH = {
            "clean as a hospital",
            "cleaner than it has any right to be",
            "not a single problem",
            "suspiciously peaceful around here",
            "as empty as your excuses",
            "the calm before the academic storm",
            "you sure you're a student?",
            "cleaner than a crime scene",
            "a little too clean, don't you think?"
    };
    private static final String[] HOME = {
            "hmm, it seems like u did finish all ur work... or are u?",
            "not even a deadline to worry about"
    };
    private static final String[] SCHEDULE = {
            "how suspicious. a schedule this clean, really?",
            "did you forget you have school?"
    };

    private static final Random R = new Random();

    private static String pick(String[] extra) {
        int i = R.nextInt(BOTH.length + extra.length);
        return i < extra.length ? extra[i] : BOTH[i - extra.length];
    }

    static String home() {
        return pick(HOME);
    }

    static String schedule() {
        return pick(SCHEDULE);
    }
}
