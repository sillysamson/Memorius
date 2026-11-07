package com.memorius.app;

import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Canvas;
import java.io.IOException;
import java.io.OutputStream;

/** Builds a PDF (A4) from styled text using Android's built-in PdfDocument. */
public class PdfMaker {
    static void write(OutputStream os, CharSequence text) throws IOException {
        SpannableStringBuilder sb = new SpannableStringBuilder(text.toString());
        if (text instanceof Spanned) {
            Spanned sp = (Spanned) text;
            for (StyleSpan s : sp.getSpans(0, sp.length(), StyleSpan.class))
                sb.setSpan(new StyleSpan(s.getStyle()), sp.getSpanStart(s), sp.getSpanEnd(s), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            for (AbsoluteSizeSpan s : sp.getSpans(0, sp.length(), AbsoluteSizeSpan.class))
                sb.setSpan(new AbsoluteSizeSpan(s.getSize(), false), sp.getSpanStart(s), sp.getSpanEnd(s), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        final int W = 595, H = 842, M = 40, TW = W - 2 * M, TH = H - 2 * M;
        TextPaint tp = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        tp.setTextSize(14);
        tp.setColor(Color.BLACK);
        StaticLayout lay = StaticLayout.Builder.obtain(sb, 0, sb.length(), tp, TW)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
        PdfDocument doc = new PdfDocument();
        int count = lay.getLineCount();
        int line = 0, pageNo = 1;
        do {
            int top = lay.getLineTop(line);
            int end = line;
            while (end < count && lay.getLineBottom(end) - top <= TH) end++;
            if (end == line) end = line + 1;
            int bottom = lay.getLineBottom(end - 1);
            PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo).create());
            Canvas c = page.getCanvas();
            c.translate(M, M);
            c.clipRect(0, 0, TW, bottom - top);
            c.translate(0, -top);
            lay.draw(c);
            doc.finishPage(page);
            line = end;
            pageNo++;
        } while (line < count);
        doc.writeTo(os);
        doc.close();
    }
}
