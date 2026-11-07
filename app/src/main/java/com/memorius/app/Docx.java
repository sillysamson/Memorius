package com.memorius.app;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Minimal .docx reader/writer: text, bold, italic, size. */
public class Docx {
    static final String HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";

    static void put(ZipOutputStream z, String name, String content) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(content.getBytes(StandardCharsets.UTF_8));
        z.closeEntry();
    }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static void write(OutputStream os, Spanned sp) throws IOException {
        ZipOutputStream z = new ZipOutputStream(os);
        put(z, "[Content_Types].xml", HEAD + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
        put(z, "_rels/.rels", HEAD + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
        StringBuilder b = new StringBuilder(HEAD);
        b.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");
        String t = sp.toString();
        int start = 0;
        while (true) {
            int nl = t.indexOf('\n', start);
            int end = nl < 0 ? t.length() : nl;
            paragraph(b, sp, start, end);
            if (nl < 0) break;
            start = nl + 1;
        }
        b.append("</w:body></w:document>");
        put(z, "word/document.xml", b.toString());
        z.finish();
        z.close();
    }

    static void paragraph(StringBuilder b, Spanned sp, int s, int e) {
        b.append("<w:p>");
        int i = s;
        while (i < e) {
            int n = sp.nextSpanTransition(i, e, Object.class);
            if (n <= i || n > e) n = e;
            run(b, sp, i, n);
            i = n;
        }
        b.append("</w:p>");
    }

    static void run(StringBuilder b, Spanned sp, int i, int n) {
        boolean bold = false, it = false;
        int size = 14;
        for (Object o : sp.getSpans(i, n, Object.class)) {
            if (o instanceof StyleSpan) {
                int st = ((StyleSpan) o).getStyle();
                if ((st & Typeface.BOLD) != 0) bold = true;
                if ((st & Typeface.ITALIC) != 0) it = true;
            } else if (o instanceof AbsoluteSizeSpan) {
                size = ((AbsoluteSizeSpan) o).getSize();
            }
        }
        String txt = esc(sp.subSequence(i, n).toString());
        b.append("<w:r><w:rPr>").append(bold ? "<w:b/>" : "").append(it ? "<w:i/>" : "")
                .append("<w:sz w:val=\"").append(size * 2).append("\"/></w:rPr><w:t xml:space=\"preserve\">")
                .append(txt).append("</w:t></w:r>");
    }

    static SpannableStringBuilder read(InputStream in) throws Exception {
        ZipInputStream z = new ZipInputStream(in);
        ZipEntry e;
        byte[] data = null;
        while ((e = z.getNextEntry()) != null) {
            if (e.getName().equals("word/document.xml")) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = z.read(buf)) > 0) bo.write(buf, 0, n);
                data = bo.toByteArray();
                break;
            }
        }
        if (data == null) throw new IOException("not a docx");
        XmlPullParser p = Xml.newPullParser();
        p.setInput(new ByteArrayInputStream(data), "UTF-8");
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean bold = false, it = false, first = true;
        int size = 14;
        for (int ev = p.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = p.next()) {
            if (ev != XmlPullParser.START_TAG) continue;
            String n = p.getName();
            if (n.equals("w:p")) {
                if (!first) sb.append('\n');
                first = false;
            } else if (n.equals("w:r")) {
                bold = false; it = false; size = 14;
            } else if (n.equals("w:b")) {
                String v = p.getAttributeValue(null, "w:val");
                bold = !"0".equals(v) && !"false".equals(v);
            } else if (n.equals("w:i")) {
                String v = p.getAttributeValue(null, "w:val");
                it = !"0".equals(v) && !"false".equals(v);
            } else if (n.equals("w:sz")) {
                try { size = Integer.parseInt(p.getAttributeValue(null, "w:val")) / 2; } catch (Exception ex) { }
            } else if (n.equals("w:tab")) {
                sb.append('\t');
            } else if (n.equals("w:br")) {
                sb.append('\n');
            } else if (n.equals("w:t")) {
                String txt = p.nextText();
                if (txt.isEmpty()) continue;
                int st = sb.length();
                sb.append(txt);
                int en = sb.length();
                if (bold || it)
                    sb.setSpan(new StyleSpan(bold && it ? Typeface.BOLD_ITALIC : bold ? Typeface.BOLD : Typeface.ITALIC), st, en, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new AbsoluteSizeSpan(Math.max(6, size), true), st, en, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return sb;
    }
}
