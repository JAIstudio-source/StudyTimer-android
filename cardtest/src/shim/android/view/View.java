package android.view;

import android.content.Context;
import android.graphics.Canvas;

public class View {
    private final Context mContext;
    private int mWidth;
    private int mHeight;

    public View(Context context) {
        mContext = context;
    }

    public View(Context context, android.util.AttributeSet attrs) {
        this(context);
    }

    public View(Context context, android.util.AttributeSet attrs, int defStyleAttr) {
        this(context);
    }

    public final Context getContext() {
        return mContext;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void invalidate() {}

    public void measure(int widthMeasureSpec, int heightMeasureSpec) {
        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);
    }

    public void layout(int l, int t, int r, int b) {
        mWidth = r - l;
        mHeight = b - t;
    }

    public void draw(Canvas canvas) {
        onDraw(canvas);
    }

    protected void onDraw(Canvas canvas) {}

    public static class MeasureSpec {
        public static final int UNSPECIFIED = 0;
        public static final int EXACTLY = 1 << 30;
        public static final int AT_MOST = 2 << 30;

        public static int makeMeasureSpec(int size, int mode) {
            return size + mode;
        }

        public static int getSize(int spec) {
            return spec & ~(3 << 30);
        }

        public static int getMode(int spec) {
            return spec & (3 << 30);
        }
    }
}
