package android.graphics;

import java.awt.geom.Path2D;

public class Path {
    public enum Direction { CW, CCW }

    private final Path2D.Float p = new Path2D.Float();

    public void addRoundRect(RectF r, float rx, float ry, Direction dir) {
        p.append(new java.awt.geom.RoundRectangle2D.Float(
                r.left, r.top, r.right - r.left, r.bottom - r.top, 2 * rx, 2 * ry), false);
    }

    public void moveTo(float x, float y) {
        p.moveTo(x, y);
    }

    public void lineTo(float x, float y) {
        p.lineTo(x, y);
    }

    public void quadTo(float x1, float y1, float x2, float y2) {
        p.quadTo(x1, y1, x2, y2);
    }

    public void close() {
        p.closePath();
    }

    public java.awt.Shape toAwt() {
        return p;
    }
}
