package com.memorius.app;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Rule based fun English. Hand-written labels live in Str; this covers all the other text.
// Whole words are swapped (keeping capital letters), and "-ing" endings change where it fits.
final class Dialects {
    private Dialects() {
    }

    private static final Map<String, String> PIRATE = map(
            "you", "ye", "your", "yer", "my", "me", "yes", "aye", "hello", "ahoy", "friend", "matey",
            "is", "be", "are", "be", "to", "t'", "of", "o'", "for", "fer", "and", "an'", "nothing", "naught",
            "here", "'ere", "files", "booty", "file", "booty", "notifications", "signals", "notification", "signal",
            "reminders", "remindin's", "reminder", "reminder", "delete", "scuttle", "folder", "chest", "the", "th'");
    private static final Map<String, String> LOL = map(
            "the", "teh", "you", "u", "your", "ur", "are", "r", "is", "iz", "have", "has", "with", "wif", "please", "plz",
            "for", "fer", "this", "dis", "that", "dat", "my", "mai", "and", "n", "of", "ov", "what", "wut",
            "files", "filez", "file", "filez", "cards", "cardz", "card", "cardz", "can", "can", "no", "noes", "yes", "yus");
    private static final Map<String, String> JAM = map(
            "the", "di", "this", "dis", "that", "dat", "them", "dem", "they", "dem", "there", "deh", "with", "wid",
            "you", "yuh", "your", "yuh", "my", "mi", "no", "nuh", "nothing", "nuttin", "everything", "everyting",
            "thing", "ting", "think", "tink", "want", "waan", "going", "gwaan");
    private static final Map<String, String> SHAKE = map(
            "you", "thou", "your", "thy", "yours", "thine", "are", "art", "have", "hast", "do", "dost", "does", "doth",
            "my", "mine", "hello", "hail", "yes", "aye", "no", "nay", "please", "prithee", "here", "hither",
            "cannot", "canst not", "files", "scrolls", "file", "scroll", "settings", "ordinances", "notifications", "tidings",
            "reminders", "remembrances", "reminder", "remembrance", "delete", "smite", "done", "'tis done");

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static final Pattern WORD = Pattern.compile("[A-Za-z']+");

    static String apply(int lang, String en) {
        Map<String, String> m;
        switch (lang) {
            case 11:
                m = PIRATE;
                break;
            case 12:
                m = LOL;
                break;
            case 13:
                m = JAM;
                break;
            default:
                m = SHAKE;
        }
        // keep %s and similar markers untouched
        Matcher mt = WORD.matcher(en);
        StringBuffer sb = new StringBuffer();
        while (mt.find()) {
            String w = mt.group();
            String low = w.toLowerCase(Locale.ROOT);
            String r = m.get(low);
            if (r == null && low.length() > 4 && low.endsWith("ing") && lang != 14) r = low.substring(0, low.length() - 1) + (lang == 12 ? "" : "'");
            if (r == null) r = w;
            else if (Character.isUpperCase(w.charAt(0)) && r.length() > 0) r = Character.toUpperCase(r.charAt(0)) + r.substring(1);
            mt.appendReplacement(sb, Matcher.quoteReplacement(r));
        }
        mt.appendTail(sb);
        String out = sb.toString();
        if (lang == 12) out = out.toLowerCase(Locale.ROOT); // lolcat is all lowercase
        if (out.endsWith(".")) {
            String tail = lang == 11 ? ", arr." : lang == 12 ? " kthx." : lang == 13 ? ", seen." : ", forsooth.";
            out = out.substring(0, out.length() - 1) + tail;
        }
        return out;
    }
}
