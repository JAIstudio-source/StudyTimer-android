package android.graphics;

public class Color {
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;
    public static final int TRANSPARENT = 0x00000000;

    public static int rgb(int r, int g, int b) {
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    public static int argb(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static java.awt.Color awt(int argb) {
        return new java.awt.Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF);
    }
}
