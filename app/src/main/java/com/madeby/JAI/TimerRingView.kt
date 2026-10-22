package com.madeby.JAI

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.min

class TimerRingView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private var strokePx = 13f * density
    private var padPx = 14f * density

    var isFullscreen = false
        set(value) {
            if (field != value) {
                field = value
                strokePx = if (value) 14f * density else 13f * density
                padPx = if (value) 10f * density else 14f * density
                ringPaint.strokeWidth = strokePx
                trackPaint.strokeWidth = strokePx
                requestLayout()
                invalidate()
            }
        }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokePx
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = strokePx
    }

    private var color = 0xFF000000.toInt()
    private var trackColor = 0x1A000000.toInt()
    private var sweepDeg = 0f
    private var targetDeg = 0f
    private var animator: ValueAnimator? = null

    init {
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val base = if (h > 0) min(w, h) else w
        val scale = if (isFullscreen) 0.96f else 0.91f
        val size = (base * scale).toInt().coerceAtLeast(1)
        setMeasuredDimension(size, size)
    }

    fun setProgress(fraction: Float, color: Int, track: Int, animate: Boolean = true) {
        val f = fraction.coerceIn(0f, 1f)
        val target = f * 360f
        val colorChanged = color != this.color || track != this.trackColor
        if (!colorChanged && abs(target - targetDeg) < 0.02f) return
        this.color = color
        this.trackColor = track
        targetDeg = target
        val from = sweepDeg
        if (!animate || abs(target - from) < 0.02f) {
            animator?.cancel()
            sweepDeg = target
            invalidate()
            return
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(from, target).apply {
            duration = 480
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                sweepDeg = a.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun blend(from: Int, to: Int, t: Float): Int {
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    private val tipGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var pulseAlpha = 0.85f
    private var pulseAnimator: ValueAnimator? = null

    fun setTimerActive(active: Boolean) {
        if (active && pulseAnimator == null) {
            pulseAnimator = ValueAnimator.ofFloat(0.65f, 1.0f).apply {
                duration = 1500
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                addUpdateListener { a ->
                    pulseAlpha = a.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else if (!active) {
            pulseAnimator?.cancel()
            pulseAnimator = null
            pulseAlpha = 0.85f
            invalidate()
        }
    }

    private val ringBounds = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val radius = min(w, h) / 2f - strokePx / 2f - padPx
        if (radius <= 0f) return

        trackPaint.color = trackColor
        canvas.drawCircle(cx, cy, radius, trackPaint)

        if (sweepDeg > 0.01f) {
            ringPaint.color = color
            ringPaint.shader = null
            ringPaint.clearShadowLayer()

            if (sweepDeg >= 359.5f) {
                // Perfect seamless circle
                canvas.drawCircle(cx, cy, radius, ringPaint)
            } else {
                ringBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
                canvas.drawArc(ringBounds, -90f, sweepDeg, false, ringPaint)

                // Breathing Glow Tip Dot at active progress tip when running
                if (pulseAnimator != null && sweepDeg > 2f) {
                    val tipAngleRad = Math.toRadians((-90f + sweepDeg).toDouble())
                    val tipX = (cx + radius * Math.cos(tipAngleRad)).toFloat()
                    val tipY = (cy + radius * Math.sin(tipAngleRad)).toFloat()

                    val tipRadius = strokePx / 2.2f
                    val glowRadius = tipRadius * (1.2f + 0.3f * (pulseAlpha - 0.65f) / 0.35f)

                    tipGlowPaint.color = Color.argb((160 * pulseAlpha).toInt(), Color.red(color), Color.green(color), Color.blue(color))
                    canvas.drawCircle(tipX, tipY, glowRadius, tipGlowPaint)

                    tipGlowPaint.color = blend(color, -1, 0.4f)
                    canvas.drawCircle(tipX, tipY, tipRadius, tipGlowPaint)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility != View.VISIBLE) {
            pulseAnimator?.cancel()
            pulseAnimator = null
        }
    }
}
