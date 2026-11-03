package com.memorius.app;

import java.time.LocalDate;
import java.util.ArrayList;

public class Task {
    long id;
    String kind = "homework";
    String subject = "", notes = "";
    LocalDate due = LocalDate.now().plusDays(1);
    boolean done;
    ArrayList<String> files = new ArrayList<>();

    boolean exam() { return kind.equals("exam") || kind.startsWith("Incoming"); }

    static String enc(String name, String uri) { return name + "\u0001" + (uri == null ? "" : uri); }
    static String nameOf(String f) { int i = f.indexOf('\u0001'); return i < 0 ? f : f.substring(0, i); }
    static String uriOf(String f) { int i = f.indexOf('\u0001'); return i < 0 ? "" : f.substring(i + 1); }
}
