package android.graphics;

public class DashPathEffect {
    public final float[] intervals;
    public final float phase;

    public DashPathEffect(float[] intervals, float phase) {
        this.intervals = intervals.clone();
        this.phase = phase;
    }
}
