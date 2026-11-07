package com.memorius.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

final class Store {
    static List<Card> cards = new ArrayList<>();
    static List<Opus> opus = new ArrayList<>();
    static List<Slot> slots = new ArrayList<>();
    static Prefs prefs = new Prefs();
    private static File file;
    private static boolean loaded = false;

    static String newId() {
        return Long.toString(System.currentTimeMillis(), 36) + Integer.toString((int) (Math.random() * 1679616), 36);
    }

    static void ensure(Context c) {
        if (!loaded) init(c);
    }

    static void init(Context c) {
        file = new File(c.getApplicationContext().getFilesDir(), "data.json");
        loaded = true;
        load();
    }

    static synchronized void save() {
        if (file == null) return;
        try {
            JSONObject o = new JSONObject();

            JSONArray ca = new JSONArray();
            for (Card c : cards) {
                JSONObject j = new JSONObject();
                j.put("id", c.id);
                j.put("subject", c.subject);
                j.put("due", c.due);
                j.put("type", c.type);
                j.put("note", c.note);
                j.put("color", c.color);
                j.put("done", c.done);
                JSONArray fa = new JSONArray();
                for (FileRef f : c.files) {
                    JSONObject fj = new JSONObject();
                    fj.put("name", f.name);
                    fj.put("uri", f.uri);
                    fa.put(fj);
                }
                j.put("files", fa);
                ca.put(j);
            }
            o.put("cards", ca);

            JSONArray oa = new JSONArray();
            for (Opus p : opus) {
                JSONObject j = new JSONObject();
                j.put("id", p.id);
                j.put("title", p.title);
                j.put("ext", p.ext);
                j.put("kind", p.kind);
                j.put("content", p.content);
                j.put("progress", p.progress);
                oa.put(j);
            }
            o.put("opus", oa);

            JSONArray sa = new JSONArray();
            for (Slot s : slots) {
                JSONObject j = new JSONObject();
                j.put("id", s.id);
                j.put("day", s.day);
                j.put("subject", s.subject);
                j.put("time", s.time);
                j.put("note", s.note);
                sa.put(j);
            }
            o.put("slots", sa);

            JSONObject pj = new JSONObject();
            pj.put("freq", prefs.freq);
            pj.put("time", prefs.time);
            pj.put("folder", prefs.folder);
            pj.put("folderUri", prefs.folderUri);
            pj.put("lang", prefs.lang);
            pj.put("theme", prefs.theme);
            pj.put("dark", prefs.dark);
            pj.put("cb", prefs.cb);
            o.put("prefs", pj);

            FileOutputStream os = new FileOutputStream(file);
            os.write(o.toString().getBytes("UTF-8"));
            os.close();
        } catch (Exception e) {
            // keep running; data stays in memory
        }
    }

    private static void load() {
        cards = new ArrayList<>();
        opus = new ArrayList<>();
        slots = new ArrayList<>();
        prefs = new Prefs();
        if (file == null || !file.exists()) return;
        try {
            FileInputStream in = new FileInputStream(file);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            in.close();
            JSONObject o = new JSONObject(bo.toString("UTF-8"));

            JSONArray ca = o.optJSONArray("cards");
            if (ca != null) {
                for (int i = 0; i < ca.length(); i++) {
                    JSONObject j = ca.getJSONObject(i);
                    Card c = new Card();
                    c.id = j.optString("id", c.id);
                    c.subject = j.optString("subject", "");
                    c.due = j.optString("due", "");
                    c.type = j.optString("type", "homework");
                    c.note = j.optString("note", "");
                    c.color = j.optInt("color", 0);
                    c.done = j.optBoolean("done", false);
                    JSONArray fa = j.optJSONArray("files");
                    if (fa != null) {
                        for (int k = 0; k < fa.length(); k++) {
                            JSONObject fj = fa.getJSONObject(k);
                            c.files.add(new FileRef(fj.optString("name", ""), fj.optString("uri", "")));
                        }
                    }
                    cards.add(c);
                }
            }

            JSONArray oa = o.optJSONArray("opus");
            if (oa != null) {
                for (int i = 0; i < oa.length(); i++) {
                    JSONObject j = oa.getJSONObject(i);
                    Opus p = new Opus();
                    p.id = j.optString("id", p.id);
                    p.title = j.optString("title", "");
                    p.ext = j.optString("ext", "txt");
                    p.kind = j.optString("kind", "text");
                    p.content = j.optString("content", "");
                    p.progress = j.optInt("progress", 0);
                    opus.add(p);
                }
            }

            JSONArray sa = o.optJSONArray("slots");
            if (sa != null) {
                for (int i = 0; i < sa.length(); i++) {
                    JSONObject j = sa.getJSONObject(i);
                    Slot s = new Slot();
                    s.id = j.optString("id", s.id);
                    s.day = j.optInt("day", 0);
                    s.subject = j.optString("subject", "");
                    s.time = j.optString("time", "");
                    s.note = j.optString("note", "");
                    slots.add(s);
                }
            }

            JSONObject pj = o.optJSONObject("prefs");
            if (pj != null) {
                prefs.freq = pj.optString("freq", "due");
                prefs.time = pj.optString("time", "08:00");
                prefs.folder = pj.optString("folder", "");
                prefs.folderUri = pj.optString("folderUri", "");
                prefs.lang = pj.optInt("lang", 0);
                prefs.theme = pj.optInt("theme", 0);
                prefs.dark = pj.optBoolean("dark", false);
                prefs.cb = pj.optBoolean("cb", false);
            }
        } catch (Exception e) {
            // unreadable file: start clean
        }
    }
}
