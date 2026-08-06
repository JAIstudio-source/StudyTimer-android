package android.graphics;

import java.awt.BasicStroke;

public class Paint {
    public static final int ANTI_ALIAS_FLAG = 1;

    public enum Style { FILL, STROKE }

    public enum Cap { BUTT, ROUND, SQUARE }

    public static class FontMetrics {
        public float ascent;
        public float descent;
    }

    int color = 0xFF000000;
    int alpha = 255;

    public float strokeWidth = 1f;
    public Cap strokeCap = Cap.BUTT;
    public Style style = Style.FILL;
    public DashPathEffect pathEffect;
    public Shader shader;
    public float textSize = 12f;
    public Typeface typeface = Typeface.DEFAULT;
    public float letterSpacing = 0f;
    public boolean isAntiAlias = true;

    public Paint() {}

    public Paint(int flags) {
        isAntiAlias = (flags & ANTI_ALIAS_FLAG) != 0;
    }

    public void setColor(int c) {
        color = c;
        alpha = (c >>> 24) & 0xFF;
    }

    public int getColor() {
        return color;
    }

    public void setAlpha(int a) {
        alpha = a & 0xFF;
    }

    public int getAlpha() {
        return alpha;
    }

    public int effectiveArgb() {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public FontMetrics getFontMetrics() {
        FontMetrics m = new FontMetrics();
        java.awt.FontMetrics fm = MetricsHolder.metrics(toAwtFont());
        m.ascent = -fm.getAscent();
        m.descent = fm.getDescent();
        return m;
    }

    public float measureText(String text) {
        if (text == null || text.isEmpty()) return 0f;
        java.awt.FontMetrics fm = MetricsHolder.metrics(toAwtFont());
        float w = 0f;
        for (int i = 0; i < text.length(); i++) {
            w += fm.stringWidth(String.valueOf(text.charAt(i)));
            if (letterSpacing != 0f && i < text.length() - 1) w += letterSpacing * textSize;
        }
        return w;
    }

    public java.awt.Font toAwtFont() {
        return typeface.toAwt(textSize);
    }

    public java.awt.Stroke toAwtStroke() {
        float[] dash = null;
        float phase = 0f;
        if (pathEffect != null) {
            dash = new float[pathEffect.intervals.length];
            for (int i = 0; i < dash.length; i++) dash[i] = pathEffect.intervals[i];
            phase = pathEffect.phase;
        }
        int cap = strokeCap == Cap.ROUND ? BasicStroke.CAP_ROUND
                : strokeCap == Cap.SQUARE ? BasicStroke.CAP_SQUARE : BasicStroke.CAP_BUTT;
        return new BasicStroke(strokeWidth, cap, BasicStroke.JOIN_ROUND, 10f, dash, phase);
    }
}
