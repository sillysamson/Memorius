package com.memorius.app;

import java.util.ArrayList;
import java.util.List;

class FileRef {
    String name;
    String uri;

    FileRef(String name, String uri) {
        this.name = name;
        this.uri = uri;
    }
}

class Card {
    String id = Store.newId();
    String subject = "";
    String due = "";
    String type = "homework";
    String note = "";
    int color = 0;
    boolean done = false;
    List<FileRef> files = new ArrayList<>();
}

class Opus {
    String id = Store.newId();
    String title = "";
    String ext = "txt";
    String kind = "text"; // text | pdf
    String content = "";  // text, or path of a copied pdf
    int progress = 0;
}

class Slot {
    String id = Store.newId();
    int day = 0;
    String subject = "";
    String time = "";
    String note = "";
}

class Prefs {
    // reminders (independent of the notification settings)
    String freq = "due";      // off | due | dx | daily
    int daysBefore = 1;       // used by dx
    String time = "08:00:00"; // HH:mm:ss
    // notifications
    String soundGeneral = ""; // "" = default, "silent", or a ringtone uri
    String soundDeadline = "";
    int soundRev = 0;         // bumped when a sound changes (Android channel sounds cannot be edited)
    // directory
    String folder = "";
    String folderUri = "";
    // language
    int lang = 0;
    // theme
    int theme = 0;            // background colour number
    int gradient = -1;        // -1 = plain colour
    boolean texPaper = false, texConcrete = false, texGrain = true;
    boolean contrast = false;
    int fontPct = 100;        // 75..130
    // display
    boolean dark = true;
    boolean warmth = false;
    boolean battery = false;
    // accessibility
    boolean cb = false;
    boolean reduceMotion = false;
    boolean haptics = true;
    // experimental
    boolean windowBlur = true;
    boolean motionBlur = true;

    Prefs copy() {
        Prefs p = new Prefs();
        p.freq = freq;
        p.daysBefore = daysBefore;
        p.time = time;
        p.soundGeneral = soundGeneral;
        p.soundDeadline = soundDeadline;
        p.soundRev = soundRev;
        p.folder = folder;
        p.folderUri = folderUri;
        p.lang = lang;
        p.theme = theme;
        p.gradient = gradient;
        p.texPaper = texPaper;
        p.texConcrete = texConcrete;
        p.texGrain = texGrain;
        p.contrast = contrast;
        p.fontPct = fontPct;
        p.dark = dark;
        p.warmth = warmth;
        p.battery = battery;
        p.cb = cb;
        p.reduceMotion = reduceMotion;
        p.haptics = haptics;
        p.windowBlur = windowBlur;
        p.motionBlur = motionBlur;
        return p;
    }

    boolean same(Prefs o) {
        return freq.equals(o.freq) && daysBefore == o.daysBefore && time.equals(o.time)
                && soundGeneral.equals(o.soundGeneral) && soundDeadline.equals(o.soundDeadline)
                && folder.equals(o.folder) && folderUri.equals(o.folderUri) && lang == o.lang
                && theme == o.theme && gradient == o.gradient && texPaper == o.texPaper
                && texConcrete == o.texConcrete && texGrain == o.texGrain && contrast == o.contrast
                && fontPct == o.fontPct && dark == o.dark && warmth == o.warmth && battery == o.battery
                && cb == o.cb && reduceMotion == o.reduceMotion && haptics == o.haptics
                && windowBlur == o.windowBlur && motionBlur == o.motionBlur;
    }
}
