package android.graphics;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public class Canvas {
    private final Graphics2D g;

    public Canvas(java.awt.image.BufferedImage img) {
        g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public void clipPath(Path path) {
        g.clip(path.toAwt());
    }

    private final java.util.ArrayList<java.awt.Shape> clipStack = new java.util.ArrayList<>();

    public int save() {
        clipStack.add(g.getClip());
        return clipStack.size();
    }

    public void restore() {
        if (!clipStack.isEmpty()) g.setClip(clipStack.remove(clipStack.size() - 1));
    }

    public void drawPath(Path path, Paint p) {
        g.setPaint(awtPaint(p));
        g.setStroke(p.toAwtStroke());
        if (p.style == Paint.Style.STROKE) g.draw(path.toAwt());
        else g.fill(path.toAwt());
    }

    public void drawBitmap(Bitmap bmp, float left, float top, Paint p) {
        g.drawImage(bmp.img, Math.round(left), Math.round(top), null);
    }

    private java.awt.Paint awtPaint(Paint p) {
        if (p.shader instanceof LinearGradient) return ((LinearGradient) p.shader).toAwtPaint();
        return Color.awt(p.effectiveArgb());
    }

    public void drawRect(float l, float t, float r, float b, Paint p) {
        g.setPaint(awtPaint(p));
        g.fill(new Rectangle2D.Float(l, t, r - l, b - t));
    }

    public void drawRoundRect(RectF rf, float rx, float ry, Paint p) {
        g.setPaint(awtPaint(p));
        RoundRectangle2D.Float rr = new RoundRectangle2D.Float(
                rf.left, rf.top, rf.right - rf.left, rf.bottom - rf.top, 2 * rx, 2 * ry);
        if (p.style == Paint.Style.STROKE) {
            g.setStroke(p.toAwtStroke());
            g.draw(rr);
        } else {
            g.fill(rr);
        }
    }

    public void drawCircle(float cx, float cy, float r, Paint p) {
        g.setPaint(awtPaint(p));
        Ellipse2D.Float e = new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r);
        if (p.style == Paint.Style.STROKE) {
            g.setStroke(p.toAwtStroke());
            g.draw(e);
        } else {
            g.fill(e);
        }
    }

    public void drawLine(float x1, float y1, float x2, float y2, Paint p) {
        g.setPaint(awtPaint(p));
        g.setStroke(p.toAwtStroke());
        g.draw(new Line2D.Float(x1, y1, x2, y2));
    }

    public void drawArc(RectF r, float start, float sweep, boolean useCenter, Paint p) {
        float x = r.left, y = r.top, w = r.right - r.left, h = r.bottom - r.top;
        if (p.shader instanceof SweepGradient) {
            drawSweepArc(x, y, w, h, start, sweep, p);
            return;
        }
        g.setPaint(awtPaint(p));
        g.setStroke(p.toAwtStroke());
        g.drawArc((int) x, (int) y, (int) w, (int) h, (int) -start, (int) -sweep);
    }

    private void drawSweepArc(float x, float y, float w, float h, float start, float sweep, Paint p) {
        SweepGradient sg = (SweepGradient) p.shader;
        int steps = Math.max(1, (int) Math.ceil(Math.abs(sweep) / 2.0));
        for (int i = 0; i < steps; i++) {
            double a0 = start + sweep * i / (double) steps;
            double a1 = start + sweep * (i + 1) / (double) steps;
            double t = sweep != 0 ? (a0 - start) / sweep : 0;
            Paint seg = new Paint();
            seg.color = sg.colorAt(t);
            seg.alpha = (sg.colorAt(t) >>> 24) & 0xFF;
            seg.strokeWidth = p.strokeWidth;
            seg.strokeCap = p.strokeCap;
            seg.style = p.style;
            seg.pathEffect = p.pathEffect;
            g.setPaint(Color.awt(seg.effectiveArgb()));
            g.setStroke(seg.toAwtStroke());
            g.drawArc((int) x, (int) y, (int) w, (int) h, (int) -a0, (int) -(a1 - a0));
        }
    }

    public void drawText(String text, float x, float y, Paint p) {
        java.awt.Font f = p.toAwtFont();
        g.setFont(f);
        g.setPaint(awtPaint(p));
        if (p.letterSpacing == 0f) {
            g.drawString(text, x, y);
        } else {
            java.awt.FontMetrics fm = MetricsHolder.metrics(f);
            float cx = x;
            for (int i = 0; i < text.length(); i++) {
                String ch = String.valueOf(text.charAt(i));
                g.drawString(ch, cx, y);
                cx += fm.stringWidth(ch) + p.letterSpacing * p.textSize;
            }
        }
    }
}
