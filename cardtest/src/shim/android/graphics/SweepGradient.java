package android.graphics;

public class SweepGradient extends Shader {
    public final float cx, cy;
    public final int[] colors;
    public final float[] positions;

    public SweepGradient(float cx, float cy, int[] colors, float[] positions) {
        this.cx = cx;
        this.cy = cy;
        this.colors = colors.clone();
        this.positions = positions.clone();
    }

    public int colorAt(double t) {
        if (colors.length == 1) return colors[0];
        double tt = Math.max(0.0, Math.min(1.0, t));
        if (tt <= positions[0]) return colors[0];
        if (tt >= positions[positions.length - 1]) return colors[colors.length - 1];
        for (int i = 0; i < positions.length - 1; i++) {
            float p0 = positions[i];
            float p1 = positions[i + 1];
            if (tt >= p0 && tt <= p1) {
                double f = (p1 == p0) ? 0.0 : (tt - p0) / (p1 - p0);
                return blend(colors[i], colors[i + 1], f);
            }
        }
        return colors[colors.length - 1];
    }

    private static int blend(int a, int b, double f) {
        int aA = (a >>> 24) & 0xFF, aR = (a >> 16) & 0xFF, aG = (a >> 8) & 0xFF, aB = a & 0xFF;
        int bA = (b >>> 24) & 0xFF, bR = (b >> 16) & 0xFF, bG = (b >> 8) & 0xFF, bB = b & 0xFF;
        int A = (int) Math.round(aA + (bA - aA) * f);
        int R = (int) Math.round(aR + (bR - aR) * f);
        int G = (int) Math.round(aG + (bG - aG) * f);
        int B = (int) Math.round(aB + (bB - aB) * f);
        return (A << 24) | (R << 16) | (G << 8) | B;
    }
}
