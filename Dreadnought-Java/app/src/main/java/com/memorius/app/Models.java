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
    String freq = "due"; // off | due | d1 | daily
    String time = "08:00";
    String folder = "";
    String folderUri = "";
    int lang = 0;
    int theme = 0;
    boolean dark = false;
    boolean cb = false;

    Prefs copy() {
        Prefs p = new Prefs();
        p.freq = freq;
        p.time = time;
        p.folder = folder;
        p.folderUri = folderUri;
        p.lang = lang;
        p.theme = theme;
        p.dark = dark;
        p.cb = cb;
        return p;
    }

    boolean same(Prefs o) {
        return freq.equals(o.freq) && time.equals(o.time) && folder.equals(o.folder)
                && folderUri.equals(o.folderUri) && lang == o.lang && theme == o.theme
                && dark == o.dark && cb == o.cb;
    }
}
