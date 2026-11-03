package com.memorius.app;

import android.content.Context;
import org.json.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Store {
    static ArrayList<Task> load(Context c) {
        ArrayList<Task> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(S.p(c).getString("tasks", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Task t = new Task();
                t.id = o.getLong("id");
                t.kind = o.optString("kind", "homework");
                t.subject = o.optString("subject");
                t.notes = o.optString("notes");
                t.due = LocalDate.parse(o.getString("due"));
                t.done = o.optBoolean("done");
                JSONArray f = o.optJSONArray("files");
                if (f != null) for (int j = 0; j < f.length(); j++) t.files.add(f.getString(j));
                out.add(t);
            }
        } catch (Exception e) { }
        return out;
    }

    static void save(Context c, List<Task> tasks) {
        try {
            JSONArray a = new JSONArray();
            for (Task t : tasks) {
                JSONObject o = new JSONObject();
                o.put("id", t.id);
                o.put("kind", t.exam() ? "exam" : "homework");
                o.put("subject", t.subject);
                o.put("notes", t.notes);
                o.put("due", t.due.toString());
                o.put("done", t.done);
                o.put("files", new JSONArray(t.files));
                a.put(o);
            }
            S.p(c).edit().putString("tasks", a.toString()).apply();
        } catch (Exception e) { }
    }
}
