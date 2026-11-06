package com.memorius.app;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.Html;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class Files {

    static String queryName(Context c, Uri u) {
        String name = null;
        try (Cursor cur = c.getContentResolver().query(u, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                int i = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) name = cur.getString(i);
            }
        } catch (Exception ignored) {
        }
        if (name == null) {
            name = u.getLastPathSegment();
            if (name == null) name = "file";
        }
        return name;
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return bo.toByteArray();
    }

    static byte[] readFile(String path) throws IOException {
        return readAll(new FileInputStream(new File(path)));
    }

    static String readText(Context c, Uri u) throws IOException {
        return new String(readAll(c.getContentResolver().openInputStream(u)), "UTF-8");
    }

    // text of a .docx (word/document.xml, tags stripped)
    static String docxText(Context c, Uri u) throws IOException {
        ZipInputStream z = new ZipInputStream(c.getContentResolver().openInputStream(u));
        try {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                if (e.getName().equals("word/document.xml")) {
                    String xml = new String(readAll0(z), "UTF-8");
                    xml = xml.replace("</w:p>", "\n").replaceAll("<w:br[^>]*/>", "\n").replaceAll("<w:tab[^>]*/>", "\t");
                    xml = xml.replaceAll("<[^>]+>", "");
                    return xml.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                            .replace("&quot;", "\"").replace("&apos;", "'").trim();
                }
            }
        } finally {
            z.close();
        }
        throw new IOException("no document.xml");
    }

    private static byte[] readAll0(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toByteArray();
    }

    static String htmlToText(String html) {
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim();
    }

    static String copyToApp(Context c, Uri u) throws IOException {
        File dir = new File(c.getFilesDir(), "pdf");
        dir.mkdirs();
        File out = new File(dir, Store.newId() + ".pdf");
        FileOutputStream os = new FileOutputStream(out);
        os.write(readAll(c.getContentResolver().openInputStream(u)));
        os.close();
        return out.getAbsolutePath();
    }

    static byte[] docBytes(String text) throws IOException {
        String html = "<html><head><meta charset=\"utf-8\"></head><body>"
                + text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")
                + "</body></html>";
        return html.getBytes("UTF-8");
    }

    // simple text PDF, A4, wraps and paginates
    static byte[] pdfBytes(String text) throws IOException {
        final int pw = 595, ph = 842, margin = 40;
        PdfDocument doc = new PdfDocument();
        TextPaint tp = new TextPaint();
        tp.setAntiAlias(true);
        tp.setTextSize(12f);
        int width = pw - 2 * margin;
        int avail = ph - 2 * margin;
        StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), tp, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(2f, 1f).build();
        int lines = layout.getLineCount();
        int start = 0, pageNo = 1;
        do {
            int end = start;
            while (end < lines && layout.getLineBottom(end) - layout.getLineTop(start) <= avail) end++;
            if (end == start) end = start + 1;
            PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(pw, ph, pageNo).create());
            Canvas cv = page.getCanvas();
            cv.save();
            cv.translate(margin, margin - layout.getLineTop(start));
            cv.clipRect(0, layout.getLineTop(start), width, layout.getLineBottom(end - 1));
            layout.draw(cv);
            cv.restore();
            doc.finishPage(page);
            start = end;
            pageNo++;
        } while (start < lines);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        doc.writeTo(bo);
        doc.close();
        return bo.toByteArray();
    }
}
