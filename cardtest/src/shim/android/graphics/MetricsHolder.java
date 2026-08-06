package android.graphics;

final class MetricsHolder {
    static final java.awt.image.BufferedImage IMG =
            new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);

    static java.awt.FontMetrics metrics(java.awt.Font f) {
        return ((java.awt.Graphics2D) IMG.getGraphics()).getFontMetrics(f);
    }
}
