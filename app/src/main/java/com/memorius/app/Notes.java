package com.memorius.app;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Notes live in the app's private storage as .docx files; the list is kept in preferences. */
public class Notes {
    static class Note { long id; String title; long updated; }

    static ArrayList<Note> list(Context c) {
        ArrayList<Note> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(S.p(c).getString("notes", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Note n = new Note();
                n.id = o.getLong("id");
                n.title = o.optString("title");
                n.updated = o.optLong("updated");
                out.add(n);
            }
        } catch (Exception e) { }
        Collections.sort(out, (x, y) -> Long.compare(y.updated, x.updated));
        return out;
    }

    static void saveMeta(Context c, List<Note> l) {
        try {
            JSONArray a = new JSONArray();
            for (Note n : l) {
                JSONObject o = new JSONObject();
                o.put("id", n.id);
                o.put("title", n.title);
                o.put("updated", n.updated);
                a.put(o);
            }
            S.p(c).edit().putString("notes", a.toString()).apply();
        } catch (Exception e) { }
    }

    static File file(Context c, long id) { return new File(c.getFilesDir(), "note_" + id + ".docx"); }

    static SpannableStringBuilder load(Context c, long id) {
        try (FileInputStream in = new FileInputStream(file(c, id))) {
            return Docx.read(in);
        } catch (Exception e) { return new SpannableStringBuilder(); }
    }

    static void save(Context c, long id, String title, Spanned body) {
        try (FileOutputStream os = new FileOutputStream(file(c, id))) {
            Docx.write(os, body);
        } catch (Exception e) { return; }
        ArrayList<Note> l = list(c);
        Note f = null;
        for (Note n : l) if (n.id == id) f = n;
        if (f == null) { f = new Note(); f.id = id; l.add(f); }
        f.title = title;
        f.updated = System.currentTimeMillis();
        saveMeta(c, l);
    }

    static void delete(Context c, long id) {
        file(c, id).delete();
        ArrayList<Note> l = list(c);
        l.removeIf(n -> n.id == id);
        saveMeta(c, l);
    }

    static String preview(Context c, long id) {
        String s = load(c, id).toString().replace('\n', ' ').trim();
        return s.length() > 140 ? s.substring(0, 140) : s;
    }
}
