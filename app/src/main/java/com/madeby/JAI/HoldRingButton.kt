package com.madeby.JAI

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.AppCompatButton
import kotlin.math.min

class HoldRingButton(context: Context) : AppCompatButton(context) {

    init {
        setWillNotDraw(false)
    }

    private var decayAnimator: ValueAnimator? = null

    var progress: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field != clamped) {
                field = clamped
                invalidate()
            }
        }

    fun startHold() {
        decayAnimator?.cancel()
        decayAnimator = null
        progress = 0f
    }

    fun animateProgressToZero(durationMs: Long = 220L) {
        decayAnimator?.cancel()
        if (progress <= 0f) return
        decayAnimator = ValueAnimator.ofFloat(progress, 0f).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
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
            .setInterpolator(if (pressed) DecelerateInterpolator() else OvershootInterpolator(1.3f))
            .start()
    }

    private val strokePx = 4.5f * resources.displayMetrics.density

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
    private val pathMeasure = PathMeasure()
    private var lastMeasuredW = 0
    private var lastMeasuredH = 0
    private var totalPathLen = 0f

    private fun rebuildFullPath(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val inset = strokePx / 2f + 1f
        val left = inset
        val top = inset
        val right = w - inset
        val bottom = h - inset
        val innerW = right - left
        val innerH = bottom - top
        if (innerW <= 0f || innerH <= 0f) return

        val r = min(innerH / 2f, innerW / 2f)
        val cx = (left + right) / 2f

        fullPath.reset()
        // Start top-center, progress clockwise around the capsule
        fullPath.moveTo(cx, top)
        fullPath.lineTo(right - r, top)
        fullPath.arcTo(right - 2f * r, top, right, top + 2f * r, 270f, 90f, false)
        fullPath.lineTo(right, bottom - r)
        fullPath.arcTo(right - 2f * r, bottom - 2f * r, right, bottom, 0f, 90f, false)
        fullPath.lineTo(left + r, bottom)
        fullPath.arcTo(left, bottom - 2f * r, left + 2f * r, bottom, 90f, 90f, false)
        fullPath.lineTo(left, top + r)
        fullPath.arcTo(left, top, left + 2f * r, top + 2f * r, 180f, 90f, false)
        fullPath.close()

        pathMeasure.setPath(fullPath, false)
        totalPathLen = pathMeasure.length
        lastMeasuredW = w
        lastMeasuredH = h
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rebuildFullPath(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (progress <= 0.001f) return
        if (lastMeasuredW != width || lastMeasuredH != height || totalPathLen <= 0f) {
            rebuildFullPath(width, height)
        }
        if (totalPathLen <= 0f) return

        // 1. Subtle perimeter track
        trackPaint.color = (ringColor and 0x00FFFFFF) or 0x33000000
        canvas.drawPath(fullPath, trackPaint)

        // 2. Hardware-accelerated native DashPathEffect progress sweep
        paint.color = ringColor
        val drawLen = (totalPathLen * progress).coerceIn(0f, totalPathLen)
        paint.pathEffect = DashPathEffect(floatArrayOf(drawLen, totalPathLen), 0f)
        canvas.drawPath(fullPath, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        decayAnimator?.cancel()
        decayAnimator = null
    }
}
