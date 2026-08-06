package android.graphics;

public class Bitmap {
    final java.awt.image.BufferedImage img;

    Bitmap(java.awt.image.BufferedImage img) {
        this.img = img;
    }

    public int getWidth() {
        return img.getWidth();
    }

    public int getHeight() {
        return img.getHeight();
    }

    public static Bitmap createScaledBitmap(Bitmap src, int w, int h, boolean filter) {
        java.awt.image.BufferedImage out =
                new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src.img, 0, 0, w, h, null);
        g.dispose();
        return new Bitmap(out);
    }
}
