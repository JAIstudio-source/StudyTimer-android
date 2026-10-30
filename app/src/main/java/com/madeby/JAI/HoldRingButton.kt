package com.madeby.JAI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.appcompat.widget.AppCompatButton
import kotlin.math.PI
import kotlin.math.min

class HoldRingButton(context: Context) : AppCompatButton(context) {

    init {
        setWillNotDraw(false)
    }

    private var decayAnimator: android.animation.ValueAnimator? = null

    var progress: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field != clamped) {
                field = clamped
                invalidate()
                postInvalidateOnAnimation()
            }
        }

    fun animateProgressToZero(durationMs: Long = 220L) {
        decayAnimator?.cancel()
        if (progress <= 0f) return
        decayAnimator = android.animation.ValueAnimator.ofFloat(progress, 0f).apply {
            duration = durationMs
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
            }
            start()
        }
    }

    var ringColor: Int = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        (background as? Soft3DBubbleDrawable)?.isPressed = pressed
        animate().scaleX(if (pressed) 0.96f else 1.0f)
            .scaleY(if (pressed) 0.96f else 1.0f)
            .setDuration(if (pressed) 90L else 220L)
            .setInterpolator(if (pressed) android.view.animation.DecelerateInterpolator() else android.view.animation.OvershootInterpolator(1.3f))
            .start()
    }

    private val strokePx = 3.5f * resources.displayMetrics.density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fullPath = Path()
    private val drawPath = Path()
    private val pathMeasure = android.graphics.PathMeasure()
    private var lastMeasuredW = 0
    private var lastMeasuredH = 0

    private fun rebuildFullPath(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val inset = strokePx / 2f + 1f
        val left = inset
        val top = inset
        val right = w - inset
        val bottom = h - inset

        // 20dp corner radius matching the outlined button shape
        val cornerRadius = min(20f * resources.displayMetrics.density, min(w, h) / 2f)

        fullPath.reset()
        val rectF = RectF(left, top, right, bottom)
        fullPath.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)

        pathMeasure.setPath(fullPath, false)
        lastMeasuredW = w
        lastMeasuredH = h
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildFullPath(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawProgressRing(canvas)
    }

    private fun drawProgressRing(canvas: Canvas) {
        if (progress <= 0.001f) return
        if (lastMeasuredW != width || lastMeasuredH != height) {
            rebuildFullPath(width, height)
        }
        val len = pathMeasure.length
        if (len <= 0f) return

        // Subtle background track
        trackPaint.color = (ringColor and 0x00FFFFFF) or 0x33000000
        canvas.drawPath(fullPath, trackPaint)

        // Active progress ring
        paint.color = ringColor
        drawPath.reset()
        if (pathMeasure.getSegment(0f, len * progress, drawPath, true)) {
            canvas.drawPath(drawPath, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        decayAnimator?.cancel()
        decayAnimator = null
    }
}
