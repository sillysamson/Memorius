package com.memorius.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

// Simple line icons drawn in code (no image files). One stroke weight, round ends.
final class IconView extends View {
    static final int CARDS = 0, PROJECTS = 1, SCHEDULE = 2, SETTINGS = 3, MORE = 4, THEME = 5, DISPLAY = 6,
            NOTIF = 7, REMINDER = 8, LANGUAGE = 9, FOLDER = 10, ACCESS = 11, EXPERIMENT = 12, ABOUT = 13,
            CHEVRON = 14, BACK = 15;

    private int kind;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    IconView(Context c, int kind, int color) {
        super(c);
        this.kind = kind;
        p.setColor(color);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setColor(int color) {
        p.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float s = Math.min(getWidth(), getHeight()) / 24f;
        c.save();
        c.translate((getWidth() - 24 * s) / 2f, (getHeight() - 24 * s) / 2f);
        c.scale(s, s);
        if ((kind == CHEVRON || kind == BACK) && getLayoutDirection() == LAYOUT_DIRECTION_RTL) c.scale(-1f, 1f, 12f, 12f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.8f);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        Path a = new Path();
        switch (kind) {
            case CARDS:
                a.moveTo(7, 5.5f); a.lineTo(18.5f, 5.5f); a.quadTo(21, 5.5f, 21, 8); a.lineTo(21, 15);
                c.drawPath(a, p);
                c.drawRoundRect(new RectF(3, 9, 17, 20.5f), 3f, 3f, p);
                break;
            case PROJECTS:
                c.drawRoundRect(new RectF(6, 3, 18, 21), 2.5f, 2.5f, p);
                a.moveTo(9, 9); a.lineTo(15, 9); a.moveTo(9, 13); a.lineTo(15, 13);
                c.drawPath(a, p);
                break;
            case FOLDER:
                a.moveTo(3, 7); a.lineTo(3, 18); a.quadTo(3, 19.5f, 4.5f, 19.5f); a.lineTo(19.5f, 19.5f);
                a.quadTo(21, 19.5f, 21, 18); a.lineTo(21, 9.5f); a.quadTo(21, 8, 19.5f, 8); a.lineTo(11.5f, 8);
                a.lineTo(9.5f, 5.5f); a.lineTo(4.5f, 5.5f); a.quadTo(3, 5.5f, 3, 7); a.close();
                c.drawPath(a, p);
                break;
            case SCHEDULE:
                c.drawRoundRect(new RectF(4, 5, 20, 20), 2.5f, 2.5f, p);
                a.moveTo(4, 10); a.lineTo(20, 10); a.moveTo(8, 3); a.lineTo(8, 7); a.moveTo(16, 3); a.lineTo(16, 7);
                c.drawPath(a, p);
                break;
            case SETTINGS:
                a.moveTo(4, 8); a.lineTo(20, 8); a.moveTo(4, 16); a.lineTo(20, 16);
                c.drawPath(a, p);
                c.drawCircle(9, 8, 2.5f, p);
                c.drawCircle(15, 16, 2.5f, p);
                break;
            case MORE:
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(6, 12, 1.8f, p); c.drawCircle(12, 12, 1.8f, p); c.drawCircle(18, 12, 1.8f, p);
                break;
            case THEME:
                c.drawCircle(12, 12, 9, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(8.5f, 10.5f, 1.3f, p); c.drawCircle(12, 7.8f, 1.3f, p);
                c.drawCircle(15.5f, 10.5f, 1.3f, p); c.drawCircle(12, 15.5f, 1.3f, p);
                break;
            case DISPLAY:
                c.drawCircle(12, 12, 9, p);
                p.setStyle(Paint.Style.FILL);
                a.moveTo(12, 3); a.arcTo(new RectF(3, 3, 21, 21), -90, 180); a.close();
                c.drawPath(a, p);
                break;
            case NOTIF:
                a.moveTo(6, 16.5f); a.lineTo(6, 11); a.quadTo(6, 6, 12, 6); a.quadTo(18, 6, 18, 11);
                a.lineTo(18, 16.5f); a.lineTo(19.5f, 18); a.lineTo(4.5f, 18); a.close();
                a.moveTo(12, 3.5f); a.lineTo(12, 6); a.moveTo(10, 20.5f); a.quadTo(12, 22.5f, 14, 20.5f);
                c.drawPath(a, p);
                break;
            case REMINDER:
                c.drawCircle(12, 12, 9, p);
                a.moveTo(12, 7); a.lineTo(12, 12); a.lineTo(15.5f, 14);
                c.drawPath(a, p);
                break;
            case LANGUAGE:
                c.drawCircle(12, 12, 9, p);
                c.drawOval(new RectF(8, 3, 16, 21), p);
                a.moveTo(3, 12); a.lineTo(21, 12);
                c.drawPath(a, p);
                break;
            case ACCESS:
                c.drawCircle(12, 5.5f, 2f, p);
                a.moveTo(5, 9.5f); a.lineTo(19, 9.5f); a.moveTo(12, 9.5f); a.lineTo(12, 15);
                a.moveTo(12, 15); a.lineTo(8.5f, 21); a.moveTo(12, 15); a.lineTo(15.5f, 21);
                c.drawPath(a, p);
                break;
            case EXPERIMENT:
                a.moveTo(9.5f, 3.5f); a.lineTo(14.5f, 3.5f); a.moveTo(10.5f, 3.5f); a.lineTo(10.5f, 9.5f);
                a.lineTo(5, 18.5f); a.quadTo(4, 20.5f, 6.2f, 20.5f); a.lineTo(17.8f, 20.5f);
                a.quadTo(20, 20.5f, 19, 18.5f); a.lineTo(13.5f, 9.5f); a.lineTo(13.5f, 3.5f);
                a.moveTo(7.5f, 15); a.lineTo(16.5f, 15);
                c.drawPath(a, p);
                break;
            case ABOUT:
                c.drawCircle(12, 12, 9, p);
                a.moveTo(12, 11); a.lineTo(12, 16.5f);
                c.drawPath(a, p);
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(12, 7.8f, 1.2f, p);
                break;
            case CHEVRON:
                a.moveTo(9.5f, 6); a.lineTo(15.5f, 12); a.lineTo(9.5f, 18);
                c.drawPath(a, p);
                break;
            default: // BACK
                a.moveTo(14.5f, 6); a.lineTo(8.5f, 12); a.lineTo(14.5f, 18);
                c.drawPath(a, p);
        }
        c.restore();
    }
}
