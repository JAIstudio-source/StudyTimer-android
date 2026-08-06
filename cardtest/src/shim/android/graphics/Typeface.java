package android.graphics;

import java.awt.BasicStroke;
import java.awt.Font;

public class Typeface {
    public static final int NORMAL = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;

    public static final Typeface DEFAULT = new Typeface(false);
    public static final Typeface DEFAULT_BOLD = new Typeface(true);

    private final boolean bold;

    private Typeface(boolean bold) {
        this.bold = bold;
    }

    public static Typeface create(String family, int style) {
        return new Typeface(style == BOLD);
    }

    public Font toAwt(float size) {
        Font f = new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, 1);
        return f.deriveFont(size);
    }
}
